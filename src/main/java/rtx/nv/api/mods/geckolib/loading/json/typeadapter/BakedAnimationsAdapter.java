package rtx.nv.api.mods.geckolib.loading.json.typeadapter;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.doubles.DoubleObjectPair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.util.JsonHelper;
import org.apache.commons.lang3.math.NumberUtils;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.animation.object.EasingType;
import rtx.nv.api.mods.geckolib.animation.object.LoopType;
import rtx.nv.api.mods.geckolib.cache.animation.Animation;
import rtx.nv.api.mods.geckolib.cache.animation.Animation.KeyframeMarkers;
import rtx.nv.api.mods.geckolib.cache.animation.BoneAnimation;
import rtx.nv.api.mods.geckolib.cache.animation.Keyframe;
import rtx.nv.api.mods.geckolib.cache.animation.KeyframeStack;
import rtx.nv.api.mods.geckolib.loading.math.MathParser;
import rtx.nv.api.mods.geckolib.loading.math.MathValue;
import rtx.nv.api.mods.geckolib.loading.math.function.misc.ToRadFunction;
import rtx.nv.api.mods.geckolib.loading.math.value.Constant;
import rtx.nv.api.mods.geckolib.loading.math.value.Negative;
import rtx.nv.api.mods.geckolib.loading.object.BakedAnimations;
import rtx.nv.api.mods.geckolib.object.CompoundException;
import rtx.nv.api.mods.geckolib.util.JsonUtil;

public final class BakedAnimationsAdapter {
    public static ConcurrentMap<Double, Constant> COMPRESSION_CACHE = null;

    private static Animation bakeAnimation(String string, JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) throws CompoundException {
        double d = jsonObject.has("animation_length") ? JsonHelper.getDouble((JsonObject)jsonObject, (String)"animation_length") : -1.0;
        LoopType loopType = LoopType.fromJson(jsonObject.get("loop"));
        BoneAnimation[] boneAnimationArray = BakedAnimationsAdapter.bakeBoneAnimations(JsonHelper.getObject((JsonObject)jsonObject, (String)"bones", (JsonObject)new JsonObject()));
        Animation.KeyframeMarkers keyframeMarkers = (Animation.KeyframeMarkers)jsonDeserializationContext.deserialize((JsonElement)jsonObject, Animation.KeyframeMarkers.class);
        if (d == -1.0) {
            d = BakedAnimationsAdapter.calculateAnimationLength(boneAnimationArray);
        }
        return Animation.create(string, d, loopType, boneAnimationArray, keyframeMarkers);
    }

    private static List<DoubleObjectPair<JsonElement>> getKeyframes(JsonElement jsonElement) {
        if (jsonElement == null) {
            return List.of();
        }
        if (jsonElement instanceof JsonPrimitive jsonPrimitive) {
            JsonArray jsonArray = new JsonArray(3);
            jsonArray.add(jsonPrimitive);
            jsonArray.add(jsonPrimitive);
            jsonArray.add(jsonPrimitive);
            jsonElement = jsonArray;
        }
        if (jsonElement instanceof JsonArray jsonArray) {
            return ObjectArrayList.of(DoubleObjectPair.of(0.0, (JsonElement) jsonArray));
        }
        if (jsonElement instanceof JsonObject jsonObject) {
            if (jsonObject.has("vector")) {
                return ObjectArrayList.of(DoubleObjectPair.of(0.0, (JsonElement) jsonObject));
            }
            List<DoubleObjectPair<JsonElement>> resultList = new ObjectArrayList<>();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                double d = BakedAnimationsAdapter.readTimestamp(entry.getKey());
                if (d == 0.0 && !resultList.isEmpty()) {
                    throw new JsonParseException("Invalid keyframe data - multiple starting keyframes?" + entry.getKey());
                }
                JsonElement v = entry.getValue();
                if (v instanceof JsonObject entryObj && !entryObj.has("vector")) {
                    BakedAnimationsAdapter.addBedrockKeyframes(d, entryObj, resultList);
                    continue;
                }
                resultList.add(DoubleObjectPair.of(d, entry.getValue()));
            }
            return resultList;
        }
        throw new JsonParseException("Invalid object type provided to getTripletObj, got: " + String.valueOf(jsonElement));
    }

    private static double readTimestamp(String string) {
        return NumberUtils.isCreatable((String)string) ? Double.parseDouble(string) : 0.0;
    }

    private static void addBedrockKeyframes(double d, JsonObject jsonObject, List<DoubleObjectPair<JsonElement>> list) {
        boolean bl = false;
        if (jsonObject.has("pre")) {
            bl = true;
            list.add(DoubleObjectPair.of(d == 0.0 ? d : d - 0.001, (JsonElement) BakedAnimationsAdapter.extractBedrockKeyframe(jsonObject.get("pre"))));
        }
        if (jsonObject.has("post")) {
            JsonArray jsonArray = BakedAnimationsAdapter.extractBedrockKeyframe(jsonObject.get("post"));
            if (jsonObject.has("lerp_mode")) {
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.add("vector", (JsonElement)jsonArray);
                jsonObject2.add("easing", jsonObject.get("lerp_mode"));
                list.add(DoubleObjectPair.of(d, (JsonElement) jsonObject2));
            } else {
                list.add(DoubleObjectPair.of(d, (JsonElement) jsonArray));
            }
            return;
        }
        if (!bl) {
            throw new JsonParseException("Invalid keyframe data - expected array, found " + String.valueOf(jsonObject));
        }
    }

    private static BoneAnimation[] bakeBoneAnimations(JsonObject jsonObject) throws CompoundException {
        BoneAnimation[] boneAnimationArray = new BoneAnimation[jsonObject.size()];
        int n = 0;
        for (Map.Entry entry : jsonObject.entrySet()) {
            JsonObject jsonObject2 = ((JsonElement)entry.getValue()).getAsJsonObject();
            KeyframeStack keyframeStack = BakedAnimationsAdapter.buildKeyframeStack(BakedAnimationsAdapter.getKeyframes(jsonObject2.get("scale")), false);
            KeyframeStack keyframeStack2 = BakedAnimationsAdapter.buildKeyframeStack(BakedAnimationsAdapter.getKeyframes(jsonObject2.get("position")), false);
            KeyframeStack keyframeStack3 = BakedAnimationsAdapter.buildKeyframeStack(BakedAnimationsAdapter.getKeyframes(jsonObject2.get("rotation")), true);
            boneAnimationArray[n] = new BoneAnimation((String)entry.getKey(), keyframeStack3, keyframeStack2, keyframeStack);
            ++n;
        }
        return boneAnimationArray;
    }

    private static JsonArray extractBedrockKeyframe(JsonElement jsonElement) {
        if (jsonElement.isJsonArray()) {
            return jsonElement.getAsJsonArray();
        }
        if (!jsonElement.isJsonObject()) {
            throw new JsonParseException("Invalid keyframe data - expected array or object, found " + String.valueOf(jsonElement));
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has("vector")) {
            return jsonObject.get("vector").getAsJsonArray();
        }
        if (jsonObject.has("pre")) {
            return jsonObject.get("pre").getAsJsonArray();
        }
        return jsonObject.get("post").getAsJsonArray();
    }

    private static MathValue compressMathValue(MathValue mathValue) {
        if (COMPRESSION_CACHE == null || mathValue.isMutable()) {
            return mathValue;
        }
        return COMPRESSION_CACHE.computeIfAbsent(mathValue.get(null), Constant::new);
    }

    private static List<Keyframe> addSplineArgs(List<Keyframe> list) {
        Keyframe keyframe;
        if (list.size() == 1 && (keyframe = list.getFirst()).easingType() != EasingType.LINEAR) {
            list.set(0, new Keyframe(keyframe.startTime(), keyframe.length(), keyframe.startValue(), keyframe.endValue()));
            return list;
        }
        for (int i = 0; i < list.size(); ++i) {
            Keyframe keyframe2 = list.get(i);
            if (keyframe2.easingType() != EasingType.CATMULLROM) continue;
            MathValue[] values = new MathValue[]{i == 0 ? keyframe2.startValue() : list.get(i - 1).endValue(), i + 1 >= list.size() ? keyframe2.endValue() : list.get(i + 1).endValue()};
            list.set(i, new Keyframe(keyframe2.startTime(), keyframe2.length(), keyframe2.startValue(), keyframe2.endValue(), keyframe2.easingType(), values));
        }
        return list;
    }

    private static KeyframeStack buildKeyframeStack(List<DoubleObjectPair<JsonElement>> list, boolean bl) throws CompoundException {
        if (list.isEmpty()) {
            return KeyframeStack.EMPTY;
        }
        List<Keyframe> xList = new ObjectArrayList<>();
        List<Keyframe> yList = new ObjectArrayList<>();
        List<Keyframe> zList = new ObjectArrayList<>();
        MathValue prevX = null;
        MathValue prevY = null;
        MathValue prevZ = null;
        DoubleObjectPair<JsonElement> prevPair = null;
        for (DoubleObjectPair<JsonElement> pair : list) {
            JsonElement jsonElement = pair.right();
            double prevTime = prevPair != null ? prevPair.leftDouble() : 0.0;
            double curTime = pair.leftDouble();
            double duration = curTime - prevTime;
            JsonArray jsonArray = jsonElement instanceof JsonArray array ? array : JsonHelper.getArray(jsonElement.getAsJsonObject(), "vector");
            MathValue rawX = MathParser.parseJson(jsonArray.get(0));
            MathValue rawY = MathParser.parseJson(jsonArray.get(1));
            MathValue rawZ = MathParser.parseJson(jsonArray.get(2));
            MathValue curX = BakedAnimationsAdapter.compressMathValue(bl ? new Negative(new ToRadFunction(rawX)) : rawX);
            MathValue curY = BakedAnimationsAdapter.compressMathValue(bl ? new Negative(new ToRadFunction(rawY)) : rawY);
            MathValue curZ = BakedAnimationsAdapter.compressMathValue(bl ? new ToRadFunction(rawZ) : rawZ);
            JsonObject jsonObject = jsonElement instanceof JsonObject obj ? obj : null;
            EasingType easing = jsonObject != null && jsonObject.has("easing") ? EasingType.fromJson(jsonObject.get("easing")) : EasingType.LINEAR;
            List<MathValue> easingArgs = jsonObject != null && jsonObject.has("easingArgs") ? JsonUtil.jsonArrayToList(JsonHelper.getArray(jsonObject, "easingArgs"), el -> new Constant(el.getAsDouble())) : new ObjectArrayList<>();
            xList.add(new Keyframe(curTime, duration, prevPair == null ? curX : prevX, curX, easing, easingArgs));
            yList.add(new Keyframe(curTime, duration, prevPair == null ? curY : prevY, curY, easing, easingArgs));
            zList.add(new Keyframe(curTime, duration, prevPair == null ? curZ : prevZ, curZ, easing, easingArgs));
            prevX = curX;
            prevY = curY;
            prevZ = curZ;
            prevPair = pair;
        }
        return new KeyframeStack(BakedAnimationsAdapter.addSplineArgs(xList), BakedAnimationsAdapter.addSplineArgs(yList), BakedAnimationsAdapter.addSplineArgs(zList));
    }

    private static double calculateAnimationLength(BoneAnimation[] boneAnimationArray) {
        double d = 0.0;
        for (BoneAnimation boneAnimation : boneAnimationArray) {
            d = Math.max(d, boneAnimation.rotationKeyFrames().getTotalKeyframeTime());
            d = Math.max(d, boneAnimation.positionKeyFrames().getTotalKeyframeTime());
            d = Math.max(d, boneAnimation.scaleKeyFrames().getTotalKeyframeTime());
        }
        return d == 0.0 ? Double.MAX_VALUE : d;
    }

    public static JsonDeserializer<BakedAnimations> deserializer() throws JsonParseException {
        return BakedAnimationsAdapter::fromJson;
    }

    public static BakedAnimations fromJson(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Object2ObjectOpenHashMap object2ObjectOpenHashMap = new Object2ObjectOpenHashMap(jsonObject.size());
        for (Map.Entry entry : jsonObject.entrySet()) {
            try {
                object2ObjectOpenHashMap.put((String)entry.getKey(), BakedAnimationsAdapter.bakeAnimation((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsJsonObject(), jsonDeserializationContext));
            }
            catch (Exception exception) {
                if (exception instanceof CompoundException) {
                    CompoundException compoundException = (CompoundException)exception;
                    compoundException.withMessage("Unable to parse animation: " + (String)entry.getKey());
                } else {
                    GeckoLibConstants.LOGGER.error("Unable to parse animation: {}", entry.getKey());
                }
                rtx.nv.NV.LOGGER.error("Error parsing animation: " + entry.getKey(), exception);
            }
        }
        return new BakedAnimations((Map<String, Animation>)object2ObjectOpenHashMap);
    }
}

