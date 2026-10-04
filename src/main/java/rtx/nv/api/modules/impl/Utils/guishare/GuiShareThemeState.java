package rtx.nv.api.modules.impl.Utils.guishare;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record GuiShareThemeState(
    int mode,
    int[] shades,
    int[] palette,
    boolean movement,
    boolean usesSecond,
    int gradientStyleId,
    float rainbowSpeed,
    float rainbowSpread,
    float rainbowSaturation,
    float cornerRadius,
    float backdropBlur,
    float refraction,
    float edgeStrength,
    float edgeSharpness,
    boolean glow,
    float glowIntensity,
    float glowRadius,
    float gradientSweep,
    float gradientPrevStyle
) {
    public static final GuiShareThemeState DEFAULTS = new GuiShareThemeState(
        0, new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 0}, false, false, 0,
        1.0f, 1.0f, 1.0f, 8.0f, 10.0f, 0.0f, 1.0f, 1.0f, false, 1.0f, 8.0f, 0.0f, 0.0f
    );

    public static GuiShareThemeState capture() {
        return DEFAULTS;
    }

    public static GuiShareThemeState fromJson(JsonObject json) {
        return DEFAULTS;
    }

    public void writeTo(JsonObject json) {
    }
}
