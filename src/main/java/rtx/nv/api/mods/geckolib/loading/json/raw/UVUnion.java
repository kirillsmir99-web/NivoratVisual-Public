package rtx.nv.api.mods.geckolib.loading.json.raw;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import rtx.nv.api.mods.geckolib.loading.json.raw.UVFaces;
import rtx.nv.api.mods.geckolib.util.JsonUtil;

public record UVUnion(Either<double[], UVFaces> uvData) {
    public boolean isBoxUV() {
        return this.uvData.left().isPresent();
    }

    public static JsonDeserializer<UVUnion> deserializer() throws JsonParseException {
        return (jsonElement, type, jsonDeserializationContext) -> {
            if (jsonElement.isJsonObject()) {
                return new UVUnion(Either.right(jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject(), UVFaces.class)));
            }
            if (jsonElement.isJsonArray()) {
                return new UVUnion(Either.left(JsonUtil.jsonArrayToDoubleArray(jsonElement.getAsJsonArray())));
            }
            throw new JsonParseException("Invalid format provided for UVUnion, must be either double array or UVFaces collection");
        };
    }
}

