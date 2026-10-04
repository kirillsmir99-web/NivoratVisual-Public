package rtx.nv.api.mods.geckolib.loading.json.raw;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.JsonHelper;
import rtx.nv.api.mods.geckolib.loading.json.raw.MinecraftGeometry;
import rtx.nv.api.mods.geckolib.util.JsonUtil;

public record Model(String formatVersion, MinecraftGeometry[] minecraftGeometry) {
    public static JsonDeserializer<Model> deserializer() throws JsonParseException {
        return (jsonElement, type, jsonDeserializationContext) -> {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String string = jsonObject.get("format_version").getAsString();
            MinecraftGeometry[] minecraftGeometryArray = JsonUtil.jsonArrayToObjectArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"minecraft:geometry", null), jsonDeserializationContext, MinecraftGeometry.class);
            if (minecraftGeometryArray == null) {
                minecraftGeometryArray = new MinecraftGeometry[]{};
            }
            return new Model(string, minecraftGeometryArray);
        };
    }
}

