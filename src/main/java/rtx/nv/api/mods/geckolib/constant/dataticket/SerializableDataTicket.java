package rtx.nv.api.mods.geckolib.constant.dataticket;

import com.google.common.reflect.TypeToken;
import it.unimi.dsi.fastutil.Pair;
import java.lang.reflect.Type;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.mods.geckolib.constant.DataTickets;

public final class SerializableDataTicket<D> extends DataTicket<D> {
    public static final PacketCodec<RegistryByteBuf, SerializableDataTicket<?>> STREAM_CODEC = PacketCodec.tuple(Identifier.PACKET_CODEC, SerializableDataTicket::getRegisteredId, SerializableDataTicket::enforceValidTicket);
    private final PacketCodec<? super RegistryByteBuf, D> streamCodec;
    private final Identifier registeredId;

    private SerializableDataTicket(Identifier identifier, Class<? extends D> clazz, Type type, PacketCodec<? super RegistryByteBuf, D> packetCodec) {
        super(identifier.toString(), clazz, type);
        this.registeredId = identifier;
        this.streamCodec = packetCodec;
    }

    public static <D> SerializableDataTicket<D> of(Identifier identifier, Class<? extends D> clazz, Type type, PacketCodec<? super RegistryByteBuf, D> packetCodec) {
        return (SerializableDataTicket<D>) IDENTITY_CACHE.computeIfAbsent((Pair<Type, String>) Pair.of(type, identifier.toString()), pair -> DataTickets.registerSerializable(new SerializableDataTicket<D>(identifier, clazz, type, packetCodec)));
    }

    public static <D> SerializableDataTicket<D> create(Identifier identifier, Class<? extends D> clazz, TypeToken<D> typeToken, PacketCodec<? super RegistryByteBuf, D> packetCodec) {
        return SerializableDataTicket.of(identifier, clazz, typeToken.getType(), packetCodec);
    }

    public static <D> SerializableDataTicket<D> create(Identifier identifier, Class<? extends D> clazz, PacketCodec<? super RegistryByteBuf, D> packetCodec) {
        return SerializableDataTicket.of(identifier, clazz, clazz, packetCodec);
    }

    public static SerializableDataTicket<String> ofString(Identifier identifier) {
        return SerializableDataTicket.create(identifier, String.class, PacketCodecs.STRING);
    }

    public static SerializableDataTicket<Boolean> ofBoolean(Identifier identifier) {
        return SerializableDataTicket.create(identifier, Boolean.class, PacketCodecs.BOOLEAN);
    }

    public static SerializableDataTicket<Integer> ofInt(Identifier identifier) {
        return SerializableDataTicket.create(identifier, Integer.class, PacketCodecs.INTEGER);
    }

    public static SerializableDataTicket<?> enforceValidTicket(Identifier identifier) throws IllegalStateException {
        SerializableDataTicket<?> serializableDataTicket = DataTickets.byName(identifier);
        if (serializableDataTicket == null) {
            throw new IllegalStateException("Attempted to retrieve a SerializableDataTicket that does not exist! Likely didn't register the ticket properly: " + String.valueOf(identifier));
        }
        return serializableDataTicket;
    }

    public Identifier getRegisteredId() {
        return this.registeredId;
    }

    public PacketCodec<? super RegistryByteBuf, D> streamCodec() {
        return this.streamCodec;
    }

    public static SerializableDataTicket<Vec3d> ofVec3(Identifier identifier) {
        return SerializableDataTicket.create(identifier, Vec3d.class, PacketCodecs.VECTOR_3F.xmap(Vec3d::new, Vec3d::toVector3f));
    }

    public static <E extends Enum<E>> SerializableDataTicket<E> ofEnum(Identifier identifier, Class<E> clazz) {
        PacketCodec<RegistryByteBuf, E> codec = new PacketCodec<RegistryByteBuf, E>() {
            @Override
            public E decode(RegistryByteBuf buf) {
                return Enum.valueOf(clazz, buf.readString());
            }

            @Override
            public void encode(RegistryByteBuf buf, E value) {
                buf.writeString(value.name());
            }
        };
        return SerializableDataTicket.create(identifier, clazz, codec);
    }

    public static SerializableDataTicket<BlockPos> ofBlockPos(Identifier identifier) {
        return SerializableDataTicket.create(identifier, BlockPos.class, (PacketCodec) BlockPos.PACKET_CODEC);
    }
}
