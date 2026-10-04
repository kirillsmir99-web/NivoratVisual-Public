package rtx.nv.api.mods.geckolib.animatable.instance;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.instance.AnimatableInstanceCache;
import rtx.nv.api.mods.geckolib.animatable.manager.AnimatableManager;

public class InstancedAnimatableInstanceCache
extends AnimatableInstanceCache {
    protected final Supplier<AnimatableManager<?>> manager = Suppliers.memoize(() -> new AnimatableManager(this.animatable));

    public InstancedAnimatableInstanceCache(GeoAnimatable geoAnimatable) {
        super(geoAnimatable);
    }

    @Override
    public AnimatableManager<?> getManagerForId(long l) {
        return (AnimatableManager)(Object)this.manager.get();
    }
}

