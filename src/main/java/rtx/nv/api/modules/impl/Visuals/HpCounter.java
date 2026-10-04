package rtx.nv.api.modules.impl.Visuals;
import rtx.nv.api.events.EventHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import rtx.nv.api.drags.Position;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.render.HudRenderEvent;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.render2d.Render2D;

public final class HpCounter
extends Module {
    private static final long LIFETIME_MS = 900L;
    private static final int DAMAGE_COLOR = -46531;
    private static final int HEAL_COLOR = -12659877;
    private static final String FONT = "montserrat-semibold";
    private static final Vec3d[] OFFSETS = new Vec3d[]{new Vec3d(-0.36, -0.08, 0.12), new Vec3d(-0.12, 0.1, -0.3), new Vec3d(0.22, -0.14, -0.22), new Vec3d(0.4, 0.04, 0.09), new Vec3d(0.11, 0.16, 0.33), new Vec3d(-0.28, -0.18, 0.27), new Vec3d(0.34, -0.03, -0.12), new Vec3d(-0.05, 0.22, 0.05)};
    private final Map<Integer, Float> healthByEntity = new HashMap<Integer, Float>();
    private final List<HpCounter.FloatingNumber> numbers = new ArrayList<HpCounter.FloatingNumber>();
    private final List<ScreenNumber> screenNumbers = new ArrayList<ScreenNumber>();
    private final Vector4f projectionScratch = new Vector4f();
    private int nextOffset;

    public HpCounter() {
        super("HP Counter", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u0443\u0440\u043e\u043d \u0438 \u043b\u0435\u0447\u0435\u043d\u0438\u0435 \u0432\u0438\u0434\u0438\u043c\u044b\u0445 \u0438\u0433\u0440\u043e\u043a\u043e\u0432.", Category.VISUALS);
    }

    private static String format(float f) {
        if (Math.abs(f - (float)Math.round(f)) < 0.05f) {
            return Integer.toString(Math.round(f));
        }
        return String.format(Locale.ROOT, "%.1f", Float.valueOf(f)).replace('.', ',');
    }

    private static float smooth(float f) {
        return f * f * (3.0f - 2.0f * f);
    }

    @EventHandler
    private void onHud(HudRenderEvent hudRenderEvent) {
        if (this.screenNumbers.isEmpty()) {
            return;
        }
        DrawContext drawContext = hudRenderEvent.getGraphics();
        Render2D.beginFrame(drawContext);
        for (ScreenNumber screenNumber : this.screenNumbers) {
            HpCounter.FloatingNumber floatingNumber = screenNumber.number;
            float f = HpCounter.smooth(MathHelper.clamp((float)(screenNumber.progress / 0.14f), (float)0.0f, (float)1.0f)) * HpCounter.smooth(MathHelper.clamp((float)((1.0f - screenNumber.progress) / 0.35f), (float)0.0f, (float)1.0f));
            float f2 = 10.0f * screenNumber.distanceScale * (0.88f + 0.12f * f);
            String string = (floatingNumber.heal ? "+" : "-") + HpCounter.format(floatingNumber.amount);
            float f3 = Render2D.msdfWidth(FONT, string, f2);
            float f4 = screenNumber.x - f3 * 0.5f;
            int n = ColorUtil.multAlpha(floatingNumber.heal ? -12659877 : -46531, f);
            Render2D.msdfText(FONT, string, f4 + 0.7f, screenNumber.y + 0.7f, f2, ColorUtil.multAlpha(-16777216, f * 0.55f));
            Render2D.msdfText(FONT, string, f4, screenNumber.y, f2, n);
        }
        Render2D.flush();
    }

    @Override
    protected void onDisable() {
        this.healthByEntity.clear();
        this.numbers.clear();
        this.screenNumbers.clear();
        this.nextOffset = 0;
    }

    @EventHandler
    private void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPre()) {
            return;
        }
        if (this.mc.player == null || this.mc.world == null) {
            this.healthByEntity.clear();
            this.numbers.clear();
            return;
        }
        long l = System.currentTimeMillis();
        for (PlayerEntity playerEntity : this.mc.world.getPlayers()) {
            if (playerEntity == this.mc.player) continue;
            float f = playerEntity.getHealth();
            Float f2 = this.healthByEntity.put(playerEntity.getId(), Float.valueOf(f));
            if (f2 == null || Math.abs(f - f2.floatValue()) < 0.01f || !this.isVisible(playerEntity)) continue;
            float f3 = f - f2.floatValue();
            Vec3d vec3d = OFFSETS[this.nextOffset++ % OFFSETS.length];
            Vec3d vec3d2 = playerEntity.getEntityPos().add(vec3d.x, (double)playerEntity.getHeight() * 0.55 + vec3d.y, vec3d.z);
            this.numbers.add(new HpCounter.FloatingNumber(vec3d2, Math.abs(f3), f3 > 0.0f, l));
        }
        this.healthByEntity.keySet().removeIf(n -> this.mc.world.getEntityById(n.intValue()) == null);
        this.numbers.removeIf(n -> l - n.createdAtMs >= 900L);
    }

    private boolean isVisible(PlayerEntity playerEntity) {
        return playerEntity.isAlive() && !playerEntity.isInvisible() && this.mc.player.canSee((Entity)playerEntity);
    }

    private Vector4f project(Matrix4f matrix4f, Matrix4f matrix4f2, Vec3d vec3d, double d, double d2, double d3) {
        Vector4f vector4f = this.projectionScratch.set((float)(d - vec3d.x), (float)(d2 - vec3d.y), (float)(d3 - vec3d.z), 1.0f);
        matrix4f.transform(vector4f);
        matrix4f2.transform(vector4f);
        return vector4f.w > 1.0E-4f ? vector4f : null;
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent worldRenderEvent) {
        this.screenNumbers.clear();
        if (this.mc.player == null || this.mc.world == null || this.numbers.isEmpty()) {
            return;
        }
        long l = System.currentTimeMillis();
        Vec3d vec3d = worldRenderEvent.getCamera() == null ? this.mc.gameRenderer.getCamera().getCameraPos() : worldRenderEvent.getCamera().getCameraPos();
        Matrix4f matrix4f = worldRenderEvent.getPositionMatrix();
        Matrix4f matrix4f2 = worldRenderEvent.getProjectionMatrix();
        float f = worldRenderEvent.getPartialTicks();
        float f2 = Position.screenWidth();
        float f3 = Position.screenHeight();
        for (HpCounter.FloatingNumber floatingNumber : this.numbers) {
            float f4 = MathHelper.clamp((float)((float)(l - floatingNumber.createdAtMs) / 900.0f), (float)0.0f, (float)1.0f);
            Vec3d vec3d2 = floatingNumber.position.add(0.0, (double)f4 * 0.65, 0.0);
            Vector4f vector4f = this.project(matrix4f, matrix4f2, vec3d, vec3d2.x, vec3d2.y, vec3d2.z);
            if (vector4f == null) continue;
            float f5 = (vector4f.x / vector4f.w * 0.5f + 0.5f) * f2;
            float f6 = (1.0f - (vector4f.y / vector4f.w * 0.5f + 0.5f)) * f3;
            if (!Float.isFinite(f5) || !Float.isFinite(f6)) continue;
            float f7 = (float)Math.sqrt(vec3d.squaredDistanceTo(floatingNumber.position));
            this.screenNumbers.add(new ScreenNumber(floatingNumber, f5, f6, f4, MathHelper.clamp((float)(7.0f / f7), (float)0.38f, (float)1.05f)));
        }
    }

    public static record FloatingNumber(Vec3d position, float amount, boolean heal, long createdAtMs) {
    }

    public static record ScreenNumber(FloatingNumber number, float x, float y, float progress, float distanceScale) {
    }
}

