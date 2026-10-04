package rtx.nv.api.mods.geckolib.loading.math.function.generic;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class AbsFunction
extends MathFunction {
    private final MathValue value;

    public AbsFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.value = mathValueArray[0];
    }

    @Override
    public String getName() {
        return "math.abs";
    }

    @Override
    public double compute(ControllerState controllerState) {
        return Math.abs(this.value.get(controllerState));
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[]{this.value};
    }

    @Override
    public int getMinArgs() {
        return 1;
    }
}

