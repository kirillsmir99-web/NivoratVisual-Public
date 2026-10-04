package rtx.nv.api.mods.geckolib.loading.json.raw;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Map;
import net.minecraft.util.JsonHelper;
import rtx.nv.api.mods.geckolib.loading.json.raw.Cube;
import rtx.nv.api.mods.geckolib.loading.json.raw.LocatorValue;
import rtx.nv.api.mods.geckolib.loading.json.raw.PolyMesh;
import rtx.nv.api.mods.geckolib.loading.json.raw.TextureMesh;
import rtx.nv.api.mods.geckolib.util.JsonUtil;

public record Bone(Cube[] cubes, Boolean debug, Double inflate, Map<String, LocatorValue> locators, Boolean mirror, String name, Boolean neverRender, String parent, double[] pivot, PolyMesh polyMesh, Long renderGroupId, double[] rotation, TextureMesh[] textureMeshes) {
    public static JsonDeserializer<Bone> deserializer() throws JsonParseException {
        return (jsonElement, type, jsonDeserializationContext) -> {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            Cube[] cubeArray = JsonUtil.jsonArrayToObjectArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"cubes", null), jsonDeserializationContext, Cube.class);
            Boolean bl = JsonUtil.getOptionalBoolean(jsonObject, "debug");
            Double d = JsonUtil.getOptionalDouble(jsonObject, "inflate");
            Map<String, LocatorValue> map = jsonObject.has("locators") ? JsonUtil.jsonObjToMap(JsonHelper.getObject((JsonObject)jsonObject, (String)"locators"), jsonDeserializationContext, LocatorValue.class) : null;
            Boolean bl2 = JsonUtil.getOptionalBoolean(jsonObject, "mirror");
            String string = JsonHelper.getString((JsonObject)jsonObject, (String)"name");
            Boolean bl3 = JsonUtil.getOptionalBoolean(jsonObject, "neverRender");
            String string2 = JsonHelper.getString((JsonObject)jsonObject, (String)"parent", null);
            double[] dArray = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"pivot", null));
            PolyMesh polyMesh = (PolyMesh)JsonHelper.deserialize((JsonObject)jsonObject, (String)"poly_mesh", null, (JsonDeserializationContext)jsonDeserializationContext, PolyMesh.class);
            Long l = JsonUtil.getOptionalLong(jsonObject, "render_group_id");
            double[] dArray2 = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"rotation", null));
            TextureMesh[] textureMeshArray = JsonUtil.jsonArrayToObjectArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"texture_meshes", null), jsonDeserializationContext, TextureMesh.class);
            if (cubeArray == null) {
                cubeArray = new Cube[]{};
            }
            return new Bone(cubeArray, bl, d, map, bl2, string, bl3, string2, dArray, polyMesh, l, dArray2, textureMeshArray);
        };
    }
}

