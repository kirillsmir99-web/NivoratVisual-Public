package rtx.nv.api.mods.geckolib.model;

import net.minecraft.util.Identifier;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;

public class DefaultedEntityGeoModel<T extends GeoAnimatable> extends DefaultedGeoModel<T> {
    protected final String headBone;

    public DefaultedEntityGeoModel(Identifier identifier) {
        this(identifier, null);
    }

    public DefaultedEntityGeoModel(Identifier identifier, String headBone) {
        super(identifier);
        this.headBone = headBone;
    }

    @Override
    protected String subtype() {
        return "entity";
    }
}
