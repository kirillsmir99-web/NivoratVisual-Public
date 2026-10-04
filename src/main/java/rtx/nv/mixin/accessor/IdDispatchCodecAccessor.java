package rtx.nv.mixin.accessor;

import java.util.List;
import net.minecraft.network.handler.PacketCodecDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PacketCodecDispatcher.class)
public interface IdDispatchCodecAccessor {
    @Accessor("packetTypes")
    List<?> nv_getById();
}
