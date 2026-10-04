package rtx.nv.api.mods.geckolib.loading.json.raw;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.JsonHelper;
import rtx.nv.api.mods.geckolib.loading.json.raw.PolysUnion;
import rtx.nv.api.mods.geckolib.util.JsonUtil;

public record PolyMesh(Boolean normalizedUVs, double[] normals, PolysUnion polysUnion, double[] positions, double[] uvs) {
    public static JsonDeserializer<PolyMesh> deserializer() throws JsonParseException {
        return (jsonElement, type, jsonDeserializationContext) -> {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            Boolean bl = JsonUtil.getOptionalBoolean(jsonObject, "normalized_uvs");
            double[] dArray = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"normals", null));
            PolysUnion polysUnion = (PolysUnion)JsonHelper.deserialize((JsonObject)jsonObject, (String)"polys", null, (JsonDeserializationContext)jsonDeserializationContext, PolysUnion.class);
            double[] dArray2 = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"positions", null));
            double[] dArray3 = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"uvs", null));
            return new PolyMesh(bl, dArray, polysUnion, dArray2, dArray3);
        };
    }
}

