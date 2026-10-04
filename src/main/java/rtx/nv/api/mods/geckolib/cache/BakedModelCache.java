package rtx.nv.api.mods.geckolib.cache;
import java.util.Map;
import net.minecraft.util.Identifier;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.cache.model.BakedGeoModel;

public record BakedModelCache(Map<Identifier, BakedGeoModel> cache) {
    private static Identifier stripLegacyPath(Identifier identifier) {
        String string = identifier.getPath();
        if (string.startsWith("geckolib/")) {
            string = string.substring(9);
        }
        if (string.startsWith("models/")) {
            string = string.substring(7);
        }
        if (string.endsWith(".json")) {
            string = string.substring(0, string.length() - 5);
        }
        if (string.endsWith(".geo")) {
            string = string.substring(0, string.length() - 4);
        }
        return !string.equals(identifier.getPath()) ? identifier.withPath(string) : identifier;
    }

    public BakedGeoModel getModel(Identifier identifier) {
        BakedGeoModel bakedGeoModel = this.cache.get(identifier);
        if (bakedGeoModel == null) {
            Identifier identifier2 = BakedModelCache.stripLegacyPath(identifier);
            if (!identifier.equals((Object)identifier2)) {
                GeckoLibConstants.LOGGER.error("Superfluous prefix or suffix found in model resource path: '{}'. Should be '{}'", (Object)identifier, (Object)identifier2);
                bakedGeoModel = this.cache.get(identifier2);
            }
            if (bakedGeoModel == null) {
                throw new IllegalArgumentException("Unable to find model file: " + String.valueOf(identifier));
            }
        }
        return bakedGeoModel;
    }
}

