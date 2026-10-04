package rtx.nv.utils.optimization;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Iterator;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

public final class OcclusionCuller {
    private static final int[] BUDGET = new int[]{16, 24, 32};
    private static final long[] HIDDEN_TTL = new long[]{150L, 120L, 90L};
    private static final long[] VISIBLE_TTL = new long[]{450L, 350L, 250L};
    private static final double MIN_DIST_SQ = 64.0;
    private static final double MAX_ENTITY_SIZE = 6.0;
    private static final int MAX_RAY_STEPS = 172;
    private static final Int2ObjectOpenHashMap<Entry> CACHE = new Int2ObjectOpenHashMap<Entry>();
    private static final BlockPos.Mutable POS = new BlockPos.Mutable();
    private static ClientWorld lastLevel;
    private static int checksThisFrame;
    private static long currentFrameTime;
    private static long lastSweep;

    private OcclusionCuller() {
    }

    public static boolean isVisible(Entity entity, double d, double d2, double d3, int n) {
        double d4;
        double d5;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ClientWorld clientWorld = minecraftClient.world;
        if (clientWorld == null || entity == minecraftClient.player || entity.isGlowing()) {
            return true;
        }
        double d6 = entity.getX() - d;
        if (d6 * d6 + (d5 = entity.getY() - d2) * d5 + (d4 = entity.getZ() - d3) * d4 < 64.0) {
            return true;
        }
        Box box = entity.getBoundingBox();
        if (box.getLengthX() > 6.0 || box.getLengthY() > 6.0 || box.getLengthZ() > 6.0) {
            return true;
        }
        int n2 = MathHelper.clamp((int)n, (int)0, (int)2);
        long l = currentFrameTime != 0L ? currentFrameTime : System.currentTimeMillis();
        Entry entry = CACHE.get(entity.getId());
        if (entry != null && l < entry.next) {
            return entry.visible;
        }
        if (checksThisFrame >= BUDGET[n2]) {
            return entry == null || entry.visible;
        }
        ++checksThisFrame;
        boolean bl = OcclusionCuller.raycastVisible(clientWorld, d, d2, d3, box.expand(0.1));
        if (entry == null) {
            entry = new Entry();
            CACHE.put(entity.getId(), entry);
        }
        entry.visible = bl;
        entry.next = l + (bl ? VISIBLE_TTL[n2] : HIDDEN_TTL[n2]) + (long)(entity.getId() * 31 & 0x3F);
        return bl;
    }

    public static void beginFrame() {
        checksThisFrame = 0;
        currentFrameTime = System.currentTimeMillis();
        long l = currentFrameTime;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.world != lastLevel) {
            CACHE.clear();
            lastLevel = minecraftClient.world;
        }
        if (l - lastSweep > 5000L) {
            lastSweep = l;
            Iterator<Entry> iterator = CACHE.values().iterator();
            while (iterator.hasNext()) {
                if (l - iterator.next().next <= 5000L) continue;
                iterator.remove();
            }
        }
    }

    private static boolean sightBlocked(ClientWorld clientWorld, double d, double d2, double d3, double d4, double d5, double d6) {
        double d7;
        double d8;
        double d9;
        double d10 = d4 - d;
        double d11 = d5 - d2;
        double d12 = d6 - d3;
        int n3 = MathHelper.floor((double)d);
        int n4 = MathHelper.floor((double)d2);
        int n5 = MathHelper.floor((double)d3);
        int n6 = MathHelper.floor((double)d4);
        int n7 = MathHelper.floor((double)d5);
        int n8 = MathHelper.floor((double)d6);
        int n2 = d10 > 0.0 ? 1 : (d10 < 0.0 ? -1 : 0);
        int n = d11 > 0.0 ? 1 : (d11 < 0.0 ? -1 : 0);
        int n11 = d12 > 0.0 ? 1 : (d12 < 0.0 ? -1 : 0);
        double d13 = n2 == 0 ? Double.MAX_VALUE : Math.abs(1.0 / d10);
        double d14 = n == 0 ? Double.MAX_VALUE : Math.abs(1.0 / d11);
        d9 = n11 == 0 ? Double.MAX_VALUE : Math.abs(1.0 / d12);
        d8 = n2 == 0 ? Double.MAX_VALUE : (n2 > 0 ? Math.floor(d) + 1.0 - d : d - Math.floor(d)) * d13;
        d7 = n == 0 ? Double.MAX_VALUE : (n > 0 ? Math.floor(d2) + 1.0 - d2 : d2 - Math.floor(d2)) * d14;
        double d18 = n11 == 0 ? Double.MAX_VALUE : (n11 > 0 ? Math.floor(d3) + 1.0 - d3 : d3 - Math.floor(d3)) * d9;
        for (int i = 0; i < 172; ++i) {
            if (Math.min(d8, Math.min(d7, d18)) >= 1.0) {
                return false;
            }
            if (d8 <= d7 && d8 <= d18) {
                n3 += n2;
                d8 += d13;
            } else if (d7 <= d18) {
                n4 += n;
                d7 += d14;
            } else {
                n5 += n11;
                d18 += d9;
            }
            if (n3 == n6 && n4 == n7 && n5 == n8) {
                return false;
            }
            BlockState blockState = clientWorld.getBlockState((BlockPos)POS.set(n3, n4, n5));
            if (blockState.isAir() || !blockState.isOpaque()) continue;
            return true;
        }
        return false;
    }

    private static boolean raycastVisible(ClientWorld clientWorld, double d, double d2, double d3, Box box) {
        if (!OcclusionCuller.sightBlocked(clientWorld, d, d2, d3, (box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5)) {
            return true;
        }
        for (int i = 0; i < 8; ++i) {
            double d4;
            double d5 = (i & 1) == 0 ? box.minX : box.maxX;
            double d6 = (i & 2) == 0 ? box.minY : box.maxY;
            double d7 = d4 = (i & 4) == 0 ? box.minZ : box.maxZ;
            if (OcclusionCuller.sightBlocked(clientWorld, d, d2, d3, d5, d6, d4)) continue;
            return true;
        }
        return false;
    }

    public static class Entry {
        public boolean visible;
        public long next;
    }
}

