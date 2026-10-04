package rtx.nv.utils.inventory;
import java.util.ArrayDeque;
import java.util.Deque;

public final class InventorySequence {
    private final Deque<Step> steps = new ArrayDeque<Step>();
    private long readyAt;
    private boolean started;

    public void start() {
        this.started = true;
        this.readyAt = System.currentTimeMillis();
    }

    public void cancel() {
        this.steps.clear();
        this.started = false;
    }

    public boolean isRunning() {
        return this.started && !this.steps.isEmpty();
    }

    public InventorySequence thenAfter(long l, Runnable runnable) {
        if (runnable != null) {
            this.steps.add(new Step(runnable, Math.max(0L, l)));
        }
        return this;
    }

    public void tick() {
        if (!this.started) {
            return;
        }
        long l = System.currentTimeMillis();
        while (!this.steps.isEmpty() && l >= this.readyAt) {
            Step step = this.steps.poll();
            step.action.run();
            Step step2 = this.steps.peek();
            if (step2 == null) {
                this.started = false;
                continue;
            }
            this.readyAt = l + step2.delayMs;
        }
    }

    public InventorySequence then(Runnable runnable) {
        return this.thenAfter(0L, runnable);
    }

    public static record Step(Runnable action, long delayMs) {}
}

