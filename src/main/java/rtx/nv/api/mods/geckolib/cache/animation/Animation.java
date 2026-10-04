package rtx.nv.api.mods.geckolib.cache.animation;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import rtx.nv.api.mods.geckolib.animation.object.LoopType;
import rtx.nv.api.mods.geckolib.cache.animation.BoneAnimation;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.CustomInstructionKeyframeData;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.SoundKeyframeData;
import rtx.nv.api.mods.geckolib.loading.math.value.Variable;

public record Animation(String name, double length, LoopType loopType, BoneAnimation[] boneAnimations, Set<Variable> usedVariables, Animation.KeyframeMarkers keyframeMarkers) {
    public static Animation create(String string, double d, LoopType loopType, BoneAnimation[] boneAnimationArray, Animation.KeyframeMarkers keyframeMarkers) {
        ReferenceOpenHashSet referenceOpenHashSet = new ReferenceOpenHashSet();
        for (BoneAnimation boneAnimation : boneAnimationArray) {
            referenceOpenHashSet.addAll(boneAnimation.getUsedVariables());
        }
        return new Animation(string, d, loopType, boneAnimationArray, (Set<Variable>)new ReferenceArraySet((Set)referenceOpenHashSet), keyframeMarkers);
    }

    public static Animation generateWaitAnimation(double d) {
        return new Animation("internal.wait", d, LoopType.PLAY_ONCE, new BoneAnimation[0], Set.of(), new Animation.KeyframeMarkers(new SoundKeyframeData[0], new ParticleKeyframeData[0], new CustomInstructionKeyframeData[0]));
    }


    public static record KeyframeMarkers(SoundKeyframeData[] sounds, ParticleKeyframeData[] particles, CustomInstructionKeyframeData[] customInstructions) {
        public boolean isEmpty() {
            return this.sounds.length == 0 && this.particles.length == 0 && this.customInstructions.length == 0;
        }
    }
}

