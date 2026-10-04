package rtx.nv.utils.animations;
import rtx.nv.utils.animations.Easing;
import rtx.nv.utils.animations.Easings;

public class SmoothAnimation {
    private long start;
    private double duration;
    private double fromValue;
    private double toValue;
    private double value;
    private double prevValue;
    private Easing easing = Easings.EXPO_OUT;
    private boolean finished = false;

    public SmoothAnimation run(double d, double d2, Easing easing, boolean bl) {
        if (this.check(bl, d)) {
            return this;
        }
        this.easing = easing;
        this.start = System.currentTimeMillis();
        this.duration = d2 * 1000.0;
        this.fromValue = this.value;
        this.toValue = d;
        this.finished = this.fromValue == this.toValue;
        return this;
    }

    public SmoothAnimation run(double d, double d2, boolean bl) {
        return this.run(d, d2, Easings.EXPO_OUT, bl);
    }

    public SmoothAnimation run(double d, double d2, Easing easing) {
        return this.run(d, d2, easing, false);
    }

    public SmoothAnimation run(double d, double d2) {
        return this.run(d, d2, Easings.EXPO_OUT, false);
    }

    public boolean update() {
        this.prevValue = this.value;
        boolean bl = this.isAlive();
        if ((double)(System.currentTimeMillis() - this.start) > this.duration / 1.5) {
            boolean bl2 = this.finished = this.fromValue == this.toValue;
        }
        if (bl) {
            double d = Math.min(1.0, Math.max(0.0, this.calculatePart()));
            this.value = this.interpolate(this.fromValue, this.toValue, this.easing.ease(d));
        } else {
            this.start = 0L;
            this.value = this.toValue;
        }
        return bl;
    }

    public double getValue() {
        return this.value;
    }

    public float get() {
        return (float)this.value;
    }

    public void set(double d) {
        this.run(d, 1.0E-4);
        this.update();
        this.value = d;
    }

    public boolean isAlive() {
        return !this.isFinished();
    }

    public void setValue(double d) {
        this.value = d;
    }

    public boolean check(boolean bl, double d) {
        return bl && this.isAlive() && (d == this.fromValue || d == this.toValue || d == this.value);
    }

    public double interpolate(double d, double d2, double d3) {
        return d + (d2 - d) * d3;
    }

    public double getDuration() {
        return this.duration;
    }

    public boolean isFinished() {
        return this.calculatePart() >= 1.0;
    }

    public Easing getEasing() {
        return this.easing;
    }

    public double getToValue() {
        return this.toValue;
    }

    public float getPrev() {
        return (float)this.prevValue;
    }

    public long getStart() {
        return this.start;
    }

    public double calculatePart() {
        if (this.duration <= 0.0) {
            return 1.0;
        }
        return (double)(System.currentTimeMillis() - this.start) / this.duration;
    }

    public double getFromValue() {
        return this.fromValue;
    }

    public double getPrevValue() {
        return this.prevValue;
    }
}

