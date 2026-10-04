package rtx.nv.mixin.chatheads;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import rtx.nv.api.mods.chatheads.ChatHeads;
import rtx.nv.api.mods.chatheads.HeadData;
import rtx.nv.api.mods.chatheads.config.RenderPosition;

@Mixin(targets = {"net.minecraft.client.gui.hud.ChatHud"})

public abstract class ChatComponentInnerMixin {
    @ModifyArgs(method={"method_75802"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/ChatHud$Backend;text(IFLnet/minecraft/text/OrderedText;)Z"), require = 0)
    private static void chatheads_renderChatHeadAndOffsetChatMessage(Args args, @Local(argsOnly=true) ChatHudLine.Visible line, @Share(value="chatOffset") LocalIntRef chatOffset) {
        if (ChatHeads.CONFIG.renderPosition() == RenderPosition.BEFORE_LINE) {
            int y = (Integer)args.get(0);
            float opacity = ((Float)args.get(1)).floatValue();
            HeadData headData = ChatHeads.getHeadData(line);
            chatOffset.set(ChatHeads.getChatOffset(headData));
            if (ChatHeads.guiGraphics != null && headData != HeadData.EMPTY) {
                ChatHeads.renderChatHead(ChatHeads.guiGraphics, 0, y, headData.playerInfo, opacity);
            }
            ChatHeads.chatGraphicsAccess.updatePose(matrix3x2f -> matrix3x2f.translate((float)chatOffset.get(), 0.0f));
        }
    }

    @Inject(method={"method_75802"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/ChatHud$Backend;text(IFLnet/minecraft/text/OrderedText;)Z", shift=At.Shift.AFTER)}, require = 0)
    private static void chatheads_undoChatMessageOffset(CallbackInfo ci, @Share(value="chatOffset") LocalIntRef chatOffset) {
        if (ChatHeads.CONFIG.renderPosition() == RenderPosition.BEFORE_LINE) {
            ChatHeads.chatGraphicsAccess.updatePose(matrix3x2f -> matrix3x2f.translate((float)(-chatOffset.get()), 0.0f));
        }
    }

    @Inject(method={"method_75802"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/ChatHud$Backend;indicatorIcon(IIZLnet/minecraft/client/gui/hud/MessageIndicator;Lnet/minecraft/client/gui/hud/MessageIndicator$Icon;)V")}, require = 0)
    private static void chatheads_offsetTagIcon(CallbackInfo ci, @Share(value="chatOffset") LocalIntRef chatOffset) {
        if (ChatHeads.CONFIG.renderPosition() == RenderPosition.BEFORE_LINE) {
            ChatHeads.chatGraphicsAccess.updatePose(matrix3x2f -> matrix3x2f.translate((float)chatOffset.get(), 0.0f));
        }
    }

    @Inject(method={"method_75802"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/ChatHud$Backend;indicatorIcon(IIZLnet/minecraft/client/gui/hud/MessageIndicator;Lnet/minecraft/client/gui/hud/MessageIndicator$Icon;)V", shift=At.Shift.AFTER)}, require = 0)
    private static void chatheads_undoTagIconOffset(CallbackInfo ci, @Share(value="chatOffset") LocalIntRef chatOffset) {
        if (ChatHeads.CONFIG.renderPosition() == RenderPosition.BEFORE_LINE) {
            ChatHeads.chatGraphicsAccess.updatePose(matrix3x2f -> matrix3x2f.translate((float)(-chatOffset.get()), 0.0f));
        }
    }
}

