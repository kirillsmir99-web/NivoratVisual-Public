package rtx.nv.utils.animations.fx;

public class Decelerate {
    public final Decelerate.Counter counter = new Decelerate.Counter();
    private long ms = 200L;
    private double value = 1.0;
    private Direction direction = Direction.IN;

    public double getValue() {
        double d = this.counter.getElapsed();
        double d2 = this.ms > 0L ? Math.min(d / (double)this.ms, 1.0) : 1.0;
        double d3 = 1.0 - (1.0 - d2) * (1.0 - d2);
        if (this.direction == Direction.OUT) {
            return this.value * (1.0 - d3);
        }
        return this.value * d3;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public boolean isFinished() {
        return this.counter.getElapsed() >= this.ms;
    }

    public void setMs(long ms) {
        this.ms = ms;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public static class Counter {
        private long time = System.currentTimeMillis();
    
        public void reset() {
            this.time = System.currentTimeMillis();
        }
    
        public void setTime(long l) {
            this.time = l;
        }
    
        public long getElapsed() {
            return System.currentTimeMillis() - this.time;
        }
    }
}
