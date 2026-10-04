package rtx.nv.mixin.chatheads.fabric;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import rtx.nv.api.mods.chatheads.ChatHeads;

@Mixin(net.minecraft.client.network.message.MessageHandler.class)

public abstract class ChatListenerMixin {
    @ModifyArg(method={"method_45745"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/ChatHud;method_1812(Lnet/minecraft/class_2561;)V", ordinal=0), require = 0)
    public Text chatheads_handleAddedDisguisedMessage(Text message) {
        return ChatHeads.handleAddedMessage(message, null);
    }
}

