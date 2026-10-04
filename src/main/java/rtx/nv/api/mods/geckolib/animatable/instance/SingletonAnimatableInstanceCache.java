package rtx.nv.api.mods.geckolib.animatable.instance;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.manager.AnimatableManager;

public class SingletonAnimatableInstanceCache extends AnimatableInstanceCache {
    protected final Long2ObjectMap<AnimatableManager<?>> managers = new Long2ObjectOpenHashMap<>();

    public SingletonAnimatableInstanceCache(GeoAnimatable geoAnimatable) {
        super(geoAnimatable);
    }

    @Override
    public AnimatableManager<?> getManagerForId(long l) {
        return this.managers.computeIfAbsent(l, id -> new AnimatableManager<>(this.animatable));
    }
}
