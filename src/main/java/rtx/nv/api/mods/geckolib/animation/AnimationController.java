package rtx.nv.api.mods.geckolib.animation;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.util.math.MathHelper;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.manager.AnimatableManager;
import rtx.nv.api.mods.geckolib.animation.RawAnimation;
import rtx.nv.api.mods.geckolib.animation.object.EasingType;
import rtx.nv.api.mods.geckolib.animation.object.PlayState;
import rtx.nv.api.mods.geckolib.animation.state.AnimationPoint;
import rtx.nv.api.mods.geckolib.animation.state.AnimationTest;
import rtx.nv.api.mods.geckolib.animation.state.AnimationTimeline;
import rtx.nv.api.mods.geckolib.animation.state.AnimationTimeline.Stage;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.cache.animation.Animation;
import rtx.nv.api.mods.geckolib.cache.animation.Animation.KeyframeMarkers;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.CustomInstructionKeyframeData;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.SoundKeyframeData;
import rtx.nv.api.mods.geckolib.loading.math.MolangQueries;
import rtx.nv.api.mods.geckolib.loading.math.MolangQueries.Actor;
import rtx.nv.api.mods.geckolib.loading.math.value.Variable;
import rtx.nv.api.mods.geckolib.model.GeoModel;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.util.ClientUtil;

public class AnimationController<T extends GeoAnimatable> {
    protected final String name;
    protected final AnimationController.AnimationStateHandler<T> stateHandler;
    protected final Supplier<Map<String, RawAnimation>> triggerableAnimations = Suppliers.memoize(Object2ObjectOpenHashMap::new);
    protected AnimationController.KeyframeEventHandler<T, SoundKeyframeData> soundKeyframeHandler = null;
    protected AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> particleKeyframeHandler = null;
    protected AnimationController.KeyframeEventHandler<T, CustomInstructionKeyframeData> customKeyframeHandler = null;
    protected boolean additiveAnimations = false;
    protected int transitionTicks;
    public double animationSpeed = 1.0;
    protected boolean handlesTriggeredAnimations = false;
    protected EasingType easingOverride = null;
    protected AnimationTimeline timeline = null;
    protected PlayState playState = PlayState.STOP;
    protected RawAnimation currentRawAnimation = null;
    protected AnimationPoint transitionFromPoint = null;
    protected AnimationPoint animationPoint = null;
    protected double timelineTime = -1.0;
    protected double lastAnimatableAge = 0.0;
    protected double triggeredAnimTime = -1.0;
    protected static final int NOT_TRIGGERED = -1;
    protected static final int NOT_ANIMATING = -1;
    protected static final int FINISHED_ANIMATING = -2;

    public AnimationController(String string, int n, AnimationController.AnimationStateHandler<T> animationStateHandler) {
        this.name = string;
        this.stateHandler = animationStateHandler;
        this.transitionTicks = n;
    }

    public AnimationController(String string, AnimationController.AnimationStateHandler<T> animationStateHandler) {
        this(string, 0, animationStateHandler);
    }

    public AnimationController(AnimationController.AnimationStateHandler<T> animationStateHandler) {
        this("Default", 0, animationStateHandler);
    }

    public void reset() {
        this.playState = PlayState.STOP;
        this.transitionFromPoint = null;
        this.animationPoint = null;
        this.timeline = null;
        this.currentRawAnimation = null;
        this.timelineTime = 0.0;
        this.triggeredAnimTime = -1.0;
    }

    public String getName() {
        return this.name;
    }

    public boolean isPlayingTriggeredAnimation() {
        return this.triggeredAnimTime >= 0.0 && this.isAnimatingBones();
    }

    public AnimationController<T> setCustomInstructionKeyframeHandler(AnimationController.KeyframeEventHandler<T, CustomInstructionKeyframeData> keyframeEventHandler) {
        this.customKeyframeHandler = keyframeEventHandler;
        return this;
    }

    public Set<Variable> getUsedVariables() {
        if (this.animationPoint == null) {
            return Set.of();
        }
        ReferenceArraySet referenceArraySet = new ReferenceArraySet(this.animationPoint.animation().usedVariables());
        if (this.transitionFromPoint != null) {
            referenceArraySet.addAll(this.transitionFromPoint.animation().usedVariables());
        }
        return referenceArraySet;
    }

    public AnimationPoint getCurrentAnimationPoint() {
        return this.animationPoint;
    }

    public boolean isTransitioning() {
        return this.timeline != null && this.timelineTime >= 0.0 && this.timeline.getStage(this.timelineTime).isTransition();
    }

    public boolean hasAnimationFinished() {
        return this.animationPoint != null && this.animationPoint.hasFinished() && this.timeline != null && this.timelineTime >= this.timeline.lastAnimationEndTime();
    }

    public AnimationController<T> setTransitionTicks(int n) {
        this.transitionTicks = n;
        return this;
    }

    public double getAnimationSpeed() {
        return this.animationSpeed;
    }

    public boolean triggerAnimation(String string) {
        RawAnimation rawAnimation = (RawAnimation)((Map)(Object)this.triggerableAnimations.get()).get(string);
        if (rawAnimation == null) {
            return false;
        }
        this.currentRawAnimation = rawAnimation;
        this.transitionFromPoint = this.getCurrentTimelineTime() > 0.0 ? this.animationPoint : null;
        this.triggeredAnimTime = ClientUtil.getCurrentTick();
        this.animationPoint = null;
        this.playState = PlayState.CONTINUE;
        return true;
    }

    public boolean tryTriggerAnimation(String string) {
        return this.triggerAnimation(string);
    }

    public boolean stopTriggeredAnimation(String string) {
        return this.stopTriggeredAnimation();
    }

    public PlayState getPlayState() {
        return this.playState;
    }

    public AnimationController<T> triggerableAnim(String string, RawAnimation rawAnimation) {
        ((Map)(Object)this.triggerableAnimations.get()).put(string, rawAnimation);
        return this;
    }

    public AnimationController<T> setSoundKeyframeHandler(AnimationController.KeyframeEventHandler<T, SoundKeyframeData> keyframeEventHandler) {
        this.soundKeyframeHandler = keyframeEventHandler;
        return this;
    }

    public boolean isAnimatingBones() {
        return this.animationPoint != null && this.timeline != null && this.timelineTime >= 0.0;
    }

    public int getTransitionTicks() {
        return this.transitionTicks;
    }

    public AnimationController<T> setOverrideEasingType(EasingType easingType) {
        this.easingOverride = easingType;
        return this;
    }

    public AnimationController<T> receiveTriggeredAnimations() {
        this.handlesTriggeredAnimations = true;
        return this;
    }

    public AnimationController<T> additiveAnimations() {
        this.additiveAnimations = true;
        return this;
    }

    public AnimationTimeline getTimeline() {
        return this.timeline;
    }

    public double getCurrentAnimationTime() {
        return this.animationPoint == null ? 0.0 : this.animationPoint.animTime();
    }

    public RawAnimation getCurrentRawAnimation() {
        return this.currentRawAnimation;
    }

    public AnimationController<T> setParticleKeyframeHandler(AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> keyframeEventHandler) {
        this.particleKeyframeHandler = keyframeEventHandler;
        return this;
    }

    private double safetyCheckTickLinearity(double d, double d2) {
        if (d > d2) {
            return d;
        }
        if (d < d2 - 1.0) {
            return d2;
        }
        return (double)((int)d2) + (d - (double)((int)d));
    }

    public boolean isTriggeredAnimation(String string) {
        return this.currentRawAnimation != null && this.currentRawAnimation.equals(((Map)(Object)this.triggerableAnimations.get()).get(string));
    }

    protected boolean checkControllerState(T t, GeoRenderState geoRenderState, AnimatableManager<T> animatableManager, GeoModel<T> geoModel) {
        double d;
        boolean bl = this.playState == PlayState.STOP;
        int n = this.transitionTicks;
        RawAnimation rawAnimation = this.currentRawAnimation;
        double d2 = this.animationSpeed;
        double d3 = this.timelineTime;
        double d4 = d = this.playState == PlayState.PAUSE ? 0.0 : (this.safetyCheckTickLinearity(geoRenderState.getAnimatableAge(), this.lastAnimatableAge) - this.lastAnimatableAge) / 20.0 * d2;
        this.timelineTime = this.timeline == null ? -1.0 : (this.timelineTime < 0.0 ? this.timelineTime : MathHelper.clamp((double)(this.timelineTime + d), (double)0.0, (double)this.timeline.totalTime()));
        this.lastAnimatableAge = geoRenderState.getAnimatableAge();
        if (this.triggeredAnimTime == -1.0 || this.handlesTriggeredAnimations) {
            this.playState = this.stateHandler.handle(new AnimationTest(t, geoRenderState, animatableManager, this));
        }
        if (this.playState != PlayState.STOP) {
            if (this.animationPoint == null || !Objects.equals(rawAnimation, this.currentRawAnimation)) {
                this.initializeNewAnimation(t, geoRenderState, geoModel, d2, n);
            } else if (this.timelineTime >= 0.0 || d < 0.0 == (this.timelineTime == -2.0)) {
                this.progressExistingAnimation(t, geoRenderState, d3, d);
            }
        } else if (!bl) {
            if (this.timelineTime >= 0.0) {
                this.timelineTime = this.timeline == null ? -1.0 : this.timeline.lastAnimationEndTime();
            }
        } else if (this.animationPoint != null && this.timeline != null && (this.timelineTime >= 0.0 || d < 0.0 == (this.timelineTime == -2.0))) {
            this.progressExistingAnimation(t, geoRenderState, d3, d);
        }
        return this.isAnimatingBones();
    }

    private void validateKeyframeListeners(T t) {
        if (this.timeline == null) {
            return;
        }
        for (AnimationTimeline.Stage stage : this.timeline.stages()) {
            if (stage.animation() == null) continue;
            Animation animation = stage.animation();
            Animation.KeyframeMarkers keyframeMarkers = animation.keyframeMarkers();
            if (keyframeMarkers.customInstructions().length > 0 && this.customKeyframeHandler == null) {
                GeckoLibConstants.LOGGER.warn("AnimationController {} for {} loaded animation {} with custom instruction keyframe markers, but no custom instruction handler has been set!", (Object)this.name, (Object)t.getClass().getName(), (Object)animation.name());
            }
            if (keyframeMarkers.sounds().length > 0 && this.soundKeyframeHandler == null) {
                GeckoLibConstants.LOGGER.warn("AnimationController {} for {} loaded animation {} with sound instruction keyframe markers, but no sound instruction handler has been set!", (Object)this.name, (Object)t.getClass().getName(), (Object)animation.name());
            }
            if (keyframeMarkers.particles().length <= 0 || this.particleKeyframeHandler != null) continue;
            GeckoLibConstants.LOGGER.warn("AnimationController {} for {} loaded animation {} with particle instruction keyframe markers, but no particle instruction handler has been set!", (Object)this.name, (Object)t.getClass().getName(), (Object)animation.name());
        }
    }

    public void setAnimationTime(double d) {
        if (d < 0.0) {
            throw new IllegalArgumentException("Attempting to set a negative animation time (" + d + ") on controller " + this.name + "?");
        }
        if (this.animationPoint == null || this.timeline == null || this.timelineTime < 0.0) {
            this.timelineTime = d + (double)((float)this.transitionTicks / 20.0f);
        } else {
            AnimationTimeline.Stage stage = this.timeline.getAnimationStage(this.timelineTime);
            if (stage.animation() != this.animationPoint.animation()) {
                stage = this.timeline.getAnimationStage(0.0);
            }
            this.timelineTime = Math.min(stage.startTime() + d, stage.endTime());
        }
    }

    public ControllerState extractControllerState(T t, GeoRenderState geoRenderState, AnimatableManager<T> animatableManager, MolangQueries.Actor<T> actor, GeoModel<T> geoModel) {
        if (!this.checkControllerState(t, geoRenderState, animatableManager, geoModel)) {
            return null;
        }
        Reference2DoubleOpenHashMap reference2DoubleOpenHashMap = new Reference2DoubleOpenHashMap();
        MolangQueries.buildActorVariables(actor, this.getUsedVariables(), (Reference2DoubleMap<Variable>)reference2DoubleOpenHashMap);
        return new ControllerState(this.animationPoint, this.transitionFromPoint, this.timeline.getTransitionTime(this.timelineTime), this.timeline.getTransitionLength(), this.additiveAnimations, this.easingOverride, geoRenderState, (Reference2DoubleMap<Variable>)reference2DoubleOpenHashMap);
    }

    protected void initializeNewAnimation(T t, GeoRenderState geoRenderState, GeoModel<T> geoModel, double d, int n) {
        if (this.currentRawAnimation == null) {
            return;
        }
        double d2 = this.triggeredAnimTime >= 0.0 ? (this.safetyCheckTickLinearity(ClientUtil.getCurrentTick(), this.triggeredAnimTime) - this.triggeredAnimTime) / 20.0 * d : 0.0;
        this.timeline = AnimationTimeline.create(this.currentRawAnimation, t, geoModel, this.triggeredAnimTime > 0.0 ? n : this.transitionTicks);
        if (this.timeline == null) {
            return;
        }
        this.timelineTime = d2;
        this.animationPoint = this.timeline.createAnimationPoint(this.timelineTime, this.animationPoint, this.easingOverride);
        if (d2 > 0.0) {
            this.timeline.triggerKeyframeMarkersBetween(t, geoRenderState, 0.0, d2, this, this.soundKeyframeHandler, this.particleKeyframeHandler, this.customKeyframeHandler);
        }
        this.validateKeyframeListeners(t);
    }

    protected void progressExistingAnimation(T t, GeoRenderState geoRenderState, double d, double d2) {
        boolean bl;
        if (d2 == 0.0 || this.animationPoint == null || this.timeline == null) {
            return;
        }
        boolean bl2 = bl = d2 < 0.0;
        if (this.timelineTime < 0.0 && bl == (this.timelineTime == -2.0)) {
            this.timelineTime = bl ? this.timeline.totalTime() : 0.0;
        }
        AnimationTimeline.Stage stage = this.timeline.getAnimationStage(d);
        AnimationTimeline.Stage stage2 = this.timeline.getAnimationStage(this.timelineTime);
        if (this.transitionFromPoint != null && stage != stage2) {
            this.transitionFromPoint = null;
        }
        if (stage != stage2 || this.timelineTime >= stage2.endTime()) {
            double d3 = this.timelineTime;
            if (!bl && this.animationPoint.loopType().shouldKeepPlaying((GeoAnimatable)t, this.animationPoint, stage, geoRenderState, this)) {
                this.timeline.triggerKeyframeMarkersBetween(t, geoRenderState, d, stage.endTime(), this, this.soundKeyframeHandler, this.particleKeyframeHandler, this.customKeyframeHandler);
                this.animationPoint = this.timeline.createAnimationPoint(this.timelineTime, this.animationPoint, this.easingOverride);
                this.transitionFromPoint = null;
                if (this.timelineTime > stage.startTime() && this.timelineTime < Math.min(d3, stage.endTime())) {
                    this.timeline.triggerKeyframeMarkersBetween(t, geoRenderState, stage.startTime(), this.timelineTime, this, this.soundKeyframeHandler, this.particleKeyframeHandler, this.customKeyframeHandler);
                }
                return;
            }
            if (d < stage2.endTime() && this.timelineTime < this.timeline.lastAnimationEndTime()) {
                this.transitionFromPoint = d < this.timelineTime ? this.animationPoint : this.timeline.createAnimationPoint(stage2.endTime(), null, this.easingOverride);
            }
        }
        this.timeline.triggerKeyframeMarkersBetween(t, geoRenderState, d, this.timelineTime, this, this.soundKeyframeHandler, this.particleKeyframeHandler, this.customKeyframeHandler);
        this.animationPoint = this.timeline.createAnimationPoint(this.timelineTime, this.animationPoint, this.easingOverride);
        if (d2 > 0.0 ? this.timelineTime >= this.timeline.totalTime() : this.timelineTime == 0.0) {
            this.timelineTime = bl ? -1.0 : -2.0;
        }
    }

    public void setAnimation(RawAnimation rawAnimation) {
        if (rawAnimation == null || rawAnimation.getStageCount() == 0) {
            GeckoLibConstants.LOGGER.warn("Tried to set an empty or null animation on controller {}!", (Object)this.name);
            return;
        }
        if (rawAnimation.equals(this.currentRawAnimation)) {
            return;
        }
        this.currentRawAnimation = rawAnimation;
        this.transitionFromPoint = this.getCurrentTimelineTime() > 0.0 ? this.animationPoint : null;
        this.animationPoint = null;
        this.triggeredAnimTime = -1.0;
    }

    public boolean stopTriggeredAnimation() {
        if (this.triggeredAnimTime == -1.0) {
            return false;
        }
        this.triggeredAnimTime = -1.0;
        if (this.timeline == null || this.animationPoint == null || this.currentRawAnimation == null) {
            this.reset();
        } else {
            this.timelineTime = this.timeline.lastAnimationEndTime();
        }
        return true;
    }

    public void setTimelineTime(double d) {
        this.timelineTime = d;
    }

    public double getCurrentTimelineTime() {
        return this.timelineTime;
    }


    public static interface AnimationStateHandler<A extends GeoAnimatable> {
        public PlayState handle(AnimationTest<A> var1);
    }

    public static interface KeyframeEventHandler<A extends GeoAnimatable, E> {
        public void handle(rtx.nv.api.mods.geckolib.animation.state.KeyFrameEvent<A, E> var1);
    }
}

