package rtx.nv.api.liteapi.packets;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record LiteApiPayload(String json) implements CustomPayload
{
    public static final Identifier CHANNEL = Identifier.of((String)"liteapi", (String)"feature-control");
    public static final CustomPayload.Id<LiteApiPayload> TYPE = new CustomPayload.Id(CHANNEL);
    private static final int MAX_BYTES = 262144;
    public static final PacketCodec<PacketByteBuf, LiteApiPayload> CODEC = PacketCodec.ofStatic((packetByteBuf, liteApiPayload) -> {
        byte[] byArray = liteApiPayload.json().getBytes(StandardCharsets.UTF_8);
        packetByteBuf.writeBytes(byArray);
    }, packetByteBuf -> {
        try {
            int n = packetByteBuf.readableBytes();
            if (n <= 0 || n > 262144) {
                if (n > 0) {
                    packetByteBuf.skipBytes(n);
                }
                return new LiteApiPayload("");
            }
            byte[] byArray = new byte[n];
            packetByteBuf.readBytes(byArray);
            return new LiteApiPayload(new String(byArray, StandardCharsets.UTF_8));
        }
        catch (RuntimeException runtimeException) {
            if (packetByteBuf.isReadable()) {
                packetByteBuf.skipBytes(packetByteBuf.readableBytes());
            }
            return new LiteApiPayload("");
        }
    });

    public CustomPayload.Id<LiteApiPayload> getId() {
        return TYPE;
    }
}

