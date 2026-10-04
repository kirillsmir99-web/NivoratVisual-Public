package rtx.nv.mixin.chatheads;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.message.SignedMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rtx.nv.api.mods.chatheads.mixininterface.Ownable;

@Mixin(net.minecraft.network.message.SignedMessage.class)

public abstract class PlayerChatMessageMixin
implements Ownable {
    @Unique
    private PlayerListEntry chatheads_owner;

    @Override
    public void chatheads_setOwner(PlayerListEntry playerInfo) {
        this.chatheads_owner = playerInfo;
    }

    @Override
    public PlayerListEntry chatheads_getOwner() {
        return this.chatheads_owner;
    }
}

