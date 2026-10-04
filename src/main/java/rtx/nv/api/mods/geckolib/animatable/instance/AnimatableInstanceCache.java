package rtx.nv.api.mods.geckolib.animatable.instance;
import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import org.apache.commons.lang3.mutable.MutableObject;
import rtx.nv.api.mods.geckolib.GeckoLibServices;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.SingletonGeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.client.GeoRenderProvider;
import rtx.nv.api.mods.geckolib.animatable.manager.AnimatableManager;
import rtx.nv.api.mods.geckolib.constant.dataticket.DataTicket;

public abstract class AnimatableInstanceCache {
    protected final GeoAnimatable animatable;
    protected final Supplier<GeoRenderProvider> renderProvider;

    public AnimatableInstanceCache(GeoAnimatable geoAnimatable) {
        this.animatable = geoAnimatable;
        this.renderProvider = Suppliers.memoize(() -> {
            if (this.animatable instanceof SingletonGeoAnimatable singletonGeoAnimatable && GeckoLibServices.PLATFORM.isPhysicalClient()) {
                MutableObject<GeoRenderProvider> providerRef = new MutableObject<>(GeoRenderProvider.DEFAULT);
                singletonGeoAnimatable.createGeoRenderer(providerRef::setValue);
                return providerRef.getValue();
            }
            return null;
        });
    }

    public abstract AnimatableManager<?> getManagerForId(long var1);

    public Object getRenderProvider() {
        return this.renderProvider.get();
    }

    public <D> void addDataPoint(long l, DataTicket<D> dataTicket, D d) {
        this.getManagerForId(l).setAnimatableData(dataTicket, d);
    }

    public <D> D getDataPoint(long l, DataTicket<D> dataTicket) {
        return this.getManagerForId(l).getAnimatableData(dataTicket);
    }
}

