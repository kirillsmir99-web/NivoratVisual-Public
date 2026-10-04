package rtx.nv.api.modules.impl.Visuals.custompet.control;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import rtx.nv.api.modules.impl.Visuals.custompet.CustomPetVariant;
import rtx.nv.api.modules.impl.Visuals.custompet.entity.CustomPetEntity;

public final class CustomPetFollowerController {
    private static final double MAX_WANDER_DISTANCE = 15.0;
    private static final double SNAP_BACK_DISTANCE = 18.0;
    private static final double FOLLOW_BREAK_DISTANCE = 13.0;
    private static final double FOLLOW_NEAR_DISTANCE = 6.0;
    private static final double TARGET_REACHED_DISTANCE = 0.9;
    private static final double PLAYER_PERSONAL_SPACE = 0.65;
    private static final double OWL_HOVER_RADIUS_MIN = 1.6;
    private static final double OWL_HOVER_RADIUS_MAX = 2.8;
    private static final double OWL_HOVER_HEIGHT_MIN = 1.6;
    private static final double OWL_HOVER_HEIGHT_MAX = 3.2;
    private static final int OWL_FLIGHT_COOLDOWN_MIN = 300;
    private static final int OWL_FLIGHT_COOLDOWN_MAX = 640;
    private static final int OWL_FLIGHT_DURATION_MIN = 90;
    private static final int OWL_FLIGHT_DURATION_MAX = 190;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = Random.create();
    private CustomPetEntity pet;
    private boolean petIsOwl;
    private Vec3d roamTarget;
    private Vec3d airHoverOffset;
    private Vec3d airSmoothedTarget;
    private Vec3d airFlightDirection;
    private boolean airChasing;
    private int idleTicks;
    private int followRefreshTicks;
    private int followSide = 1;
    private int ticks;
    private int flightTicksRemaining;
    private int nextFlightAtTick;
    private int gazeBuildTicks;
    private int gazeLookActiveTicks;
    private int gazeCooldownTicks;

    public void reset() {
        if (this.pet != null && !this.pet.isRemoved()) {
            if (this.mc.world != null) {
                this.mc.world.removeEntity(this.pet.getId(), Entity.RemovalReason.DISCARDED);
            }
            this.pet.discard();
        }
        this.pet = null;
        this.roamTarget = null;
        this.airHoverOffset = null;
        this.airSmoothedTarget = null;
        this.airFlightDirection = null;
        this.airChasing = false;
        this.idleTicks = 0;
        this.followRefreshTicks = 0;
        this.followSide = 1;
        this.flightTicksRemaining = 0;
    }

    private void ensurePet(PlayerEntity playerEntity, CustomPetVariant customPetVariant, boolean bl) {
        if (this.pet != null && this.pet.getEntityWorld() == this.mc.world && !this.pet.isRemoved()) {
            this.pet.setPetVariant(customPetVariant);
            this.pet.setOwl(bl);
            this.petIsOwl = bl;
            return;
        }
        this.reset();
        this.petIsOwl = bl;
        this.pet = new CustomPetEntity((World)(Object)this.mc.world);
        this.pet.setPetVariant(customPetVariant);
        this.pet.setOwl(bl);
        this.nextFlightAtTick = this.ticks + this.randomBetween(300, 640);
        this.roamTarget = customPetVariant.alwaysFlies() ? this.pickRobotRoamTarget(playerEntity) : this.pickRoamTarget(playerEntity, true);
        this.airHoverOffset = null;
        this.airSmoothedTarget = null;
        this.airFlightDirection = null;
        this.airChasing = false;
        this.idleTicks = this.randomBetween(18, 34);
        this.followRefreshTicks = 0;
        this.followSide = this.random.nextBoolean() ? 1 : -1;
        this.pet.snapTo(this.roamTarget, this.random.nextFloat() * 360.0f);
        this.mc.world.addEntity((Entity)(Object)this.pet);
    }

    private void tickPet(PlayerEntity playerEntity, CustomPetVariant customPetVariant) {
        double d;
        boolean bl;
        if (this.pet == null) {
            return;
        }
        this.pet.setPetVariant(customPetVariant);
        Vec3d vec3d = this.pet.getEntityPos();
        Vec3d vec3d3 = playerEntity.getEntityPos();
        double d2 = vec3d.squaredDistanceTo(vec3d3);
        double d3 = Math.sqrt(d2);
        double d4 = playerEntity.getVelocity().horizontalLength();
        boolean bl2 = bl = customPetVariant != null && customPetVariant.alwaysFlies();
        if (bl) {
            this.tickRobotFlight(playerEntity, vec3d, vec3d3, d3, d4);
            return;
        }
        Vec3d vec3d2 = this.snapCurrentToGround(vec3d, playerEntity.getY());
        ++this.ticks;
        boolean bl3 = playerEntity.isGliding() || playerEntity.getAbilities().flying;
        boolean bl4 = false;
        if (this.petIsOwl) {
            if (this.flightTicksRemaining > 0) {
                --this.flightTicksRemaining;
                bl4 = true;
                if (this.flightTicksRemaining == 0) {
                    this.nextFlightAtTick = this.ticks + this.randomBetween(300, 640);
                }
            } else if (!bl3 && this.ticks >= this.nextFlightAtTick && d4 < 0.12 && d3 < 8.0) {
                this.flightTicksRemaining = this.randomBetween(90, 190);
                this.nextFlightAtTick = this.ticks + this.flightTicksRemaining + this.randomBetween(300, 640);
                bl4 = true;
            }
        }
        boolean bl5 = bl3 || bl4;
        boolean bl6 = d4 > 0.025 || Math.abs(playerEntity.getVelocity().y) > 0.05;
        boolean bl7 = d4 > 0.17 || Math.abs(playerEntity.getVelocity().y) > 0.08;
        boolean bl8 = d2 > 169.0 || bl6 && d2 > 36.0;
        boolean bl9 = this.isRainingAbove(vec3d);
        if (d2 > 324.0) {
            this.roamTarget = customPetVariant.alwaysFlies() ? this.pickRobotRoamTarget(playerEntity) : this.pickRoamTarget(playerEntity, true);
            this.airFlightDirection = null;
            this.airChasing = false;
            this.flightTicksRemaining = 0;
            this.idleTicks = this.randomBetween(12, 22);
            this.followRefreshTicks = 0;
            this.pet.snapTo(this.roamTarget, this.pet.getYaw());
            this.pet.setBehavior(this.roamTarget, 0.0, false, bl9, false, 1.0);
            return;
        }
        if (this.pet.isMovementBlocked() && d2 <= 169.0) {
            this.roamTarget = null;
            this.airFlightDirection = null;
            this.airChasing = false;
            this.followRefreshTicks = 0;
            if (this.idleTicks <= 0) {
                this.idleTicks = this.randomBetween(18, 38);
            }
            --this.idleTicks;
            this.pet.setBehavior(vec3d2, 0.0, false, bl9, false, 1.0);
            return;
        }
        if (!bl5 && !bl8 && d4 < 0.02 && d2 < 7.290000000000001) {
            this.followRefreshTicks = 0;
            this.roamTarget = null;
            this.airFlightDirection = null;
            this.airChasing = false;
            if (this.idleTicks <= 0) {
                this.idleTicks = this.randomBetween(24, 52);
            }
            --this.idleTicks;
            this.pet.setBehavior(vec3d2, 0.0, false, bl9, false, 1.0);
            return;
        }
        if (bl5) {
            Vec3d vec3d4;
            this.idleTicks = 0;
            boolean isMoving = d4 > 0.08 || Math.abs(playerEntity.getVelocity().y) > 0.08;
            if (isMoving) {
                this.followRefreshTicks = 0;
                this.airHoverOffset = null;
                vec3d4 = this.computeAirFollowTarget(playerEntity, d3, d4);
            } else {
                if (this.airHoverOffset == null || this.followRefreshTicks <= 0) {
                    this.airHoverOffset = this.pickAirHoverOffset(playerEntity, vec3d);
                    this.followRefreshTicks = this.randomBetween(20, 42);
                } else {
                    --this.followRefreshTicks;
                }
                vec3d4 = this.computeAirIdleTarget(playerEntity);
            }
            if (this.airSmoothedTarget == null) {
                this.airSmoothedTarget = vec3d;
            }
            d = Math.clamp(0.07 + playerEntity.getVelocity().length() * (playerEntity.isGliding() ? 0.025 : 0.05) + Math.max(0.0, d3 - 1.5) * 0.012, 0.07, playerEntity.isGliding() ? 0.2 : 0.28);
            this.roamTarget = this.airSmoothedTarget = this.airSmoothedTarget.lerp(vec3d4, d);
        } else if (bl8) {
            this.airHoverOffset = null;
            this.airSmoothedTarget = null;
            this.airFlightDirection = null;
            this.airChasing = false;
            this.idleTicks = 0;
            if (this.followRefreshTicks <= 0 || this.roamTarget == null) {
                if (d3 < 4.5 && this.random.nextFloat() < 0.08f) {
                    this.followSide *= -1;
                }
                Vec3d vec3d5 = this.computeFollowTarget(playerEntity, d3, d4);
                float f = (float)Math.clamp(0.22 + Math.max(0.0, d3 - 2.0) * 0.05 + d4 * 0.35, 0.22, 0.62);
                this.roamTarget = this.roamTarget == null ? vec3d5 : this.roamTarget.lerp(vec3d5, (double)f);
                this.followRefreshTicks = d3 > 7.0 ? 1 : 2;
            } else {
                --this.followRefreshTicks;
            }
        } else {
            this.airHoverOffset = null;
            this.airSmoothedTarget = null;
            this.airFlightDirection = null;
            this.airChasing = false;
            this.followRefreshTicks = 0;
            if (this.idleTicks > 0) {
                --this.idleTicks;
                this.pet.setBehavior(vec3d2, 0.0, false, bl9, false, 1.0);
                return;
            }
            if (this.roamTarget == null || this.shouldPickNewTarget(playerEntity, vec3d)) {
                if (this.random.nextFloat() < 0.24f) {
                    this.roamTarget = null;
                    this.idleTicks = this.randomBetween(18, 42);
                    this.pet.setBehavior(vec3d2, 0.0, false, bl9, false, 1.0);
                    return;
                }
                this.followSide = this.random.nextBoolean() ? 1 : -1;
                boolean fast = vec3d.squaredDistanceTo(vec3d3) > 16.0 || playerEntity.getVelocity().horizontalLengthSquared() > 0.04;
                this.roamTarget = this.pickRoamTarget(playerEntity, fast);
            }
        }
        if (this.roamTarget == null) {
            this.idleTicks = this.randomBetween(18, 36);
            this.pet.setBehavior(vec3d2, 0.0, false, bl9, false, 1.0);
            return;
        }
        Vec3d vec3d6 = this.roamTarget;
        Vec3d vec3d7 = vec3d6.subtract(vec3d);
        d = Math.hypot(vec3d7.x, vec3d7.z);
        double d5 = vec3d7.length();
        if (bl5) {
            boolean bl11;
            StatusEffectInstance statusEffectInstance = playerEntity.getStatusEffect(StatusEffects.SPEED);
            int n = statusEffectInstance != null ? statusEffectInstance.getAmplifier() + 1 : 0;
            double d6 = playerEntity.getVelocity().length();
            double d7 = playerEntity.isGliding() ? 0.82 : 0.48;
            double d8 = playerEntity.isGliding() ? 1.28 : 0.78;
            boolean bl12 = bl11 = d3 > 4.0 || d6 > 1.2 || d4 > 0.55;
            if (bl11 || d5 >= d8) {
                this.airChasing = true;
            } else if (d5 <= d7) {
                this.airChasing = false;
            }
            double d9 = Math.clamp(0.22 + d6 * (playerEntity.isGliding() ? 2.45 : 1.65) + Math.max(0.0, d3 - 1.0) * (playerEntity.isGliding() ? 0.24 : 0.18) + (double)n * 0.07 + (playerEntity.isSprinting() ? 0.08 : 0.0), 0.16, playerEntity.isGliding() ? (d3 > 18.0 ? 6.8 : (d3 > 12.0 ? 5.0 : (d3 > 7.0 ? 3.6 : 1.9))) : (d3 > 16.0 ? 3.2 : (d3 > 10.0 ? 2.4 : (d3 > 6.0 ? 1.55 : 0.95))));
            double d10 = Math.clamp(1.15 + Math.max(0.0, d3 - 1.2) * 0.22 + d6 * (playerEntity.isGliding() ? 1.8 : 1.35) + (double)n * 0.16, 1.0, playerEntity.isGliding() ? 3.1 : 2.4);
            this.pet.setBehavior(vec3d6, d9, this.airChasing, false, true, d10);
            return;
        }
        if (d <= 0.9 && Math.abs(vec3d7.y) <= 0.45 && !bl8) {
            this.roamTarget = null;
            this.idleTicks = this.randomBetween(18, 42);
            this.pet.setBehavior(vec3d2, 0.0, false, bl9, false, 1.0);
            return;
        }
        StatusEffectInstance statusEffectInstance = playerEntity.getStatusEffect(StatusEffects.SPEED);
        int n = statusEffectInstance != null ? statusEffectInstance.getAmplifier() + 1 : 0;
        double d11 = bl8 ? Math.clamp((d - 0.4) / 1.8, 0.12, 1.0) : 1.0;
        double d12 = Math.clamp(0.055 + d4 * 0.31 + Math.max(0.0, d3 - 1.8) * 0.072 + (double)n * 0.028 + (playerEntity.isSprinting() ? 0.028 : 0.0), 0.045, d3 > 8.5 ? 0.52 : (d3 > 6.0 ? 0.42 : (d3 > 4.0 ? 0.31 : 0.22))) * d11;
        double d13 = Math.clamp(1.0 + Math.max(0.0, d3 - 1.4) * 0.14 + d4 * 0.88 + (double)n * 0.12 + (playerEntity.isSprinting() ? 0.06 : 0.0), 0.95, 1.85) * (bl8 ? Math.clamp(d11 + 0.3, 0.4, 1.0) : 1.0);
        this.pet.setBehavior(vec3d6, d12, d12 > 0.02, bl9, false, d13);
        this.pushOutOfPlayer(playerEntity);
    }

    public CustomPetEntity getPet() {
        return this.pet;
    }

    private Vec3d computeFollowTarget(PlayerEntity playerEntity, double d, double d2) {
        Vec3d vec3d = playerEntity.getVelocity();
        Vec3d vec3d2 = new Vec3d(vec3d.x, 0.0, vec3d.z);
        if (vec3d2.lengthSquared() < 1.0E-4) {
            float f = playerEntity.getYaw() * ((float)Math.PI / 180);
            vec3d2 = new Vec3d((double)(-MathHelper.sin((double)f)), 0.0, (double)MathHelper.cos((double)f));
        }
        vec3d2 = vec3d2.normalize();
        double d3 = Math.clamp(0.6 + d2 * 7.0 + Math.max(0.0, d - 3.0) * 0.35, 0.6, 3.0);
        Vec3d vec3d3 = playerEntity.getEntityPos().add(playerEntity.getVelocity().multiply(d3));
        double d4 = d > 5.5 ? 0.65 : 1.45;
        double d5 = d > 5.5 ? 0.0 : 0.55 * (double)this.followSide;
        Vec3d vec3d4 = new Vec3d(-vec3d2.z, 0.0, vec3d2.x);
        Vec3d vec3d5 = vec3d3.subtract(vec3d2.multiply(d4)).add(vec3d4.multiply(d5));
        double d6 = this.findGroundY(vec3d5.x, vec3d5.z, playerEntity.getY());
        return CustomPetFollowerController.enforcePersonalSpace(new Vec3d(vec3d5.x, d6, vec3d5.z), playerEntity);
    }

    private boolean isRainingAbove(Vec3d vec3d) {
        if (this.mc.world == null) {
            return false;
        }
        return this.mc.world.hasRain(BlockPos.ofFloored((double)vec3d.x, (double)(vec3d.y + 1.1), (double)vec3d.z));
    }

    private Vec3d pickRobotRoamTarget(PlayerEntity playerEntity) {
        double d = this.random.nextDouble() * 6.2831854820251465;
        double d2 = 1.7 + this.random.nextDouble() * 1.3;
        double d3 = playerEntity.getX() + Math.cos(d) * d2;
        double d4 = playerEntity.getZ() + Math.sin(d) * d2;
        double d5 = playerEntity.getY() + 1.0 + this.random.nextDouble() * 0.6;
        double d6 = this.ceilingClearance(d3, d4, d5, playerEntity.getY());
        return CustomPetFollowerController.enforcePersonalSpace(new Vec3d(d3, Math.max(d5, d6), d4), playerEntity);
    }

    private double ceilingClearance(double d, double d2, double d3, double d4) {
        if (this.mc.world == null) {
            return d3;
        }
        int n = MathHelper.floor((double)d);
        int n2 = MathHelper.floor((double)d2);
        int n3 = MathHelper.floor((double)d3);
        BlockPos.Mutable mutable = new BlockPos.Mutable(n, n3, n2);
        for (int i = n3; i <= n3 + 3; ++i) {
            mutable.setY(i);
            BlockState blockState = this.mc.world.getBlockState((BlockPos)mutable);
            VoxelShape voxelShape = blockState.getCollisionShape((BlockView)(Object)this.mc.world, (BlockPos)mutable);
            if (voxelShape.isEmpty()) continue;
            return (double)i + voxelShape.getMax(Direction.Axis.Y) + 0.6;
        }
        return d3;
    }

    private static Vec3d enforcePersonalSpace(Vec3d vec3d, PlayerEntity playerEntity) {
        double d;
        double d2 = vec3d.x - playerEntity.getX();
        double d3 = d2 * d2 + (d = vec3d.z - playerEntity.getZ()) * d;
        if (d3 >= 1.5625) {
            return vec3d;
        }
        double d4 = Math.sqrt(d3);
        if (d4 < 1.0E-4) {
            float f = playerEntity.getYaw() * ((float)Math.PI / 180);
            d2 = -MathHelper.sin((double)f);
            d = MathHelper.cos((double)f);
            d4 = 1.0;
        }
        double d5 = 1.25 / d4;
        return new Vec3d(playerEntity.getX() + d2 * d5, vec3d.y, playerEntity.getZ() + d * d5);
    }

    private void tickRobotFlight(PlayerEntity playerEntity, Vec3d vec3d, Vec3d vec3d2, double d, double d2) {
        boolean bl;
        Vec3d vec3d3;
        Vec3d vec3d4;
        double d3;
        double d4;
        double d5;
        boolean bl2;
        if (d > 18.0) {
            Vec3d vec3d5;
            this.airSmoothedTarget = vec3d5 = this.pickRobotRoamTarget(playerEntity);
            this.roamTarget = vec3d5;
            this.airFlightDirection = null;
            this.followRefreshTicks = 0;
            this.idleTicks = this.randomBetween(30, 60);
            this.pet.snapTo(vec3d5, this.pet.getYaw());
            this.pet.setBehavior(vec3d5, 0.0, false, false, true, 1.0);
            return;
        }
        Vec3d vec3d6 = playerEntity.getVelocity();
        Vec3d vec3d7 = new Vec3d(vec3d6.x, 0.0, vec3d6.z);
        if (vec3d7.lengthSquared() < 1.0E-4) {
            float f = playerEntity.getYaw() * ((float)Math.PI / 180);
            vec3d7 = new Vec3d((double)(-MathHelper.sin((double)f)), 0.0, (double)MathHelper.cos((double)f));
        }
        vec3d7 = vec3d7.normalize();
        this.airFlightDirection = this.airFlightDirection == null ? vec3d7 : this.airFlightDirection.lerp(vec3d7, 0.18).normalize();
        boolean bl3 = d2 > 0.08;
        this.airChasing = d > 15.0 || (bl3 && d > (this.airChasing ? 2.6 : 4.2));
        boolean bl4 = bl2 = this.airChasing;
        if (bl2) {
            this.roamTarget = null;
            this.idleTicks = 0;
        }
        if (bl2) {
            Vec3d vec3d8 = this.airFlightDirection;
            Vec3d vec3d9 = new Vec3d(-vec3d8.z, 0.0, vec3d8.x);
            d5 = d > 5.0 ? 0.5 : 1.2;
            d4 = 1.3 * (double)this.followSide;
            d3 = Math.clamp(d2 * 4.5, 0.0, 2.2);
            vec3d4 = vec3d2.add(vec3d8.multiply(d3 - d5)).add(vec3d9.multiply(d4));
            double d6 = playerEntity.getY() + 1.5;
            double d7 = this.ceilingClearance(vec3d4.x, vec3d4.z, d6, playerEntity.getY());
            vec3d3 = CustomPetFollowerController.enforcePersonalSpace(new Vec3d(vec3d4.x, Math.max(d6, d7), vec3d4.z), playerEntity);
            bl = true;
        } else if (this.idleTicks > 0) {
            --this.idleTicks;
            if (this.idleTicks <= 0) {
                this.roamTarget = null;
            }
            vec3d3 = this.roamTarget != null ? this.roamTarget : vec3d;
            bl = false;
        } else if (this.roamTarget != null) {
            double d8 = vec3d.distanceTo(this.roamTarget);
            if (d8 < 1.5) {
                this.roamTarget = vec3d;
                this.idleTicks = this.randomBetween(60, 140);
                vec3d3 = vec3d;
                bl = false;
            } else {
                vec3d3 = this.roamTarget;
                bl = true;
            }
        } else {
            vec3d3 = this.roamTarget = this.pickRobotRoamTarget(playerEntity);
            bl = true;
        }
        double d9 = Math.sin((double)playerEntity.age * 0.075 + (double)this.followSide * 1.7) * 0.12;
        d5 = Math.sin((double)playerEntity.age * 0.125 + (double)this.followSide * 0.4) * 0.04;
        d4 = d9 + d5;
        d3 = bl ? 0.0 : Math.sin((double)playerEntity.age * 0.018 + (double)this.followSide * 0.9) * 0.1;
        vec3d4 = new Vec3d(-this.airFlightDirection.z, 0.0, this.airFlightDirection.x);
        Vec3d vec3d10 = new Vec3d(vec3d3.x + vec3d4.x * d3, vec3d3.y + d4, vec3d3.z + vec3d4.z * d3);
        if (this.airSmoothedTarget == null) {
            this.airSmoothedTarget = vec3d;
        }
        double d10 = bl ? Math.clamp(0.1 + d2 * 0.08 + Math.max(0.0, d - 1.5) * 0.018, 0.1, 0.34) : 0.07;
        this.airSmoothedTarget = this.airSmoothedTarget.lerp(vec3d10, d10);
        Vec3d vec3d11 = this.airSmoothedTarget.subtract(vec3d2);
        if (vec3d11.length() > 17.5) {
            this.airSmoothedTarget = vec3d2.add(vec3d11.normalize().multiply(17.5));
        }
        double d11 = vec3d.distanceTo(this.airSmoothedTarget);
        double d12 = bl ? Math.clamp(0.08 + d11 * 0.06 + d2 * 0.8 + (playerEntity.isSprinting() ? 0.06 : 0.0), 0.06, bl2 ? 1.2 : 0.25) : 0.04;
        double d13 = Math.clamp(1.0 + d11 * 0.08 + d2 * 0.6, 1.0, 1.8);
        this.tickGazeReaction(playerEntity, vec3d);
        this.pet.setBehavior(this.airSmoothedTarget, d12, bl || d11 > 0.3, false, true, d13);
        this.pushOutOfPlayer(playerEntity);
    }

    private Vec3d pickAirHoverOffset(PlayerEntity playerEntity, Vec3d vec3d) {
        if (this.petIsOwl) {
            double d = this.random.nextDouble() * 6.2831854820251465;
            double d2 = MathHelper.lerp((double)this.random.nextDouble(), (double)1.6, (double)2.8);
            double d3 = MathHelper.lerp((double)this.random.nextDouble(), (double)1.6, (double)3.2);
            return new Vec3d(Math.cos(d) * d2, d3, Math.sin(d) * d2);
        }
        Vec3d vec3d2 = vec3d.subtract(playerEntity.getEntityPos());
        Vec3d vec3d3 = new Vec3d(vec3d2.x, 0.0, vec3d2.z);
        double d = vec3d3.length();
        if (d < 0.9 || d > 2.5) {
            double d4 = this.random.nextDouble() * 6.2831854820251465;
            double d5 = MathHelper.lerp((double)this.random.nextDouble(), (double)1.35, (double)1.95);
            return new Vec3d(Math.cos(d4) * d5, MathHelper.lerp((double)this.random.nextDouble(), (double)0.28, (double)0.44), Math.sin(d4) * d5);
        }
        Vec3d vec3d4 = vec3d3.normalize().multiply(MathHelper.clamp((double)d, (double)1.25, (double)1.95));
        double d6 = MathHelper.clamp((double)vec3d2.y, (double)0.24, (double)0.46);
        return new Vec3d(vec3d4.x, d6, vec3d4.z);
    }

    private int randomBetween(int n, int n2) {
        return n + this.random.nextInt(n2 - n + 1);
    }

    private Vec3d pickRoamTarget(PlayerEntity playerEntity, boolean bl) {
        Vec3d vec3d = playerEntity.getVelocity();
        double d = vec3d.horizontalLengthSquared() > 0.0025 ? Math.atan2(vec3d.z, vec3d.x) : this.random.nextDouble() * 6.2831854820251465;
        double d2 = d + MathHelper.lerp((double)this.random.nextDouble(), (double)-1.65, (double)1.65);
        double d3 = bl ? MathHelper.lerp((double)this.random.nextDouble(), (double)1.6, (double)3.0) : MathHelper.lerp((double)this.random.nextDouble(), (double)2.5, (double)14.75);
        double d4 = playerEntity.getX() + Math.cos(d2) * d3;
        double d5 = playerEntity.getZ() + Math.sin(d2) * d3;
        double d6 = this.findGroundY(d4, d5, playerEntity.getY());
        return CustomPetFollowerController.enforcePersonalSpace(new Vec3d(d4, d6, d5), playerEntity);
    }

    private Vec3d snapCurrentToGround(Vec3d vec3d, double d) {
        double d2 = this.findGroundY(vec3d.x, vec3d.z, Math.max(vec3d.y, d));
        return new Vec3d(vec3d.x, d2, vec3d.z);
    }

    private boolean shouldPickNewTarget(PlayerEntity playerEntity, Vec3d vec3d) {
        if (this.roamTarget == null) {
            return true;
        }
        if (this.roamTarget.squaredDistanceTo(playerEntity.getEntityPos()) > 225.0) {
            return true;
        }
        return vec3d.squaredDistanceTo(playerEntity.getEntityPos()) > 169.0 && vec3d.squaredDistanceTo(this.roamTarget) > 25.0;
    }

    private Vec3d computeAirFollowTarget(PlayerEntity playerEntity, double d, double d2) {
        Vec3d vec3d = playerEntity.getVelocity();
        double d3 = vec3d.length();
        Vec3d vec3d2 = new Vec3d(vec3d.x, 0.0, vec3d.z);
        if (vec3d2.lengthSquared() < 1.0E-4) {
            float f = playerEntity.getYaw() * ((float)Math.PI / 180);
            vec3d2 = new Vec3d((double)(-MathHelper.sin((double)f)), 0.0, (double)MathHelper.cos((double)f));
        }
        vec3d2 = vec3d2.normalize();
        if (this.airFlightDirection == null || this.airFlightDirection.lengthSquared() < 1.0E-4) {
            this.airFlightDirection = vec3d2;
        } else {
            double d4 = playerEntity.isGliding() ? 0.14 : 0.26;
            this.airFlightDirection = this.airFlightDirection.lerp(vec3d2, d4);
            if (this.airFlightDirection.lengthSquared() < 1.0E-4) {
                this.airFlightDirection = vec3d2;
            }
        }
        Vec3d vec3d3 = this.airFlightDirection.normalize();
        Vec3d vec3d4 = new Vec3d(-vec3d3.z, 0.0, vec3d3.x);
        boolean bl = this.mc.options.getPerspective().isFirstPerson();
        double d5 = (playerEntity.isGliding() ? 0.82 : 0.65) * (double)this.followSide;
        double d6 = (playerEntity.isGliding() ? 1.45 : 1.05) + Math.min(d * 0.08, playerEntity.isGliding() ? 0.55 : 0.35);
        if (playerEntity.isGliding() && bl) {
            d5 *= 1.18;
            d6 += 0.42;
        }
        double d7 = playerEntity.isGliding() ? 0.2 : 0.45;
        double d8 = Math.clamp(0.45 + d2 * (playerEntity.isGliding() ? 5.8 : 4.8) + Math.max(0.0, d3 - 0.8) * (playerEntity.isGliding() ? 0.45 : 0.35), 0.45, playerEntity.isGliding() ? 2.8 : 1.6);
        Vec3d vec3d5 = playerEntity.getEntityPos().add(vec3d3.multiply(d8));
        double d9 = Math.sin((double)(playerEntity.age + this.followSide * 7) * 0.1) * 0.028;
        return vec3d5.subtract(vec3d3.multiply(d6)).add(vec3d4.multiply(d5)).add(0.0, playerEntity.getY() - vec3d5.y + d7 + d9, 0.0);
    }

    private void pushOutOfPlayer(PlayerEntity player) {
        if (this.pet == null) return;
        Vec3d current = this.pet.getEntityPos();
        Vec3d delta = current.subtract(player.getEntityPos());
        double horizontal = Math.hypot(delta.x, delta.z);
        if (horizontal >= 1.7 || Math.abs(delta.y) > 2.5) return;
        Vec3d away;
        if (horizontal > 0.08) {
            away = new Vec3d(delta.x / horizontal, 0, delta.z / horizontal);
        } else {
            double yaw = Math.toRadians(player.getYaw());
            away = new Vec3d(Math.cos(yaw) * this.followSide, 0, Math.sin(yaw) * this.followSide);
        }
        Vec3d safe = player.getEntityPos().add(away.multiply(2.0)).add(0, delta.y, 0);
        this.airSmoothedTarget = this.airSmoothedTarget == null ? current : this.airSmoothedTarget;
        this.airSmoothedTarget = this.airSmoothedTarget.lerp(safe, 0.16);
        this.roamTarget = this.airSmoothedTarget;
        this.idleTicks = 0;
        this.pet.setBehavior(this.roamTarget, 0.16 + Math.min(0.18, player.getVelocity().horizontalLength() * 0.5), true, false, this.pet.getPetVariant().alwaysFlies(), 1.15);
    }

    private Vec3d computeAirIdleTarget(PlayerEntity playerEntity) {
        Vec3d vec3d = this.petIsOwl ? new Vec3d(1.9 * (double)this.followSide, 1.6, 0.0) : new Vec3d(1.6 * (double)this.followSide, 0.35, 0.0);
        Vec3d vec3d2 = this.airHoverOffset != null ? this.airHoverOffset : vec3d;
        double d = this.petIsOwl ? 0.25 : 0.09;
        double d2 = Math.sin((double)(playerEntity.age + this.followSide * 7) * 0.085) * d;
        return playerEntity.getEntityPos().add(vec3d2.x, vec3d2.y + d2, vec3d2.z);
    }

    private void tickGazeReaction(PlayerEntity playerEntity, Vec3d vec3d) {
        if (this.pet == null) {
            return;
        }
        Vec3d vec3d2 = playerEntity.getRotationVector();
        Vec3d vec3d3 = vec3d.subtract(playerEntity.getEntityPos());
        double d = Math.hypot(vec3d3.x, vec3d3.z);
        boolean bl = false;
        if (d > 0.05 && d < 3.5) {
            Vec3d vec3d4 = new Vec3d(vec3d2.x, 0.0, vec3d2.z);
            Vec3d vec3d5 = new Vec3d(vec3d3.x, 0.0, vec3d3.z);
            if (vec3d4.lengthSquared() > 1.0E-4 && vec3d5.lengthSquared() > 1.0E-4) {
                vec3d4 = vec3d4.normalize();
                vec3d5 = vec3d5.normalize();
                double d2 = vec3d4.x * vec3d5.x + vec3d4.z * vec3d5.z;
                boolean bl2 = bl = d2 > 0.88;
            }
        }
        if (this.gazeCooldownTicks > 0) {
            --this.gazeCooldownTicks;
        }
        if (this.gazeLookActiveTicks > 0) {
            --this.gazeLookActiveTicks;
            float f = (float)Math.toDegrees(MathHelper.atan2((double)(playerEntity.getZ() - vec3d.z), (double)(playerEntity.getX() - vec3d.x))) - 90.0f;
            this.pet.overrideLookYaw(f, 1.0f);
            if (this.gazeLookActiveTicks == 0) {
                this.gazeCooldownTicks = this.randomBetween(80, 160);
            }
            return;
        }
        this.pet.overrideLookYaw(0.0f, 0.0f);
        if (!bl || this.gazeCooldownTicks > 0) {
            if (!bl) {
                this.gazeBuildTicks = 0;
            }
            return;
        }
        ++this.gazeBuildTicks;
        if (this.gazeBuildTicks >= 25) {
            this.gazeBuildTicks = 0;
            if (this.random.nextFloat() < 0.65f) {
                this.gazeLookActiveTicks = this.randomBetween(40, 70);
            } else {
                this.gazeCooldownTicks = this.randomBetween(40, 90);
            }
        }
    }

    private double findGroundY(double d, double d2, double d3) {
        if (this.mc.world == null) {
            return d3;
        }
        int n = MathHelper.floor((double)d);
        int n2 = MathHelper.floor((double)d2);
        int n3 = MathHelper.floor((double)d3);
        BlockPos.Mutable mutable = new BlockPos.Mutable(n, n3 + 2, n2);
        for (int i = n3 + 2; i >= n3 - 6; --i) {
            mutable.setY(i);
            BlockState blockState = this.mc.world.getBlockState((BlockPos)mutable);
            VoxelShape voxelShape = blockState.getCollisionShape((BlockView)(Object)this.mc.world, (BlockPos)mutable);
            if (voxelShape.isEmpty()) continue;
            return (double)i + voxelShape.getMax(Direction.Axis.Y);
        }
        return d3;
    }

    public void tick(PlayerEntity playerEntity, CustomPetVariant customPetVariant, boolean bl) {
        if (playerEntity == null || playerEntity.getEntityWorld() == null || this.mc.world == null) {
            this.reset();
            return;
        }
        this.ensurePet(playerEntity, customPetVariant, bl);
        this.tickPet(playerEntity, customPetVariant);
    }
}

