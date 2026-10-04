package rtx.nv.mixin;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import rtx.nv.api.chat.commands.helpers.AnimatedChatText;

@Mixin(net.minecraft.client.gui.hud.ChatHud.class)

public abstract class AnimatedChatHeaderMixin {
    @ModifyArgs(method="method_75802", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/ChatHud$Backend;text(IFLnet/minecraft/text/OrderedText;)Z"), require = 0)
    private static void nv_animateHeader(Args args) {
        OrderedText text = (OrderedText)args.get(2);
        if (text != null && AnimatedChatText.hasSentinel(text)) {
            args.set(2, (Object)AnimatedChatText.animate(text));
        }
    }
}

