package rtx.nv.api.mods.geckolib.loading.definition.geometry;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.util.JsonUtil;

public record GeometryDescription(String identifier, Float visibleBoundsWidth, Float visibleBoundsHeight, Vec3d visibleBoundsOffset, int textureWidth, int textureHeight) {
    public static JsonDeserializer<GeometryDescription> gsonDeserializer() throws JsonParseException {
        return (jsonElement, type, jsonDeserializationContext) -> {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String string = JsonHelper.getString((JsonObject)jsonObject, (String)"identifier", null);
            Float f = JsonUtil.getOptionalFloat(jsonObject, "visible_bounds_width");
            Float f2 = JsonUtil.getOptionalFloat(jsonObject, "visible_bounds_height");
            Vec3d vec3d = JsonUtil.jsonToVec3((JsonElement)JsonHelper.getArray((JsonObject)jsonObject, (String)"visible_bounds_offset", null));
            int n = JsonHelper.getInt((JsonObject)jsonObject, (String)"texture_width", (int)16);
            int n2 = JsonHelper.getInt((JsonObject)jsonObject, (String)"texture_height", (int)16);
            if (!jsonObject.has("texture_width") || !jsonObject.has("texture_height")) {
                GeckoLibConstants.LOGGER.warn("GeckoLib model {} does not have texture dimensions specified, likely an invalid geometry json!", (Object)string);
            }
            return new GeometryDescription(string == null ? String.valueOf(jsonObject.hashCode()) : string, f, f2, vec3d, n, n2);
        };
    }
}

