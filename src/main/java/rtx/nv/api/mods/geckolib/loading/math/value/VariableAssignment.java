package rtx.nv.api.mods.geckolib.loading.math.value;

import java.util.Set;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;

public record VariableAssignment(Variable variable, MathValue value) implements MathValue {
    @Override
    public double get(ControllerState state) {
        double v = value.get(state);
        if (state != null) {
            state.setVariable(variable, v);
        }
        return v;
    }

    @Override
    public Set<Variable> getUsedVariables() {
        return MathValue.collectUsedVariables(variable, value);
    }
}