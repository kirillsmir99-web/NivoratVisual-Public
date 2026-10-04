package rtx.nv.mixin.chatheads;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.mods.chatheads.ChatHeads;

@Mixin(net.minecraft.client.gui.hud.ChatHud.class)

public abstract class ChatComponentMixin {
    @Inject(method="render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at={@At(value="HEAD")}, require = 1)
    private void chatheads_captureGuiGraphics(CallbackInfo ci, @Local(argsOnly=true) DrawContext guiGraphics) {
        ChatHeads.guiGraphics = guiGraphics;
    }

    @Inject(method="render(Lnet/minecraft/client/font/DrawnTextConsumer;IIZ)V", at={@At(value="HEAD")}, require = 1)
    private void chatheads_noGraphics(CallbackInfo ci) {
        ChatHeads.guiGraphics = null;
    }

    @Inject(method="render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at={@At(value="HEAD")}, require = 1)
    private void chatheads_captureChatGraphicsAccess(CallbackInfo ci, @Local(argsOnly=true) ChatHud.Backend chatGraphicsAccess) {
        ChatHeads.chatGraphicsAccess = chatGraphicsAccess;
    }

    @ModifyArg(method={"method_75802", "method_75802"}, at=@At(value="INVOKE", target="Lnet/minecraft/class_338$class_12233;method_75809(IIIII)V"), index=2, require = 0)
    private static int chatheads_fixTextOverflow(int original) {
        return original + ChatHeads.getTextWidthDifference(ChatHeads.getLineData());
    }

    @Inject(method="render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at={@At(value="RETURN")}, require = 1)
    private void chatheads_forgetGraphics(CallbackInfo ci) {
        ChatHeads.guiGraphics = null;
        ChatHeads.chatGraphicsAccess = null;
    }

    @Inject(method="addMessage", at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/ChatHud;method_1815(Lnet/minecraft/class_303;)V")}, require = 0)
    private void chatheads_nonRefreshingPath(CallbackInfo ci) {
        ChatHeads.refreshing = false;
    }

    @ModifyArg(method="refresh", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/ChatHud;method_1815(Lnet/minecraft/class_303;)V"), require = 0)
    private ChatHudLine chatheads_transferMessageOwner(ChatHudLine guiMessage) {
        ChatHeads.refreshing = true;
        ChatHeads.refreshingLineData = ChatHeads.getHeadData(guiMessage);
        return guiMessage;
    }

    @Inject(method="refresh", at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/ChatHud;method_1815(Lnet/minecraft/class_303;)V", shift=At.Shift.AFTER)}, require = 0)
    private void chatheads_finishedRefreshing(CallbackInfo ci) {
        ChatHeads.refreshing = false;
    }
}

