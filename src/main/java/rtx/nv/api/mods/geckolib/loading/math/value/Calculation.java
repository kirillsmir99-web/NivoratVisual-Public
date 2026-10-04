package rtx.nv.api.mods.geckolib.loading.math.value;

import java.util.Set;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.Operator;

public record Calculation(Operator operator, MathValue a, MathValue b) implements MathValue {
    @Override
    public double get(ControllerState state) {
  return operator.apply(a.get(state), b.get(state));
    }

    @Override
    public Set<Variable> getUsedVariables() {
        return MathValue.collectUsedVariables(a, b);
    }
}