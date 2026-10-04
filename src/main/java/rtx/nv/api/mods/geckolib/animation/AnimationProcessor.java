package rtx.nv.api.mods.geckolib.animation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Objects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.manager.AnimatableManager;
import rtx.nv.api.mods.geckolib.animation.AnimationController;
import rtx.nv.api.mods.geckolib.animation.RawAnimation;
import rtx.nv.api.mods.geckolib.animation.object.EasingType;
import rtx.nv.api.mods.geckolib.animation.state.AnimationPoint;
import rtx.nv.api.mods.geckolib.animation.state.BoneSnapshot;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.animation.state.EasingState;
import rtx.nv.api.mods.geckolib.cache.animation.Animation;
import rtx.nv.api.mods.geckolib.cache.animation.BoneAnimation;
import rtx.nv.api.mods.geckolib.cache.animation.Keyframe;
import rtx.nv.api.mods.geckolib.cache.model.GeoBone;
import rtx.nv.api.mods.geckolib.constant.DataTickets;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.MolangQueries;
import rtx.nv.api.mods.geckolib.model.GeoModel;
import rtx.nv.api.mods.geckolib.renderer.base.BoneSnapshots;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.util.ClientUtil;

public class AnimationProcessor {
    public static void createBoneSnapshots(ControllerState controllerState, BoneSnapshots boneSnapshots) {
        AnimationPoint animationPoint = controllerState.animationPoint();
        AnimationPoint animationPoint2 = controllerState.prevAnimationPoint();
        EasingType easingType = controllerState.easingOverride();
        boolean bl = controllerState.additive();
        BoneAnimation[] boneAnimationArray2 = animationPoint.animation().boneAnimations();
        ObjectOpenHashSet<String> objectOpenHashSet = controllerState.transitionTime() >= 0.0 ? new ObjectOpenHashSet<>(boneAnimationArray2.length) : null;
        for (int i = 0; i < boneAnimationArray2.length; ++i) {
            String boneName = boneAnimationArray2[i].boneName();
            BoneSnapshot boneSnapshot = boneSnapshots.get(boneName).orElse(null);
            if (boneSnapshot == null) continue;
            if (objectOpenHashSet != null) {
                objectOpenHashSet.add(boneName);
            }
            AnimationProcessor.setSnapshotScale(boneSnapshot, i, controllerState, bl, animationPoint, animationPoint2, easingType);
            AnimationProcessor.setSnapshotRotation(boneSnapshot, i, controllerState, bl, animationPoint, animationPoint2, easingType);
            AnimationProcessor.setSnapshotTranslation(boneSnapshot, i, controllerState, bl, animationPoint, animationPoint2, easingType);
        }
        if (animationPoint2 != null && objectOpenHashSet != null) {
            ControllerState controllerState2 = AnimationProcessor.createTransitionControllerState(animationPoint2, controllerState, controllerState.transitionTime());
            BoneAnimation[] boneAnimationArray = animationPoint2.animation().boneAnimations();
            for (int i = 0; i < boneAnimationArray.length; ++i) {
                BoneSnapshot boneSnapshot;
                String string = boneAnimationArray[i].boneName();
                if (objectOpenHashSet.contains(string) || (boneSnapshot = boneSnapshots.get(string).orElse(null)) == null) continue;
                AnimationProcessor.setSnapshotScale(boneSnapshot, i, controllerState2, bl, animationPoint2, null, easingType);
                AnimationProcessor.setSnapshotRotation(boneSnapshot, i, controllerState2, bl, animationPoint2, null, easingType);
                AnimationProcessor.setSnapshotTranslation(boneSnapshot, i, controllerState2, bl, animationPoint2, null, easingType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends GeoAnimatable> void extractControllerStates(T t, GeoRenderState geoRenderState, GeoModel<T> geoModel) {
        AnimatableManager<T> animatableManager = (AnimatableManager<T>) geoRenderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (animatableManager == null) return;
        Collection<AnimationController<T>> collection = animatableManager.getAnimationControllers().values();
        double d = geoRenderState.getAnimatableAge();
        ObjectArrayList<ControllerState> objectArrayList = new ObjectArrayList<>(collection.size());
        animatableManager.markRenderedAt(d);
        if (!collection.isEmpty()) {
            float f = geoRenderState.getPartialTick();
            World world = ClientUtil.getLevel();
            PlayerEntity playerEntity = ClientUtil.getClientPlayer();
            Vec3d vec3d = ClientUtil.getCameraPos();
            double d2 = animatableManager.getFirstRenderTick() - d;
            if (world != null && playerEntity != null) {
                for (AnimationController<T> animationController : collection) {
                    MolangQueries.Actor<T> actor = new MolangQueries.Actor<>(t, geoRenderState, animationController, d2, f, world, playerEntity, vec3d);
                    ControllerState controllerState = animationController.extractControllerState(t, geoRenderState, animatableManager, actor, geoModel);
                    if (controllerState == null) continue;
                    objectArrayList.add(controllerState);
                }
            }
        }
        geoRenderState.addGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES, (ControllerState[]) objectArrayList.toArray(new ControllerState[0]));
    }

    private static ControllerState createTransitionControllerState(AnimationPoint animationPoint, ControllerState controllerState, double d) {
        return new ControllerState(animationPoint, null, d, controllerState.transitionTicks(), controllerState.additive(), controllerState.easingOverride(), controllerState.renderState(), controllerState.queryValues());
    }

    private static void setSnapshotRotation(BoneSnapshot boneSnapshot, int n, ControllerState controllerState, boolean bl, AnimationPoint animationPoint, AnimationPoint animationPoint2, EasingType easingType) {
        float f = AnimationProcessor.findAnimationPointValue(boneSnapshot, controllerState, animationPoint, animationPoint2, n, AnimationPoint.Transform.ROTATION, AnimationPoint.Axis.X, easingType);
        float f2 = AnimationProcessor.findAnimationPointValue(boneSnapshot, controllerState, animationPoint, animationPoint2, n, AnimationPoint.Transform.ROTATION, AnimationPoint.Axis.Y, easingType);
        float f3 = AnimationProcessor.findAnimationPointValue(boneSnapshot, controllerState, animationPoint, animationPoint2, n, AnimationPoint.Transform.ROTATION, AnimationPoint.Axis.Z, easingType);
        if (bl) {
            GeoBone geoBone = boneSnapshot.getBone();
            f += boneSnapshot.getRotX() - geoBone.baseRotX();
            f2 += boneSnapshot.getRotY() - geoBone.baseRotY();
            f3 += boneSnapshot.getRotZ() - geoBone.baseRotZ();
        }
        boneSnapshot.setRotation(f, f2, f3);
    }

    public static float findAnimationPointValue(BoneSnapshot boneSnapshot, ControllerState controllerState, AnimationPoint animationPoint, AnimationPoint animationPoint2, int n, AnimationPoint.Transform transform, AnimationPoint.Axis axis, EasingType easingType) {
        Keyframe keyframe;
        if (controllerState.transitionTime() >= 0.0) {
            return animationPoint2 != null ? AnimationProcessor.findTransitionPointValue(boneSnapshot, controllerState, animationPoint, animationPoint2, n, transform, axis, easingType) : AnimationProcessor.findResetPointValue(boneSnapshot, controllerState, animationPoint, n, transform, axis, easingType);
        }
        Keyframe keyframe2 = animationPoint.getCurrentKeyframe(n, transform, axis);
        if (keyframe2 == null || (keyframe = animationPoint.getNextKeyframe(n, transform, axis)) == null) {
            return transform.defaultValue;
        }
        double d = keyframe2.endValue().get(controllerState);
        double d2 = keyframe.endValue().get(controllerState);
        double d3 = keyframe.length() == 0.0 ? 0.0 : (animationPoint.animTime() - keyframe2.startTime()) / keyframe.length();
        EasingState easingState = new EasingState(easingType != null ? easingType : keyframe.easingType(), keyframe.easingArgs(), d3, d, d2);
        return (float)EasingType.lerpWithOverride(easingState, controllerState);
    }

    private static float findResetPointValue(BoneSnapshot boneSnapshot, ControllerState controllerState, AnimationPoint animationPoint, int n, AnimationPoint.Transform transform, AnimationPoint.Axis axis, EasingType easingType) {
        ControllerState controllerState2 = new ControllerState(controllerState.animationPoint(), null, -1.0, 0, controllerState.additive(), controllerState.easingOverride(), controllerState.renderState(), controllerState.queryValues());
        double d = Math.min(1.0, controllerState.transitionTime() / ((double)controllerState.transitionTicks() / 20.0));
        double d2 = AnimationProcessor.wrapRotation(AnimationProcessor.findAnimationPointValue(boneSnapshot, controllerState2, animationPoint, null, n, transform, axis, easingType), transform);
        double d3 = AnimationProcessor.getSnapshotResetTarget(boneSnapshot, transform, axis, controllerState.additive());
        EasingState easingState = new EasingState(easingType == null ? EasingType.LINEAR : easingType, new MathValue[0], d, d2, d3);
        return (float)EasingType.lerpWithOverride(easingState, controllerState);
    }

    private static void setSnapshotScale(BoneSnapshot boneSnapshot, int n, ControllerState controllerState, boolean bl, AnimationPoint animationPoint, AnimationPoint animationPoint2, EasingType easingType) {
        float f = AnimationProcessor.findAnimationPointValue(boneSnapshot, controllerState, animationPoint, animationPoint2, n, AnimationPoint.Transform.SCALE, AnimationPoint.Axis.X, easingType);
        float f2 = AnimationProcessor.findAnimationPointValue(boneSnapshot, controllerState, animationPoint, animationPoint2, n, AnimationPoint.Transform.SCALE, AnimationPoint.Axis.Y, easingType);
        float f3 = AnimationProcessor.findAnimationPointValue(boneSnapshot, controllerState, animationPoint, animationPoint2, n, AnimationPoint.Transform.SCALE, AnimationPoint.Axis.Z, easingType);
        if (bl) {
            f *= boneSnapshot.getScaleX();
            f2 *= boneSnapshot.getScaleY();
            f3 *= boneSnapshot.getScaleZ();
        }
        boneSnapshot.setScale(f, f2, f3);
    }

    private static void setSnapshotTranslation(BoneSnapshot boneSnapshot, int n, ControllerState controllerState, boolean bl, AnimationPoint animationPoint, AnimationPoint animationPoint2, EasingType easingType) {
        float f = AnimationProcessor.findAnimationPointValue(boneSnapshot, controllerState, animationPoint, animationPoint2, n, AnimationPoint.Transform.TRANSLATION, AnimationPoint.Axis.X, easingType);
        float f2 = AnimationProcessor.findAnimationPointValue(boneSnapshot, controllerState, animationPoint, animationPoint2, n, AnimationPoint.Transform.TRANSLATION, AnimationPoint.Axis.Y, easingType);
        float f3 = AnimationProcessor.findAnimationPointValue(boneSnapshot, controllerState, animationPoint, animationPoint2, n, AnimationPoint.Transform.TRANSLATION, AnimationPoint.Axis.Z, easingType);
        if (bl) {
            f += boneSnapshot.getTranslateX();
            f2 += boneSnapshot.getTranslateY();
            f3 += boneSnapshot.getTranslateZ();
        }
        boneSnapshot.setTranslation(f, f2, f3);
    }

    public static <T extends GeoAnimatable> Animation getOrCreateAnimation(RawAnimation.Stage stage, T t, GeoModel<T> geoModel) {
        if (stage.animationName().equals("internal.wait")) {
            return Animation.generateWaitAnimation(stage.waitTicks());
        }
        return geoModel.getBakedAnimation(t, stage.animationName());
    }

    private static float findTransitionPointValue(BoneSnapshot boneSnapshot, ControllerState controllerState, AnimationPoint animationPoint, AnimationPoint animationPoint2, int n, AnimationPoint.Transform transform, AnimationPoint.Axis axis, EasingType easingType) {
        Keyframe keyframe = animationPoint.getCurrentKeyframe(n, transform, axis);
        if (keyframe == null) {
            return transform.defaultValue;
        }
        int n2 = animationPoint2.findBoneIndex(boneSnapshot.getBone());
        double d = n2 < 0 ? (double)transform.defaultValue : AnimationProcessor.wrapRotation(AnimationProcessor.findAnimationPointValue(boneSnapshot, AnimationProcessor.createTransitionControllerState(animationPoint2, controllerState, -1.0), animationPoint2, null, n2, transform, axis, animationPoint2.easingOverride()), transform);
        double d2 = keyframe.startValue().get(controllerState);
        double d3 = controllerState.transitionTicks() == 0 ? 1.0 : Math.min(1.0, controllerState.transitionTime() / ((double)controllerState.transitionTicks() / 20.0));
        EasingState easingState = new EasingState(easingType != null ? easingType : keyframe.easingType(), keyframe.easingArgs(), d3, d, d2);
        return (float)EasingType.lerpWithOverride(easingState, controllerState);
    }

    private static float getSnapshotResetTarget(BoneSnapshot boneSnapshot, AnimationPoint.Transform transform, AnimationPoint.Axis axis, boolean bl) {
        if (!bl) {
            return transform.defaultValue;
        }
        return switch (transform) {
            case SCALE -> switch (axis) {
                case X -> boneSnapshot.getScaleX();
                case Y -> boneSnapshot.getScaleY();
                case Z -> boneSnapshot.getScaleZ();
            };
            case ROTATION -> switch (axis) {
                case X -> boneSnapshot.getRotX();
                case Y -> boneSnapshot.getRotY();
                case Z -> boneSnapshot.getRotZ();
            };
            case TRANSLATION -> switch (axis) {
                case X -> boneSnapshot.getTranslateX();
                case Y -> boneSnapshot.getTranslateY();
                case Z -> boneSnapshot.getTranslateZ();
            };
        };
    }

    private static double wrapRotation(double d, AnimationPoint.Transform transform) {
        if (transform != AnimationPoint.Transform.ROTATION) {
            return d;
        }
        return MathHelper.wrapDegrees((double)(d * 57.2957763671875)) * 0.01745329238474369;
    }
}
