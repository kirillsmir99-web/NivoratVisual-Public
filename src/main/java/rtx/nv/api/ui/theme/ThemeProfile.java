package rtx.nv.api.ui.theme;

public final class ThemeProfile {
    public static final ThemeProfile BALANCED = new ThemeProfile("balanced", "Balanced", 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
    public static final ThemeProfile DEEP = new ThemeProfile("deep", "Deep", 1.15f, 1.25f, 0.85f, 0.9f, 0.75f, 0.8f, 0.7f, 0.85f, 1.2f);
    public static final ThemeProfile CRYSTAL = new ThemeProfile("crystal", "Crystal", 0.85f, 0.8f, 1.35f, 1.3f, 1.25f, 1.1f, 1.3f, 1.4f, 0.95f);
    public static final ThemeProfile SOFT = new ThemeProfile("soft", "Soft", 0.9f, 0.9f, 0.8f, 0.8f, 0.85f, 0.75f, 0.6f, 0.7f, 0.85f);
    public static final ThemeProfile VIVID = new ThemeProfile("vivid", "Vivid", 1.2f, 1.05f, 1.15f, 1.2f, 1.35f, 1.25f, 1.15f, 1.2f, 1.25f);
    public static final ThemeProfile WARM = new ThemeProfile("warm", "Warm", 1.1f, 1.0f, 0.95f, 0.95f, 1.15f, 0.9f, 0.85f, 1.05f, 1.05f);
    public static final ThemeProfile MONO = new ThemeProfile("mono", "Mono", 0.75f, 1.2f, 0.9f, 1.15f, 0.7f, 0.6f, 0.4f, 1.1f, 1.3f);

    public static final ThemeProfile GLASS = new ThemeProfile("glass", "Glass", 1.0f, 1.0f, 1.1f, 1.0f, 1.0f, 1.0f, 1.0f, 1.2f, 1.0f);
    public static final ThemeProfile MATTE = new ThemeProfile("matte", "Matte", 0.8f, 1.3f, 0.5f, 0.8f, 0.6f, 0.9f, 0.4f, 0.5f, 1.2f);
    public static final ThemeProfile VOID = new ThemeProfile("void", "Void", 1.4f, 1.8f, 0.8f, 1.2f, 1.3f, 0.8f, 0.9f, 0.8f, 1.4f);
    public static final ThemeProfile NEON = new ThemeProfile("neon", "Neon", 1.2f, 0.9f, 1.4f, 1.5f, 1.8f, 1.3f, 1.4f, 1.5f, 1.3f);

    public static ThemeProfile fromName(String name) {
        if (name == null) return GLASS;
        return switch (name.toLowerCase()) {
            case "glass" -> GLASS;
            case "matte" -> MATTE;
            case "void" -> VOID;
            case "neon" -> NEON;
            case "balanced" -> BALANCED;
            case "deep" -> DEEP;
            case "crystal" -> CRYSTAL;
            case "soft" -> SOFT;
            case "vivid" -> VIVID;
            case "warm" -> WARM;
            case "mono" -> MONO;
            default -> GLASS;
        };
    }

    public final String id;
    public final String displayName;
    public final float glassTint;
    public final float darkening;
    public final float refraction;
    public final float edge;
    public final float glow;
    public final float motion;
    public final float chromatic;
    public final float specular;
    public final float contrast;
    public final float blurRadius;
    public final float gradientAngle;

    public ThemeProfile(String id, String displayName, float glassTint, float darkening, float refraction, float edge, float glow, float motion, float chromatic, float specular, float contrast) {
        this(id, displayName, glassTint, darkening, refraction, edge, glow, motion, chromatic, specular, contrast, 18.0f, 0.0f);
    }

    public ThemeProfile(String id, String displayName, float glassTint, float darkening, float refraction, float edge, float glow, float motion, float chromatic, float specular, float contrast, float blurRadius, float gradientAngle) {
        this.id = id;
        this.displayName = displayName;
        this.glassTint = glassTint;
        this.darkening = darkening;
        this.refraction = refraction;
        this.edge = edge;
        this.glow = glow;
        this.motion = motion;
        this.chromatic = chromatic;
        this.specular = specular;
        this.contrast = contrast;
        this.blurRadius = blurRadius;
        this.gradientAngle = gradientAngle;
    }

    public ThemeProfile lerp(ThemeProfile target, float t) {
        if (target == null) {
            return this;
        }
        float k = Math.max(0.0f, Math.min(1.0f, t));
        if (k <= 0.0f) {
            return this;
        }
        if (k >= 1.0f) {
            return target;
        }
        return new ThemeProfile(
            k < 0.5f ? this.id : target.id,
            k < 0.5f ? this.displayName : target.displayName,
            this.glassTint + (target.glassTint - this.glassTint) * k,
            this.darkening + (target.darkening - this.darkening) * k,
            this.refraction + (target.refraction - this.refraction) * k,
            this.edge + (target.edge - this.edge) * k,
            this.glow + (target.glow - this.glow) * k,
            this.motion + (target.motion - this.motion) * k,
            this.chromatic + (target.chromatic - this.chromatic) * k,
            this.specular + (target.specular - this.specular) * k,
            this.contrast + (target.contrast - this.contrast) * k,
            this.blurRadius + (target.blurRadius - this.blurRadius) * k,
            this.gradientAngle + (target.gradientAngle - this.gradientAngle) * k
        );
    }

    public com.google.gson.JsonObject toJson() {
        var json = new com.google.gson.JsonObject();
        json.addProperty("tint", glassTint); json.addProperty("darkening", darkening);
        json.addProperty("refraction", refraction); json.addProperty("edge", edge);
        json.addProperty("glow", glow); json.addProperty("motion", motion);
        json.addProperty("chromatic", chromatic); json.addProperty("specular", specular);
        json.addProperty("contrast", contrast); json.addProperty("blur", blurRadius);
        json.addProperty("angle", gradientAngle);
        return json;
    }

    public static ThemeProfile fromJson(ThemeProfile base, com.google.gson.JsonObject json) {
        if (json == null) return base;
        return new ThemeProfile(base.id, base.displayName,
            value(json, "tint", base.glassTint, 2), value(json, "darkening", base.darkening, 2),
            value(json, "refraction", base.refraction, 2), value(json, "edge", base.edge, 2),
            value(json, "glow", base.glow, 2), value(json, "motion", base.motion, 2),
            value(json, "chromatic", base.chromatic, 2), value(json, "specular", base.specular, 2),
            value(json, "contrast", base.contrast, 2), value(json, "blur", base.blurRadius, 32),
            value(json, "angle", base.gradientAngle, 360));
    }

    private static float value(com.google.gson.JsonObject json, String key, float fallback, float max) {
        try {
            float v = json.has(key) ? json.get(key).getAsFloat() : fallback;
            return Float.isFinite(v) ? Math.clamp(v, 0.0f, max) : fallback;
        } catch (RuntimeException invalid) { return fallback; }
    }
}
