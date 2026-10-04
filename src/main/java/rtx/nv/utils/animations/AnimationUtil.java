package rtx.nv.utils.animations;

public final class AnimationUtil {
    private static final float EPSILON = 1.0E-4f;
    private static final float FRAME_NS = 1.6666667E7f;
    public static final Curve NONE = x -> x;
    private long lastUpdateNs;
    private float anim;
    private float to;
    private float speed;
    private AnimationUtil.Curve easing;

    public static void init() {
    }

    public AnimationUtil() {
        this(0.0f, 0.0f, 0.1f, NONE);
    }

    public AnimationUtil(float f, float f2, float f3) {
        this(f, f2, f3, NONE);
    }

    public AnimationUtil(float f, float f2, float f3, AnimationUtil.Curve curve) {
        this.anim = AnimationUtil.sanitize(f);
        this.to = AnimationUtil.sanitize(f2);
        this.speed = Math.max(0.0f, AnimationUtil.sanitize(f3));
        this.easing = curve == null ? NONE : curve;
        this.lastUpdateNs = System.nanoTime();
    }

    public void reset() {
        this.lastUpdateNs = System.nanoTime();
    }

    private void update() {
        long l = System.nanoTime();
        float f = Math.min((float)(l - this.lastUpdateNs) / 1.6666667E7f, 12.0f);
        this.lastUpdateNs = l;
        if (f <= 0.0f) {
            return;
        }
        float f2 = this.to - this.anim;
        if (Math.abs(f2) < 1.0E-4f) {
            this.anim = this.to;
            return;
        }
        float f3 = 1.0f - (float)Math.exp(-this.speed * f);
        if (this.easing != null && this.easing != NONE) {
            f3 = this.easing.apply(Math.min(1.0f, f3));
        }
        this.anim += f2 * f3;
        if (Math.abs(this.to - this.anim) < 1.0E-4f) {
            this.anim = this.to;
        }
    }

    private static float sanitize(float f) {
        if (Float.isNaN(f) || Float.isInfinite(f)) {
            return 0.0f;
        }
        return f;
    }

    public float getAnim() {
        this.update();
        return this.anim;
    }

    public void setAnim(float f) {
        float f2 = AnimationUtil.sanitize(f);
        this.anim = f2;
        this.to = f2;
        this.lastUpdateNs = System.nanoTime();
    }

    public void setTo(float f) {
        float f2 = AnimationUtil.sanitize(f);
        if (Float.compare(this.to, f2) == 0) {
            return;
        }
        this.update();
        this.to = f2;
    }

    public void setSpeed(float f) {
        this.speed = Math.max(0.0f, AnimationUtil.sanitize(f));
    }

    public void setEasing(AnimationUtil.Curve curve) {
        this.easing = curve == null ? NONE : curve;
    }

    public void setToAsBoolean(boolean bl) {
        this.setTo(bl ? 1.0f : 0.0f);
    }

    public float getAngleAnim() {
        this.update();
        float f = this.anim % 360.0f;
        if (f >= 180.0f) {
            f -= 360.0f;
        } else if (f < -180.0f) {
            f += 360.0f;
        }
        return f;
    }

    public float getTo() {
        return this.to;
    }

    @FunctionalInterface
    public static interface Curve {
        public float apply(float var1);
    }
}
