package rtx.nv.mixin.chatheads;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.mods.chatheads.ChatHeads;

@Mixin(net.minecraft.client.gui.hud.ChatHud.class)

public abstract class ChatComponentRenderMixin {
    @Inject(method="render", at={@At(value="HEAD")}, require = 0)
    public void chatheads_isInsideChat(CallbackInfo ci) {
        ChatHeads.customHeadRendering = true;
    }

    @Inject(method="render", at={@At(value="RETURN")}, require = 0)
    public void chatheads_isOutsideChat(CallbackInfo ci) {
        ChatHeads.customHeadRendering = false;
    }
}

