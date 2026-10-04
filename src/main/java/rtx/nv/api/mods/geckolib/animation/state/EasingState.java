package rtx.nv.api.mods.geckolib.animation.state;
import java.util.Arrays;
import rtx.nv.api.mods.geckolib.animation.object.EasingType;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;

public record EasingState(EasingType easingType, MathValue[] easingArgs, double delta, double fromValue, double toValue) {
    @Override
    public String toString() {
        return "EasingType: " + String.valueOf(this.easingType) + " | EasingArgs: " + Arrays.toString(this.easingArgs) + " | Delta: " + this.delta + " | From: " + this.fromValue + " | To: " + this.toValue;
    }
}

