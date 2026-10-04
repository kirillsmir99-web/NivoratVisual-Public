package rtx.nv.api.mods.geckolib.loading.math;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.value.Variable;

public interface MathValue
extends ToDoubleFunction<ControllerState> {
    public double get(ControllerState var1);

    default public double applyAsDouble(ControllerState controllerState) {
        return this.get(controllerState);
    }

    default public Set<Variable> getUsedVariables() {
        return Set.of();
    }

    public static Set<Variable> collectUsedVariables(MathValue ... mathValueArray) {
        if (mathValueArray.length == 0) {
            return Set.of();
        }
        if (mathValueArray.length == 1) {
            return mathValueArray[0].getUsedVariables();
        }
        ReferenceOpenHashSet referenceOpenHashSet = new ReferenceOpenHashSet();
        for (MathValue mathValue : mathValueArray) {
            referenceOpenHashSet.addAll(mathValue.getUsedVariables());
        }
        return referenceOpenHashSet;
    }

    default public boolean isMutable() {
        return true;
    }
}

