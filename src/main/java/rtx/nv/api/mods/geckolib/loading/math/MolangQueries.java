package rtx.nv.api.mods.geckolib.loading.math;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentHolder;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.ai.pathing.AmphibiousSwimNavigation;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.SpiderNavigation;
import net.minecraft.entity.ai.pathing.SwimNavigation;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animation.AnimationController;
import rtx.nv.api.mods.geckolib.animation.state.ControllerState;
import rtx.nv.api.mods.geckolib.constant.DataTickets;
import rtx.nv.api.mods.geckolib.loading.math.value.Variable;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.util.ClientUtil;

public final class MolangQueries {
    public static final String ACTOR_COUNT = "query.actor_count";
    public static final String ANIM_TIME = "query.anim_time";
    public static final String BLOCK_STATE = "query.block_state";
    public static final String BLOCKING = "query.blocking";
    public static final String BODY_X_ROTATION = "query.body_x_rotation";
    public static final String BODY_Y_ROTATION = "query.body_y_rotation";
    public static final String CAN_CLIMB = "query.can_climb";
    public static final String CAN_FLY = "query.can_fly";
    public static final String CAN_SWIM = "query.can_swim";
    public static final String CAN_WALK = "query.can_walk";
    public static final String CARDINAL_FACING = "query.cardinal_facing";
    public static final String CARDINAL_FACING_2D = "query.cardinal_facing_2d";
    public static final String CARDINAL_PLAYER_FACING = "query.cardinal_player_facing";
    public static final String CONTROLLER_SPEED = "query.controller_speed";
    public static final String DAY = "query.day";
    public static final String DEATH_TICKS = "query.death_ticks";
    public static final String DISTANCE_FROM_CAMERA = "query.distance_from_camera";
    public static final String EQUIPMENT_COUNT = "query.equipment_count";
    public static final String FRAME_ALPHA = "query.frame_alpha";
    public static final String GET_ACTOR_INFO_ID = "query.get_actor_info_id";
    public static final String GROUND_SPEED = "query.ground_speed";
    public static final String HAS_CAPE = "query.has_cape";
    public static final String HAS_COLLISION = "query.has_collision";
    public static final String HAS_GRAVITY = "query.has_gravity";
    public static final String HAS_HEAD_GEAR = "query.has_head_gear";
    public static final String HAS_OWNER = "query.has_owner";
    public static final String HAS_PLAYER_RIDER = "query.has_player_rider";
    public static final String HAS_RIDER = "query.has_rider";
    public static final String HEAD_X_ROTATION = "query.head_x_rotation";
    public static final String HEAD_Y_ROTATION = "query.head_y_rotation";
    public static final String HEALTH = "query.health";
    public static final String HURT_TIME = "query.hurt_time";
    public static final String INVULNERABLE_TICKS = "query.invulnerable_ticks";
    public static final String IS_ALIVE = "query.is_alive";
    public static final String IS_ANGRY = "query.is_angry";
    public static final String IS_BABY = "query.is_baby";
    public static final String IS_BREATHING = "query.is_breathing";
    public static final String IS_ENCHANTED = "query.is_enchanted";
    public static final String IS_FIRE_IMMUNE = "query.is_fire_immune";
    public static final String IS_FIRST_PERSON = "query.is_first_person";
    public static final String IS_IN_CONTACT_WITH_WATER = "query.is_in_contact_with_water";
    public static final String IS_IN_LAVA = "query.is_in_lava";
    public static final String IS_IN_WATER = "query.is_in_water";
    public static final String IS_IN_WATER_OR_RAIN = "query.is_in_water_or_rain";
    public static final String IS_INVISIBLE = "query.is_invisible";
    public static final String IS_LEASHED = "query.is_leashed";
    public static final String IS_MOVING = "query.is_moving";
    public static final String IS_ON_FIRE = "query.is_on_fire";
    public static final String IS_ON_GROUND = "query.is_on_ground";
    public static final String IS_RIDING = "query.is_riding";
    public static final String IS_SADDLED = "query.is_saddled";
    public static final String IS_SILENT = "query.is_silent";
    public static final String IS_SLEEPING = "query.is_sleeping";
    public static final String IS_SNEAKING = "query.is_sneaking";
    public static final String IS_SPRINTING = "query.is_sprinting";
    public static final String IS_STACKABLE = "query.is_stackable";
    public static final String IS_SWIMMING = "query.is_swimming";
    public static final String IS_USING_ITEM = "query.is_using_item";
    public static final String IS_WALL_CLIMBING = "query.is_wall_climbing";
    public static final String ITEM_MAX_USE_DURATION = "query.item_max_use_duration";
    public static final String LIFE_TIME = "query.life_time";
    public static final String LIMB_SWING = "query.limb_swing";
    public static final String LIMB_SWING_AMOUNT = "query.limb_swing_amount";
    public static final String MAIN_HAND_ITEM_MAX_DURATION = "query.main_hand_item_max_duration";
    public static final String MAIN_HAND_ITEM_USE_DURATION = "query.main_hand_item_use_duration";
    public static final String MAX_DURABILITY = "query.max_durability";
    public static final String MAX_HEALTH = "query.max_health";
    public static final String MOON_BRIGHTNESS = "query.moon_brightness";
    public static final String MOON_PHASE = "query.moon_phase";
    public static final String MOVEMENT_DIRECTION = "query.movement_direction";
    public static final String PLAYER_LEVEL = "query.player_level";
    public static final String REMAINING_DURABILITY = "query.remaining_durability";
    public static final String RIDER_BODY_X_ROTATION = "query.rider_body_x_rotation";
    public static final String RIDER_BODY_Y_ROTATION = "query.rider_body_y_rotation";
    public static final String RIDER_HEAD_X_ROTATION = "query.rider_head_x_rotation";
    public static final String RIDER_HEAD_Y_ROTATION = "query.rider_head_y_rotation";
    public static final String SCALE = "query.scale";
    public static final String SLEEP_ROTATION = "query.sleep_rotation";
    public static final String TIME_OF_DAY = "query.time_of_day";
    public static final String TIME_STAMP = "query.time_stamp";
    public static final String VERTICAL_SPEED = "query.vertical_speed";
    public static final String YAW_SPEED = "query.yaw_speed";
    private static final Map<String, Variable> VARIABLES = new Object2ObjectOpenHashMap();
    private static final Map<Variable, ToDoubleFunction<MolangQueries.Actor<? extends GeoAnimatable>>> ACTOR_VARIABLES = new Reference2ObjectOpenHashMap();

    static {
        MolangQueries.setDefaultQueryValues();
    }

    public static boolean isExistingVariable(String string) {
        return VARIABLES.containsKey(string);
    }

    public static <T extends GeoAnimatable> void buildActorVariables(MolangQueries.Actor<T> actor, Set<Variable> set, Reference2DoubleMap<Variable> reference2DoubleMap) {
        for (Variable variable : set) {
            ToDoubleFunction<MolangQueries.Actor<? extends GeoAnimatable>> toDoubleFunction = ACTOR_VARIABLES.get(variable);
            if (toDoubleFunction == null || reference2DoubleMap.containsKey(variable)) continue;
            reference2DoubleMap.put(variable, toDoubleFunction.applyAsDouble(actor));
        }
    }

    static Variable getVariableFor(String string2) {
        return VARIABLES.computeIfAbsent(MolangQueries.applyPrefixAliases(string2, "query.", "q."), string -> new Variable((String)string, 0.0));
    }

    static void registerVariable(Variable variable) {
        VARIABLES.put(variable.name(), variable);
    }

    private static void setDefaultBlockEntityQueryValues() {
        MolangQueries.setActorVariable(BLOCK_STATE, actor -> ((BlockEntity)actor.animatable).getCachedState().getBlock().getStateManager().getStates().indexOf((Object)((BlockEntity)actor.animatable).getCachedState()));
    }

    private static void setDefaultEntityQueryValues() {
        MolangQueries.setActorVariable(BODY_X_ROTATION, actor -> actor.animatable instanceof LivingEntity ? 0.0 : (double)((Entity)actor.animatable).getPitch(actor.partialTick));
        MolangQueries.setActorVariable(BODY_Y_ROTATION, actor -> {
            double d;
            Object t = actor.animatable;
            if (t instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)t;
                d = MathHelper.lerp((float)actor.partialTick, (float)livingEntity.lastBodyYaw, (float)livingEntity.bodyYaw);
            } else {
                d = ((Entity)actor.animatable).getYaw(actor.partialTick);
            }
            return d;
        });
        MolangQueries.setActorVariable(CARDINAL_FACING, actor -> ((Entity)actor.animatable).getHorizontalFacing().getIndex());
        MolangQueries.setActorVariable(CARDINAL_FACING_2D, actor -> {
            int n = ((Entity)actor.animatable).getHorizontalFacing().getIndex();
            return n < 2 ? 6.0 : (double)n;
        });
        MolangQueries.setActorVariable(DISTANCE_FROM_CAMERA, actor -> actor.cameraPos.distanceTo(((Entity)actor.animatable).getEntityPos()));
        MolangQueries.setActorVariable(GET_ACTOR_INFO_ID, actor -> ((Entity)actor.animatable).getId());
        MolangQueries.setActorVariable(EQUIPMENT_COUNT, actor -> {
            long l;
            Object t = actor.animatable;
            if (t instanceof EquipmentHolder) {
                EquipmentHolder equipmentHolder = (EquipmentHolder)t;
                l = Arrays.stream(EquipmentSlot.values()).filter(EquipmentSlot::isArmorSlot).filter(equipmentSlot -> !equipmentHolder.getEquippedStack(equipmentSlot).isEmpty()).count();
            } else {
                l = 0L;
            }
            return l;
        });
        MolangQueries.setActorVariable(HAS_COLLISION, actor -> !((Entity)actor.animatable).noClip ? 1.0 : 0.0);
        MolangQueries.setActorVariable(HAS_GRAVITY, actor -> !((Entity)actor.animatable).hasNoGravity() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(HAS_OWNER, actor -> {
            Tameable tameable;
            Object t = actor.animatable;
            return t instanceof Tameable && (tameable = (Tameable)t).getOwnerReference() != null ? 1 : 0;
        });
        MolangQueries.setActorVariable(HAS_PLAYER_RIDER, actor -> ((Entity)actor.animatable).hasPassenger(PlayerEntity.class::isInstance) ? 1.0 : 0.0);
        MolangQueries.setActorVariable(HAS_RIDER, actor -> ((Entity)actor.animatable).hasPassengers() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_ALIVE, actor -> ((Entity)actor.animatable).isAlive() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_ANGRY, actor -> {
            Angerable angerable;
            Object t = actor.animatable;
            return t instanceof Angerable && (angerable = (Angerable)t).hasAngerTime() ? 1 : 0;
        });
        MolangQueries.setActorVariable(IS_BREATHING, actor -> ((Entity)actor.animatable).getAir() >= ((Entity)actor.animatable).getMaxAir() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_FIRE_IMMUNE, actor -> ((Entity)actor.animatable).getType().isFireImmune() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_INVISIBLE, actor -> ((Entity)actor.animatable).isInvisible() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_IN_CONTACT_WITH_WATER, actor -> ((Entity)actor.animatable).isTouchingWaterOrRain() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_IN_LAVA, actor -> ((Entity)actor.animatable).isInLava() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_IN_WATER, actor -> ((Entity)actor.animatable).isTouchingWater() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_IN_WATER_OR_RAIN, actor -> ((Entity)actor.animatable).isTouchingWaterOrRain() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_LEASHED, actor -> {
            Leashable leashable;
            Object t = actor.animatable;
            return t instanceof Leashable && (leashable = (Leashable)t).isLeashed() ? 1 : 0;
        });
        MolangQueries.setActorVariable(IS_MOVING, actor -> actor.renderState.getOrDefaultGeckolibData(DataTickets.IS_MOVING, false) != false ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_ON_FIRE, actor -> ((Entity)actor.animatable).isOnFire() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_ON_GROUND, actor -> ((Entity)actor.animatable).isOnGround() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_RIDING, actor -> ((Entity)actor.animatable).hasVehicle() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_SADDLED, actor -> {
            EquipmentHolder equipmentHolder;
            Object t = actor.animatable;
            return t instanceof EquipmentHolder && !(equipmentHolder = (EquipmentHolder)t).getEquippedStack(EquipmentSlot.SADDLE).isEmpty() ? 1 : 0;
        });
        MolangQueries.setActorVariable(IS_SILENT, actor -> ((Entity)actor.animatable).isSilent() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_SNEAKING, actor -> ((Entity)actor.animatable).isInSneakingPose() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_SPRINTING, actor -> ((Entity)actor.animatable).isSprinting() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_SWIMMING, actor -> ((Entity)actor.animatable).isSwimming() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(MOVEMENT_DIRECTION, actor -> actor.renderState.getOrDefaultGeckolibData(DataTickets.IS_MOVING, false) != false ? (double)Direction.getFacing((Vec3d)((Entity)actor.animatable).getVelocity()).getIndex() : 6.0);
        MolangQueries.setActorVariable(RIDER_BODY_X_ROTATION, actor -> ((Entity)actor.animatable).hasPassengers() ? (((Entity)actor.animatable).getFirstPassenger() instanceof LivingEntity ? 0.0 : (double)((Entity)actor.animatable).getFirstPassenger().getPitch(actor.partialTick)) : 0.0);
        MolangQueries.setActorVariable(RIDER_BODY_Y_ROTATION, actor -> {
            double d;
            if (((Entity)actor.animatable).hasPassengers()) {
                Entity entity = ((Entity)actor.animatable).getFirstPassenger();
                if (entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity)entity;
                    d = MathHelper.lerp((float)actor.partialTick, (float)livingEntity.lastBodyYaw, (float)livingEntity.bodyYaw);
                } else {
                    d = ((Entity)actor.animatable).getFirstPassenger().getYaw(actor.partialTick);
                }
            } else {
                d = 0.0;
            }
            return d;
        });
        MolangQueries.setActorVariable(RIDER_HEAD_X_ROTATION, actor -> {
            double d;
            Entity entity = ((Entity)actor.animatable).getFirstPassenger();
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)entity;
                d = livingEntity.getPitch(actor.partialTick);
            } else {
                d = 0.0;
            }
            return d;
        });
        MolangQueries.setActorVariable(RIDER_HEAD_Y_ROTATION, actor -> {
            double d;
            Entity entity = ((Entity)actor.animatable).getFirstPassenger();
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)entity;
                d = livingEntity.getYaw(actor.partialTick);
            } else {
                d = 0.0;
            }
            return d;
        });
        MolangQueries.setActorVariable(VERTICAL_SPEED, actor -> ((Entity)actor.animatable).getVelocity().y);
        MolangQueries.setActorVariable(YAW_SPEED, actor -> ((Entity)actor.animatable).getYaw() - ((Entity)actor.animatable).lastYaw);
    }

    private static void setDefaultLivingEntityQueryValues() {
        MolangQueries.setActorVariable(BLOCKING, actor -> ((LivingEntity)actor.animatable).isBlocking() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(DEATH_TICKS, actor -> ((LivingEntity)actor.animatable).deathTime == 0 ? 0.0 : (double)((float)((LivingEntity)actor.animatable).deathTime + actor.partialTick));
        MolangQueries.setActorVariable(GROUND_SPEED, actor -> ((LivingEntity)actor.animatable).getVelocity().horizontalLength());
        MolangQueries.setActorVariable(HAS_HEAD_GEAR, actor -> !((LivingEntity)actor.animatable).getEquippedStack(EquipmentSlot.HEAD).isEmpty() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(HEAD_X_ROTATION, actor -> actor.renderState.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, Float.valueOf(((LivingEntity)actor.animatable).getPitch(actor.partialTick))).floatValue());
        MolangQueries.setActorVariable(HEAD_Y_ROTATION, actor -> actor.renderState.getOrDefaultGeckolibData(DataTickets.ENTITY_YAW, Float.valueOf(((LivingEntity)actor.animatable).getYaw(actor.partialTick))).floatValue());
        MolangQueries.setActorVariable(HEALTH, actor -> ((LivingEntity)actor.animatable).getHealth());
        MolangQueries.setActorVariable(HURT_TIME, actor -> ((LivingEntity)actor.animatable).hurtTime == 0 ? 0.0 : (double)((float)((LivingEntity)actor.animatable).hurtTime - actor.partialTick));
        MolangQueries.setActorVariable(INVULNERABLE_TICKS, actor -> ((LivingEntity)actor.animatable).timeUntilRegen == 0 ? 0.0 : (double)((float)((LivingEntity)actor.animatable).timeUntilRegen - actor.partialTick));
        MolangQueries.setActorVariable(IS_BABY, actor -> ((LivingEntity)actor.animatable).isBaby() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_SLEEPING, actor -> ((LivingEntity)actor.animatable).isSleeping() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_USING_ITEM, actor -> ((LivingEntity)actor.animatable).isUsingItem() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_WALL_CLIMBING, actor -> ((LivingEntity)actor.animatable).isClimbing() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(LIMB_SWING, actor -> ((LivingEntity)actor.animatable).limbAnimator.getAnimationProgress());
        MolangQueries.setActorVariable(LIMB_SWING_AMOUNT, actor -> ((LivingEntity)actor.animatable).limbAnimator.getAmplitude(actor.partialTick()));
        MolangQueries.setActorVariable(MAIN_HAND_ITEM_MAX_DURATION, actor -> ((LivingEntity)actor.animatable).getMainHandStack().getMaxUseTime((LivingEntity)actor.animatable));
        MolangQueries.setActorVariable(MAIN_HAND_ITEM_USE_DURATION, actor -> ((LivingEntity)actor.animatable).getActiveHand() == Hand.MAIN_HAND ? (double)((LivingEntity)actor.animatable).getItemUseTime() / 20.0 + (double)actor.partialTick : 0.0);
        MolangQueries.setActorVariable(MAX_HEALTH, actor -> ((LivingEntity)actor.animatable).getMaxHealth());
        MolangQueries.setActorVariable(SCALE, actor -> ((LivingEntity)actor.animatable).getScale());
        MolangQueries.setActorVariable(SLEEP_ROTATION, actor -> Optional.ofNullable(((LivingEntity)actor.animatable).getSleepingDirection()).map(Direction::getPositiveHorizontalDegrees).orElse(Float.valueOf(0.0f)).floatValue());
    }

    public static <T extends GeoAnimatable> void setVariableFunction(String string, ToDoubleFunction<ControllerState> toDoubleFunction) {
        Variable variable = MolangQueries.getVariableFor(string);
        if (ACTOR_VARIABLES.containsKey(variable)) {
            throw new IllegalArgumentException("Cannot replace actor variables");
        }
        variable.set(toDoubleFunction);
    }

    private static void setDefaultQueryValues() {
        MolangQueries.setVariableValue("PI", Math.PI);
        MolangQueries.setVariableValue("E", Math.E);
        MolangQueries.setActorVariable(ACTOR_COUNT, actor -> ClientUtil.getVisibleEntityCount());
        MolangQueries.setActorVariable(ANIM_TIME, actor -> actor.controller.getCurrentAnimationTime());
        MolangQueries.setActorVariable(CONTROLLER_SPEED, actor -> actor.controller.getAnimationSpeed());
        MolangQueries.setActorVariable(CARDINAL_PLAYER_FACING, actor -> actor.clientPlayer.getHorizontalFacing().ordinal());
        MolangQueries.setActorVariable(DAY, actor -> (double)actor.level.getTime() / 24000.0);
        MolangQueries.setActorVariable(FRAME_ALPHA, actor -> actor.partialTick);
        MolangQueries.setActorVariable(HAS_CAPE, actor -> ClientUtil.clientPlayerHasCape() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_FIRST_PERSON, actor -> ClientUtil.isFirstPerson() ? 1.0 : 0.0);
        MolangQueries.setActorVariable(LIFE_TIME, actor -> actor.renderTime / 20.0);
        MolangQueries.setActorVariable(MOON_BRIGHTNESS, actor -> DimensionType.MOON_SIZES[ClientUtil.getClientMoonPhase().getIndex()]);
        MolangQueries.setActorVariable(MOON_PHASE, actor -> ClientUtil.getClientMoonPhase().getIndex());
        MolangQueries.setActorVariable(PLAYER_LEVEL, actor -> actor.clientPlayer.experienceLevel);
        MolangQueries.setActorVariable(TIME_OF_DAY, actor -> (double)actor.level.getTimeOfDay() / 24000.0);
        MolangQueries.setActorVariable(TIME_STAMP, actor -> actor.level.getTime());
        MolangQueries.setDefaultBlockEntityQueryValues();
        MolangQueries.setDefaultEntityQueryValues();
        MolangQueries.setDefaultLivingEntityQueryValues();
        MolangQueries.setDefaultMobQueryValues();
        MolangQueries.setDefaultItemQueryValues();
    }

    private static String applyPrefixAliases(String string, String string2, String ... stringArray) {
        for (String string3 : stringArray) {
            if (!string.startsWith(string3)) continue;
            return string2 + string.substring(string3.length());
        }
        return string;
    }

    private static void setDefaultMobQueryValues() {
        MolangQueries.setActorVariable(CAN_CLIMB, actor -> !((MobEntity)actor.animatable).isAiDisabled() && ((MobEntity)actor.animatable).getNavigation() instanceof SpiderNavigation ? 1.0 : 0.0);
        MolangQueries.setActorVariable(CAN_FLY, actor -> !((MobEntity)actor.animatable).isAiDisabled() && ((MobEntity)actor.animatable).getNavigation() instanceof BirdNavigation ? 1.0 : 0.0);
        MolangQueries.setActorVariable(CAN_SWIM, actor -> !((MobEntity)actor.animatable).isAiDisabled() && ((MobEntity)actor.animatable).getNavigation() instanceof SwimNavigation || ((MobEntity)actor.animatable).getNavigation() instanceof AmphibiousSwimNavigation ? 1.0 : 0.0);
        MolangQueries.setActorVariable(CAN_WALK, actor -> !((MobEntity)actor.animatable).isAiDisabled() && ((MobEntity)actor.animatable).getNavigation() instanceof MobNavigation || ((MobEntity)actor.animatable).getNavigation() instanceof AmphibiousSwimNavigation ? 1.0 : 0.0);
    }

    public static void setVariableValue(String string, double d) {
        Variable variable = MolangQueries.getVariableFor(string);
        if (ACTOR_VARIABLES.containsKey(variable)) {
            throw new IllegalArgumentException("Cannot replace actor variables");
        }
        variable.set(d);
    }

    @SuppressWarnings("unchecked")
    public static <T extends GeoAnimatable> void setActorVariable(String string, ToDoubleFunction<MolangQueries.Actor<T>> toDoubleFunction) {
        Variable variable = MolangQueries.getVariableFor(string);
        ACTOR_VARIABLES.put(variable, (ToDoubleFunction<MolangQueries.Actor<? extends GeoAnimatable>>) (ToDoubleFunction<?>) toDoubleFunction);
        variable.set(controllerState -> controllerState.getQueryValue(variable));
    }

    private static void setDefaultItemQueryValues() {
        MolangQueries.setActorVariable(IS_ENCHANTED, actor -> actor.renderState.getGeckolibData(DataTickets.IS_ENCHANTED) != false ? 1.0 : 0.0);
        MolangQueries.setActorVariable(IS_STACKABLE, actor -> actor.renderState.getGeckolibData(DataTickets.IS_STACKABLE) != false ? 1.0 : 0.0);
        MolangQueries.setActorVariable(ITEM_MAX_USE_DURATION, actor -> actor.renderState.getGeckolibData(DataTickets.MAX_USE_DURATION).intValue());
        MolangQueries.setActorVariable(MAX_DURABILITY, actor -> actor.renderState.getGeckolibData(DataTickets.MAX_DURABILITY).intValue());
        MolangQueries.setActorVariable(REMAINING_DURABILITY, actor -> actor.renderState.getGeckolibData(DataTickets.REMAINING_DURABILITY).intValue());
    }


    public static record Actor<T extends GeoAnimatable>(T animatable, GeoRenderState renderState, AnimationController<?> controller, double renderTime, float partialTick, World level, PlayerEntity clientPlayer, Vec3d cameraPos) {
    }
}

