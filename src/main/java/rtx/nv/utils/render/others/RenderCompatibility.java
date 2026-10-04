package rtx.nv.utils.render.others;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

public final class RenderCompatibility {
    private static Boolean safeWorldEffects;
    private static String safeWorldEffectsReason;
    private static final String SAFE_WORLD_EFFECTS_PROPERTY = "nv_safeWorldEffects";
    private static final String SAFE_WORLD_EFFECTS_ENV = "NV_SAFE_WORLD_EFFECTS";
    private static Boolean disableFragEffectScanShader;
    private static String disableFragEffectScanShaderReason;
    private static final String DISABLE_FRAG_EFFECT_SCAN_PROPERTY = "nv_disableFragEffectScanShader";
    private static final String DISABLE_FRAG_EFFECT_SCAN_ENV = "NV_DISABLE_FRAG_EFFECT_SCAN_SHADER";

    private RenderCompatibility() {
    }

    static {
        safeWorldEffectsReason = "unresolved";
        disableFragEffectScanShaderReason = "unresolved";
    }

    private static String normalize(String string) {
        return string == null ? "" : string.trim().toLowerCase();
    }

    private static boolean detectFragEffectScanShaderBlocklist() {
        if (!RenderSystem.isOnRenderThread()) {
            disableFragEffectScanShaderReason = "render thread unavailable";
            return false;
        }
        String string = RenderCompatibility.readGlString(7936);
        String string2 = RenderCompatibility.readGlString(7937);
        String string3 = RenderCompatibility.readGlString(7938);
        disableFragEffectScanShaderReason = "safe depth-copy shader path enabled: vendor=" + string + ", renderer=" + string2 + ", version=" + string3;
        return false;
    }

    public static String getDisableFragEffectScanShaderReason() {
        return disableFragEffectScanShaderReason;
    }

    public static boolean shouldDisableFragEffectScanShader() {
        Boolean bl = RenderCompatibility.readManualOverride(System.getProperty(DISABLE_FRAG_EFFECT_SCAN_PROPERTY));
        if (bl == null) {
            bl = RenderCompatibility.readManualOverride(System.getenv(DISABLE_FRAG_EFFECT_SCAN_ENV));
        }
        if (bl != null) {
            disableFragEffectScanShader = bl;
            disableFragEffectScanShaderReason = "manual override";
            return bl;
        }
        if (disableFragEffectScanShader == null) {
            disableFragEffectScanShader = RenderCompatibility.detectFragEffectScanShaderBlocklist();
        }
        return disableFragEffectScanShader;
    }

    public static void primeFromCurrentContext() {
        RenderCompatibility.useSafeWorldEffects();
        RenderCompatibility.shouldDisableFragEffectScanShader();
    }

    private static String readGlString(int n) {
        try {
            return RenderCompatibility.normalize(GL11.glGetString((int)n));
        }
        catch (Throwable throwable) {
            return "";
        }
    }

    public static boolean useSafeWorldEffects() {
        Boolean bl = RenderCompatibility.readManualOverride(System.getProperty(SAFE_WORLD_EFFECTS_PROPERTY));
        if (bl == null) {
            bl = RenderCompatibility.readManualOverride(System.getenv(SAFE_WORLD_EFFECTS_ENV));
        }
        if (bl != null) {
            safeWorldEffects = bl;
            safeWorldEffectsReason = "manual override";
            return bl;
        }
        if (safeWorldEffects == null) {
            safeWorldEffects = false;
            safeWorldEffectsReason = "default-path";
        }
        return safeWorldEffects;
    }

    private static Boolean readManualOverride(String string) {
        if (string == null) {
            return null;
        }
        String string2 = string.trim().toLowerCase();
        if (string2.equals("1") || string2.equals("true") || string2.equals("yes") || string2.equals("on")) {
            return true;
        }
        if (string2.equals("0") || string2.equals("false") || string2.equals("no") || string2.equals("off")) {
            return false;
        }
        return null;
    }

    public static String getSafeWorldEffectsReason() {
        return safeWorldEffectsReason;
    }
}

