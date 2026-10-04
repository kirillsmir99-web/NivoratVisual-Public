package rtx.nv.api.mods.geckolib.loading.math.function.generic;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class PowFunction
extends MathFunction {
    private final MathValue value;
    private final MathValue power;

    public PowFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.value = mathValueArray[0];
        this.power = mathValueArray[1];
    }

    @Override
    public String getName() {
        return "math.pow";
    }

    @Override
    public double compute(ControllerState controllerState) {
        return Math.pow(this.value.get(controllerState), this.power.get(controllerState));
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[]{this.value, this.power};
    }

    @Override
    public int getMinArgs() {
        return 2;
    }
}

