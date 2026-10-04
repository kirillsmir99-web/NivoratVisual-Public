package rtx.nv.api.mods.geckolib.object;
import java.util.Objects;
import java.util.function.Function;

public class DeferredCache<I, O> {
    private boolean computed = false;
    private final Function<I, O> mappingFunction;
    private I input;
    private O output = null;

    public DeferredCache(I i, Function<I, O> function) {
        this.input = Objects.requireNonNull(i);
        this.mappingFunction = function;
    }

    public O compute() {
        if (!this.computed) {
            this.output = this.mappingFunction.apply(Objects.requireNonNull(this.input));
            this.input = null;
            this.computed = true;
        }
        return this.getOutput();
    }

    public I getInput() {
        if (this.computed || this.input == null) {
            throw new IllegalStateException("Attempting to access input after output of deferred cache has been calculated!");
        }
        return this.input;
    }

    public O getOutput() {
        if (!this.computed || this.output == null) {
            throw new IllegalStateException("Attempting to access output before it has been calculated!");
        }
        return this.output;
    }
}

