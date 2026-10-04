package rtx.nv.api.mods.geckolib.loading.math.function.generic;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class ASinFunction
extends MathFunction {
    private final MathValue value;

    public ASinFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.value = mathValueArray[0];
    }

    @Override
    public String getName() {
        return "math.asin";
    }

    @Override
    public double compute(ControllerState controllerState) {
        return Math.asin(this.value.get(controllerState) * 0.01745329238474369);
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

