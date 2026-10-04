package rtx.nv.api.modules.impl.Visuals.custompet.model;
import net.minecraft.util.Identifier;
import rtx.nv.api.mods.geckolib.constant.dataticket.DataTicket;
import rtx.nv.api.mods.geckolib.model.GeoModel;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.modules.impl.Visuals.custompet.CustomPetVariant;
import rtx.nv.api.modules.impl.Visuals.custompet.entity.CustomPetEntity;
import rtx.nv.api.modules.impl.Visuals.custompet.model.FrogModel;
import rtx.nv.api.modules.impl.Visuals.custompet.model.OwlModel;
import rtx.nv.api.modules.impl.Visuals.custompet.model.RobotModel;

public class CustomPetModel
extends GeoModel<CustomPetEntity> {
    public static final DataTicket<Boolean> UMBRELLA = DataTicket.create("nv_custom_pet.umbrella", Boolean.class);
    public static final DataTicket<Boolean> AIRBORNE = DataTicket.create("nv_custom_pet.airborne", Boolean.class);
    public static final DataTicket<String> VARIANT = DataTicket.create("nv_custom_pet.variant", String.class);
    public static final DataTicket<Integer> ROBOT_TYPE = DataTicket.create("nv_custom_pet.robot_type", Integer.class);
    public static final DataTicket<Boolean> OWL = DataTicket.create("nv_custom_pet.owl", Boolean.class);
    private final FantasyPetModel fantasy = new FantasyPetModel();
    private final FrogModel frog = new FrogModel();
    private final RobotModel robot = new RobotModel();
    private final OwlModel owl = new OwlModel();

    private GeoModel<CustomPetEntity> pick(GeoRenderState geoRenderState) {
        if (CustomPetVariant.fromSerializedName(geoRenderState.getOrDefaultGeckolibData(VARIANT, "SPIRIT")).isFantasy()) return fantasy;
        if (Boolean.TRUE.equals(geoRenderState.getGeckolibData(OWL))) {
            return this.owl;
        }
        return CustomPetVariant.ROBOT.name().equals(geoRenderState.getGeckolibData(VARIANT)) ? this.robot : this.frog;
    }

    public void addAdditionalStateData(CustomPetEntity customPetEntity, Object object, GeoRenderState geoRenderState) {
        geoRenderState.addGeckolibData(UMBRELLA, customPetEntity.shouldUseUmbrella());
        geoRenderState.addGeckolibData(AIRBORNE, customPetEntity.isAirborneMode());
        geoRenderState.addGeckolibData(VARIANT, customPetEntity.getPetVariant().name());
        geoRenderState.addGeckolibData(ROBOT_TYPE, customPetEntity.getRobotType());
        geoRenderState.addGeckolibData(OWL, customPetEntity.isOwl());
    }

    @Override
    public Identifier getTextureResource(GeoRenderState geoRenderState) {
        return this.pick(geoRenderState).getTextureResource(geoRenderState);
    }

    @Override
    public Identifier getModelResource(GeoRenderState geoRenderState) {
        return this.pick(geoRenderState).getModelResource(geoRenderState);
    }

    public Identifier getAnimationResource(CustomPetEntity customPetEntity) {
        if (customPetEntity.getPetVariant().isFantasy()) return fantasy.getAnimationResource(customPetEntity);
        if (customPetEntity.isOwl()) {
            return this.owl.getAnimationResource(customPetEntity);
        }
        return customPetEntity.getPetVariant().isRobot() ? this.robot.getAnimationResource(customPetEntity) : this.frog.getAnimationResource(customPetEntity);
    }
}

