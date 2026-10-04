package rtx.nv.utils.render.renderitem;

import net.minecraft.item.ItemStack;

public record RenderItemOptions(float alpha, boolean showCount, boolean showDurability, boolean glow, int glowColor, GlintMode glintMode, float glintStrength, int color) {

    public static enum GlintMode {
        ALWAYS,
        AUTO,
        NEVER;

        public boolean enabled(ItemStack stack, boolean hasFoil) {
            return switch (this) {
                case ALWAYS -> true;
                case NEVER -> false;
                case AUTO -> hasFoil || (stack != null && stack.hasGlint());
            };
        }
    }

    public static RenderItemOptions defaults() {
        return new RenderItemOptions(1.0f, true, true, false, -1, GlintMode.AUTO, 1.0f, -1);
    }

    public RenderItemOptions(float alpha, boolean showCount, boolean showDurability, boolean glow, int glowColor) {
        this(alpha, showCount, showDurability, glow, glowColor, GlintMode.AUTO, 1.0f, -1);
    }

    public RenderItemOptions alpha(float a) {
        return new RenderItemOptions(a, showCount, showDurability, glow, glowColor, glintMode, glintStrength, color);
    }

    public RenderItemOptions withShowCount(boolean sc) {
        return new RenderItemOptions(alpha, sc, showDurability, glow, glowColor, glintMode, glintStrength, color);
    }

    public RenderItemOptions withShowDurability(boolean sd) {
        return new RenderItemOptions(alpha, showCount, sd, glow, glowColor, glintMode, glintStrength, color);
    }

    public RenderItemOptions withGlintMode(GlintMode mode) {
        return new RenderItemOptions(alpha, showCount, showDurability, glow, glowColor, mode, glintStrength, color);
    }

    public RenderItemOptions withGlintStrength(float strength) {
        return new RenderItemOptions(alpha, showCount, showDurability, glow, glowColor, glintMode, strength, color);
    }

    public RenderItemOptions withColor(int col) {
        return new RenderItemOptions(alpha, showCount, showDurability, glow, glowColor, glintMode, glintStrength, col);
    }
}