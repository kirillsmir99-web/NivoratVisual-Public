package rtx.nv.api.mods.geckolib.loading.math.function.misc;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;
import rtx.nv.api.mods.geckolib.loading.math.value.Constant;

public final class PiFunction
extends MathFunction {
    public PiFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
    }

    @Override
    public String getName() {
        return "math.pi";
    }

    @Override
    public double compute(ControllerState controllerState) {
        return Math.PI;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[]{new Constant(Math.PI)};
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public boolean isMutable(MathValue ... mathValueArray) {
        return false;
    }
}

