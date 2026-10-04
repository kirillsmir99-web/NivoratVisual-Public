package rtx.nv.mixin.chatheads;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import java.util.UUID;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.mods.chatheads.ChatHeads;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPacketListenerMixin {
    @Shadow
    @Nullable
    public abstract PlayerListEntry getPlayerListEntry(UUID var1);

    @Inject(method="onChatMessage", at={@At(value="INVOKE", target="Lnet/minecraft/client/network/message/MessageHandler;onChatMessage(Lnet/minecraft/network/message/SignedMessage;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/network/message/MessageType$Parameters;)V")}, require = 0)
    public void chatheads_captureSenderInfo(ChatMessageS2CPacket packet, CallbackInfo ci, @Share(value="senderInfo") LocalRef<PlayerListEntry> senderInfo) {
        PlayerListEntry playerInfo = this.getPlayerListEntry(packet.sender());
        senderInfo.set(playerInfo);
    }

    @ModifyArg(method="onChatMessage", at=@At(value="INVOKE", target="Lnet/minecraft/client/network/message/MessageHandler;onChatMessage(Lnet/minecraft/network/message/SignedMessage;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/network/message/MessageType$Parameters;)V"), index=0, require = 0)
    public SignedMessage chatheads_rememberSenderInfo(SignedMessage playerChatMessage, @Share(value="senderInfo") LocalRef<PlayerListEntry> senderInfo) {
        ChatHeads.setOwner(playerChatMessage, (PlayerListEntry)senderInfo.get());
        return playerChatMessage;
    }
}
