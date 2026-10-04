package rtx.nv.api.mods.geckolib.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.SingletonGeoAnimatable;

public final class SyncedSingletonAnimatableCache {
    private static final Map<String, SingletonGeoAnimatable> SYNCED_ANIMATABLES = new ConcurrentHashMap<>();

    private SyncedSingletonAnimatableCache() {}

    public static String getOrCreateId(SingletonGeoAnimatable animatable) {
        String id = animatable.getClass().getName();
        SYNCED_ANIMATABLES.put(id, animatable);
        return id;
    }

    public static GeoAnimatable getSyncedAnimatable(String syncableId) {
        return SYNCED_ANIMATABLES.get(syncableId);
    }

    public static void register(String syncableId, SingletonGeoAnimatable animatable) {
        SYNCED_ANIMATABLES.put(syncableId, animatable);
    }
}
