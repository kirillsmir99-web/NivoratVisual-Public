package rtx.nv.mixin.chatheads;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import rtx.nv.api.mods.chatheads.ChatHeads;

@Mixin(net.minecraft.client.network.message.MessageHandler.class)

public abstract class ChatListenerMixin {
    @ModifyArg(method="processChatMessageInternal", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/ChatHud;method_44811(Lnet/minecraft/class_2561;Lnet/minecraft/class_7469;Lnet/minecraft/class_7591;)V", ordinal=0), require = 0)
    public Text chatheads_handleAddedPlayerMessage(Text message, @Local(argsOnly=true) SignedMessage playerChatMessage) {
        return ChatHeads.handleAddedMessage(message, ChatHeads.getOwner(playerChatMessage));
    }

    @ModifyArg(method="onGameMessage", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/ChatHud;method_1812(Lnet/minecraft/class_2561;)V", ordinal=0), require = 0)
    public Text chatheads_handleAddedSystemMessage(Text message) {
        if (ChatHeads.CONFIG.handleSystemMessages()) {
            return ChatHeads.handleAddedMessage(message, null);
        }
        return message;
    }
}

