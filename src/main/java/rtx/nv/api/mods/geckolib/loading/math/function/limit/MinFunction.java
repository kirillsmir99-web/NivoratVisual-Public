package rtx.nv.api.mods.geckolib.loading.math.function.limit;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class MinFunction
extends MathFunction {
    private final MathValue valueA;
    private final MathValue valueB;

    public MinFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.valueA = mathValueArray[0];
        this.valueB = mathValueArray[1];
    }

    @Override
    public String getName() {
        return "math.min";
    }

    @Override
    public double compute(ControllerState controllerState) {
        return Math.min(this.valueA.get(controllerState), this.valueB.get(controllerState));
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[]{this.valueA, this.valueB};
    }

    @Override
    public int getMinArgs() {
        return 2;
    }
}

