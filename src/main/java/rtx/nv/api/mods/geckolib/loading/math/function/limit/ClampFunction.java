package rtx.nv.api.mods.geckolib.loading.math.function.limit;
import net.minecraft.util.math.MathHelper;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class ClampFunction
extends MathFunction {
    private final MathValue value;
    private final MathValue min;
    private final MathValue max;

    public ClampFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.value = mathValueArray[0];
        this.min = mathValueArray[1];
        this.max = mathValueArray[2];
    }

    @Override
    public String getName() {
        return "math.clamp";
    }

    @Override
    public double compute(ControllerState controllerState) {
        return MathHelper.clamp((double)this.value.get(controllerState), (double)this.min.get(controllerState), (double)this.max.get(controllerState));
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[]{this.value, this.min, this.max};
    }

    @Override
    public int getMinArgs() {
        return 3;
    }
}

