package rtx.nv.api.mods.chatheads.mixininterface;
import net.minecraft.client.network.PlayerListEntry;

public interface Ownable {
    public void chatheads_setOwner(PlayerListEntry var1);

    public PlayerListEntry chatheads_getOwner();
}

