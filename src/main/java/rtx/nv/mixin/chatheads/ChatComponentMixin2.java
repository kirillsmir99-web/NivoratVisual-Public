package rtx.nv.mixin.chatheads;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.mods.chatheads.ChatHeads;
import rtx.nv.api.mods.chatheads.HeadData;

@Mixin(net.minecraft.client.gui.hud.ChatHud.class)

public abstract class ChatComponentMixin2 {
    @Inject(method="addVisibleMessage", at={@At(value="HEAD")}, require = 0)
    private void chatheads_transferMessageOwner(ChatHudLine guiMessage, CallbackInfo ci) {
        ChatHeads.lineData = ChatHeads.lastSenderData;
    }

    @ModifyArg(method="addMessage", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/ChatHud;method_1815(Lnet/minecraft/class_303;)V"), require = 0)
    private ChatHudLine chatheads_setOwner(ChatHudLine message) {
        ChatHeads.setHeadData(message, ChatHeads.lastSenderData);
        return message;
    }

    @ModifyArg(method="addMessage", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/ChatHud;method_58744(Lnet/minecraft/class_303;)V"), require = 0)
    private ChatHudLine chatheads_setOwner2(ChatHudLine message) {
        ChatHeads.setHeadData(message, ChatHeads.lastSenderData);
        return message;
    }

    @Inject(method="addMessage", at={@At(value="RETURN")}, require = 0)
    private void chatheads_forgetSender(CallbackInfo ci) {
        ChatHeads.lastSenderData = HeadData.EMPTY;
    }
}

