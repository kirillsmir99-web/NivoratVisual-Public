package rtx.nv.mixin.chatheads;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import rtx.nv.api.mods.chatheads.ChatHeads;

@Mixin(targets = {"net.minecraft.client.font.PlayerHeadGlyphs$HeadGlyph"})

public abstract class PlayerGlyphProviderInstanceMixin {
    @ModifyArg(method={"draw"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/font/PlayerHeadGlyphs$HeadGlyph;drawInternal(Lorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumer;IFFFFFIFFIIII)V", ordinal=0), index=3, require = 0)
    public float leftHead(float original) {
        if (!ChatHeads.customHeadRendering) {
            return original;
        }
        return original + ChatHeads.CONFIG.threeDeeNess();
    }

    @ModifyArg(method={"draw"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/font/PlayerHeadGlyphs$HeadGlyph;drawInternal(Lorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumer;IFFFFFIFFIIII)V", ordinal=0), index=4, require = 0)
    public float rightHead(float original) {
        if (!ChatHeads.customHeadRendering) {
            return original;
        }
        return original + ChatHeads.CONFIG.threeDeeNess();
    }

    @ModifyArg(method={"draw"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/font/PlayerHeadGlyphs$HeadGlyph;drawInternal(Lorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumer;IFFFFFIFFIIII)V", ordinal=1), index=4, require = 0)
    public float rightHat(float original) {
        if (!ChatHeads.customHeadRendering) {
            return original;
        }
        return original + 2.0f * ChatHeads.CONFIG.threeDeeNess();
    }

    @ModifyArg(method={"draw"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/font/PlayerHeadGlyphs$HeadGlyph;drawInternal(Lorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumer;IFFFFFIFFIIII)V", ordinal=1), index=5, require = 0)
    public float topHat(float original) {
        if (!ChatHeads.customHeadRendering) {
            return original;
        }
        return original - ChatHeads.CONFIG.threeDeeNess();
    }

    @ModifyArg(method={"draw"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/font/PlayerHeadGlyphs$HeadGlyph;drawInternal(Lorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumer;IFFFFFIFFIIII)V", ordinal=1), index=6, require = 0)
    public float bottomHat(float original) {
        if (!ChatHeads.customHeadRendering) {
            return original;
        }
        return original + ChatHeads.CONFIG.threeDeeNess();
    }
}

