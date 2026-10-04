package rtx.nv.api.mods.geckolib.model;
import net.minecraft.util.Identifier;
import rtx.nv.api.mods.geckolib.cache.GeckoLibResources;
import rtx.nv.api.mods.geckolib.cache.animation.Animation;
import rtx.nv.api.mods.geckolib.cache.model.BakedGeoModel;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;

public abstract class GeoModel<T extends rtx.nv.api.mods.geckolib.animatable.GeoAnimatable> {
    public void addAdditionalStateData(T t, Object object, GeoRenderState geoRenderState) {
    }

    public abstract Identifier getTextureResource(GeoRenderState var1);

    public Identifier[] getAnimationResourceFallbacks(T t) {
        return new Identifier[0];
    }

    public BakedGeoModel getBakedModel(Identifier identifier) {
        return GeckoLibResources.getBakedModels().getModel(identifier);
    }

    public abstract Identifier getModelResource(GeoRenderState var1);

    public Animation getBakedAnimation(T t, String string) throws RuntimeException {
        Identifier identifier = this.getAnimationResource(t);
        Identifier[] identifierArray = this.getAnimationResourceFallbacks(t);
        return GeckoLibResources.getBakedAnimations().getAnimation(identifier, identifierArray, string);
    }

    public abstract Identifier getAnimationResource(T var1);
}

