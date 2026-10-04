package rtx.nv.mixin.chatheads;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.font.DrawnSpriteGlyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import rtx.nv.api.mods.chatheads.ChatHeads;

@Mixin(DrawnSpriteGlyph.class)
public interface PlainTextRenderableMixin {
    @Shadow
    int shadowColor();

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/DrawnSpriteGlyph;shadowColor()I"), require = 0)
    default int chatheads_disableShadow(int original) {
        return ChatHeads.customHeadRendering && !ChatHeads.CONFIG.drawShadow() ? 0 : original;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/DrawnSpriteGlyph;draw(Lorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumer;IFFFI)V", ordinal = 1), index = 4, require = 0)
    default float chatheads_moveDownWhenShadowDisabled(float offsetY) {
        return ChatHeads.customHeadRendering && !ChatHeads.CONFIG.drawShadow() && this.shadowColor() != 0 ? offsetY + 1.0f : offsetY;
    }
}
