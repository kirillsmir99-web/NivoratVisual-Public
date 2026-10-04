package rtx.nv.api.mods.geckolib.loading.math.function.round;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class CeilFunction
extends MathFunction {
    private final MathValue value;

    public CeilFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.value = mathValueArray[0];
    }

    @Override
    public String getName() {
        return "math.ceil";
    }

    @Override
    public double compute(ControllerState controllerState) {
        return Math.ceil(this.value.get(controllerState));
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

