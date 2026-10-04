package rtx.nv.api.mods.geckolib.loading.math.function.round;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class HermiteBlendFunction
extends MathFunction {
    private final MathValue valueA;

    public HermiteBlendFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.valueA = mathValueArray[0];
    }

    @Override
    public String getName() {
        return "math.hermite_blend";
    }

    @Override
    public double compute(ControllerState controllerState) {
        double d = this.valueA.get(controllerState);
        return 3.0 * d * d - 2.0 * d * d * d;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[]{this.valueA};
    }

    @Override
    public int getMinArgs() {
        return 1;
    }
}

