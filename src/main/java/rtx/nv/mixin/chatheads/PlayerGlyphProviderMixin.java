package rtx.nv.mixin.chatheads;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.font.GlyphMetrics;
import net.minecraft.client.font.PlayerHeadGlyphs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.mods.chatheads.PaddedChatGlyph;

@Mixin(net.minecraft.client.font.FontStorage.class)

public abstract class PlayerGlyphProviderMixin {
    @ModifyExpressionValue(method="<clinit>", at={@At(value="INVOKE", target="Lnet/minecraft/client/font/GlyphMetrics;method_73329(F)Lnet/minecraft/class_379;")}, require = 0)
    private static GlyphMetrics chatheads_addPaddingInsideChat(GlyphMetrics original) {
        return new PaddedChatGlyph(original);
    }
}

