package rtx.nv.mixin.chatanim;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.BetterMinecraft;

@Mixin(ChatHud.class)
public abstract class ChatComponentMixin {
    @Shadow
    private int scrolledLines;

    @Shadow
    public abstract int getLineHeight();

    @Unique
    private long nv_chatAnimLastMessageTime = 0L;

    @Unique
    private float nv_chatAnimCalculateDisplacement() {
        if (!BetterMinecraft.chatAnimationsEnabled() || this.scrolledLines != 0) {
            return 0.0f;
        }
        float fadeTime = (float) BetterMinecraft.chatAnimDurationMs();
        float maxDisplacement = (float)this.getLineHeight() * (BetterMinecraft.isReducedIntensity() ? 0.45f : 0.85f);
        long lifetime = System.currentTimeMillis() - this.nv_chatAnimLastMessageTime;
        if (lifetime >= (long)fadeTime) {
            return 0.0f;
        }
        float progress = Math.min((float)lifetime / fadeTime, 1.0f);
        float ease = 1.0f - (1.0f - progress) * (1.0f - progress) * (1.0f - progress);
        return maxDisplacement * (1.0f - ease);
    }

    @WrapOperation(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V"), require = 0)
    private void nv_chatAnimWrapRender(ChatHud instance, ChatHud.Backend queueMessage, int restrictedMessageWidth, int restrictedMessage, boolean focused, Operation<Void> original, @Local(argsOnly = true) DrawContext graphics) {
        float displacement = this.nv_chatAnimCalculateDisplacement();
        if (displacement != 0.0f) {
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(0.0f, displacement);
        }
        original.call(instance, queueMessage, restrictedMessageWidth, restrictedMessage, focused);
        if (displacement != 0.0f) {
            graphics.getMatrices().popMatrix();
        }
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("TAIL"), require = 0)
    private void nv_chatAnimAddMessage(Text contents, CallbackInfo ci) {
        if (BetterMinecraft.chatAnimationsEnabled()) {
            this.nv_chatAnimLastMessageTime = System.currentTimeMillis();
        }
    }
}
