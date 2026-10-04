package rtx.nv.api.mods.geckolib.loading.json.raw;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import net.minecraft.util.JsonHelper;
import rtx.nv.api.mods.geckolib.loading.json.raw.UVFaces;
import rtx.nv.api.mods.geckolib.loading.json.raw.UVUnion;
import rtx.nv.api.mods.geckolib.util.JsonUtil;

public record Cube(Double inflate, Boolean mirror, double[] origin, double[] pivot, double[] rotation, double[] size, UVUnion uv) {
    public static JsonDeserializer<Cube> deserializer() throws JsonParseException {
        return (jsonElement, type, jsonDeserializationContext) -> {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            Double d = JsonUtil.getOptionalDouble(jsonObject, "inflate");
            Boolean bl = JsonUtil.getOptionalBoolean(jsonObject, "mirror");
            double[] dArray = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"origin", null));
            double[] dArray2 = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"pivot", null));
            double[] dArray3 = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"rotation", null));
            double[] dArray4 = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"size", null));
            UVUnion uVUnion = JsonHelper.deserialize(jsonObject, "uv", new UVUnion(Either.left(new double[]{0.0, 0.0})), jsonDeserializationContext, UVUnion.class);
            return new Cube(d, bl, dArray, dArray2, dArray3, dArray4, uVUnion);
        };
    }
}

