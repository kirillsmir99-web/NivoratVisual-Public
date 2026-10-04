package rtx.nv.api.mods.geckolib.loading.json.raw;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.JsonHelper;
import rtx.nv.api.mods.geckolib.util.JsonUtil;

public record LocatorClass(Boolean ignoreInheritedScale, double[] offset, double[] rotation) {
    public static JsonDeserializer<LocatorClass> deserializer() throws JsonParseException {
        return (jsonElement, type, jsonDeserializationContext) -> {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            Boolean bl = JsonUtil.getOptionalBoolean(jsonObject, "ignore_inherited_scale");
            double[] dArray = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"offset", null));
            double[] dArray2 = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"rotation", null));
            return new LocatorClass(bl, dArray, dArray2);
        };
    }
}

