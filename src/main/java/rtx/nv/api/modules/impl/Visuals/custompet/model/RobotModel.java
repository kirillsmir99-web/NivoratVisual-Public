package rtx.nv.api.modules.impl.Visuals.custompet.model;
import net.minecraft.util.Identifier;
import rtx.nv.api.mods.geckolib.model.GeoModel;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.modules.impl.Visuals.custompet.entity.CustomPetEntity;
import rtx.nv.api.modules.impl.Visuals.custompet.model.CustomPetModel;

public final class RobotModel
extends GeoModel<CustomPetEntity> {
    public static final int TYPE_COUNT = 4;
    private static final Identifier MODEL = Identifier.of((String)"nv", (String)"robot/robot");
    private static final Identifier ANIMATIONS = Identifier.of((String)"nv", (String)"robot/robot");
    private static final Identifier[] TEXTURES = RobotModel.buildTextures();

    @Override
    public Identifier getTextureResource(GeoRenderState geoRenderState) {
        Integer n = geoRenderState.getGeckolibData(CustomPetModel.ROBOT_TYPE);
        int n2 = n == null ? 0 : Math.floorMod(n, 4);
        return TEXTURES[n2];
    }

    @Override
    public Identifier getModelResource(GeoRenderState geoRenderState) {
        return MODEL;
    }

    public Identifier getAnimationResource(CustomPetEntity customPetEntity) {
        return ANIMATIONS;
    }

    private static Identifier[] buildTextures() {
        Identifier[] identifierArray = new Identifier[4];
        for (int i = 0; i < 4; ++i) {
            identifierArray[i] = Identifier.of((String)"nv", (String)("textures/entity/robot/type_" + (i + 1) + ".png"));
        }
        return identifierArray;
    }
}

