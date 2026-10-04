package rtx.nv.api.mods.geckolib.loading.json.raw;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.mods.geckolib.loading.definition.geometry.GeometryDescription;

public record ModelProperties(String identifier, Float visibleBoundsWidth, Float visibleBoundsHeight, Vec3d visibleBoundsOffset, int textureWidth, int textureHeight) {
    public static ModelProperties fromDescription(GeometryDescription geometryDescription) {
        return new ModelProperties(geometryDescription.identifier(), geometryDescription.visibleBoundsWidth(), geometryDescription.visibleBoundsHeight(), geometryDescription.visibleBoundsOffset(), geometryDescription.textureWidth(), geometryDescription.textureHeight());
    }
}

