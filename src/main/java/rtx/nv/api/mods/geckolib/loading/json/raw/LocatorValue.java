package rtx.nv.api.mods.geckolib.loading.json.raw;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import rtx.nv.api.mods.geckolib.loading.json.raw.LocatorClass;
import rtx.nv.api.mods.geckolib.util.JsonUtil;

public record LocatorValue(LocatorClass locatorClass, double[] values) {
    public static JsonDeserializer<LocatorValue> deserializer() throws JsonParseException {
        return (jsonElement, type, jsonDeserializationContext) -> {
            if (jsonElement.isJsonArray()) {
                return new LocatorValue(null, JsonUtil.jsonArrayToDoubleArray(jsonElement.getAsJsonArray()));
            }
            if (jsonElement.isJsonObject()) {
                return new LocatorValue((LocatorClass)jsonDeserializationContext.deserialize((JsonElement)jsonElement.getAsJsonObject(), LocatorClass.class), new double[0]);
            }
            throw new JsonParseException("Invalid format for LocatorValue in json");
        };
    }
}

