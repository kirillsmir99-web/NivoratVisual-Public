package rtx.nv.api.events.impl.network;
import net.minecraft.network.packet.Packet;
import rtx.nv.api.events.CancellableEvent;

public final class PacketReceiveEvent
extends CancellableEvent {
    private final Packet<?> packet;

    public PacketReceiveEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public <T extends Packet<?>> T getPacketAs(Class<T> clazz) {
        return (T)(clazz.isInstance(this.packet) ? this.packet : null);
    }
}

