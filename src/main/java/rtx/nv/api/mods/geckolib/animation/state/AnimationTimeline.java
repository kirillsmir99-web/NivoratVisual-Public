package rtx.nv.api.mods.geckolib.animation.state;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.util.math.MathHelper;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animation.AnimationController;
import rtx.nv.api.mods.geckolib.animation.AnimationController.KeyframeEventHandler;
import rtx.nv.api.mods.geckolib.animation.AnimationProcessor;
import rtx.nv.api.mods.geckolib.animation.RawAnimation;
import rtx.nv.api.mods.geckolib.animation.RawAnimation.Stage;
import rtx.nv.api.mods.geckolib.animation.object.EasingType;
import rtx.nv.api.mods.geckolib.animation.object.LoopType;
import rtx.nv.api.mods.geckolib.animation.state.AnimationPoint;
import rtx.nv.api.mods.geckolib.animation.state.KeyFrameEvent;
import rtx.nv.api.mods.geckolib.cache.animation.Animation;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.CustomInstructionKeyframeData;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.KeyFrameData;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.SoundKeyframeData;
import rtx.nv.api.mods.geckolib.model.GeoModel;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;

public record AnimationTimeline(AnimationTimeline.Stage[] stages) {
    public static <T extends GeoAnimatable> AnimationTimeline create(RawAnimation rawAnimation, T t, GeoModel<T> geoModel, int n) {
        List<RawAnimation.Stage> list = rawAnimation.getAnimationStages();
        ObjectArrayList objectArrayList = new ObjectArrayList(list.size());
        double d = (float)n / 20.0f;
        double d2 = 0.0;
        for (RawAnimation.Stage stage : list) {
            Animation animation = AnimationProcessor.getOrCreateAnimation(stage, t, geoModel);
            if (animation == null) continue;
            if (d > 0.0) {
                objectArrayList.add(AnimationTimeline.Stage.transition(d2, d, animation));
                d2 += d;
            }
            objectArrayList.add(AnimationTimeline.Stage.animation(d2, animation, stage.loopType()));
            d2 += animation.length();
        }
        if (objectArrayList.isEmpty()) {
            return null;
        }
        if (d > 0.0) {
            objectArrayList.add(AnimationTimeline.Stage.transition(d2, d, ((AnimationTimeline.Stage)objectArrayList.getLast()).animation()));
        }
        return new AnimationTimeline((AnimationTimeline.Stage[]) objectArrayList.toArray(new AnimationTimeline.Stage[0]));
    }

    public <T extends GeoAnimatable> void triggerKeyframeMarkersBetween(T t, GeoRenderState geoRenderState, double d, double d2, AnimationController<T> animationController, AnimationController.KeyframeEventHandler<T, SoundKeyframeData> keyframeEventHandler, AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> keyframeEventHandler2, AnimationController.KeyframeEventHandler<T, CustomInstructionKeyframeData> keyframeEventHandler3) {
        if (keyframeEventHandler == null && keyframeEventHandler2 == null && keyframeEventHandler3 == null) {
            return;
        }
        ObjectArrayList<SoundKeyframeData> objectArrayList = new ObjectArrayList<>();
        ObjectArrayList<ParticleKeyframeData> objectArrayList2 = new ObjectArrayList<>();
        ObjectArrayList<CustomInstructionKeyframeData> objectArrayList3 = new ObjectArrayList<>();
        double d3 = Math.min(d, d2);
        double d4 = Math.max(d, d2);
        for (AnimationTimeline.Stage stage : this.stages) {
            if (stage.startTime > d4) break;
            if (!(stage.endTime > d3) || stage.isTransition) continue;
            double d5 = Math.max(0.0, d3 - stage.startTime);
            double d6 = Math.min(stage.animation.length(), d4 - stage.startTime);
            if (keyframeEventHandler != null) {
                objectArrayList.addAll(this.getKeyframesForAnimation(d5, d6, stage.animation.keyframeMarkers().sounds()));
            }
            if (keyframeEventHandler2 != null) {
                objectArrayList2.addAll(this.getKeyframesForAnimation(d5, d6, stage.animation.keyframeMarkers().particles()));
            }
            if (keyframeEventHandler3 == null) continue;
            objectArrayList3.addAll(this.getKeyframesForAnimation(d5, d6, stage.animation.keyframeMarkers().customInstructions()));
        }
        if (!objectArrayList.isEmpty()) {
            for (SoundKeyframeData soundKeyframeData : d2 < d ? objectArrayList.reversed() : objectArrayList) {
                keyframeEventHandler.handle(new KeyFrameEvent<>(t, geoRenderState, animationController, soundKeyframeData));
            }
        }
        if (!objectArrayList2.isEmpty()) {
            for (ParticleKeyframeData particleKeyframeData : d2 < d ? objectArrayList2.reversed() : objectArrayList2) {
                keyframeEventHandler2.handle(new KeyFrameEvent<>(t, geoRenderState, animationController, particleKeyframeData));
            }
        }
        if (!objectArrayList3.isEmpty()) {
            for (CustomInstructionKeyframeData customInstructionKeyframeData : d2 < d ? objectArrayList3.reversed() : objectArrayList3) {
                keyframeEventHandler3.handle(new KeyFrameEvent<>(t, geoRenderState, animationController, customInstructionKeyframeData));
            }
        }
    }

    public double lastAnimationEndTime() {
        AnimationTimeline.Stage stage = this.stages[this.stages.length - 1];
        return stage.isTransition ? stage.startTime : stage.endTime;
    }

    public AnimationPoint createAnimationPoint(double d, AnimationPoint animationPoint, EasingType easingType) {
        double d2;
        int n = this.getStageIndex(d);
        double d3 = (double)this.getTransitionLength() / 20.0;
        AnimationTimeline.Stage stage = this.stages[n];
        double d4 = d - stage.startTime;
        if (stage.isTransition) {
            boolean bl = n >= this.stages.length - 1;
            stage = this.stages[bl ? n - 1 : n + 1];
            d4 = bl ? stage.endTime : stage.startTime - d3;
        }
        double d5 = d2 = animationPoint == null ? 0.0 : stage.startTime + animationPoint.animTime();
        if (animationPoint == null || animationPoint.animation() != stage.animation || this.totalTime() - d2 < d2) {
            return AnimationPoint.createFor(stage.animation, easingType, stage.loopType(), d4);
        }
        return animationPoint.createNext(d4);
    }

    public int getTransitionLength() {
        return this.stages[0].isTransition ? MathHelper.ceil((double)(this.stages[0].endTime * 20.0)) : 0;
    }

    public double getTransitionTime(double d) {
        AnimationTimeline.Stage stage = this.getStage(d);
        return stage.isTransition ? d - stage.startTime : -1.0;
    }

    public AnimationTimeline.Stage getAnimationStage(double time) {
        return this.getStage(time);
    }

    public AnimationTimeline.Stage getStage(double d) {
        int n = this.getStageIndex(d);
        AnimationTimeline.Stage stage = this.stages[n];
        if (!stage.isTransition) {
            return stage;
        }
        return n >= this.stages.length - 1 ? this.stages[n - 1] : this.stages[n + 1];
    }

    public AnimationTimeline.Stage stage(double d) {
        return this.stages[this.getStageIndex(d)];
    }

    public double totalTime() {
        return this.stages[this.stages.length - 1].endTime;
    }

    public boolean hasFinishedAnimations(double d) {
        return d >= this.lastAnimationEndTime();
    }

    public int getStageIndex(double d) {
        for (int i = 0; i < this.stages.length; ++i) {
            if (!(d < this.stages[i].endTime)) continue;
            return i;
        }
        return this.stages.length - 1;
    }

    private <M extends KeyFrameData> List<M> getKeyframesForAnimation(double d, double d2, M[] MArray) {
        ObjectArrayList objectArrayList = new ObjectArrayList();
        for (M m : MArray) {
            if (((KeyFrameData)m).getTime() > d2) break;
            if (!(((KeyFrameData)m).getTime() > d) && d != 0.0) continue;
            objectArrayList.add(m);
        }
        return objectArrayList;
    }


    public static record Stage(double startTime, double endTime, boolean isTransition, Animation animation, LoopType loopType) {
        private static Stage transition(double d, double d2, Animation animation) {
            return new Stage(d, d + d2, true, animation, null);
        }
    
        private static Stage animation(double d, Animation animation, LoopType loopType) {
            return new Stage(d, d + animation.length(), false, animation, loopType);
        }
    }
}

