package rtx.nv.api.mods.geckolib.cache.animation;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.List;
import java.util.Set;
import rtx.nv.api.mods.geckolib.cache.animation.Keyframe;
import rtx.nv.api.mods.geckolib.loading.math.value.Variable;

public record KeyframeStack(Keyframe[] xKeyframes, Keyframe[] yKeyframes, Keyframe[] zKeyframes) {
    public static final KeyframeStack EMPTY = new KeyframeStack(new Keyframe[0], new Keyframe[0], new Keyframe[0]);

    public KeyframeStack(List<Keyframe> list, List<Keyframe> list2, List<Keyframe> list3) {
        this(list.toArray(new Keyframe[0]), list2.toArray(new Keyframe[0]), list3.toArray(new Keyframe[0]));
    }

    public double getTotalKeyframeTime() {
        Keyframe keyframe;
        double d = 0.0;
        if (this.xKeyframes.length > 0) {
            keyframe = this.xKeyframes[this.xKeyframes.length - 1];
            d = Math.max(d, keyframe.startTime() + keyframe.length());
        }
        if (this.yKeyframes.length > 0) {
            keyframe = this.yKeyframes[this.yKeyframes.length - 1];
            d = Math.max(d, keyframe.startTime() + keyframe.length());
        }
        if (this.zKeyframes.length > 0) {
            keyframe = this.zKeyframes[this.zKeyframes.length - 1];
            d = Math.max(d, keyframe.startTime() + keyframe.length());
        }
        return d;
    }

    public Set<Variable> getUsedVariables() {
        ReferenceOpenHashSet referenceOpenHashSet = new ReferenceOpenHashSet();
        for (Keyframe keyframe : this.xKeyframes) {
            referenceOpenHashSet.addAll(keyframe.getUsedVariables());
        }
        for (Keyframe keyframe : this.yKeyframes) {
            referenceOpenHashSet.addAll(keyframe.getUsedVariables());
        }
        for (Keyframe keyframe : this.zKeyframes) {
            referenceOpenHashSet.addAll(keyframe.getUsedVariables());
        }
        return referenceOpenHashSet;
    }
}

