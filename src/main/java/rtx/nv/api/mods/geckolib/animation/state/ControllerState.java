package rtx.nv.api.mods.geckolib.animation.state;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import rtx.nv.api.mods.geckolib.animation.object.EasingType;
import rtx.nv.api.mods.geckolib.animation.state.AnimationPoint;
import rtx.nv.api.mods.geckolib.loading.math.value.Variable;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;

public record ControllerState(AnimationPoint animationPoint, AnimationPoint prevAnimationPoint, double transitionTime, int transitionTicks, boolean additive, EasingType easingOverride, GeoRenderState renderState, Reference2DoubleMap<Variable> queryValues) {
    public float partialTick() {
        return this.renderState.getPartialTick();
    }

    public double getQueryValue(Variable variable) {
        return this.queryValues.getDouble((Object)variable);
    }

    public void setVariable(Variable variable, double value) {
        if (this.queryValues != null) {
            this.queryValues.put(variable, value);
        }
    }

    public void setVariable(String name, double value) {
        if (this.queryValues != null) {
            this.queryValues.put(new Variable(name, value), value);
        }
    }
}

