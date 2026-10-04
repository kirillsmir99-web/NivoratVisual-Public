package rtx.nv.api.mods.geckolib.loading.math.function.random;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.MathFunction;

public final class DieRollFunction
extends MathFunction {
    private final MathValue rolls;
    private final MathValue min;
    private final MathValue max;
    private final MathValue seed;
    private final Random random;

    public DieRollFunction(MathValue ... mathValueArray) {
        super(mathValueArray);
        this.rolls = mathValueArray[0];
        this.min = mathValueArray[1];
        this.max = mathValueArray[2];
        this.seed = mathValueArray.length >= 4 ? mathValueArray[3] : null;
        this.random = this.seed != null ? new Random() : null;
    }

    @Override
    public String getName() {
        return "math.die_roll";
    }

    @Override
    public double compute(ControllerState controllerState) {
        Random random;
        int n = (int)Math.floor(this.rolls.get(controllerState));
        double d = this.min.get(controllerState);
        double d2 = this.max.get(controllerState);
        double d3 = 0.0;
        if (this.random != null && this.seed != null) {
            random = this.random;
            random.setSeed((long)this.seed.get(controllerState));
        } else {
            random = ThreadLocalRandom.current();
        }
        for (int i = 0; i < n; ++i) {
            d3 += d + random.nextDouble() * (d2 - d);
        }
        return d3;
    }

    @Override
    public MathValue[] getArgs() {
        if (this.seed != null) {
            return new MathValue[]{this.rolls, this.min, this.max, this.seed};
        }
        return new MathValue[]{this.rolls, this.min, this.max};
    }

    @Override
    public int getMinArgs() {
        return 3;
    }

    @Override
    public boolean isMutable(MathValue ... mathValueArray) {
        if (mathValueArray.length < 4) {
            return true;
        }
        return super.isMutable(mathValueArray);
    }
}

