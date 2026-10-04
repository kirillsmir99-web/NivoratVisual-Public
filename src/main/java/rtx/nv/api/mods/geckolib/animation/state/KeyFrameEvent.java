package rtx.nv.api.mods.geckolib.animation.state;

import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animation.AnimationController;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;

public class KeyFrameEvent<T extends GeoAnimatable, E> {
    private final T animatable;
    private final double animationTick;
    private final AnimationController<T> controller;
    private final E event;
    private final GeoRenderState renderState;

    public KeyFrameEvent(T animatable, double animationTick, AnimationController<T> controller, E event) {
        this.animatable = animatable;
        this.animationTick = animationTick;
        this.controller = controller;
        this.event = event;
        this.renderState = null;
    }

    public KeyFrameEvent(T animatable, GeoRenderState renderState, AnimationController<T> controller, E event) {
        this.animatable = animatable;
        this.animationTick = renderState != null ? renderState.getAnimatableAge() : 0.0;
        this.controller = controller;
        this.event = event;
        this.renderState = renderState;
    }

    public T getAnimatable() {
        return this.animatable;
    }

    public double getAnimationTick() {
        return this.animationTick;
    }

    public AnimationController<T> getController() {
        return this.controller;
    }

    public E getEvent() {
        return this.event;
    }

    public GeoRenderState getRenderState() {
        return this.renderState;
    }
}
