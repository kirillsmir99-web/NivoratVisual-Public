package rtx.nv.api.mods.geckolib.loading.math.function.generic;
import net.minecraft.util.math.MathHelper;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class CosFunction
extends MathFunction {
    private final MathValue value;

    public CosFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.value = mathValueArray[0];
    }

    @Override
    public String getName() {
        return "math.cos";
    }

    @Override
    public double compute(ControllerState controllerState) {
        return MathHelper.cos((double)((float)this.value.get(controllerState) * ((float)Math.PI / 180)));
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

