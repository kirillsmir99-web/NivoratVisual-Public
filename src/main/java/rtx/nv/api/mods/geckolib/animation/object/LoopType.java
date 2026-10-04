package rtx.nv.api.mods.geckolib.animation.object;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animation.AnimationController;
import rtx.nv.api.mods.geckolib.animation.state.AnimationPoint;
import rtx.nv.api.mods.geckolib.animation.state.AnimationTimeline;
import rtx.nv.api.mods.geckolib.animation.state.AnimationTimeline.Stage;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;

public interface LoopType {
    public static final Map<String, LoopType> LOOP_TYPES = new ConcurrentHashMap<String, LoopType>(4);
    public static final LoopType DEFAULT = (geoAnimatable, animationPoint, stage, geoRenderState, animationController) -> animationPoint.animation().loopType().shouldKeepPlaying(geoAnimatable, animationPoint, stage, geoRenderState, animationController);
    public static final LoopType PLAY_ONCE = LoopType.register("play_once", LoopType.register("false", (geoAnimatable, animationPoint, stage, geoRenderState, animationController) -> false));
    public static final LoopType HOLD_ON_LAST_FRAME = LoopType.register("hold_on_last_frame", (geoAnimatable, animationPoint, stage, geoRenderState, animationController) -> {
        animationController.setTimelineTime(stage.endTime());
        return true;
    });
    public static final LoopType LOOP = LoopType.register("loop", LoopType.register("true", (geoAnimatable, animationPoint, stage, geoRenderState, animationController) -> {
        animationController.setTimelineTime(stage.startTime() + (animationController.getCurrentTimelineTime() - stage.endTime()));
        return true;
    }));

    public static LoopType register(String string, LoopType loopType) {
        LOOP_TYPES.put(string, loopType);
        return loopType;
    }

    default public String getId() throws IllegalStateException {
        for (String string : LOOP_TYPES.keySet()) {
            if (LOOP_TYPES.get(string) != this) continue;
            return string;
        }
        throw new IllegalStateException("LoopType has not been registered before being used!");
    }

    public static LoopType fromString(String string) {
        return LOOP_TYPES.getOrDefault(string, PLAY_ONCE);
    }

    public boolean shouldKeepPlaying(GeoAnimatable var1, AnimationPoint var2, AnimationTimeline.Stage var3, GeoRenderState var4, AnimationController<? extends GeoAnimatable> var5);

    public static LoopType fromJson(JsonElement jsonElement) {
        if (jsonElement == null || !jsonElement.isJsonPrimitive()) {
            return PLAY_ONCE;
        }
        JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
        if (jsonPrimitive.isBoolean()) {
            return jsonPrimitive.getAsBoolean() ? LOOP : PLAY_ONCE;
        }
        if (jsonPrimitive.isString()) {
            return LoopType.fromString(jsonPrimitive.getAsString());
        }
        return PLAY_ONCE;
    }
}

