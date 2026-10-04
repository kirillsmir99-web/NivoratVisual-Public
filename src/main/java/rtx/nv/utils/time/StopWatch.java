package rtx.nv.utils.time;

public class StopWatch {
    private long startTime;

    public StopWatch() {
        this.reset();
    }

    public void reset() {
        this.startTime = System.currentTimeMillis();
    }

    public long getStartTime() {
        return this.startTime;
    }

    public boolean finished(double d) {
        return (double)System.currentTimeMillis() - d >= (double)this.startTime;
    }

    public boolean every(double d) {
        boolean bl = this.finished(d);
        if (bl) {
            this.reset();
        }
        return bl;
    }

    public StopWatch setMs(long l) {
        this.startTime = System.currentTimeMillis() - l;
        return this;
    }

    public long elapsedTime() {
        return System.currentTimeMillis() - this.startTime;
    }
}

