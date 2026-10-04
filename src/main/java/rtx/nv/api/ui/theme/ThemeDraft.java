package rtx.nv.api.ui.theme;

public final class ThemeDraft {
    private String name = "Новая тема";
    private ThemeProfile profile = ThemeProfile.GLASS;
    private int[] palette = new int[]{0x6C72CB, 0x8E52AA, 0xCB69C1, 0x5D8AA8};
    private int[] paletteAlpha = new int[]{255, 255, 255, 255};
    private int activeSlot = 0; // 0=Primary (Основной), 1=Secondary (Второй), 2=Accent (Акцент), 3=Light (Свет)

    // Advanced parameters
    private float alpha = 1.0f;
    private float blur = 18.0f;
    private float glow = 0.35f;
    private float refraction = 0.30f;
    private float edgeStrength = 0.18f;
    private float gradientAngle = 0.0f;
    private boolean colorMovement = false;

    // Workspace options
    private boolean autoApply = true;
    private boolean darkPreviewBg = true;
    private int previewBgMode = 0; // 0=Dark, 1=Light, 2=World
    private boolean dirty = false;
    private Snapshot saved;

    private record Snapshot(String name, String profile, int[] colors, int[] opacity, float alpha, float blur, float glow, float refraction, float edge, float angle, boolean motion) {
        static Snapshot of(ThemeDraft d) {
            return new Snapshot(d.name, d.profile.id, d.palette.clone(), d.paletteAlpha.clone(), d.alpha, d.blur, d.glow, d.refraction, d.edgeStrength, d.gradientAngle, d.colorMovement);
        }
        boolean matches(ThemeDraft d) {
            return java.util.Objects.equals(name, d.name) && profile.equals(d.profile.id)
                && java.util.Arrays.equals(colors, d.palette) && java.util.Arrays.equals(opacity, d.paletteAlpha)
                && alpha == d.alpha && blur == d.blur && glow == d.glow && refraction == d.refraction
                && edge == d.edgeStrength && angle == d.gradientAngle && motion == d.colorMovement;
        }
    }

    // Associated editing custom theme (null if creating new)
    private CustomTheme editingTheme = null;

    public ThemeDraft() {
        resetToNew();
    }

    public void resetToNew() {
        this.editingTheme = null;
        this.name = rtx.nv.api.localization.Lang.get("theme.editor.new_name", "Новая тема");
        this.profile = ThemeProfile.GLASS;
        this.palette = new int[]{0x6C72CB, 0x8E52AA, 0xCB69C1, 0x5D8AA8};
        this.paletteAlpha = new int[]{255, 255, 255, 255};
        this.activeSlot = 0;
        this.alpha = 1.0f;
        this.blur = 18.0f;
        this.glow = 0.35f;
        this.refraction = 0.30f;
        this.edgeStrength = 0.18f;
        this.gradientAngle = 0.0f;
        this.colorMovement = false;
        this.autoApply = true;
        this.darkPreviewBg = true;
        this.dirty = false;
        this.saved = Snapshot.of(this);
    }

    public void loadFromTheme(ITheme theme) {
        if (theme == null) {
            resetToNew();
            return;
        }
        if (theme instanceof CustomTheme) {
            this.editingTheme = (CustomTheme) theme;
        } else {
            this.editingTheme = null;
        }
        this.name = theme.displayName();
        this.profile = theme.profile() != null ? theme.profile() : ThemeProfile.GLASS;
        this.alpha = this.profile.glassTint;
        this.glow = this.profile.glow;
        this.refraction = this.profile.refraction;
        this.edgeStrength = this.profile.edge;
        this.colorMovement = this.profile.motion > 1.0f;
        this.blur = this.profile.blurRadius;
        this.gradientAngle = this.profile.gradientAngle;
        int[] pal = theme.palette();
        if (pal != null && pal.length >= 4) {
            this.palette = pal.clone();
        } else if (pal != null && pal.length > 0) {
            this.palette = new int[]{
                pal[0],
                pal.length > 1 ? pal[1] : pal[0],
                pal.length > 2 ? pal[2] : pal[pal.length - 1],
                pal[pal.length - 1]
            };
        } else {
            this.palette = new int[]{0x6C72CB, 0x8E52AA, 0xCB69C1, 0x5D8AA8};
        }
        this.paletteAlpha = new int[]{255, 255, 255, 255};
        if (theme instanceof CustomTheme custom) this.paletteAlpha = custom.paletteAlpha();
        this.activeSlot = 0;
        this.dirty = false;
        this.saved = Snapshot.of(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.dirty = true;
    }

    public ThemeProfile getProfile() {
        return profile;
    }

    public void setProfile(ThemeProfile profile) {
        this.profile = profile != null ? profile : ThemeProfile.GLASS;
        this.alpha = this.profile.glassTint;
        this.glow = this.profile.glow;
        this.refraction = this.profile.refraction;
        this.edgeStrength = this.profile.edge;
        this.colorMovement = this.profile.motion > 1.0f;
        this.dirty = true;
    }

    public int[] getPalette() {
        return palette;
    }

    public void setPalette(int[] palette) {
        this.palette = palette;
        this.dirty = true;
    }

    public int[] getPaletteAlpha() {
        return paletteAlpha;
    }

    public int getActiveSlot() {
        return activeSlot;
    }

    public void setActiveSlot(int activeSlot) {
        this.activeSlot = Math.max(0, Math.min(3, activeSlot));
    }

    public int getActiveColor() {
        return palette[activeSlot] & 0xFFFFFF;
    }

    public int getActiveAlpha() {
        return paletteAlpha[activeSlot] & 0xFF;
    }

    public void setActiveColor(int rgb) {
        this.palette[activeSlot] = rgb & 0xFFFFFF;
        this.dirty = true;
    }

    public void setActiveAlpha(int alpha) {
        this.paletteAlpha[activeSlot] = Math.max(0, Math.min(255, alpha));
        this.dirty = true;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
        this.dirty = true;
    }

    public float getBlur() {
        return blur;
    }

    public void setBlur(float blur) {
        this.blur = blur;
        this.dirty = true;
    }

    public float getGlow() {
        return glow;
    }

    public void setGlow(float glow) {
        this.glow = glow;
        this.dirty = true;
    }

    public float getRefraction() {
        return refraction;
    }

    public void setRefraction(float refraction) {
        this.refraction = refraction;
        this.dirty = true;
    }

    public float getEdgeStrength() {
        return edgeStrength;
    }

    public void setEdgeStrength(float edgeStrength) {
        this.edgeStrength = edgeStrength;
        this.dirty = true;
    }

    public float getGradientAngle() {
        return gradientAngle;
    }

    public void setGradientAngle(float gradientAngle) {
        this.gradientAngle = gradientAngle;
        this.dirty = true;
    }

    public boolean isColorMovement() {
        return colorMovement;
    }

    public void setColorMovement(boolean colorMovement) {
        this.colorMovement = colorMovement;
        this.dirty = true;
    }

    public boolean isAutoApply() {
        return autoApply;
    }

    public void setAutoApply(boolean autoApply) {
        this.autoApply = autoApply;
    }

    public boolean isDarkPreviewBg() {
        return darkPreviewBg;
    }

    public void setDarkPreviewBg(boolean darkPreviewBg) {
        this.darkPreviewBg = darkPreviewBg;
        this.previewBgMode = darkPreviewBg ? 0 : 1;
    }

    public int getPreviewBgMode() {
        return previewBgMode;
    }

    public void setPreviewBgMode(int previewBgMode) {
        this.previewBgMode = Math.max(0, Math.min(2, previewBgMode));
        this.darkPreviewBg = (this.previewBgMode == 0);
    }

    public boolean isDirty() {
        return dirty && (saved == null || !saved.matches(this));
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (!dirty) this.saved = Snapshot.of(this);
    }

    public CustomTheme getEditingTheme() {
        return editingTheme;
    }

    public void setEditingTheme(CustomTheme editingTheme) {
        this.editingTheme = editingTheme;
    }

    public ITheme toLiveTheme() {
        int[] pal = this.palette != null ? this.palette.clone() : new int[]{0x6C72CB, 0x8E52AA, 0xCB69C1, 0x5D8AA8};
        ThemeProfile baseProf = this.profile != null ? this.profile : ThemeProfile.GLASS;
        ThemeProfile liveProf = new ThemeProfile(
            baseProf.id,
            baseProf.displayName,
            this.alpha,
            baseProf.darkening,
            this.refraction,
            this.edgeStrength,
            this.glow,
            this.colorMovement ? 1.5f : 0.0f,
            baseProf.chromatic,
            baseProf.specular,
            baseProf.contrast,
            this.blur,
            this.gradientAngle
        );
        String id = this.editingTheme != null ? this.editingTheme.id() : "draft_live";
        CustomTheme theme = new CustomTheme(id, this.name, liveProf, pal);
        theme.setPaletteAlpha(this.paletteAlpha);
        return theme;
    }
}
