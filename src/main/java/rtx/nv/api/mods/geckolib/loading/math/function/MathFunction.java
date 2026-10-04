package rtx.nv.api.mods.geckolib.loading.math.function;
import java.util.Set;
import java.util.StringJoiner;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.value.Variable;

public abstract class MathFunction
implements MathValue {
    private final boolean isMutable;
    private final Set<Variable> usedVariables;
    private double cachedValue = Double.MIN_VALUE;

    protected MathFunction(MathValue ... mathValueArray) {
        this.validate(mathValueArray);
        this.isMutable = this.isMutable(mathValueArray);
        this.usedVariables = MathValue.collectUsedVariables(mathValueArray);
    }

    public String toString() {
        MathValue[] mathValueArray = this.getArgs();
        StringJoiner stringJoiner = new StringJoiner(", ", "(", ")");
        for (MathValue mathValue : mathValueArray) {
            stringJoiner.add(mathValue.toString());
        }
        return this.getName() + String.valueOf(stringJoiner);
    }

    public abstract String getName();

    @Override
    public final double get(ControllerState controllerState) {
        if (this.isMutable) {
            return this.compute(controllerState);
        }
        if (this.cachedValue == Double.MIN_VALUE) {
            this.cachedValue = this.compute(controllerState);
        }
        return this.cachedValue;
    }

    public void validate(MathValue ... mathValueArray) throws IllegalArgumentException {
        int n = this.getMinArgs();
        if (mathValueArray.length < n) {
            throw new IllegalArgumentException(String.format("Function '%s' at least %s arguments. Only %s given!", this.getName(), n, mathValueArray.length));
        }
    }

    public abstract double compute(ControllerState var1);

    public abstract MathValue[] getArgs();

    @Override
    public Set<Variable> getUsedVariables() {
        return this.usedVariables;
    }

    public abstract int getMinArgs();

    @Override
    public final boolean isMutable() {
        return this.isMutable;
    }

    public boolean isMutable(MathValue ... mathValueArray) {
        for (MathValue mathValue : mathValueArray) {
            if (!mathValue.isMutable()) continue;
            return true;
        }
        return false;
    }


    public static interface Factory<T extends MathFunction> {
        public T create(MathValue ... var1);
    }
}

