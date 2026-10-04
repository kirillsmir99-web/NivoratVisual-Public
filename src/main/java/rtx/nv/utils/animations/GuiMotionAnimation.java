package rtx.nv.utils.animations;

public final class GuiMotionAnimation {
    private static final long DURATION_NS = 500000000L;
    private static final double OPEN_START_SCALE = 1.25;
    private static final double REST_SCALE = 1.0;
    private static final double CLOSE_END_SCALE = 0.75;
    private static final float SCALE_MIN = 0.8f;
    private static final float SCALE_RANGE = 0.2f;
    private static final float MAX_BLUR_RADIUS = 9.0f;
    private static final GuiMotionAnimation.Ease CUBIC_OUT = d -> 1.0 - Math.pow(1.0 - d, 3.0);
    private static final GuiMotionAnimation.Ease CUBIC_IN = d -> Math.pow(d, 3.0);
    private static final GuiMotionAnimation.Ease QUINT_OUT = d -> 1.0 - Math.pow(1.0 - d, 5.0);
    private static final GuiMotionAnimation.Ease BACK_IN = d -> 2.70158 * Math.pow(d, 3.0) - 1.70158 * Math.pow(d, 2.0);
    private long startNs = System.nanoTime() - 500000000L;
    private double alphaFrom;
    private double alphaTo;
    private GuiMotionAnimation.Ease alphaEase = CUBIC_OUT;
    private double scaleFrom = 1.25;
    private double scaleTo = 1.0;
    private GuiMotionAnimation.Ease scaleEase = QUINT_OUT;
    private boolean closing;
    private float frameAlpha;
    private float frameScale = 1.0f;
    private float frameBlurRadius;
    private double frameProgress = 1.0;

    public float scale() {
        return this.frameScale;
    }

    public float alpha() {
        return this.frameAlpha;
    }

    private static float clamp01(float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    public float blurRadius() {
        return this.frameBlurRadius;
    }

    public void snapOpen() {
        this.closing = false;
        this.alphaFrom = 1.0;
        this.alphaTo = 1.0;
        this.scaleFrom = 1.0;
        this.scaleTo = 1.0;
        this.startNs = System.nanoTime() - 500000000L;
        this.updateFrame();
    }

    public void snapClosed() {
        this.closing = false;
        this.alphaFrom = 0.0;
        this.alphaTo = 0.0;
        this.scaleFrom = 0.75;
        this.scaleTo = 0.75;
        this.startNs = System.nanoTime() - 500000000L;
        this.updateFrame();
    }

    public boolean isClosing() {
        return this.closing;
    }

    private double currentScaleRaw() {
        double d = this.progress();
        return this.scaleFrom + (this.scaleTo - this.scaleFrom) * this.scaleEase.apply(d);
    }

    private double currentAlpha() {
        double d = this.progress();
        return this.alphaFrom + (this.alphaTo - this.alphaFrom) * this.alphaEase.apply(d);
    }

    public void resumeOpening() {
        double d = this.currentAlpha();
        double d2 = this.currentScaleRaw();
        this.closing = false;
        this.startNs = System.nanoTime();
        this.alphaFrom = d;
        this.alphaTo = 1.0;
        this.alphaEase = CUBIC_OUT;
        this.scaleFrom = d2;
        this.scaleTo = 1.0;
        this.scaleEase = QUINT_OUT;
        this.updateFrame();
    }

    public void startClosing() {
        if (this.closing) {
            return;
        }
        double d = this.currentAlpha();
        double d2 = this.currentScaleRaw();
        this.closing = true;
        this.startNs = System.nanoTime();
        this.alphaFrom = d;
        this.alphaTo = 0.0;
        this.alphaEase = CUBIC_IN;
        this.scaleFrom = d2;
        this.scaleTo = 0.75;
        this.scaleEase = BACK_IN;
        this.updateFrame();
    }

    public boolean isCloseFinished() {
        return this.closing && this.frameProgress >= 1.0;
    }

    public boolean isAnimating() {
        return this.frameProgress < 1.0;
    }

    public void startOpening() {
        this.closing = false;
        this.startNs = System.nanoTime();
        this.alphaFrom = 0.0;
        this.alphaTo = 1.0;
        this.alphaEase = CUBIC_OUT;
        this.scaleFrom = 1.25;
        this.scaleTo = 1.0;
        this.scaleEase = QUINT_OUT;
        this.updateFrame();
    }

    public float closeProgress() {
        return this.closing ? (float)this.frameProgress : 0.0f;
    }

    public void updateFrame() {
        double d;
        this.frameProgress = d = this.progress();
        double d2 = this.alphaFrom + (this.alphaTo - this.alphaFrom) * this.alphaEase.apply(d);
        double d3 = this.scaleFrom + (this.scaleTo - this.scaleFrom) * this.scaleEase.apply(d);
        this.frameAlpha = GuiMotionAnimation.clamp01((float)d2);
        this.frameScale = 0.8f + (float)d3 * 0.2f;
        this.frameBlurRadius = 9.0f * (1.0f - this.frameAlpha);
    }

    public boolean canInteract() {
        return !this.closing && this.frameAlpha >= 0.9f;
    }

    private double progress() {
        long l = System.nanoTime() - this.startNs;
        if (l >= 500000000L) {
            return 1.0;
        }
        if (l <= 0L) {
            return 0.0;
        }
        return (double)l / 5.0E8;
    }


    public static interface Ease {
        public double apply(double var1);
    }
}

