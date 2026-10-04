package rtx.nv.api.mods.geckolib.loading.math.function.generic;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class ATan2Function
extends MathFunction {
    private final MathValue y;
    private final MathValue x;

    public ATan2Function(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.y = mathValueArray[0];
        this.x = mathValueArray[1];
    }

    @Override
    public String getName() {
        return "math.atan2";
    }

    @Override
    public double compute(ControllerState controllerState) {
        return Math.atan2(this.y.get(controllerState), this.x.get(controllerState)) * 57.2957763671875;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[]{this.y, this.x};
    }

    @Override
    public int getMinArgs() {
        return 2;
    }
}

