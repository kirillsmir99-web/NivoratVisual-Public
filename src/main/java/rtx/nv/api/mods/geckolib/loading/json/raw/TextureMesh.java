package rtx.nv.api.mods.geckolib.loading.json.raw;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.JsonHelper;
import rtx.nv.api.mods.geckolib.util.JsonUtil;

public record TextureMesh(double[] localPivot, double[] position, double[] rotation, double[] scale, String texture) {
    public static JsonDeserializer<TextureMesh> deserializer() throws JsonParseException {
        return (jsonElement, type, jsonDeserializationContext) -> {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            double[] dArray = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"local_pivot", null));
            double[] dArray2 = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"position", null));
            double[] dArray3 = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"rotation", null));
            double[] dArray4 = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"scale", null));
            String string = JsonHelper.getString((JsonObject)jsonObject, (String)"texture", null);
            return new TextureMesh(dArray, dArray2, dArray3, dArray4, string);
        };
    }
}

