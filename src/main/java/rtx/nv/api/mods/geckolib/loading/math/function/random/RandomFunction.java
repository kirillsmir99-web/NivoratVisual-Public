package rtx.nv.api.mods.geckolib.loading.math.function.random;
import java.util.Random;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class RandomFunction
extends MathFunction {
    private final MathValue valueA;
    private final MathValue valueB;
    private final MathValue seed;
    private final Random random;

    public RandomFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.valueA = mathValueArray[0];
        this.valueB = mathValueArray.length >= 2 ? mathValueArray[1] : null;
        this.seed = mathValueArray.length >= 3 ? mathValueArray[2] : null;
        this.random = this.seed != null ? new Random() : null;
    }

    @Override
    public String getName() {
        return "math.random";
    }

    @Override
    public double compute(ControllerState controllerState) {
        double d;
        double d2 = this.valueA.get(controllerState);
        if (this.random != null && this.seed != null) {
            this.random.setSeed((long)this.seed.get(controllerState));
            d = this.random.nextDouble();
        } else {
            d = Math.random();
        }
        if (this.valueB != null) {
            double d3 = this.valueB.get(controllerState);
            double d4 = Math.min(d2, d3);
            double d5 = Math.max(d2, d3);
            d = d4 + d * (d5 - d4);
        } else {
            d *= d2;
        }
        return d;
    }

    @Override
    public MathValue[] getArgs() {
        if (this.seed != null) {
            return new MathValue[]{this.valueA, this.valueB, this.seed};
        }
        if (this.valueB != null) {
            return new MathValue[]{this.valueA, this.valueB};
        }
        return new MathValue[]{this.valueA};
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public boolean isMutable(MathValue ... mathValueArray) {
        if (mathValueArray.length < 3) {
            return true;
        }
        return super.isMutable(mathValueArray);
    }
}

