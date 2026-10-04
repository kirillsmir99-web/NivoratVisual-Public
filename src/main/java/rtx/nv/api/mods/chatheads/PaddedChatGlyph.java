package rtx.nv.api.mods.chatheads;
import net.minecraft.client.font.GlyphMetrics;
import rtx.nv.api.mods.chatheads.ChatHeads;

public class PaddedChatGlyph
implements GlyphMetrics {
    public GlyphMetrics glyphInfo;

    public PaddedChatGlyph(GlyphMetrics glyphMetrics) {
        this.glyphInfo = glyphMetrics;
    }

    public float getAdvance() {
        return this.glyphInfo.getAdvance() + (ChatHeads.customHeadRendering ? 1.0f + 2.0f * ChatHeads.CONFIG.threeDeeNess() : 0.0f);
    }
}

