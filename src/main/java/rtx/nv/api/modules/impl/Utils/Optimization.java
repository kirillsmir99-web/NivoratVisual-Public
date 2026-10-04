package rtx.nv.api.modules.impl.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.utils.optimization.OcclusionCuller;

public final class Optimization
extends Module {
    private static final int[] RENDER_DISTANCE_CAP = new int[]{0, 12, 8};
    private static final double[] VIEW_SCALE = new double[]{1.0, 0.8, 0.6};
    private static final double[] PARTICLE_DIST_SQ = new double[]{2304.0, 1024.0, 484.0};
    private static final double[] BLOCK_ENTITY_DIST_SQ = new double[]{2304.0, 1296.0, 576.0};
    private static Optimization instance;
    private final ModeSetting mode = this.register(new ModeSetting("\u0420\u0435\u0436\u0438\u043c", "\u0410\u0433\u0440\u0435\u0441\u0441\u0438\u0432\u043d\u043e\u0441\u0442\u044c \u043e\u043f\u0442\u0438\u043c\u0438\u0437\u0430\u0446\u0438\u0438.", "\u0421\u0440\u0435\u0434\u043d\u0438\u0439", "\u041d\u0438\u0437\u043a\u0438\u0439", "\u0421\u0440\u0435\u0434\u043d\u0438\u0439", "\u0423\u043b\u044c\u0442\u0440\u0430"));
    private final BooleanSetting entityCulling = this.register(new BooleanSetting("\u041a\u0443\u043b\u043b\u0438\u043d\u0433 \u0441\u0443\u0449\u043d\u043e\u0441\u0442\u0435\u0439", "\u041d\u0435 \u0440\u0435\u043d\u0434\u0435\u0440\u0438\u0442\u044c \u0441\u0443\u0449\u043d\u043e\u0441\u0442\u0435\u0439, \u043f\u043e\u043b\u043d\u043e\u0441\u0442\u044c\u044e \u0441\u043a\u0440\u044b\u0442\u044b\u0445 \u0437\u0430 \u0431\u043b\u043e\u043a\u0430\u043c\u0438.", true));
    private final BooleanSetting limitRenderDistance = this.register(new BooleanSetting("\u041e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u0438\u0435 \u043f\u0440\u043e\u0440\u0438\u0441\u043e\u0432\u043a\u0438", "\u0421\u0440\u0435\u0434\u043d\u0438\u0439 \u2014 \u0434\u043e 12 \u0447\u0430\u043d\u043a\u043e\u0432, \u0423\u043b\u044c\u0442\u0440\u0430 \u2014 \u0434\u043e 8.", true).visibleWhen(() -> this.tier() >= 1));

    public static int capEffectiveRenderDistance(int n) {
        if (!Optimization.active() || !Optimization.instance.limitRenderDistance.getValue()) {
            return n;
        }
        int n2 = RENDER_DISTANCE_CAP[instance.tier()];
        return n2 > 0 ? Math.min(n, n2) : n;
    }

    public Optimization() {
        super("Optimization", "\u041f\u043e\u0434\u043d\u0438\u043c\u0430\u0435\u0442 FPS: \u043a\u0443\u043b\u043b\u0438\u043d\u0433 \u0441\u0443\u0449\u043d\u043e\u0441\u0442\u0435\u0439, \u0447\u0430\u0441\u0442\u0438\u0446 \u0438 \u0442\u0430\u0439\u043b\u043e\u0432, \u0434\u0438\u0441\u0442\u0430\u043d\u0446\u0438\u044f, \u043e\u0431\u043b\u0430\u043a\u0430.", Category.UTILS);
        instance = this;
        this.mode.setChangeListener(this::updateTier);
        this.updateTier();
    }

    public static Optimization getInstance() {
        return ModuleManager.get().get(Optimization.class);
    }

    private static boolean active() {
        return instance != null && instance.isEnabled();
    }

    public static boolean allowBlockEntity(double d) {
        return !Optimization.active() || d <= BLOCK_ENTITY_DIST_SQ[instance.tier()];
    }

    public static boolean allowParticle(double d, double d2, double d3) {
        if (!Optimization.active()) {
            return true;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.gameRenderer == null) {
            return true;
        }
        Camera camera = minecraftClient.gameRenderer.getCamera();
        if (camera == null) {
            return true;
        }
        Vec3d vec3d = camera.getCameraPos();
        double d4 = d - vec3d.x;
        double d5 = d2 - vec3d.y;
        double d6 = d3 - vec3d.z;
        return d4 * d4 + d5 * d5 + d6 * d6 <= PARTICLE_DIST_SQ[instance.tier()];
    }

    public static double scaleEntityViewScale(double d) {
        return Optimization.active() ? d * VIEW_SCALE[instance.tier()] : d;
    }

    public static boolean hideEntityShadows() {
        return Optimization.active() && instance.tier() >= 1;
    }

    public static boolean shouldRenderEntity(boolean bl, Entity entity, double d, double d2, double d3) {
        if (!(bl && Optimization.active() && Optimization.instance.entityCulling.getValue())) {
            return bl;
        }
        return OcclusionCuller.isVisible(entity, d, d2, d3, instance.tier());
    }

    public static boolean shaderTransparency(boolean bl) {
        return Optimization.active() && instance.tier() >= 1 ? false : bl;
    }

    public static boolean cullClouds() {
        return Optimization.active() && instance.tier() == 2;
    }

    private int cachedTier = 1;

    private void updateTier() {
        this.cachedTier = this.mode.is("\u0423\u043b\u044c\u0442\u0440\u0430") ? 2 : (this.mode.is("\u0421\u0440\u0435\u0434\u043d\u0438\u0439") ? 1 : 0);
    }

    private int tier() {
        return this.cachedTier;
    }
}

