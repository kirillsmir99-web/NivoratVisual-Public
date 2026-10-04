package rtx.nv.api.mods.geckolib.service;
import java.util.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import rtx.nv.api.mods.geckolib.GeckoLibServices;
import rtx.nv.api.mods.geckolib.animatable.SingletonGeoAnimatable;
import rtx.nv.api.mods.geckolib.cache.SyncedSingletonAnimatableCache;
import rtx.nv.api.mods.geckolib.constant.dataticket.SerializableDataTicket;
import rtx.nv.api.mods.geckolib.network.packet.MultiloaderPacket;
import rtx.nv.api.mods.geckolib.network.packet.blockentity.BlockEntityAnimTriggerPacket;
import rtx.nv.api.mods.geckolib.network.packet.blockentity.BlockEntityDataSyncPacket;
import rtx.nv.api.mods.geckolib.network.packet.blockentity.StatelessBlockEntityPlayAnimPacket;
import rtx.nv.api.mods.geckolib.network.packet.blockentity.StatelessBlockEntityStopAnimPacket;
import rtx.nv.api.mods.geckolib.network.packet.blockentity.StopTriggeredBlockEntityAnimPacket;
import rtx.nv.api.mods.geckolib.network.packet.entity.EntityAnimTriggerPacket;
import rtx.nv.api.mods.geckolib.network.packet.entity.EntityDataSyncPacket;
import rtx.nv.api.mods.geckolib.network.packet.entity.StatelessEntityPlayAnimPacket;
import rtx.nv.api.mods.geckolib.network.packet.entity.StatelessEntityStopAnimPacket;
import rtx.nv.api.mods.geckolib.network.packet.entity.StopTriggeredEntityAnimPacket;
import rtx.nv.api.mods.geckolib.network.packet.singleton.SingletonAnimTriggerPacket;
import rtx.nv.api.mods.geckolib.network.packet.singleton.SingletonDataSyncPacket;
import rtx.nv.api.mods.geckolib.network.packet.singleton.StatelessSingletonPlayAnimPacket;
import rtx.nv.api.mods.geckolib.network.packet.singleton.StatelessSingletonStopAnimPacket;
import rtx.nv.api.mods.geckolib.network.packet.singleton.StopTriggeredSingletonAnimPacket;

public interface GeckoLibNetworking {
    public static void init() {
        GeckoLibNetworking.registerPacket(BlockEntityDataSyncPacket.TYPE, BlockEntityDataSyncPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(EntityDataSyncPacket.TYPE, EntityDataSyncPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(SingletonDataSyncPacket.TYPE, SingletonDataSyncPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(BlockEntityAnimTriggerPacket.TYPE, BlockEntityAnimTriggerPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(SingletonAnimTriggerPacket.TYPE, SingletonAnimTriggerPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(EntityAnimTriggerPacket.TYPE, EntityAnimTriggerPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(StopTriggeredBlockEntityAnimPacket.TYPE, StopTriggeredBlockEntityAnimPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(StopTriggeredEntityAnimPacket.TYPE, StopTriggeredEntityAnimPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(StopTriggeredSingletonAnimPacket.TYPE, StopTriggeredSingletonAnimPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(StatelessEntityPlayAnimPacket.TYPE, StatelessEntityPlayAnimPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(StatelessBlockEntityPlayAnimPacket.TYPE, StatelessBlockEntityPlayAnimPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(StatelessSingletonPlayAnimPacket.TYPE, StatelessSingletonPlayAnimPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(StatelessEntityStopAnimPacket.TYPE, StatelessEntityStopAnimPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(StatelessBlockEntityStopAnimPacket.TYPE, StatelessBlockEntityStopAnimPacket.CODEC, true);
        GeckoLibNetworking.registerPacket(StatelessSingletonStopAnimPacket.TYPE, StatelessSingletonStopAnimPacket.CODEC, true);
    }

    public void sendToAllPlayersTrackingBlock(MultiloaderPacket var1, ServerWorld var2, BlockPos var3);

    default public void stopTriggeredBlockEntityAnim(BlockPos blockPos, ServerWorld serverWorld, String string, String string2) {
        this.sendToAllPlayersTrackingBlock(new StopTriggeredBlockEntityAnimPacket(blockPos, Optional.ofNullable(string), Optional.ofNullable(string2)), serverWorld, blockPos);
    }

    public void sendToAllPlayersTrackingEntity(MultiloaderPacket var1, Entity var2);

    public <B extends PacketByteBuf, P extends MultiloaderPacket> void registerPacketInternal(CustomPayload.Id<P> var1, PacketCodec<B, P> var2, boolean var3);

    public void sendToPlayer(MultiloaderPacket var1, ServerPlayerEntity var2);

    default public <D> void syncBlockEntityAnimData(BlockPos blockPos, SerializableDataTicket<D> serializableDataTicket, D d, ServerWorld serverWorld) {
        this.sendToAllPlayersTrackingBlock(new BlockEntityDataSyncPacket(blockPos, serializableDataTicket, d), serverWorld, blockPos);
    }

    default public <D> void syncEntityAnimData(Entity entity, boolean bl, SerializableDataTicket<D> serializableDataTicket, D d) {
        this.sendToAllPlayersTrackingEntity(new EntityDataSyncPacket(entity.getId(), bl, serializableDataTicket, d), entity);
    }

    default public <D> void syncSingletonAnimData(SingletonGeoAnimatable singletonGeoAnimatable, long l, SerializableDataTicket<D> serializableDataTicket, D d, Entity entity) {
        this.sendToAllPlayersTrackingEntity(new SingletonDataSyncPacket(SyncedSingletonAnimatableCache.getOrCreateId((SingletonGeoAnimatable)singletonGeoAnimatable), l, serializableDataTicket, d), entity);
    }

    private static <B extends PacketByteBuf, P extends MultiloaderPacket> void registerPacket(CustomPayload.Id<P> id, PacketCodec<B, P> packetCodec, boolean bl) {
        GeckoLibServices.NETWORK.registerPacketInternal(id, packetCodec, bl);
    }

    default public void triggerEntityAnim(Entity entity, boolean bl, String string, String string2) {
        this.sendToAllPlayersTrackingEntity(new EntityAnimTriggerPacket(entity.getId(), bl, Optional.ofNullable(string), string2), entity);
    }

    default public void triggerBlockEntityAnim(BlockPos blockPos, ServerWorld serverWorld, String string, String string2) {
        this.sendToAllPlayersTrackingBlock(new BlockEntityAnimTriggerPacket(blockPos, Optional.ofNullable(string), string2), serverWorld, blockPos);
    }

    default public void stopTriggeredEntityAnim(Entity entity, boolean bl, String string, String string2) {
        this.sendToAllPlayersTrackingEntity(new StopTriggeredEntityAnimPacket(entity.getId(), bl, Optional.ofNullable(string), Optional.ofNullable(string2)), entity);
    }

    default public void triggerSingletonAnim(SingletonGeoAnimatable singletonGeoAnimatable, Entity entity, long l, String string, String string2) {
        this.sendToAllPlayersTrackingEntity(new SingletonAnimTriggerPacket(SyncedSingletonAnimatableCache.getOrCreateId((SingletonGeoAnimatable)singletonGeoAnimatable), l, Optional.ofNullable(string), string2), entity);
    }

    default public void stopTriggeredSingletonAnim(SingletonGeoAnimatable singletonGeoAnimatable, Entity entity, long l, String string, String string2) {
        this.sendToAllPlayersTrackingEntity(new StopTriggeredSingletonAnimPacket(SyncedSingletonAnimatableCache.getOrCreateId((SingletonGeoAnimatable)singletonGeoAnimatable), l, Optional.ofNullable(string), Optional.ofNullable(string2)), entity);
    }
}

