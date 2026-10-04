package rtx.nv.api.modules.impl.Visuals.custompet.model;
import net.minecraft.util.Identifier;
import rtx.nv.api.mods.geckolib.model.GeoModel;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.modules.impl.Visuals.custompet.entity.CustomPetEntity;

public final class FrogModel
extends GeoModel<CustomPetEntity> {
    private static final Identifier MODEL = Identifier.of((String)"nv", (String)"frog/custom_pet");
    private static final Identifier ANIMATIONS = Identifier.of((String)"nv", (String)"frog/custom_pet");
    private static final Identifier TEXTURE = Identifier.of((String)"nv", (String)"textures/entity/frog/custom_pet.png");

    @Override
    public Identifier getTextureResource(GeoRenderState geoRenderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getModelResource(GeoRenderState geoRenderState) {
        return MODEL;
    }

    public Identifier getAnimationResource(CustomPetEntity customPetEntity) {
        return ANIMATIONS;
    }
}

