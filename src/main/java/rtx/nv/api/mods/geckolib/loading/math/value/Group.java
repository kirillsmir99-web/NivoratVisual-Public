package rtx.nv.api.mods.geckolib.loading.math.value;

import java.util.Set;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;

public record Group(MathValue value) implements MathValue {
    @Override
    public double get(ControllerState state) {
  return value.get(state);
    }

    @Override
    public Set<Variable> getUsedVariables() {
  return value.getUsedVariables();
    }
}