package rtx.nv.api.mods.geckolib.loading.json.typeadapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.util.JsonHelper;
import rtx.nv.api.mods.geckolib.cache.GeckoLibResources;
import rtx.nv.api.mods.geckolib.cache.animation.Animation;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.CustomInstructionKeyframeData;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.KeyFrameData;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.SoundKeyframeData;

public final class KeyFrameMarkersAdapter {
    private static CustomInstructionKeyframeData[] buildCustomFrameData(JsonObject jsonObject) {
        JsonObject jsonObject2 = JsonHelper.getObject(jsonObject, "timeline", new JsonObject());
        List<CustomInstructionKeyframeData> list = new ObjectArrayList<>(jsonObject2.size());
        for (Map.Entry<String, JsonElement> entry : jsonObject2.entrySet()) {
            String string;
            JsonElement v = entry.getValue();
            if (v instanceof JsonArray jsonArray) {
                string = GeckoLibResources.GSON.fromJson(jsonArray, ObjectArrayList.class).toString();
            } else if (v instanceof JsonPrimitive jsonPrimitive) {
                string = jsonPrimitive.getAsString();
            } else {
                string = "";
            }
            list.add(new CustomInstructionKeyframeData(Double.parseDouble(entry.getKey()), string));
        }
        list.sort(Comparator.comparing(KeyFrameData::getTime));
        return list.toArray(new CustomInstructionKeyframeData[0]);
    }

    private static ParticleKeyframeData[] buildParticleFrameData(JsonObject jsonObject) {
        JsonObject jsonObject2 = JsonHelper.getObject(jsonObject, "particle_effects", new JsonObject());
        List<ParticleKeyframeData> list = new ObjectArrayList<>(jsonObject2.size());
        for (Map.Entry<String, JsonElement> entry : jsonObject2.entrySet()) {
            JsonObject jsonObject3 = entry.getValue().getAsJsonObject();
            String string = JsonHelper.getString(jsonObject3, "effect", "");
            String string2 = JsonHelper.getString(jsonObject3, "locator", "");
            String string3 = JsonHelper.getString(jsonObject3, "pre_effect_script", "");
            list.add(new ParticleKeyframeData(Double.parseDouble(entry.getKey()), string, string2, string3));
        }
        list.sort(Comparator.comparing(KeyFrameData::getTime));
        return list.toArray(new ParticleKeyframeData[0]);
    }

    private static SoundKeyframeData[] buildSoundFrameData(JsonObject jsonObject) {
        JsonObject jsonObject2 = JsonHelper.getObject(jsonObject, "sound_effects", new JsonObject());
        List<SoundKeyframeData> list = new ObjectArrayList<>(jsonObject2.size());
        for (Map.Entry<String, JsonElement> entry : jsonObject2.entrySet()) {
            list.add(new SoundKeyframeData(Double.parseDouble(entry.getKey()), JsonHelper.getString(entry.getValue().getAsJsonObject(), "effect")));
        }
        list.sort(Comparator.comparing(KeyFrameData::getTime));
        return list.toArray(new SoundKeyframeData[0]);
    }

    public static JsonDeserializer<Animation.KeyframeMarkers> deserializer() throws JsonParseException {
        return KeyFrameMarkersAdapter::fromJson;
    }

    private static Animation.KeyframeMarkers fromJson(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        SoundKeyframeData[] soundKeyframeDataArray = KeyFrameMarkersAdapter.buildSoundFrameData(jsonObject);
        ParticleKeyframeData[] particleKeyframeDataArray = KeyFrameMarkersAdapter.buildParticleFrameData(jsonObject);
        CustomInstructionKeyframeData[] customInstructionKeyframeDataArray = KeyFrameMarkersAdapter.buildCustomFrameData(jsonObject);
        return new Animation.KeyframeMarkers(soundKeyframeDataArray, particleKeyframeDataArray, customInstructionKeyframeDataArray);
    }
}
