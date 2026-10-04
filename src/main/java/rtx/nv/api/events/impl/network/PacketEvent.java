package rtx.nv.api.events.impl.network;
import net.minecraft.network.packet.Packet;
import rtx.nv.api.events.CancellableEvent;

public final class PacketEvent
extends CancellableEvent {
    private Packet<?> packet;
    private final PacketEvent.Direction direction;

    public PacketEvent(Packet<?> packet, PacketEvent.Direction direction) {
        this.packet = packet;
        this.direction = direction;
    }

    public boolean is(Class<? extends Packet<?>> clazz) {
        return clazz.isInstance(this.packet);
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public boolean isReceive() {
        return this.direction == PacketEvent.Direction.RECEIVE;
    }

    public boolean isSend() {
        return this.direction == PacketEvent.Direction.SEND;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }

    public PacketEvent.Direction direction() {
        return this.direction;
    }


    public static enum Direction {
        SEND,
        RECEIVE;
    
    }
}

