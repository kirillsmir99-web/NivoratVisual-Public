package rtx.nv.api.mods.geckolib.animation.state;
import java.util.function.Function;
import net.minecraft.util.math.MathHelper;
import rtx.nv.api.mods.geckolib.animation.object.EasingType;
import rtx.nv.api.mods.geckolib.animation.object.LoopType;
import rtx.nv.api.mods.geckolib.cache.animation.Animation;
import rtx.nv.api.mods.geckolib.cache.animation.BoneAnimation;
import rtx.nv.api.mods.geckolib.cache.animation.Keyframe;
import rtx.nv.api.mods.geckolib.cache.animation.KeyframeStack;
import rtx.nv.api.mods.geckolib.cache.model.GeoBone;
import rtx.nv.api.mods.geckolib.util.MiscUtil;

public record AnimationPoint(Animation animation, EasingType easingOverride, LoopType loopType, double animTime, int[][][] keyFramePoints) {
    public static final int NO_KEYFRAME = -2;
    public static final int BEFORE_FIRST_KEYFRAME = -1;

    public boolean hasFinished() {
        return MiscUtil.areFloatsEqual(this.animTime, this.animation.length());
    }

    public int[] scalePoints(int n) {
        return this.keyFramePoints[n][AnimationPoint.Transform.SCALE.index];
    }

    private static void findBonePoints(BoneAnimation boneAnimation, int[][] nArray, double d, boolean bl) {
        AnimationPoint.findKeyframePoints(boneAnimation.scaleKeyFrames(), nArray[AnimationPoint.Transform.SCALE.index], d, bl);
        AnimationPoint.findKeyframePoints(boneAnimation.rotationKeyFrames(), nArray[AnimationPoint.Transform.ROTATION.index], d, bl);
        AnimationPoint.findKeyframePoints(boneAnimation.positionKeyFrames(), nArray[AnimationPoint.Transform.TRANSLATION.index], d, bl);
    }

    public Keyframe getCurrentKeyframe(int n, AnimationPoint.Transform transform, AnimationPoint.Axis axis) {
        return this.getKeyframe(n, transform, axis, 0);
    }

    public Keyframe getNextKeyframe(int n, AnimationPoint.Transform transform, AnimationPoint.Axis axis) {
        return this.getKeyframe(n, transform, axis, 1);
    }

    private static int[][][] constructBoneArray(Animation animation, double d) {
        BoneAnimation[] boneAnimationArray = animation.boneAnimations();
        int[][][] nArray = new int[boneAnimationArray.length][3][3];
        for (int i = 0; i < boneAnimationArray.length; ++i) {
            AnimationPoint.findBonePoints(boneAnimationArray[i], nArray[i], d, false);
        }
        return nArray;
    }

    public Keyframe getPreviousKeyframe(int n, AnimationPoint.Transform transform, AnimationPoint.Axis axis) {
        return this.getKeyframe(n, transform, axis, -1);
    }

    public Keyframe getKeyframe(int n, AnimationPoint.Transform transform, AnimationPoint.Axis axis, int n2) {
        BoneAnimation boneAnimation = this.animation.boneAnimations()[n];
        Keyframe[] keyframeArray = axis.keyframes(transform.keyframeStack(boneAnimation));
        int n3 = this.keyFramePoints[n][transform.index][axis.index];
        return keyframeArray.length == 0 || n3 == -2 ? null : keyframeArray[MathHelper.clamp((int)(n3 + n2), (int)0, (int)(keyframeArray.length - 1))];
    }

    private static int findKeyframePointForward(Keyframe[] keyframeArray, double d, int n) {
        if (keyframeArray.length == 0) {
            return -2;
        }
        if (keyframeArray[0].startTime() > d) {
            return -1;
        }
        for (int i = Math.max(0, n); i < keyframeArray.length; ++i) {
            if (i + 1 >= keyframeArray.length || !(keyframeArray[i + 1].startTime() >= d)) continue;
            return i;
        }
        return keyframeArray.length - 1;
    }

    public int[] translationPoints(int n) {
        return this.keyFramePoints[n][AnimationPoint.Transform.TRANSLATION.index];
    }

    public int[] rotationPoints(int n) {
        return this.keyFramePoints[n][AnimationPoint.Transform.ROTATION.index];
    }

    private static int findKeyframePointReverse(Keyframe[] keyframeArray, double d, int n) {
        if (keyframeArray.length == 0) {
            return -2;
        }
        for (int i = Math.min(n, keyframeArray.length - 1); i >= 0; --i) {
            if (i - 1 < 0 || !(keyframeArray[i - 1].startTime() <= d)) continue;
            return i - 1;
        }
        return -1;
    }

    private static void findKeyframePoints(KeyframeStack keyframeStack, int[] nArray, double d, boolean bl) {
        if (bl) {
            nArray[AnimationPoint.Axis.X.index] = AnimationPoint.findKeyframePointReverse(keyframeStack.xKeyframes(), d, nArray[AnimationPoint.Axis.X.index]);
            nArray[AnimationPoint.Axis.Y.index] = AnimationPoint.findKeyframePointReverse(keyframeStack.yKeyframes(), d, nArray[AnimationPoint.Axis.Y.index]);
            nArray[AnimationPoint.Axis.Z.index] = AnimationPoint.findKeyframePointReverse(keyframeStack.zKeyframes(), d, nArray[AnimationPoint.Axis.Z.index]);
        } else {
            nArray[AnimationPoint.Axis.X.index] = AnimationPoint.findKeyframePointForward(keyframeStack.xKeyframes(), d, nArray[AnimationPoint.Axis.X.index]);
            nArray[AnimationPoint.Axis.Y.index] = AnimationPoint.findKeyframePointForward(keyframeStack.yKeyframes(), d, nArray[AnimationPoint.Axis.Y.index]);
            nArray[AnimationPoint.Axis.Z.index] = AnimationPoint.findKeyframePointForward(keyframeStack.zKeyframes(), d, nArray[AnimationPoint.Axis.Z.index]);
        }
    }

    public int findBoneIndex(GeoBone geoBone) {
        BoneAnimation[] boneAnimationArray = this.animation.boneAnimations();
        for (int i = 0; i < boneAnimationArray.length; ++i) {
            if (!boneAnimationArray[i].boneName().equals(geoBone.name())) continue;
            return i;
        }
        return -1;
    }

    public AnimationPoint createNext(double d) {
        if (MiscUtil.areFloatsEqual(d, this.animTime)) {
            return this;
        }
        d = MathHelper.clamp((double)d, (double)0.0, (double)this.animation.length());
        AnimationPoint animationPoint = new AnimationPoint(this.animation, this.easingOverride, this.loopType, d, this.keyFramePoints);
        boolean bl = d < this.animTime;
        for (int i = 0; i < this.keyFramePoints.length; ++i) {
            AnimationPoint.findBonePoints(this.animation.boneAnimations()[i], animationPoint.keyFramePoints[i], d, bl);
        }
        return animationPoint;
    }

    public static AnimationPoint createFor(Animation animation, EasingType easingType, LoopType loopType, double d) {
        d = MathHelper.clamp((double)d, (double)0.0, (double)animation.length());
        return new AnimationPoint(animation, easingType, loopType, d, AnimationPoint.constructBoneArray(animation, d));
    }


    public static enum Axis {
        X(0, KeyframeStack::xKeyframes),
        Y(1, KeyframeStack::yKeyframes),
        Z(2, KeyframeStack::zKeyframes);

        public final int index;
        public final Function<KeyframeStack, Keyframe[]> framesFunction;

        private Axis(int index, Function<KeyframeStack, Keyframe[]> framesFunction) {
            this.index = index;
            this.framesFunction = framesFunction;
        }

        public Keyframe[] keyframes(KeyframeStack keyframeStack) {
            return this.framesFunction.apply(keyframeStack);
        }
    }

    public static enum Transform {
        SCALE(0, 1.0f, BoneAnimation::scaleKeyFrames),
        ROTATION(1, 0.0f, BoneAnimation::rotationKeyFrames),
        TRANSLATION(2, 0.0f, BoneAnimation::positionKeyFrames);

        public final int index;
        public final float defaultValue;
        public final Function<BoneAnimation, KeyframeStack> stackFunction;

        private Transform(int index, float defaultValue, Function<BoneAnimation, KeyframeStack> stackFunction) {
            this.index = index;
            this.defaultValue = defaultValue;
            this.stackFunction = stackFunction;
        }

        public KeyframeStack keyframeStack(BoneAnimation boneAnimation) {
            return this.stackFunction.apply(boneAnimation);
        }
    }
}

