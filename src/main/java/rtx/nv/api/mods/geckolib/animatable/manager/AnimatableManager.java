package rtx.nv.api.mods.geckolib.animatable.manager;

import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animation.AnimationController;
import rtx.nv.api.mods.geckolib.constant.dataticket.DataTicket;

public class AnimatableManager<T extends GeoAnimatable> {
    protected final Map<String, AnimationController<T>> animationControllers;
    protected final Supplier<Map<DataTicket<?>, Object>> animatableInstanceData = Suppliers.memoize(Reference2ObjectOpenHashMap::new);
    private double lastResetTime = -1.0;
    private double firstRenderTime = -1.0;

    public AnimatableManager(GeoAnimatable geoAnimatable) {
        ControllerRegistrar controllerRegistrar = new ControllerRegistrar();
        geoAnimatable.registerControllers(controllerRegistrar);
        this.animationControllers = controllerRegistrar.build();
    }

    public Map<String, AnimationController<T>> getAnimationControllers() {
        return this.animationControllers;
    }

    public <D> void setData(DataTicket<D> dataTicket, D d) {
        this.animatableInstanceData.get().put(dataTicket, d);
    }

    public <D> void setAnimatableData(DataTicket<D> dataTicket, D d) {
        this.setData(dataTicket, d);
    }

    @SuppressWarnings("unchecked")
    public <D> D getData(DataTicket<D> dataTicket) {
        return (D)this.animatableInstanceData.get().get(dataTicket);
    }

    public <D> D getAnimatableData(DataTicket<D> dataTicket) {
        return this.getData(dataTicket);
    }

    public void tryReset() {
        this.lastResetTime = -1.0;
    }

    public boolean isStarted() {
        return this.lastResetTime != -1.0;
    }

    public boolean tryTriggerAnimation(String controllerName, String animName) {
        AnimationController<T> controller = this.animationControllers.get(controllerName);
        return controller != null && controller.tryTriggerAnimation(animName);
    }

    public boolean tryTriggerAnimation(String animName) {
        for (AnimationController<T> controller : this.animationControllers.values()) {
            if (controller.tryTriggerAnimation(animName)) {
                return true;
            }
        }
        return false;
    }

    public boolean stopTriggeredAnimation(String controllerName, String animName) {
        AnimationController<T> controller = this.animationControllers.get(controllerName);
        return controller != null && controller.stopTriggeredAnimation(animName);
    }

    public boolean stopTriggeredAnimation(String animName) {
        for (AnimationController<T> controller : this.animationControllers.values()) {
            if (controller.stopTriggeredAnimation(animName)) {
                return true;
            }
        }
        return false;
    }

    public double getFirstRenderTick() {
        return this.firstRenderTime;
    }

    public void markRenderedAt(double d) {
        if (this.firstRenderTime == -1.0) {
            this.firstRenderTime = d;
        }
        this.lastResetTime = d;
    }

    public static class ControllerRegistrar<T extends GeoAnimatable> {
        private final List<AnimationController<T>> controllers = new ObjectArrayList<>();

        public ControllerRegistrar<T> add(AnimationController<T> ... animationControllers) {
            for (AnimationController<T> controller : animationControllers) {
                this.controllers.add(controller);
            }
            return this;
        }

        public ControllerRegistrar<T> remove(String string) {
            this.controllers.removeIf(animationController -> animationController.getName().equals(string));
            return this;
        }

        public Map<String, AnimationController<T>> build() {
            Object2ObjectOpenHashMap map = new Object2ObjectOpenHashMap(this.controllers.size());
            for (AnimationController<T> animationController : this.controllers) {
                map.put(animationController.getName(), animationController);
            }
            return map;
        }
    }
}
