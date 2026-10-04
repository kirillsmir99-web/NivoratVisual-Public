package rtx.nv.utils.animations;

import rtx.nv.utils.animations.AnimationCalculation;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.time.TimerUtil;

public class Animation implements AnimationCalculation {
    public final TimerUtil counter = new TimerUtil();
    protected int ms;
    protected double value;
    protected Direction direction = Direction.FORWARDS;

    public void reset() {
        this.counter.resetCounter();
    }

    public void update() {
    }

    public boolean isDone() {
        return this.counter.isReached(this.ms);
    }

    public Animation setValue(double d) {
        this.value = d;
        return this;
    }

    private double getProgress() {
        if (this.ms <= 0) {
            return 1.0;
        }
        return Math.min(1.0, Math.max(0.0, (double)this.counter.getTime() / (double)this.ms));
    }

    protected double endValue() {
        return this.isDone() ? this.value : this.calculation(this.getProgress()) * this.value;
    }

    public Double getOutput() {
        double d = (1.0 - this.calculation(this.getProgress())) * this.value;
        return this.direction == Direction.FORWARDS ? this.endValue() : (this.isDone() ? 0.0 : d);
    }

    public boolean isFinished(Direction direction) {
        return this.direction == direction && this.isDone();
    }

    public Animation setMs(int n) {
        this.ms = n;
        return this;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public boolean isDirection(Direction direction) {
        return this.direction == direction;
    }

    public void setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            this.adjustTimer();
        }
    }

    private void adjustTimer() {
        this.counter.setTime(System.currentTimeMillis() - ((long)this.ms - Math.min((long)this.ms, this.counter.getTime())));
    }
}
