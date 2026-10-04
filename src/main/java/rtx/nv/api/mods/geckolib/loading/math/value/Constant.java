package rtx.nv.api.mods.geckolib.loading.math.value;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;

public record Constant(double value) implements MathValue
{
    @Override
    public String toString() {
        return String.valueOf(this.value);
    }

    @Override
    public double get(ControllerState controllerState) {
        return this.value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }
}

