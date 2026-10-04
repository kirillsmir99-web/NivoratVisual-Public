package rtx.nv.api.modules.impl.Utils;
import rtx.nv.api.events.EventHandler;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.input.HotBarScrollEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.BindSetting;
import rtx.nv.api.modules.settings.impl.BindSetting.Type;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.utils.animations.fx.Decelerate;
import rtx.nv.utils.animations.fx.Direction;

public final class CameraSettings
extends Module {
    private static final long ZOOM_MS = 180L;
    private static final long F5_MS = 220L;
    private static CameraSettings instance;
    private final SeparatorSetting zoomSeparator = this.register(new SeparatorSetting("\u041f\u0440\u0438\u0431\u043b\u0438\u0436\u0435\u043d\u0438\u0435"));
    private final BooleanSetting zoomEnabled = this.register(new BooleanSetting("\u041f\u0440\u0438\u0431\u043b\u0438\u0436\u0435\u043d\u0438\u0435", "\u041f\u043b\u0430\u0432\u043d\u044b\u0439 \u0437\u0443\u043c \u043a\u0430\u043c\u0435\u0440\u044b \u043f\u043e \u043a\u043b\u0430\u0432\u0438\u0448\u0435. \u0412\u043d\u0443\u0442\u0440\u0438 \u0437\u0443\u043c\u0430 \u043a\u0440\u0443\u0442\u0438\u0442\u0435 \u043a\u043e\u043b\u0435\u0441\u043e, \u0447\u0442\u043e\u0431\u044b \u043f\u0440\u0438\u0431\u043b\u0438\u0436\u0430\u0442\u044c/\u043e\u0442\u0434\u0430\u043b\u044f\u0442\u044c.", true));
    private final BindSetting zoomKey = this.register(new BindSetting("\u041a\u043b\u0430\u0432\u0438\u0448\u0430 \u043f\u0440\u0438\u0431\u043b\u0438\u0436\u0435\u043d\u0438\u044f", "\u0417\u0430\u0436\u043c\u0438\u0442\u0435, \u0447\u0442\u043e\u0431\u044b \u043f\u0440\u0438\u0431\u043b\u0438\u0437\u0438\u0442\u044c \u043a\u0430\u043c\u0435\u0440\u0443.").setType(BindSetting.Type.HOLD).visibleWhen(this.zoomEnabled::getValue));
    private final NumberSetting startZoom = this.register(new NumberSetting("\u0421\u0442\u0430\u0440\u0442\u043e\u0432\u044b\u0439 \u0437\u0443\u043c", "\u041d\u0430\u0441\u043a\u043e\u043b\u044c\u043a\u043e \u043f\u0440\u0438\u0431\u043b\u0438\u0436\u0430\u0435\u0442 \u043f\u0440\u0438 \u043d\u0430\u0436\u0430\u0442\u0438\u0438 (\u043c\u0435\u043d\u044c\u0448\u0435 \u2014 \u0441\u0438\u043b\u044c\u043d\u0435\u0435).", 0.55, 0.2, 0.9, 0.01).visibleWhen(this.zoomEnabled::getValue));
    private final SeparatorSetting f5Separator = this.register(new SeparatorSetting("\u041f\u043b\u0430\u0432\u043d\u044b\u0439 F5"));
    private final BooleanSetting smoothF5 = this.register(new BooleanSetting("\u041f\u043b\u0430\u0432\u043d\u044b\u0439 F5", "\u041f\u043b\u0430\u0432\u043d\u043e \u043f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0430\u0435\u0442 \u043a\u0430\u043c\u0435\u0440\u044b (F5) \u0447\u0435\u0440\u0435\u0437 \u0437\u0443\u043c \u0434\u0438\u0441\u0442\u0430\u043d\u0446\u0438\u0438.", false));
    private static final double SCROLL_STEP = 0.08;
    private static final float ZOOM_MIN = 0.2f;
    private static final float ZOOM_MAX = 1.0f;
    private final Decelerate zoomAnim = new Decelerate();
    private float zoomFrom = 1.0f;
    private float zoomTo = 1.0f;
    private float zoomTarget = 1.0f;
    private boolean zoomActive = false;
    private final Decelerate f5Anim = new Decelerate();
    private float f5From = 1.0f;
    private float f5To = 1.0f;
    private Perspective lastCameraType = Perspective.FIRST_PERSON;

    public CameraSettings() {
        super("Camera Settings", "\u041f\u043b\u0430\u0432\u043d\u044b\u0439 \u0437\u0443\u043c \u043a\u0430\u043c\u0435\u0440\u044b \u0438 \u043f\u043b\u0430\u0432\u043d\u043e\u0435 \u043f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 F5.", Category.UTILS);
        instance = this;
        try {
            this.zoomAnim.setMs(180L);
            this.zoomAnim.setValue(1.0);
            this.f5Anim.setMs(500L);
            this.f5Anim.setValue(1.0);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private float liveFov() {
        return MathHelper.lerp((float)CameraSettings.easedProgress(this.zoomAnim), (float)this.zoomFrom, (float)this.zoomTo);
    }

    private void updateZoom() {
        boolean bl;
        if (this.mc.player == null || this.mc.world == null || this.mc.currentScreen != null || !this.zoomEnabled.getValue()) {
            if (this.zoomActive || this.zoomTo != 1.0f) {
                this.startZoomAnim(this.liveFov(), 1.0f);
            }
            this.zoomActive = false;
            return;
        }
        boolean bl2 = bl = this.zoomKey.isBound() && this.zoomKey.getValue().isDown(this.mc.getWindow().getHandle());
        if (bl && !this.zoomActive) {
            this.zoomTarget = MathHelper.clamp((float)this.startZoom.getFloat(), (float)0.2f, (float)1.0f);
            this.startZoomAnim(this.liveFov(), this.zoomTarget);
        } else if (!bl && this.zoomActive) {
            this.startZoomAnim(this.liveFov(), 1.0f);
        }
        this.zoomActive = bl;
    }

    @Override
    protected void onDisable() {
        this.zoomTarget = 1.0f;
        this.zoomTo = 1.0f;
        this.zoomFrom = 1.0f;
        this.f5To = 1.0f;
        this.f5From = 1.0f;
        this.zoomActive = false;
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPre()) {
            return;
        }
        this.updateZoom();
        this.updateSmoothF5();
    }

    @EventHandler
    public void onScroll(HotBarScrollEvent hotBarScrollEvent) {
        if (!(this.isEnabled() && this.zoomEnabled.getValue() && this.zoomActive)) {
            return;
        }
        this.zoomTarget = MathHelper.clamp((float)((float)((double)this.zoomTarget - hotBarScrollEvent.getVertical() * 0.08)), (float)0.2f, (float)1.0f);
        this.startZoomAnim(this.liveFov(), this.zoomTarget);
        hotBarScrollEvent.setCancelled(true);
    }

    private void updateSmoothF5() {
        if (this.mc.options == null || !this.smoothF5.getValue()) {
            this.f5To = 1.0f;
            this.f5From = 1.0f;
            if (this.mc.options != null) {
                this.lastCameraType = this.mc.options.getPerspective();
            }
            return;
        }
        Perspective perspective = this.mc.options.getPerspective();
        if (perspective != this.lastCameraType) {
            if (perspective == Perspective.FIRST_PERSON) {
                this.f5To = 1.0f;
                this.f5From = 1.0f;
            } else {
                this.startF5Anim(0.2f, 1.0f);
            }
            this.lastCameraType = perspective;
        }
    }

    public static float getFovScale() {
        CameraSettings cameraSettings = instance;
        if (cameraSettings == null || !cameraSettings.isEnabled() || !cameraSettings.zoomEnabled.getValue()) {
            return 1.0f;
        }
        return cameraSettings.liveFov();
    }

    public static float getCameraDistanceScale() {
        CameraSettings cameraSettings = instance;
        if (cameraSettings == null || !cameraSettings.isEnabled() || !cameraSettings.smoothF5.getValue()) {
            return 1.0f;
        }
        return cameraSettings.liveDistance();
    }

    private static float easedProgress(Decelerate decelerate) {
        try {
            return (float)Math.min(Math.max(decelerate.getValue(), 0.0), 1.0);
        }
        catch (Throwable throwable) {
            return 1.0f;
        }
    }

    private void startZoomAnim(float f, float f2) {
        this.zoomFrom = f;
        this.zoomTo = f2;
        try {
            this.zoomAnim.setDirection(Direction.IN);
            this.zoomAnim.setValue(1.0);
            this.zoomAnim.counter.reset();
        }
        catch (Throwable throwable) {
            this.zoomFrom = f2;
        }
    }

    private void startF5Anim(float f, float f2) {
        this.f5From = f;
        this.f5To = f2;
        try {
            this.f5Anim.setDirection(Direction.IN);
            this.f5Anim.setValue(1.0);
            this.f5Anim.counter.reset();
        }
        catch (Throwable throwable) {
            this.f5From = f2;
        }
    }

    private float liveDistance() {
        return MathHelper.lerp((float)CameraSettings.easedProgress(this.f5Anim), (float)this.f5From, (float)this.f5To);
    }
}

