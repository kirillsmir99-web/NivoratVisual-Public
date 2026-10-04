package rtx.nv.api.mods.geckolib.loading.json.raw;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import rtx.nv.api.mods.geckolib.util.JsonUtil;

public record PolysUnion(double[][][] union, Type type) {
    private static double[][][] makeSizedMatrix(JsonArray jsonArray) {
        JsonArray jsonArray2 = !jsonArray.isEmpty() ? jsonArray.get(0).getAsJsonArray() : null;
        JsonArray jsonArray3 = jsonArray2 != null && !jsonArray2.isEmpty() ? jsonArray2.get(0).getAsJsonArray() : null;
        int n = jsonArray2 != null ? jsonArray2.size() : 0;
        int n2 = jsonArray3 != null ? jsonArray3.size() : 0;
        return new double[jsonArray.size()][n][n2];
    }

    public static JsonDeserializer<PolysUnion> deserializer() throws JsonParseException {
        return (jsonElement, type, jsonDeserializationContext) -> {
            if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString()) {
                return new PolysUnion(new double[0][0][0], (Type)jsonDeserializationContext.deserialize((JsonElement)jsonElement.getAsJsonPrimitive(), Type.class));
            }
            if (jsonElement.isJsonArray()) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                double[][][] dArray = PolysUnion.makeSizedMatrix(jsonArray);
                for (int i = 0; i < jsonArray.size(); ++i) {
                    JsonArray jsonArray2 = jsonArray.get(i).getAsJsonArray();
                    for (int j = 0; j < jsonArray2.size(); ++j) {
                        JsonArray jsonArray3 = jsonArray2.get(j).getAsJsonArray();
                        dArray[i][j] = JsonUtil.jsonArrayToDoubleArray(jsonArray3);
                    }
                }
                return new PolysUnion(dArray, null);
            }
            throw new JsonParseException("Invalid format for PolysUnion, must be either string or array");
        };
    }

    public enum Type {
        TRIANGLES,
        QUADS
    }
}

