package rtx.nv.api.mods.geckolib.loading.math.value;
import java.util.Set;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.value.Constant;
import rtx.nv.api.mods.geckolib.loading.math.value.Variable;

public record Negative(MathValue value) implements MathValue
{
    @Override
    public String toString() {
        if (this.value instanceof Constant) {
            return "-" + String.valueOf(this.value);
        }
        return "-(" + String.valueOf(this.value) + ")";
    }

    @Override
    public double get(ControllerState controllerState) {
        return -this.value.get(controllerState);
    }

    @Override
    public Set<Variable> getUsedVariables() {
        return this.value.getUsedVariables();
    }

    @Override
    public boolean isMutable() {
        return this.value.isMutable();
    }
}

