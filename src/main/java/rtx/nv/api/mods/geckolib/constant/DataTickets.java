package rtx.nv.api.mods.geckolib.constant;

import com.google.common.reflect.TypeToken;
import it.unimi.dsi.fastutil.Pair;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animation.AnimationController;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.animation.state.KeyFrameEvent;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.CustomInstructionKeyframeData;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import rtx.nv.api.mods.geckolib.cache.animation.keyframeevent.SoundKeyframeData;
import rtx.nv.api.mods.geckolib.constant.dataticket.DataTicket;
import rtx.nv.api.mods.geckolib.constant.dataticket.SerializableDataTicket;
import rtx.nv.api.mods.geckolib.renderer.GeoArmorRenderer;
import rtx.nv.api.mods.geckolib.renderer.GeoItemRenderer;

public final class DataTickets {
    private static final Map<Identifier, SerializableDataTicket<?>> SERIALIZABLE_TICKETS = new ConcurrentHashMap<>();

    public static final DataTicket<Void> RESET_RENDER_PASS = DataTicket.of("reset_render_pass", Void.class);
    public static final DataTicket<Entity> ENTITY = DataTicket.of("entity", Entity.class);
    public static final DataTicket<BlockEntity> BLOCK_ENTITY = DataTicket.of("block_entity", BlockEntity.class);
    public static final DataTicket<ItemStack> ITEMSTACK = DataTicket.of("itemstack", ItemStack.class);
    public static final DataTicket<EquipmentSlot> EQUIPMENT_SLOT = DataTicket.of("equipment_slot", EquipmentSlot.class);
    public static final DataTicket<GeoArmorRenderer.StackForRender> STACK_FOR_RENDER = DataTicket.of("stack_for_render", GeoArmorRenderer.StackForRender.class);
    public static final DataTicket<GeoItemRenderer.StackForRender> ITEM_RENDER_DATA = DataTicket.of("item_render_data", GeoItemRenderer.StackForRender.class);
    public static final DataTicket<ControllerState[]> ANIMATION_CONTROLLER_STATES = DataTicket.of("animation_controller_states", ControllerState[].class);
    public static final DataTicket<Double> TICK = DataTicket.of("tick", Double.class);
    public static final DataTicket<KeyFrameEvent<?, CustomInstructionKeyframeData>> CUSTOM_INSTRUCTION_KEYFRAME_EVENT = DataTicket.of("custom_instruction_keyframe_event", new TypeToken<KeyFrameEvent<?, CustomInstructionKeyframeData>>() {});
    public static final DataTicket<KeyFrameEvent<?, ParticleKeyframeData>> PARTICLE_KEYFRAME_EVENT = DataTicket.of("particle_keyframe_event", new TypeToken<KeyFrameEvent<?, ParticleKeyframeData>>() {});
    public static final DataTicket<KeyFrameEvent<?, SoundKeyframeData>> SOUND_KEYFRAME_EVENT = DataTicket.of("sound_keyframe_event", new TypeToken<KeyFrameEvent<?, SoundKeyframeData>>() {});
    public static final DataTicket<Direction> DIRECTION = DataTicket.of("direction", Direction.class);
    public static final DataTicket<Integer> RENDER_COLOR = DataTicket.of("render_color", Integer.class);
    public static final DataTicket<Integer> PACKED_LIGHT = DataTicket.of("packed_light", Integer.class);
    public static final DataTicket<Integer> PACKED_OVERLAY = DataTicket.of("packed_overlay", Integer.class);
    public static final DataTicket<Long> ANIMATABLE_INSTANCE_ID = DataTicket.of("animatable_instance_id", Long.class);
    public static final DataTicket<Object> ANIMATABLE_MANAGER = DataTicket.of("animatable_manager", Object.class);
    public static final DataTicket<Float> PARTIAL_TICK = DataTicket.of("partial_tick", Float.class);
    public static final DataTicket<Boolean> IS_MOVING = DataTicket.of("is_moving", Boolean.class);
    public static final DataTicket<Class> ANIMATABLE_CLASS = DataTicket.of("animatable_class", Class.class);
    public static final DataTicket<java.util.EnumMap> PER_SLOT_RENDER_DATA = DataTicket.of("per_slot_render_data", java.util.EnumMap.class);
    public static final DataTicket<Vec3d> POSITION = DataTicket.of("position", Vec3d.class);
    public static final DataTicket<Boolean> IS_GECKOLIB_WEARER = DataTicket.of("is_geckolib_wearer", Boolean.class);
    public static final DataTicket<Boolean> HAS_GLINT = DataTicket.of("has_glint", Boolean.class);
    public static final DataTicket<net.minecraft.client.render.entity.model.BipedEntityModel> HUMANOID_MODEL = DataTicket.of("humanoid_model", net.minecraft.client.render.entity.model.BipedEntityModel.class);
    public static final DataTicket<Float> ENTITY_PITCH = DataTicket.of("entity_pitch", Float.class);
    public static final DataTicket<Float> ENTITY_YAW = DataTicket.of("entity_yaw", Float.class);
    public static final DataTicket<Boolean> IS_ENCHANTED = DataTicket.of("is_enchanted", Boolean.class);
    public static final DataTicket<Boolean> IS_STACKABLE = DataTicket.of("is_stackable", Boolean.class);
    public static final DataTicket<Integer> MAX_USE_DURATION = DataTicket.of("max_use_duration", Integer.class);
    public static final DataTicket<Integer> MAX_DURABILITY = DataTicket.of("max_durability", Integer.class);
    public static final DataTicket<Integer> REMAINING_DURABILITY = DataTicket.of("remaining_durability", Integer.class);
    public static final DataTicket<Boolean> INVISIBLE_TO_PLAYER = DataTicket.of("invisible_to_player", Boolean.class);
    public static final DataTicket<net.minecraft.entity.EntityPose> ENTITY_POSE = DataTicket.of("entity_pose", net.minecraft.entity.EntityPose.class);
    public static final DataTicket<Float> ENTITY_BODY_YAW = DataTicket.of("entity_body_yaw", Float.class);
    public static final DataTicket<Boolean> IS_SHAKING = DataTicket.of("is_shaking", Boolean.class);
    public static final DataTicket<Vec3d> VELOCITY = DataTicket.of("velocity", Vec3d.class);
    public static final DataTicket<BlockPos> BLOCKPOS = DataTicket.of("blockpos", BlockPos.class);
    public static final DataTicket<Boolean> SPRINTING = DataTicket.of("sprinting", Boolean.class);
    public static final DataTicket<Boolean> IS_CROUCHING = DataTicket.of("is_crouching", Boolean.class);
    public static final DataTicket<Boolean> SWINGING_ARM = DataTicket.of("swinging_arm", Boolean.class);
    public static final DataTicket<Boolean> IS_DEAD_OR_DYING = DataTicket.of("is_dead_or_dying", Boolean.class);

    private DataTickets() {
    }

    public static <D> SerializableDataTicket<D> registerSerializable(SerializableDataTicket<D> ticket) {
        SERIALIZABLE_TICKETS.put(ticket.getRegisteredId(), ticket);
        return ticket;
    }

    public static SerializableDataTicket<?> byName(Identifier id) {
        return SERIALIZABLE_TICKETS.get(id);
    }
}
