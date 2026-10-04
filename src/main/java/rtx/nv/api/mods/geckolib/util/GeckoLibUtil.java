package rtx.nv.api.mods.geckolib.util;
import java.util.Objects;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentType;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.entity.Entity;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.instance.AnimatableInstanceCache;
import rtx.nv.api.mods.geckolib.animatable.instance.InstancedAnimatableInstanceCache;
import rtx.nv.api.mods.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import rtx.nv.api.mods.geckolib.animation.object.EasingType;
import rtx.nv.api.mods.geckolib.animation.object.LoopType;
import rtx.nv.api.mods.geckolib.constant.DataTickets;
import rtx.nv.api.mods.geckolib.constant.dataticket.SerializableDataTicket;
import rtx.nv.api.mods.geckolib.loading.object.BakedModelFactory;

public final class GeckoLibUtil {
    private GeckoLibUtil() {
    }

    public static boolean areComponentsMatchingIgnoringGeckoLibId(MergedComponentMap mergedComponentMap, MergedComponentMap mergedComponentMap2) {
        boolean bl;
        MergedComponentMap mergedComponentMap3;
        ComponentType<Long> componentType = GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get();
        boolean bl2 = false;
        if (mergedComponentMap.contains(componentType)) {
            mergedComponentMap3 = mergedComponentMap;
            bl = mergedComponentMap3.copyOnWrite;
            mergedComponentMap = mergedComponentMap.copy();
            mergedComponentMap.remove(componentType);
            mergedComponentMap.copyOnWrite = bl;
            bl2 = true;
        }
        if (mergedComponentMap2.contains(componentType)) {
            mergedComponentMap3 = mergedComponentMap2;
            bl = mergedComponentMap3.copyOnWrite;
            mergedComponentMap2 = mergedComponentMap2.copy();
            mergedComponentMap2.remove(componentType);
            mergedComponentMap2.copyOnWrite = bl;
            bl2 = true;
        }
        return bl2 && Objects.equals(mergedComponentMap, mergedComponentMap2);
    }

    public static AnimatableInstanceCache createInstanceCache(GeoAnimatable geoAnimatable) {
        AnimatableInstanceCache animatableInstanceCache = geoAnimatable.animatableCacheOverride();
        return animatableInstanceCache != null ? animatableInstanceCache : GeckoLibUtil.createInstanceCache(geoAnimatable, !(geoAnimatable instanceof Entity) && !(geoAnimatable instanceof BlockEntity));
    }

    public static AnimatableInstanceCache createInstanceCache(GeoAnimatable geoAnimatable, boolean bl) {
        AnimatableInstanceCache animatableInstanceCache = geoAnimatable.animatableCacheOverride();
        if (animatableInstanceCache != null) {
            return animatableInstanceCache;
        }
        return bl ? new SingletonAnimatableInstanceCache(geoAnimatable) : new InstancedAnimatableInstanceCache(geoAnimatable);
    }

    public static synchronized LoopType addCustomLoopType(String string, LoopType loopType) {
        return LoopType.register(string, loopType);
    }

    public static synchronized <D> SerializableDataTicket<D> addDataTicket(SerializableDataTicket<D> serializableDataTicket) {
        return DataTickets.registerSerializable(serializableDataTicket);
    }

    public static synchronized EasingType addCustomEasingType(String string, EasingType easingType) {
        return EasingType.register(string, easingType);
    }

    public static synchronized void addCustomBakedModelFactory(String string, BakedModelFactory bakedModelFactory) {
        BakedModelFactory.register(string, bakedModelFactory);
    }
}

