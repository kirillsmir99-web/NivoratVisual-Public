package rtx.nv.api.mods.geckolib.loading.math.value;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToDoubleFunction;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;

public record Variable(String name, AtomicReference<ToDoubleFunction<ControllerState>> value) implements MathValue
{
    public Variable(String string, ToDoubleFunction<ControllerState> toDoubleFunction) {
        this(string, new AtomicReference<ToDoubleFunction<ControllerState>>(toDoubleFunction));
    }

    public Variable(String string, double d) {
        this(string, (ControllerState controllerState) -> d);
    }

    @Override
    public String toString() {
        return "variable(" + this.name + ")";
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public double get(ControllerState controllerState) {
        try {
            return this.value.get().applyAsDouble(controllerState);
        }
        catch (Exception exception) {
            GeckoLibConstants.LOGGER.error("Attempted to use Molang variable for incompatible animatable type ({}). An animation json needs to be fixed", (Object)this.name);
            rtx.nv.NV.LOGGER.error("Failed evaluating Molang variable " + this.name, exception);
            return 0.0;
        }
    }

    public void set(double d) {
        this.value.set(controllerState -> d);
    }

    public void set(ToDoubleFunction<ControllerState> toDoubleFunction) {
        this.value.set(toDoubleFunction);
    }

    @Override
    public Set<Variable> getUsedVariables() {
        return Set.of(this);
    }
}

