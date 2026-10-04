package rtx.nv.api.mods.geckolib.loading.math.function.generic;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class ModFunction
extends MathFunction {
    private final MathValue value;
    private final MathValue modulus;

    public ModFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.value = mathValueArray[0];
        this.modulus = mathValueArray[1];
    }

    @Override
    public String getName() {
        return "math.mod";
    }

    @Override
    public double compute(ControllerState controllerState) {
        return this.value.get(controllerState) % this.modulus.get(controllerState);
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[]{this.value, this.modulus};
    }

    @Override
    public int getMinArgs() {
        return 2;
    }
}

