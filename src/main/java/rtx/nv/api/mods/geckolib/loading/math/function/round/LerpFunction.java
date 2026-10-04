package rtx.nv.api.mods.geckolib.loading.math.function.round;
import net.minecraft.util.math.MathHelper;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class LerpFunction
extends MathFunction {
    private final MathValue min;
    private final MathValue max;
    private final MathValue delta;

    public LerpFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.min = mathValueArray[0];
        this.max = mathValueArray[1];
        this.delta = mathValueArray[2];
    }

    @Override
    public String getName() {
        return "math.lerp";
    }

    @Override
    public double compute(ControllerState controllerState) {
        return MathHelper.lerp((double)this.delta.get(controllerState), (double)this.min.get(controllerState), (double)this.max.get(controllerState));
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

