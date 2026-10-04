package rtx.nv.api.mods.geckolib.loading.json.raw;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Direction;
import rtx.nv.api.mods.geckolib.loading.json.raw.FaceUV;

public record UVFaces(FaceUV north, FaceUV south, FaceUV east, FaceUV west, FaceUV up, FaceUV down) {
    public FaceUV fromDirection(Direction direction) {
        return switch (direction) {
            default -> throw new MatchException(null, null);
            case Direction.NORTH -> this.north;
            case Direction.SOUTH -> this.south;
            case Direction.EAST -> this.east;
            case Direction.WEST -> this.west;
            case Direction.UP -> this.up;
            case Direction.DOWN -> this.down;
        };
    }

    public static JsonDeserializer<UVFaces> deserializer() {
        return (jsonElement, type, jsonDeserializationContext) -> {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            FaceUV faceUV = (FaceUV)JsonHelper.deserialize((JsonObject)jsonObject, (String)"north", null, (JsonDeserializationContext)jsonDeserializationContext, FaceUV.class);
            FaceUV faceUV2 = (FaceUV)JsonHelper.deserialize((JsonObject)jsonObject, (String)"south", null, (JsonDeserializationContext)jsonDeserializationContext, FaceUV.class);
            FaceUV faceUV3 = (FaceUV)JsonHelper.deserialize((JsonObject)jsonObject, (String)"east", null, (JsonDeserializationContext)jsonDeserializationContext, FaceUV.class);
            FaceUV faceUV4 = (FaceUV)JsonHelper.deserialize((JsonObject)jsonObject, (String)"west", null, (JsonDeserializationContext)jsonDeserializationContext, FaceUV.class);
            FaceUV faceUV5 = (FaceUV)JsonHelper.deserialize((JsonObject)jsonObject, (String)"up", null, (JsonDeserializationContext)jsonDeserializationContext, FaceUV.class);
            FaceUV faceUV6 = (FaceUV)JsonHelper.deserialize((JsonObject)jsonObject, (String)"down", null, (JsonDeserializationContext)jsonDeserializationContext, FaceUV.class);
            return new UVFaces(faceUV, faceUV2, faceUV3, faceUV4, faceUV5, faceUV6);
        };
    }
}

