package rtx.nv.api.mods.geckolib.animation.state;
import java.util.Objects;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.manager.AnimatableManager;
import rtx.nv.api.mods.geckolib.animation.AnimationController;
import rtx.nv.api.mods.geckolib.animation.RawAnimation;
import rtx.nv.api.mods.geckolib.animation.object.PlayState;
import rtx.nv.api.mods.geckolib.constant.DataTickets;
import rtx.nv.api.mods.geckolib.constant.dataticket.DataTicket;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;

public record AnimationTest<T extends GeoAnimatable>(T animatable, GeoRenderState renderState, AnimatableManager<T> manager, AnimationController<T> controller) {
    public <D> D getData(DataTicket<D> dataTicket) {
        return this.renderState.getGeckolibData(dataTicket);
    }

    public boolean hasData(DataTicket<?> dataTicket) {
        return this.renderState.hasGeckolibData(dataTicket);
    }

    public <D> D getDataOrDefault(DataTicket<D> dataTicket, D d) {
        D d2 = this.getData(dataTicket);
        return d2 != null || this.renderState.hasGeckolibData(dataTicket) ? d2 : d;
    }

    public void setAnimation(RawAnimation rawAnimation) {
        this.controller.setAnimation(rawAnimation);
    }

    public boolean isMoving() {
        return this.renderState.getOrDefaultGeckolibData(DataTickets.IS_MOVING, false);
    }

    public PlayState setAndContinue(RawAnimation rawAnimation) {
        this.controller.setAnimation(rawAnimation);
        return PlayState.CONTINUE;
    }

    public boolean isCurrentAnimation(RawAnimation rawAnimation) {
        return Objects.equals(this.controller.getCurrentRawAnimation(), rawAnimation);
    }

    public boolean isCurrentAnimationStage(String string) {
        return this.controller.getCurrentAnimationPoint() != null && this.controller.getCurrentAnimationPoint().animation().name().equals(string);
    }

    public void setControllerSpeed(float f) {
        this.controller.animationSpeed = f;
    }
}

