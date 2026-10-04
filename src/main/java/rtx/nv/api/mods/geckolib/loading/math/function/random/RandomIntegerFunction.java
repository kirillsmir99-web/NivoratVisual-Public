package rtx.nv.api.mods.geckolib.loading.math.function.random;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class RandomIntegerFunction
extends MathFunction {
    private final MathValue valueA;
    private final MathValue valueB;
    private final MathValue seed;
    private final Random random;

    public RandomIntegerFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.valueA = mathValueArray[0];
        this.valueB = mathValueArray.length >= 2 ? mathValueArray[1] : null;
        this.seed = mathValueArray.length >= 3 ? mathValueArray[2] : null;
        this.random = this.seed != null ? new Random() : null;
    }

    @Override
    public String getName() {
        return "math.random_integer";
    }

    @Override
    public double compute(ControllerState controllerState) {
        int n;
        Random random;
        int n2 = (int)Math.round(this.valueA.get(controllerState));
        if (this.random != null && this.seed != null) {
            this.random.setSeed((long)this.seed.get(controllerState));
            random = this.random;
        } else {
            random = ThreadLocalRandom.current();
        }
        if (this.valueB != null) {
            int n3 = (int)Math.round(this.valueB.get(controllerState));
            int n4 = Math.min(n2, n3);
            int n5 = Math.max(n2, n3);
            n = n4 + random.nextInt(n5 + 1 - n4);
        } else {
            n = random.nextInt(0, n2 + 1);
        }
        return n;
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

