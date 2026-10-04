package rtx.nv.api.mods.geckolib.loading.json.raw;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.JsonHelper;
import rtx.nv.api.mods.geckolib.loading.definition.geometry.GeometryDescription;
import rtx.nv.api.mods.geckolib.loading.json.raw.Bone;
import rtx.nv.api.mods.geckolib.util.JsonUtil;

public record MinecraftGeometry(Bone[] bones, String cape, GeometryDescription geometryDescription) {
    public static JsonDeserializer<MinecraftGeometry> deserializer() throws JsonParseException {
        return (jsonElement, type, jsonDeserializationContext) -> {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            Bone[] boneArray = JsonUtil.jsonArrayToObjectArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"bones", null), jsonDeserializationContext, Bone.class);
            String string = JsonHelper.getString((JsonObject)jsonObject, (String)"cape", null);
            GeometryDescription geometryDescription = (GeometryDescription)JsonHelper.deserialize((JsonObject)jsonObject, (String)"description", null, (JsonDeserializationContext)jsonDeserializationContext, GeometryDescription.class);
            if (boneArray == null) {
                boneArray = new Bone[]{};
            }
            return new MinecraftGeometry(boneArray, string, geometryDescription);
        };
    }
}

