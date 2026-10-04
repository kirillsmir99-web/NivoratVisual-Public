package rtx.nv.api.mods.geckolib.util;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Vec3d;

public final class JsonUtil {
    private JsonUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] jsonArrayToObjectArray(JsonArray jsonArray, JsonDeserializationContext jsonDeserializationContext, Class<T> clazz) {
        if (jsonArray == null) {
            return null;
        }
        T[] objectArray = (T[]) Array.newInstance(clazz, jsonArray.size());
        for (int i = 0; i < jsonArray.size(); ++i) {
            objectArray[i] = jsonDeserializationContext.deserialize(jsonArray.get(i), clazz);
        }
        return objectArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] jsonArrayToObjectArray(JsonArray jsonArray, Class<T> clazz, Function<JsonElement, T> function) {
        if (jsonArray == null) {
            return null;
        }
        T[] objectArray = (T[]) Array.newInstance(clazz, jsonArray.size());
        for (int i = 0; i < jsonArray.size(); ++i) {
            T t = function.apply(jsonArray.get(i));
            if (t == null) continue;
            objectArray[i] = t;
        }
        return objectArray;
    }

    public static Boolean getOptionalBoolean(JsonObject jsonObject, String string) {
        return jsonObject.has(string) ? Boolean.valueOf(JsonHelper.getBoolean((JsonObject)jsonObject, (String)string)) : null;
    }

    public static Double getOptionalDouble(JsonObject jsonObject, String string) {
        return jsonObject.has(string) ? Double.valueOf(JsonHelper.getDouble((JsonObject)jsonObject, (String)string)) : null;
    }

    public static Float getOptionalFloat(JsonObject jsonObject, String string) {
        return jsonObject.has(string) ? Float.valueOf(JsonHelper.getFloat((JsonObject)jsonObject, (String)string)) : null;
    }

    public static <T> Map<String, T> jsonObjToMap(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, Class<T> clazz) {
        if (jsonObject == null) {
            return null;
        }
        Object2ObjectOpenHashMap object2ObjectOpenHashMap = new Object2ObjectOpenHashMap(jsonObject.size());
        for (Map.Entry entry : jsonObject.entrySet()) {
            object2ObjectOpenHashMap.put((String)entry.getKey(), jsonDeserializationContext.deserialize((JsonElement)entry.getValue(), clazz));
        }
        return object2ObjectOpenHashMap;
    }

    public static double[] jsonArrayToDoubleArray(JsonArray jsonArray) throws JsonParseException {
        if (jsonArray == null) {
            return new double[3];
        }
        double[] dArray = new double[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); ++i) {
            dArray[i] = jsonArray.get(i).getAsDouble();
        }
        return dArray;
    }

    public static Long getOptionalLong(JsonObject jsonObject, String string) {
        return jsonObject.has(string) ? Long.valueOf(JsonHelper.getLong((JsonObject)jsonObject, (String)string)) : null;
    }

    public static <T> List<T> jsonArrayToList(JsonArray jsonArray, Function<JsonElement, T> function) {
        if (jsonArray == null) {
            return new ObjectArrayList();
        }
        ObjectArrayList objectArrayList = new ObjectArrayList(jsonArray.size());
        for (JsonElement jsonElement : jsonArray) {
            objectArrayList.add(function.apply(jsonElement));
        }
        return objectArrayList;
    }

    public static Vec3d jsonToVec3(JsonElement jsonElement) {
        if (jsonElement == null) {
            return null;
        }
        if (jsonElement.isJsonArray()) {
            return JsonUtil.arrayToVec(JsonUtil.jsonArrayToDoubleArray(jsonElement.getAsJsonArray()));
        }
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (!(jsonObject.has("x") && jsonObject.has("y") && jsonObject.has("z"))) {
                throw new IllegalStateException("Json object input must have x, y, and z properties to parse into a Vec3: " + String.valueOf(jsonElement));
            }
            return new Vec3d(JsonHelper.getDouble((JsonObject)jsonObject, (String)"x"), JsonHelper.getDouble((JsonObject)jsonObject, (String)"y"), JsonHelper.getDouble((JsonObject)jsonObject, (String)"z"));
        }
        throw new IllegalStateException("Json input must be an array or object to parse into a Vec3: " + String.valueOf(jsonElement));
    }

    public static Vec3d arrayToVec(double[] dArray) {
        if (dArray[0] == 0.0 && dArray[1] == 0.0 && dArray[2] == 0.0) {
            return Vec3d.ZERO;
        }
        return new Vec3d(dArray[0], dArray[1], dArray[2]);
    }

    public static <L, R> Either<L, R> getEither(JsonObject jsonObject, Predicate<JsonObject> predicate, Predicate<JsonObject> predicate2, Predicate<JsonObject> predicate3, Function<JsonObject, L> function, Function<JsonObject, R> function2) {
        if (predicate != null && predicate.test(jsonObject)) {
            return null;
        }
        if (predicate2.test(jsonObject)) {
            return Either.left(function.apply(jsonObject));
        }
        if (predicate3.test(jsonObject)) {
            return Either.right(function2.apply(jsonObject));
        }
        throw new JsonParseException("Invalid Json object, contained value was neither of the two possible formats!");
    }

    public static <L, R> Either<L, R> getEither(JsonObject jsonObject, Predicate<JsonObject> predicate, Predicate<JsonObject> predicate2, Function<JsonObject, L> function, Function<JsonObject, R> function2) {
        return JsonUtil.getEither(jsonObject, null, predicate, predicate2, function, function2);
    }

    public static <T> Map<String, T> jsonObjToPrimitiveMap(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, Function<JsonElement, T> function) {
        if (jsonObject == null) {
            return null;
        }
        Object2ObjectOpenHashMap object2ObjectOpenHashMap = new Object2ObjectOpenHashMap(jsonObject.size());
        for (Map.Entry entry : jsonObject.entrySet()) {
            object2ObjectOpenHashMap.put((String)entry.getKey(), function.apply((JsonElement)entry.getValue()));
        }
        return object2ObjectOpenHashMap;
    }

    public static Integer getOptionalInteger(JsonObject jsonObject, String string) {
        return jsonObject.has(string) ? Integer.valueOf(JsonHelper.getInt((JsonObject)jsonObject, (String)string)) : null;
    }
}

