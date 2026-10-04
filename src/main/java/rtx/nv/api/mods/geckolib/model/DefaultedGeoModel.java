package rtx.nv.api.mods.geckolib.model;

import net.minecraft.util.Identifier;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;

public abstract class DefaultedGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    private final Identifier modelPath;
    private final Identifier texturePath;
    private final Identifier animationsPath;

    public DefaultedGeoModel(Identifier assetSubpath) {
        this.modelPath = this.buildFormattedModelPath(assetSubpath);
        this.texturePath = this.buildFormattedTexturePath(assetSubpath);
        this.animationsPath = this.buildFormattedAnimationPath(assetSubpath);
    }

    protected abstract String subtype();

    public Identifier buildFormattedModelPath(Identifier identifier) {
        return identifier.withPrefixedPath(this.subtype() + "/");
    }

    public Identifier buildFormattedTexturePath(Identifier identifier) {
        return identifier.withPrefixedPath(this.subtype() + "/");
    }

    public Identifier buildFormattedAnimationPath(Identifier identifier) {
        return identifier.withPrefixedPath(this.subtype() + "/");
    }

    @Override
    public Identifier getModelResource(GeoRenderState geoRenderState) {
        return this.modelPath;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState geoRenderState) {
        return this.texturePath;
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return this.animationsPath;
    }
}
