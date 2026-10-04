package rtx.nv.api.mods.geckolib.loading.math.value;

import java.util.Set;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;

public record Ternary(MathValue condition, MathValue trueValue, MathValue falseValue) implements MathValue {
    @Override
    public double get(ControllerState state) {
  return condition.get(state) != 0.0 ? trueValue.get(state) : falseValue.get(state);
    }

    @Override
    public Set<Variable> getUsedVariables() {
        return MathValue.collectUsedVariables(condition, trueValue, falseValue);
    }
}