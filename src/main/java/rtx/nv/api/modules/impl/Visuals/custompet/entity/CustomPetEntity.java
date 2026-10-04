package rtx.nv.api.modules.impl.Visuals.custompet.entity;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.GeoEntity;
import rtx.nv.api.mods.geckolib.animatable.instance.AnimatableInstanceCache;
import rtx.nv.api.mods.geckolib.animatable.manager.AnimatableManager;
import rtx.nv.api.mods.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import rtx.nv.api.mods.geckolib.animation.AnimationController;
import rtx.nv.api.mods.geckolib.animation.RawAnimation;
import rtx.nv.api.mods.geckolib.animation.object.PlayState;
import rtx.nv.api.mods.geckolib.animation.state.AnimationTest;
import rtx.nv.api.mods.geckolib.util.GeckoLibUtil;
import rtx.nv.api.modules.impl.Visuals.custompet.CustomPetVariant;
import rtx.nv.api.modules.impl.Visuals.custompet.sync.CustomPetRemoteState;

public class CustomPetEntity
extends FrogEntity
implements GeoEntity {
    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(-20000);
    private static final float GROUND_FULL_SPEED_ANGLE = 6.0f;
    private static final float GROUND_MIN_SPEED_ANGLE = 84.0f;
    private static final float AIR_START_MOVE_ANGLE = 9.0f;
    private static final double MOVEMENT_ANIMATION_THRESHOLD_SQR = 2.5E-5;
    private static final double MOVEMENT_VERTICAL_THRESHOLD = 0.003;
    private static SoundEvent ambientSound;
    private static SoundEvent stepSound;
    private static boolean soundsResolved;
    private static final RawAnimation IDLE;
    private static final RawAnimation WALK;
    private static final RawAnimation RAIN_IDLE;
    private static final RawAnimation RAIN_WALK;
    private static final RawAnimation HAT_IDLE;
    private static final RawAnimation HAT_WALK;
    private static final RawAnimation FISHERMAN_IDLE;
    private static final RawAnimation FISHERMAN_WALK;
    private static final RawAnimation AIR_IDLE;
    private static final RawAnimation AIR_WALK;
    private static final RawAnimation ROBOT_IDLE;
    private static final RawAnimation ROBOT_FLY;
    private static final RawAnimation OWL_IDLE;
    private static final RawAnimation OWL_WALK;
    private static final RawAnimation OWL_RUN;
    private static final RawAnimation OWL_FLY;
    private static final RawAnimation FANTASY_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation FANTASY_FLIGHT = RawAnimation.begin().thenLoop("flight");
    private static final RawAnimation FANTASY_ACCEL = RawAnimation.begin().thenLoop("accelerate");
    private static final double OWL_RUN_ANIMATION_SPEED = 1.4;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Vec3d desiredPosition = Vec3d.ZERO;
    private double desiredSpeed;
    private boolean desiredMoving;
    private boolean desiredUmbrella;
    private boolean desiredAirborne;
    private CustomPetVariant variant = CustomPetVariant.NITWIT;
    private boolean owl;
    private int robotType;
    private boolean moving;
    private boolean usingUmbrella;
    private boolean airborneMode;
    private int movingTicks;
    private int ambientSoundCooldown;
    private int stepSoundCooldown;
    private int jumpCooldown;
    private int blockedTicks;
    private int pauseTicks;
    private int sideStepDir;
    private int sideStepTicks;
    private float targetYaw;
    private float smoothedYaw;
    private float lookOverrideYaw;
    private float lookOverrideWeight;
    private double animationSpeed = 1.0;
    private double currentAnimationSpeed = 1.0;
    private double currentGroundSpeed;
    private double verticalVelocity;
    private double lastHorizontalDistance = Double.MAX_VALUE;
    private Vec3d airMotion = Vec3d.ZERO;
    private boolean networkControlled;
    private boolean hasNetworkAnchor;
    private Vec3d networkPosition = Vec3d.ZERO;
    private float networkYaw;
    private boolean networkMoving;
    private boolean networkUmbrella;
    private boolean networkAirborne;
    private double networkAnimationSpeed = 1.0;

    public CustomPetEntity(World world) {
        super(EntityType.FROG, world);
        this.noClip = false;
        this.setNoGravity(false);
        this.setAiDisabled(true);
        this.setId(NEXT_ENTITY_ID.getAndDecrement());
        this.setUuid(UUID.randomUUID());
        this.setVelocity(Vec3d.ZERO);
        this.targetYaw = this.getYaw();
        this.smoothedYaw = this.getYaw();
        this.ambientSoundCooldown = 70;
        this.verticalVelocity = 0.0;
    }

    static {
        IDLE = RawAnimation.begin().thenPlay("idle");
        WALK = RawAnimation.begin().thenPlay("walk");
        RAIN_IDLE = RawAnimation.begin().thenPlay("idle_holding_1");
        RAIN_WALK = RawAnimation.begin().thenPlay("walk_holding_1");
        HAT_IDLE = RawAnimation.begin().thenPlay("idle_holding_hat");
        HAT_WALK = RawAnimation.begin().thenPlay("walk_holding_hat");
        FISHERMAN_IDLE = RawAnimation.begin().thenPlay("idle_holding_fisherman");
        FISHERMAN_WALK = RawAnimation.begin().thenPlay("walk_holding_fisherman");
        AIR_IDLE = RawAnimation.begin().thenPlay("idle_holding_2");
        AIR_WALK = RawAnimation.begin().thenPlay("walk_holding_2");
        ROBOT_IDLE = RawAnimation.begin().thenPlay("robot_idle");
        ROBOT_FLY = RawAnimation.begin().thenPlay("robot_fly");
        OWL_IDLE = RawAnimation.begin().thenPlay("animation.owl_jump_rope.skip_idle");
        OWL_WALK = RawAnimation.begin().thenPlay("animation.owl_jump_rope.walk_skip");
        OWL_RUN = RawAnimation.begin().thenPlay("animation.owl_jump_rope.run_skip");
        OWL_FLY = RawAnimation.begin().thenPlay("animation.owl_jump_rope.fly");
    }

    private <E extends GeoAnimatable> PlayState predicate(AnimationTest<E> animationTest) {
        RawAnimation rawAnimation;
        AnimationController animationController = animationTest.controller();
        animationController.animationSpeed = this.currentAnimationSpeed;
        if (this.variant.isFantasy()) {
            double spd = this.currentAnimationSpeed;
            if (this.moving && spd > 1.35) {
                animationController.animationSpeed = Math.clamp(spd * 0.9, 1.0, 1.8);
                animationController.setAnimation(FANTASY_ACCEL);
            } else if (this.moving) {
                animationController.animationSpeed = Math.clamp(spd, 0.9, 1.4);
                animationController.setAnimation(FANTASY_FLIGHT);
            } else {
                animationController.animationSpeed = 0.85;
                animationController.setAnimation(FANTASY_IDLE);
            }
            return PlayState.CONTINUE;
        }
        if (this.owl) {
            RawAnimation rawAnimation2 = this.airborneMode ? OWL_FLY : (!this.moving ? OWL_IDLE : (this.currentAnimationSpeed >= 1.4 ? OWL_RUN : OWL_WALK));
            animationController.setAnimation(rawAnimation2);
            return PlayState.CONTINUE;
        }
        if (this.variant.isRobot()) {
            rawAnimation = this.moving || this.airborneMode ? ROBOT_FLY : ROBOT_IDLE;
        } else if (this.airborneMode) {
            rawAnimation = AIR_IDLE;
        } else {
            rawAnimation = switch (this.variant) {
                default -> throw new MatchException(null, null);
                case CustomPetVariant.FISHERMAN -> {
                    if (this.usingUmbrella) {
                        if (this.moving) {
                            yield FISHERMAN_WALK;
                        }
                        yield FISHERMAN_IDLE;
                    }
                    if (this.moving) {
                        yield AIR_WALK;
                    }
                    yield AIR_IDLE;
                }
                case CustomPetVariant.GARDENER, CustomPetVariant.SORCERER -> {
                    if (this.usingUmbrella) {
                        if (this.moving) {
                            yield HAT_WALK;
                        }
                        yield HAT_IDLE;
                    }
                    if (this.moving) {
                        yield WALK;
                    }
                    yield IDLE;
                }
                case CustomPetVariant.DEFAULT, CustomPetVariant.NITWIT, CustomPetVariant.MERCHANT -> {
                    if (this.usingUmbrella) {
                        if (this.moving) {
                            yield RAIN_WALK;
                        }
                        yield RAIN_IDLE;
                    }
                    if (this.moving) {
                        yield WALK;
                    }
                    yield IDLE;
                }
                case CustomPetVariant.ROBOT -> this.moving ? ROBOT_FLY : ROBOT_IDLE;
            };
        }
        animationController.setAnimation(rawAnimation);
        return PlayState.CONTINUE;
    }

    private double square(double d) {
        return d * d;
    }

    public boolean isOwl() {
        return this.owl;
    }

    public void setOwl(boolean bl) {
        this.owl = bl;
    }

    private static SoundEvent stepSound() {
        CustomPetEntity.resolveSounds();
        return stepSound;
    }

    public void snapTo(Vec3d vec3d, float f) {
        this.setPosition(vec3d.x, vec3d.y, vec3d.z);
        this.lastX = vec3d.x;
        this.lastY = vec3d.y;
        this.lastZ = vec3d.z;
        this.lastYaw = f;
        this.lastPitch = 0.0f;
        this.moving = false;
        this.usingUmbrella = false;
        this.airborneMode = false;
        this.movingTicks = 0;
        this.stepSoundCooldown = 0;
        this.ambientSoundCooldown = 40;
        this.jumpCooldown = 0;
        this.blockedTicks = 0;
        this.pauseTicks = 0;
        this.sideStepTicks = 0;
        this.targetYaw = f;
        this.smoothedYaw = f;
        this.animationSpeed = 1.0;
        this.currentAnimationSpeed = 1.0;
        this.currentGroundSpeed = 0.0;
        this.verticalVelocity = 0.0;
        this.lastHorizontalDistance = Double.MAX_VALUE;
        this.airMotion = Vec3d.ZERO;
        this.setVelocity(Vec3d.ZERO);
        this.setYaw(f);
        this.setHeadYaw(f);
        this.setBodyYaw(f);
        this.networkPosition = vec3d;
        this.networkYaw = f;
        this.hasNetworkAnchor = false;
    }

    public int getMaxLookPitchChange() {
        return 0;
    }

    public int getMaxHeadRotation() {
        return 0;
    }

    public void tick() {
        if (this.networkControlled) {
            super.tick();
            this.tickNetworkState();
            return;
        }
        this.setNoGravity(this.desiredAirborne);
        super.tick();
        this.tickCustomMovement();
    }

    public float getStepHeight() {
        return this.airborneMode ? 0.0f : 1.0f;
    }

    public boolean shouldRender(double distance) {
        return distance < 48.0 * 48.0;
    }

    public boolean canHit() {
        return false;
    }

    public float getLerpedYaw(float tickProgress) {
        return MathHelper.lerpAngleDegrees((float)tickProgress, (float)this.lastYaw, (float)this.getYaw());
    }

    public boolean isPushable() {
        return false;
    }

    public void pushAwayFrom(Entity entity) {
    }

    public void addVelocity(double deltaX, double deltaY, double deltaZ) {
    }

    public void addVelocity(Vec3d vec) {
    }

    public boolean collidesWith(Entity other) {
        return false;
    }

    public boolean isCollidable(Entity entity) {
        return false;
    }

    public boolean isAttackable() {
        return false;
    }

    public PistonBehavior getPistonBehavior() {
        return PistonBehavior.IGNORE;
    }

    public boolean isPetMoving() {
        return this.moving;
    }

    public double getCurrentAnimationSpeed() {
        return this.currentAnimationSpeed;
    }

    public void applyNetworkState(CustomPetRemoteState customPetRemoteState) {
        if (customPetRemoteState == null) {
            this.networkControlled = false;
            this.hasNetworkAnchor = false;
            return;
        }
        this.networkControlled = true;
        this.setPetVariant(customPetRemoteState.variant());
        this.setRobotType(customPetRemoteState.robotType());
        this.networkPosition = customPetRemoteState.position();
        this.networkYaw = customPetRemoteState.yaw();
        this.networkMoving = customPetRemoteState.moving();
        this.networkUmbrella = customPetRemoteState.umbrella();
        this.networkAirborne = customPetRemoteState.airborne();
        this.networkAnimationSpeed = customPetRemoteState.animationSpeed();
    }

    public void setRobotType(int n) {
        this.robotType = n;
    }

    public boolean shouldUseUmbrella() {
        return this.usingUmbrella;
    }

    public boolean isAirborneMode() {
        return this.airborneMode;
    }

    public void setPetVariant(CustomPetVariant customPetVariant) {
        this.variant = customPetVariant == null ? CustomPetVariant.NITWIT : customPetVariant;
    }

    private static SoundEvent ambientSound() {
        CustomPetEntity.resolveSounds();
        return ambientSound;
    }

    public int getRobotType() {
        return this.robotType;
    }

    public boolean isMovementBlocked() {
        return this.pauseTicks > 0 || this.blockedTicks >= 3;
    }

    private void tickNetworkState() {
        double d;
        double d2;
        double d3;
        double d4;
        this.movingTicks = 0;
        this.jumpCooldown = 0;
        this.blockedTicks = 0;
        this.pauseTicks = 0;
        this.currentGroundSpeed = 0.0;
        this.verticalVelocity = 0.0;
        this.airMotion = Vec3d.ZERO;
        this.lastHorizontalDistance = Double.MAX_VALUE;
        this.airborneMode = this.networkAirborne;
        this.usingUmbrella = this.networkUmbrella && !this.networkAirborne && !this.variant.isRobot();
        this.setNoGravity(true);
        this.setVelocity(Vec3d.ZERO);
        double d5 = this.getX();
        double d6 = this.getY();
        double d7 = this.getZ();
        double d8 = this.networkPosition.x - d5;
        double d9 = this.networkPosition.y - d6;
        double d10 = this.networkPosition.z - d7;
        double d11 = d8 * d8 + d9 * d9 + d10 * d10;
        if (!this.hasNetworkAnchor || d11 > 36.0) {
            d4 = this.networkPosition.x;
            d3 = this.networkPosition.y;
            d2 = this.networkPosition.z;
            this.lastX = d4;
            this.lastY = d3;
            this.lastZ = d2;
            this.hasNetworkAnchor = true;
        } else {
            d = 0.35;
            d4 = d5 + d8 * d;
            d3 = d6 + d9 * d;
            d2 = d7 + d10 * d;
            this.lastX = d5;
            this.lastY = d6;
            this.lastZ = d7;
        }
        this.setPosition(d4, d3, d2);
        d = (d4 - this.lastX) * (d4 - this.lastX) + (d2 - this.lastZ) * (d2 - this.lastZ);
        boolean bl = d > 2.5E-5 || Math.abs(d3 - this.lastY) > 0.003;
        this.moving = this.networkMoving && bl;
        this.currentAnimationSpeed = this.animationSpeed = this.moving ? this.networkAnimationSpeed : 1.0;
        this.lastYaw = this.getYaw();
        this.targetYaw = this.networkYaw;
        this.smoothedYaw = MathHelper.stepUnwrappedAngleTowards((float)this.getYaw(), (float)this.networkYaw, (float)18.0f);
        this.setYaw(this.smoothedYaw);
        this.setHeadYaw(this.smoothedYaw);
        this.setBodyYaw(this.smoothedYaw);
    }

    private void tickClientAudio(boolean bl, double d) {
        SoundEvent soundEvent;
        if (this.getEntityWorld() == null || !this.emitsAudioCues()) {
            return;
        }
        if (this.variant.isRobot()) {
            return;
        }
        if (this.ambientSoundCooldown > 0) {
            --this.ambientSoundCooldown;
        }
        if (this.stepSoundCooldown > 0) {
            --this.stepSoundCooldown;
        }
        if (bl && d > 4.0E-4 && this.isOnGround() && this.stepSoundCooldown <= 0) {
            soundEvent = CustomPetEntity.stepSound();
            if (soundEvent != null) {
                this.getEntityWorld().playSoundClient(this.getX(), this.getY(), this.getZ(), soundEvent, this.getSoundCategory(), 0.55f, 0.94f + this.random.nextFloat() * 0.12f, false);
            }
            this.stepSoundCooldown = 8 + this.random.nextInt(4);
        }
        if (!bl && this.ambientSoundCooldown <= 0) {
            soundEvent = CustomPetEntity.ambientSound();
            if (soundEvent != null) {
                this.getEntityWorld().playSoundClient(this.getX(), this.getY(), this.getZ(), soundEvent, this.getSoundCategory(), 0.75f, 0.94f + this.random.nextFloat() * 0.14f, false);
            }
            this.ambientSoundCooldown = 120 + this.random.nextInt(100);
        }
    }

    public CustomPetVariant getPetVariant() {
        return this.variant;
    }

    private void snapToGroundIfClose() {
        if (this.getEntityWorld() == null) {
            return;
        }
        double d = this.getX();
        double d2 = this.getY();
        double d3 = this.getZ();
        int n = MathHelper.floor((double)d);
        int n2 = MathHelper.floor((double)d3);
        int n3 = MathHelper.floor((double)(d2 + 0.2));
        BlockPos.Mutable mutable = new BlockPos.Mutable(n, n3 + 1, n2);
        for (int i = n3 + 1; i >= n3 - 4; --i) {
            double d4;
            double d5;
            mutable.set(n, i, n2);
            BlockState blockState = this.getEntityWorld().getBlockState((BlockPos)mutable);
            VoxelShape voxelShape = blockState.getCollisionShape((BlockView)(Object)this.getEntityWorld(), (BlockPos)mutable);
            if (voxelShape.isEmpty() || !((d5 = d2 - (d4 = (double)i + voxelShape.getMax(Direction.Axis.Y))) >= -0.08) || !(d5 <= 0.72)) continue;
            this.setPosition(d, d4, d3);
            this.verticalVelocity = 0.0;
            this.fallDistance = 0.0;
            this.setVelocity(Vec3d.ZERO);
            return;
        }
    }

    private static void resolveSounds() {
        if (soundsResolved) {
            return;
        }
        soundsResolved = true;
        try {
            ambientSound = SoundEvent.of((Identifier)Identifier.of((String)"nv", (String)"entity.ribbit.ambient"));
            stepSound = SoundEvent.of((Identifier)Identifier.of((String)"nv", (String)"entity.ribbit.step"));
        }
        catch (Throwable throwable) {
            ambientSound = null;
            stepSound = null;
        }
    }

    protected boolean emitsAudioCues() {
        return !this.owl && !this.variant.isFantasy();
    }

    public void setBehavior(Vec3d vec3d, double d, boolean bl, boolean bl2, boolean bl3, double d2) {
        if (vec3d.squaredDistanceTo(this.desiredPosition) > 0.2) {
            this.blockedTicks = 0;
            this.pauseTicks = 0;
            this.lastHorizontalDistance = Double.MAX_VALUE;
        }
        this.desiredPosition = vec3d;
        this.desiredSpeed = d;
        this.desiredMoving = bl;
        this.desiredUmbrella = bl2;
        this.desiredAirborne = bl3;
        this.animationSpeed = d2;
    }

    private void tickAirMovement() {
        boolean bl;
        double d;
        double d2;
        float f;
        boolean bl2;
        Vec3d vec3d = this.getEntityPos();
        Vec3d vec3d2 = this.desiredPosition.subtract(vec3d);
        Vec3d vec3d3 = new Vec3d(vec3d2.x, 0.0, vec3d2.z);
        double d3 = vec3d3.horizontalLength();
        double d4 = vec3d2.length();
        double d5 = Math.abs(vec3d2.y);
        boolean bl3 = bl2 = this.desiredMoving && d4 > 0.02;
        if (bl2 && d3 > 0.06) {
            f = (float)Math.toDegrees(MathHelper.atan2((double)vec3d3.z, (double)vec3d3.x)) - 90.0f;
            this.targetYaw = MathHelper.stepUnwrappedAngleTowards((float)this.targetYaw, (float)f, (float)9.0f);
        } else if (this.lookOverrideWeight > 0.05f) {
            this.targetYaw = MathHelper.stepUnwrappedAngleTowards((float)this.targetYaw, (float)this.lookOverrideYaw, (float)(4.0f * this.lookOverrideWeight));
        }
        this.smoothedYaw = MathHelper.stepUnwrappedAngleTowards((float)this.smoothedYaw, (float)this.targetYaw, (float)(bl2 ? 10.0f : 5.0f));
        f = Math.abs(MathHelper.wrapDegrees((float)(this.targetYaw - this.smoothedYaw)));
        boolean bl4 = bl2 && (f <= 9.0f || d4 <= 0.35);
        this.movingTicks = 0;
        this.jumpCooldown = 0;
        this.blockedTicks = 0;
        this.pauseTicks = 0;
        this.verticalVelocity = 0.0;
        boolean bl5 = d3 <= 0.16 && d5 <= 0.012;
        boolean bl6 = d3 <= 0.42 && d5 <= 0.09;
        Vec3d vec3d4 = Vec3d.ZERO;
        if (bl4 && d4 > 1.0E-5 && !bl5) {
            d2 = MathHelper.clamp((double)((d4 - 0.2) / 1.6), (double)0.08, (double)1.0);
            d = Math.clamp(this.desiredSpeed * d2, 0.012, 6.5);
            vec3d4 = vec3d2.normalize().multiply(Math.min(d4, d));
        } else if (!this.desiredMoving && d5 > 0.001) {
            d2 = MathHelper.clamp((double)(d5 * 0.38), (double)0.0025, (double)0.018);
            vec3d4 = new Vec3d(0.0, Math.copySign(Math.min(d5, d2), vec3d2.y), 0.0);
        }
        d2 = Math.clamp(0.12 + d4 * 0.06 + this.desiredSpeed * 0.028, 0.12, 0.38);
        this.airMotion = this.airMotion.lerp(vec3d4, d2);
        if (!this.desiredMoving) {
            this.airMotion = new Vec3d(this.airMotion.x * 0.4, this.airMotion.y, this.airMotion.z * 0.4);
        }
        d = bl5 ? 0.18 : (bl6 ? 0.42 : (d4 < 1.5 ? 0.78 : 0.9));
        boolean bl7 = bl = !this.desiredMoving && d5 > 0.001;
        if (!(bl4 && !(vec3d4.lengthSquared() < 1.0E-6) || bl)) {
            this.airMotion = this.airMotion.multiply(d);
        } else if (bl) {
            this.airMotion = new Vec3d(this.airMotion.x, this.airMotion.y * 0.96, this.airMotion.z);
        }
        if (bl5 && d5 <= 0.0015 || this.airMotion.lengthSquared() < 4.0E-5) {
            this.airMotion = Vec3d.ZERO;
        }
        Vec3d vec3d5 = this.airMotion.lengthSquared() > 0.0 ? (this.airMotion.length() > d4 ? vec3d2 : this.airMotion) : Vec3d.ZERO;
        this.move(MovementType.SELF, vec3d5);
        if (this.horizontalCollision && this.desiredMoving) {
            this.move(MovementType.SELF, new Vec3d(0.0, 0.12, 0.0));
        }
        Vec3d vec3d6 = this.getEntityPos();
        double d6 = this.square(vec3d6.x - vec3d.x) + this.square(vec3d6.z - vec3d.z);
        this.moving = bl2 && (d6 > 2.5E-5 || Math.abs(vec3d6.y - vec3d.y) > 0.003);
        this.lastHorizontalDistance = bl2 ? d3 : Double.MAX_VALUE;
        double d7 = this.moving ? Math.max(1.0, this.animationSpeed) : 1.0;
        this.currentAnimationSpeed += (d7 - this.currentAnimationSpeed) * (this.moving ? 0.2 : 0.16);
        this.setVelocity(Vec3d.ZERO);
        this.setYaw(this.smoothedYaw);
        this.setHeadYaw(this.smoothedYaw);
        this.setBodyYaw(this.smoothedYaw);
        this.tickClientAudio(false, 0.0);
    }

    public void overrideLookYaw(float f, float f2) {
        this.lookOverrideYaw = f;
        this.lookOverrideWeight = Math.max(0.0f, Math.min(1.0f, f2));
    }

    private void tickCustomMovement() {
        boolean bl;
        boolean bl2;
        Vec3d vec3d;
        Vec3d vec3d2;
        boolean bl3;
        float f;
        float f2;
        boolean bl4;
        boolean bl5 = this.airborneMode;
        this.airborneMode = this.desiredAirborne;
        this.usingUmbrella = this.desiredUmbrella && !this.airborneMode && !this.variant.isRobot();
        this.setNoGravity(this.airborneMode);
        if (!(this.airborneMode || !bl5 && this.desiredMoving)) {
            this.snapToGroundIfClose();
        }
        if (this.jumpCooldown > 0) {
            --this.jumpCooldown;
        }
        if (this.pauseTicks > 0) {
            --this.pauseTicks;
            this.moving = false;
            this.movingTicks = 0;
            this.currentGroundSpeed = 0.0;
            this.verticalVelocity = 0.0;
            this.airMotion = Vec3d.ZERO;
            this.setVelocity(Vec3d.ZERO);
            this.currentAnimationSpeed += (1.0 - this.currentAnimationSpeed) * 0.28;
            this.lastYaw = this.smoothedYaw;
            this.setYaw(this.smoothedYaw);
            this.setHeadYaw(this.smoothedYaw);
            this.setBodyYaw(this.smoothedYaw);
            this.tickClientAudio(false, 0.0);
            return;
        }
        if (this.airborneMode) {
            this.tickAirMovement();
            return;
        }
        this.airMotion = Vec3d.ZERO;
        Vec3d vec3d3 = this.getEntityPos();
        Vec3d vec3d4 = this.desiredPosition.subtract(vec3d3);
        Vec3d vec3d5 = new Vec3d(vec3d4.x, 0.0, vec3d4.z);
        double d = vec3d5.horizontalLength();
        boolean bl6 = bl4 = this.desiredMoving && d > 0.045;
        if (bl4) {
            f2 = (float)Math.toDegrees(MathHelper.atan2((double)vec3d5.z, (double)vec3d5.x)) - 90.0f;
            f = (float)Math.clamp(9.5 + d * 2.4 + this.desiredSpeed * 68.0, 9.5, 24.0);
            this.targetYaw = MathHelper.stepUnwrappedAngleTowards((float)this.targetYaw, (float)f2, (float)f);
        }
        f2 = bl4 ? (float)Math.clamp(8.5 + d * 1.8 + this.desiredSpeed * 86.0, 9.0, 22.0) : 4.8f;
        this.smoothedYaw = MathHelper.stepUnwrappedAngleTowards((float)this.smoothedYaw, (float)this.targetYaw, (float)f2);
        f = Math.abs(MathHelper.wrapDegrees((float)(this.targetYaw - this.smoothedYaw)));
        double d2 = bl4 ? MathHelper.clamp((double)(1.0 - Math.max(0.0, (double)(f - 6.0f)) / 78.0), (double)0.18, (double)1.0) : 0.0;
        double d3 = bl4 ? MathHelper.clamp((double)((d - 0.04) / 1.6), (double)0.34, (double)1.0) : 0.0;
        double d4 = bl4 ? Math.min(d, this.desiredSpeed * d3 * d2) : 0.0;
        double d5 = bl4 ? Math.clamp(0.18 + this.desiredSpeed * 0.95 + d * 0.06, 0.18, 0.52) : 0.24;
        this.currentGroundSpeed += (d4 - this.currentGroundSpeed) * d5;
        if (Math.abs(this.currentGroundSpeed) < 1.0E-4) {
            this.currentGroundSpeed = 0.0;
        }
        boolean bl7 = bl3 = this.currentGroundSpeed > 0.012;
        this.moving = bl4 ? bl3 || d > 0.18 : bl3;
        Vec3d vec3d6 = vec3d2 = this.currentGroundSpeed > 1.0E-5 && d > 1.0E-5 ? vec3d5.normalize().multiply(Math.min(d, this.currentGroundSpeed)) : Vec3d.ZERO;
        if (this.horizontalCollision && bl4 && this.blockedTicks >= 1 && d > 0.25 && this.currentGroundSpeed > 0.02) {
            if (this.sideStepTicks <= 0) {
                this.sideStepDir = this.random.nextBoolean() ? 1 : -1;
                this.sideStepTicks = 12;
            }
            Vec3d vec3d7 = vec3d5.lengthSquared() > 1.0E-6 ? vec3d5.normalize() : new Vec3d(0.0, 0.0, 1.0);
            Vec3d vec3d8 = new Vec3d(-vec3d7.z * (double)this.sideStepDir, 0.0, vec3d7.x * (double)this.sideStepDir);
            vec3d = vec3d7.multiply(0.45).add(vec3d8.multiply(0.9));
            if (vec3d.lengthSquared() > 1.0E-6) {
                double d6 = Math.max(this.currentGroundSpeed, this.desiredSpeed * 0.7);
                vec3d2 = vec3d.normalize().multiply(Math.min(d, d6));
            }
        }
        if (this.sideStepTicks > 0) {
            --this.sideStepTicks;
        }
        double d7 = this.desiredPosition.y - this.getY();
        if (this.isOnGround()) {
            if (d7 > 1.15 && d <= 1.6 && bl4 && this.currentGroundSpeed > 0.035 && this.jumpCooldown <= 0 && this.blockedTicks < 3) {
                this.verticalVelocity = 0.42;
                this.jumpCooldown = 11;
            } else {
                this.verticalVelocity = d7 < -0.35 ? Math.max(-0.16, d7) : 0.0;
            }
        } else {
            this.verticalVelocity = Math.max(this.verticalVelocity - 0.08, -0.36);
        }
        vec3d = new Vec3d(vec3d2.x, this.verticalVelocity, vec3d2.z);
        this.move(MovementType.SELF, vec3d);
        Vec3d vec3d9 = this.getEntityPos();
        double d8 = this.square(vec3d9.x - vec3d3.x) + this.square(vec3d9.z - vec3d3.z);
        boolean bl8 = bl2 = d8 > 2.5E-5 || Math.abs(vec3d9.y - vec3d3.y) > 0.003;
        if (this.isOnGround() && this.verticalVelocity < 0.0) {
            this.verticalVelocity = 0.0;
        }
        if (this.isOnGround() && this.verticalVelocity <= 0.0 && this.desiredPosition.y <= this.getY() && Math.abs(this.desiredPosition.y - this.getY()) < 0.18) {
            this.setPosition(this.getX(), this.desiredPosition.y, this.getZ());
        }
        this.moving = bl4 && bl2;
        Vec3d vec3d10 = this.desiredPosition.subtract(this.getEntityPos());
        double d9 = Math.hypot(vec3d10.x, vec3d10.z);
        boolean bl9 = bl = d9 + 0.025 < this.lastHorizontalDistance;
        if (this.currentGroundSpeed > 0.02 && this.horizontalCollision && this.isOnGround()) {
            ++this.blockedTicks;
        } else if (!bl4 || bl || d9 < 0.8) {
            this.blockedTicks = 0;
        }
        if (this.blockedTicks >= 7) {
            this.pauseTicks = 8;
            this.blockedTicks = 0;
            this.sideStepTicks = 0;
            this.moving = false;
            this.movingTicks = 0;
            this.currentGroundSpeed = 0.0;
            this.verticalVelocity = 0.0;
        }
        this.lastHorizontalDistance = bl4 ? d9 : Double.MAX_VALUE;
        double d10 = this.moving ? Math.max(1.0, this.animationSpeed) : 1.0;
        this.currentAnimationSpeed += (d10 - this.currentAnimationSpeed) * (this.moving ? 0.22 : 0.18);
        this.setVelocity(Vec3d.ZERO);
        this.setYaw(this.smoothedYaw);
        this.setHeadYaw(this.smoothedYaw);
        this.setBodyYaw(this.smoothedYaw);
        this.tickClientAudio(this.moving, vec3d2.horizontalLengthSquared());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add((AnimationController<?>)new AnimationController("controller", 5, this::predicate));
    }
}

