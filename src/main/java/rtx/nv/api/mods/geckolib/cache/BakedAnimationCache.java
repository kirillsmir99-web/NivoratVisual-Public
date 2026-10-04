package rtx.nv.api.mods.geckolib.cache;
import java.util.Map;
import net.minecraft.util.Identifier;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.cache.animation.Animation;
import rtx.nv.api.mods.geckolib.loading.object.BakedAnimations;

public record BakedAnimationCache(Map<Identifier, BakedAnimations> cache) {
    public Animation getAnimation(Identifier identifier, Identifier[] identifierArray, String string) {
        BakedAnimations bakedAnimations = null;
        for (int i = -1; i < identifierArray.length; ++i) {
            Identifier identifier2 = i == -1 ? identifier : identifierArray[i];
            bakedAnimations = this.cache.get(identifier2);
            if (bakedAnimations == null) {
                Identifier stripped = BakedAnimationCache.stripLegacyPath(identifier2);
                if (!stripped.equals(identifier2)) {
                    GeckoLibConstants.LOGGER.error("Superfluous prefix or suffix found in animation resource path: '{}'. Should be '{}'", (Object)identifier2, stripped);
                    identifier = stripped;
                    bakedAnimations = this.cache.get(identifier);
                }
            }
            if (bakedAnimations == null) continue;
            Animation anim = bakedAnimations.getAnimation(string);
            if (anim != null) {
                return anim;
            }
        }
        if (bakedAnimations == null) {
            throw new IllegalArgumentException("Unable to find animation file '" + String.valueOf(identifier) + "'");
        }
        GeckoLibConstants.LOGGER.error("Unable to find animation: '{}' in animation file '{}'", (Object)string, (Object)identifier);
        return null;
    }

    private static Identifier stripLegacyPath(Identifier identifier) {
        String string = identifier.getPath();
        if (string.startsWith("geckolib/")) {
            string = string.substring(9);
        }
        if (string.startsWith("animations/")) {
            string = string.substring(11);
        }
        if (string.endsWith(".json")) {
            string = string.substring(0, string.length() - 5);
        }
        if (string.endsWith(".animations")) {
            string = string.substring(0, string.length() - 10);
        }
        return !string.equals(identifier.getPath()) ? identifier.withPath(string) : identifier;
    }
}

