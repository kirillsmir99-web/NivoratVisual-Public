package rtx.nv.api.mods.geckolib.cache.animation;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import rtx.nv.api.mods.geckolib.animation.object.EasingType;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.value.Variable;
import rtx.nv.api.mods.geckolib.util.MiscUtil;

public record Keyframe(double startTime, double length, MathValue startValue, MathValue endValue, EasingType easingType, MathValue[] easingArgs) {
    public Keyframe(double d, double d2, MathValue mathValue, MathValue mathValue2, EasingType easingType, List<MathValue> list) {
        this(d, d2, mathValue, mathValue2, easingType, list.toArray(new MathValue[0]));
    }

    public Keyframe(double d, double d2, MathValue mathValue, MathValue mathValue2, EasingType easingType) {
        this(d, d2, mathValue, mathValue2, easingType, new MathValue[0]);
    }

    public Keyframe(double d, double d2, MathValue mathValue, MathValue mathValue2) {
        this(d, d2, mathValue, mathValue2, EasingType.LINEAR);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || this.getClass() != object.getClass()) {
            return false;
        }
        Keyframe keyframe = (Keyframe)object;
        if (!MiscUtil.areFloatsEqual(this.length, keyframe.length)) {
            return false;
        }
        if (!this.startValue.equals(keyframe.startValue) || !this.endValue.equals(keyframe.endValue)) {
            return false;
        }
        if (this.easingType != keyframe.easingType) {
            return false;
        }
        return Arrays.equals(this.easingArgs, keyframe.easingArgs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.length, this.startValue, this.endValue, this.easingType, Arrays.hashCode(this.easingArgs));
    }

    public Set<Variable> getUsedVariables() {
        ReferenceOpenHashSet referenceOpenHashSet = new ReferenceOpenHashSet();
        if (this.startValue.isMutable()) {
            referenceOpenHashSet.addAll(this.startValue.getUsedVariables());
        }
        if (this.endValue.isMutable()) {
            referenceOpenHashSet.addAll(this.endValue.getUsedVariables());
        }
        for (MathValue mathValue : this.easingArgs) {
            if (!mathValue.isMutable()) continue;
            referenceOpenHashSet.addAll(mathValue.getUsedVariables());
        }
        return referenceOpenHashSet;
    }
}

