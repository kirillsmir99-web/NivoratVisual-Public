package rtx.nv.api.mods.geckolib.loading.math.value;

import java.util.Set;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;

public record CompoundValue(MathValue[] values) implements MathValue {
    @Override
    public double get(ControllerState state) {
        double last = 0.0;
        for (MathValue v : values) {
            last = v.get(state);
        }
        return last;
    }

    @Override
    public Set<Variable> getUsedVariables() {
        return MathValue.collectUsedVariables(values);
    }
}