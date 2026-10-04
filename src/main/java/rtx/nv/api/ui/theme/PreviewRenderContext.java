package rtx.nv.api.ui.theme;

/**
 * Single-Source-of-Truth Preview Context for Theme Workspace 4.0.
 * Directly binds live parameters from ThemeDraft without double/triple alpha multiplication,
 * ensuring authentic color fidelity, contrast, and crispness.
 */
public final class PreviewRenderContext {

    private final ThemeDraft draft;
    private final float alpha;
    private final int bgMode; // 0 = World, 1 = Dark, 2 = Light
    private final float dt;

    public PreviewRenderContext(ThemeDraft draft, float alpha, int bgMode, float dt) {
        this.draft = draft;
        this.alpha = alpha;
        this.bgMode = bgMode;
        this.dt = dt;
    }

    public ThemeDraft getDraft() {
        return this.draft;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public int getBgMode() {
        return this.bgMode;
    }

    public float getDt() {
        return this.dt;
    }

    public int primaryColor() {
        if (this.draft == null) return 0x6C72CB;
        int[] pal = this.draft.getPalette();
        return pal.length > 0 ? pal[0] : 0x6C72CB;
    }

    public int secondaryColor() {
        if (this.draft == null) return 0x8E52AA;
        int[] pal = this.draft.getPalette();
        return pal.length > 1 ? pal[1] : 0x8E52AA;
    }

    public int accentColor() {
        if (this.draft == null) return 0xCB69C1;
        int[] pal = this.draft.getPalette();
        return pal.length > 2 ? pal[2] : 0xCB69C1;
    }

    public int lightColor() {
        if (this.draft == null) return 0x5D8AA8;
        int[] pal = this.draft.getPalette();
        return pal.length > 3 ? pal[3] : 0x5D8AA8;
    }

    public ThemeProfile profile() {
        return this.draft != null ? this.draft.getProfile() : ThemeProfile.GLASS;
    }

    public float draftAlpha() {
        return this.draft != null ? this.draft.getAlpha() : 0.85f;
    }

    public float draftBlur() {
        return this.draft != null ? this.draft.getBlur() : 18.0f;
    }

    public float draftGlow() {
        return this.draft != null ? this.draft.getGlow() : 0.6f;
    }

    public float draftRefraction() {
        return this.draft != null ? this.draft.getRefraction() : 0.3f;
    }

    public float draftEdgeStrength() {
        return this.draft != null ? this.draft.getEdgeStrength() : 0.70f;
    }

    public boolean draftColorMovement() {
        return this.draft != null && this.draft.isColorMovement();
    }

    public static int rgba(int rgb, float a) {
        int alpha = Math.max(0, Math.min(255, Math.round(a)));
        return (alpha & 0xFF) << 24 | (rgb & 0xFFFFFF);
    }

    public int primary(float a) {
        return rgba(primaryColor(), a * this.alpha);
    }

    public int secondary(float a) {
        return rgba(secondaryColor(), a * this.alpha);
    }

    public int accent(float a) {
        return rgba(accentColor(), a * this.alpha);
    }

    public int light(float a) {
        return rgba(lightColor(), a * this.alpha);
    }

    public int shellBg() {
        ThemeProfile prof = profile();
        float da = draftAlpha();
        return switch (prof.id) {
            case "matte" -> rgba(0x191C24, 235.0f * da * this.alpha);
            case "void" -> rgba(0x050609, 252.0f * da * this.alpha);
            case "neon" -> rgba(0x120E20, 215.0f * da * this.alpha);
            default -> rgba(0x0E1118, 185.0f * da * this.alpha);
        };
    }
}
