package rtx.nv.api.mods.geckolib.network.packet;
import java.util.function.Consumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.CustomPayload;

public interface MultiloaderPacket
extends CustomPayload {
    public void receiveMessage(PlayerEntity var1, Consumer<Runnable> var2);
}

