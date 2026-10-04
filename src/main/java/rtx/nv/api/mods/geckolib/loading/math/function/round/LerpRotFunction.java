package rtx.nv.api.mods.geckolib.loading.math.function.round;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;
import rtx.nv.api.mods.geckolib.util.MiscUtil;

public final class LerpRotFunction
extends MathFunction {
    private final MathValue min;
    private final MathValue max;
    private final MathValue delta;

    public LerpRotFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.min = mathValueArray[0];
        this.max = mathValueArray[1];
        this.delta = mathValueArray[2];
    }

    @Override
    public String getName() {
        return "math.lerprotate";
    }

    @Override
    public double compute(ControllerState controllerState) {
        return MiscUtil.lerpYaw(this.delta.get(controllerState), this.min.get(controllerState), this.max.get(controllerState));
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[]{this.min, this.max, this.delta};
    }

    @Override
    public int getMinArgs() {
        return 3;
    }
}

