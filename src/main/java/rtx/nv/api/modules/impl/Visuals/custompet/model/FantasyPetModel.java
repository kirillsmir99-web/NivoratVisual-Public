package rtx.nv.api.modules.impl.Visuals.custompet.model;

import net.minecraft.util.Identifier;
import rtx.nv.api.mods.geckolib.model.GeoModel;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.modules.impl.Visuals.custompet.CustomPetVariant;
import rtx.nv.api.modules.impl.Visuals.custompet.entity.CustomPetEntity;

/** Original NV companions; identifiers are reused across frames and resource reloads. */
public final class FantasyPetModel extends GeoModel<CustomPetEntity> {
    private static final Identifier[] MODELS = {id("pets/spirit"), id("pets/dragon"), id("pets/moth"), id("pets/mascot")};
    private static final Identifier[] TEXTURES = {id("textures/entity/pets/spirit.png"), id("textures/entity/pets/dragon.png"), id("textures/entity/pets/moth.png"), id("textures/entity/pets/mascot.png")};

    private static Identifier id(String path) { return Identifier.of("nv", path); }
    private static int index(CustomPetVariant variant) {
        return switch (variant) { case DRAGON -> 1; case MOTH -> 2; case MASCOT -> 3; default -> 0; };
    }
    private int index(GeoRenderState state) {
        return index(CustomPetVariant.fromSerializedName(state.getOrDefaultGeckolibData(CustomPetModel.VARIANT, "SPIRIT")));
    }
    @Override public Identifier getTextureResource(GeoRenderState state) { return TEXTURES[index(state)]; }
    @Override public Identifier getModelResource(GeoRenderState state) { return MODELS[index(state)]; }
    @Override public Identifier getAnimationResource(CustomPetEntity entity) { return MODELS[index(entity.getPetVariant())]; }
}
