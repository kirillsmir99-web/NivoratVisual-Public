package rtx.nv.api.modules.impl.Visuals;
import rtx.nv.api.events.EventHandler;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.player.AttackEntityEvent;
import rtx.nv.api.events.impl.player.TotemPopEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.BooleanSetting;

public class FakePlayer
extends Module {
    private static final String FAKE_NAME = "BRAT12344321";
    private static final float WALK_RADIUS = 2.0f;
    private static final float WALK_SPEED = 0.1f;
    private static final int ROTATION_LERP_TICKS = 3;
    private final BooleanSetting walk = this.register(new BooleanSetting("\u0425\u043e\u0434\u044c\u0431\u0430", "\u0424\u0435\u0439\u043a-\u0438\u0433\u0440\u043e\u043a \u0445\u043e\u0434\u0438\u0442 \u0438\u0437 \u0441\u0442\u043e\u0440\u043e\u043d\u044b \u0432 \u0441\u0442\u043e\u0440\u043e\u043d\u0443.", true));
    private final BooleanSetting rotate = this.register(new BooleanSetting("\u041f\u043e\u0432\u043e\u0440\u043e\u0442 \u043a \u0438\u0433\u0440\u043e\u043a\u0443", "\u0424\u0435\u0439\u043a-\u0438\u0433\u0440\u043e\u043a \u0441\u043c\u043e\u0442\u0440\u0438\u0442 \u043d\u0430 \u0442\u0435\u0431\u044f.", true));
    private final BooleanSetting totem = this.register(new BooleanSetting("\u0422\u043e\u0442\u0435\u043c", "\u0424\u0435\u0439\u043a-\u0438\u0433\u0440\u043e\u043a \u0434\u0435\u0440\u0436\u0438\u0442 \u0442\u043e\u0442\u0435\u043c \u0438 \u043b\u043e\u043c\u0430\u0435\u0442 \u0435\u0433\u043e \u043a\u0430\u0436\u0434\u044b\u0435 3 \u0443\u0434\u0430\u0440\u0430.", false));
    private OtherClientPlayerEntity fakePlayer;
    private Vec3d spawnPosition = Vec3d.ZERO;
    private Vec3d walkAxis = Vec3d.ZERO;
    private float spawnYaw;
    private float walkPhase;
    private int hitsSinceTotem;

    public FakePlayer() {
        super("FakePlayer", "\u0421\u043f\u0430\u0432\u043d\u0438\u0442 \u043a\u043b\u0438\u0435\u043d\u0442\u0441\u043a\u043e\u0433\u043e \u0444\u0435\u0439\u043a-\u0438\u0433\u0440\u043e\u043a\u0430 \u0434\u043b\u044f \u0431\u0438\u0442\u044c\u044f/\u0442\u0435\u0441\u0442\u0430.", Category.VISUALS);
    }

    private void move() {
        double d;
        Vec3d vec3d = this.spawnPosition;
        float f = this.spawnYaw;
        float f2 = 0.0f;
        if (this.walk.getValue()) {
            this.walkPhase += 0.1f;
            vec3d = this.spawnPosition.add(this.walkAxis.multiply((double)(MathHelper.sin((double)this.walkPhase) * 2.0f)));
            d = MathHelper.cos((double)this.walkPhase) >= 0.0f ? 1.0 : -1.0;
            Vec3d vec3d2 = this.walkAxis.multiply(d);
            f = (float)Math.toDegrees(MathHelper.atan2((double)(-vec3d2.x), (double)vec3d2.z));
        }
        if (this.rotate.getValue()) {
            d = this.mc.player.getX() - this.fakePlayer.getX();
            double d2 = this.mc.player.getEyeY() - this.fakePlayer.getEyeY();
            double d3 = this.mc.player.getZ() - this.fakePlayer.getZ();
            f = (float)Math.toDegrees(MathHelper.atan2((double)d3, (double)d)) - 90.0f;
            f2 = (float)(-Math.toDegrees(MathHelper.atan2((double)d2, (double)Math.hypot(d, d3))));
        }
        this.fakePlayer.getInterpolator().refreshPositionAndAngles(vec3d, f, f2);
        this.fakePlayer.updateTrackedHeadRotation(f, 3);
    }

    @Override
    protected void onDisable() {
        this.despawn();
    }

    @EventHandler
    private void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPre()) {
            return;
        }
        if (this.mc.player == null || this.mc.world == null) {
            this.fakePlayer = null;
            return;
        }
        if (this.fakePlayer == null || this.fakePlayer.isRemoved() || this.fakePlayer.getEntityWorld() != this.mc.world) {
            this.spawn();
        }
        this.syncTotem();
        this.move();
    }

    private void spawn() {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        FakePlayer.FakeRemotePlayer fakeRemotePlayer = new FakePlayer.FakeRemotePlayer(this.mc.world, new GameProfile(UUID.randomUUID(), FAKE_NAME));
        fakeRemotePlayer.setId(-threadLocalRandom.nextInt(1000000, 2000000));
        this.spawnPosition = this.mc.player.getEntityPos();
        this.spawnYaw = this.mc.player.getYaw();
        float f = this.spawnYaw * ((float)Math.PI / 180);
        this.walkAxis = new Vec3d((double)(-MathHelper.cos((double)f)), 0.0, (double)(-MathHelper.sin((double)f)));
        this.walkPhase = 0.0f;
        fakeRemotePlayer.refreshPositionAndAngles(this.spawnPosition.x, this.spawnPosition.y, this.spawnPosition.z, this.spawnYaw, 0.0f);
        fakeRemotePlayer.setHeadYaw(this.spawnYaw);
        fakeRemotePlayer.setBodyYaw(this.spawnYaw);
        this.mc.world.addEntity((Entity)fakeRemotePlayer);
        this.fakePlayer = fakeRemotePlayer;
        this.hitsSinceTotem = 0;
        this.syncTotem();
    }

    private void popTotem() {
        ItemStack itemStack = new ItemStack((ItemConvertible)Items.TOTEM_OF_UNDYING);
        this.fakePlayer.equipStack(EquipmentSlot.OFFHAND, itemStack);
        this.mc.particleManager.addEmitter((Entity)(Object)this.fakePlayer, (ParticleEffect)ParticleTypes.TOTEM_OF_UNDYING, 30);
        this.mc.world.playSoundClient(this.fakePlayer.getX(), this.fakePlayer.getY(), this.fakePlayer.getZ(), SoundEvents.ITEM_TOTEM_USE, this.fakePlayer.getSoundCategory(), 1.0f, 1.0f, false);
        EventBus.get().post(new TotemPopEvent((LivingEntity)(Object)this.fakePlayer, false));
    }

    private void syncTotem() {
        if (this.fakePlayer == null) {
            return;
        }
        this.fakePlayer.equipStack(EquipmentSlot.OFFHAND, this.totem.getValue() ? new ItemStack((ItemConvertible)Items.TOTEM_OF_UNDYING) : ItemStack.EMPTY);
        if (!this.totem.getValue()) {
            this.hitsSinceTotem = 0;
            this.fakePlayer.setHealth(this.fakePlayer.getMaxHealth());
        }
    }

    private void despawn() {
        if (this.fakePlayer == null) {
            return;
        }
        World world = this.fakePlayer.getEntityWorld();
        if (world instanceof ClientWorld) {
            ClientWorld clientWorld = (ClientWorld)world;
            clientWorld.removeEntity(this.fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
        }
        this.fakePlayer = null;
    }

    public static boolean isFakePlayer(Entity entity) {
        return entity instanceof FakePlayer.FakeRemotePlayer;
    }

    private void triggerTotemPop() {
        this.hitsSinceTotem = 0;
        this.fakePlayer.setHealth(1.0f);
        this.popTotem();
        this.fakePlayer.setHealth(this.fakePlayer.getMaxHealth());
    }

    @EventHandler
    private void onAttackEntity(AttackEntityEvent attackEntityEvent) {
        if (this.fakePlayer == null || attackEntityEvent.getTarget() != this.fakePlayer) {
            return;
        }
        attackEntityEvent.cancel();
        if (this.mc.player != null) {
            this.mc.player.attack((Entity)(Object)this.fakePlayer);
            this.spawnHitParticles();
            if (this.totem.getValue()) {
                if (++this.hitsSinceTotem >= 3) {
                    this.triggerTotemPop();
                } else {
                    float f = this.fakePlayer.getMaxHealth() * (float)(3 - this.hitsSinceTotem) / 3.0f;
                    this.fakePlayer.setHealth(Math.max(1.0f, f));
                }
            }
        }
    }

    private void spawnHitParticles() {
        double d = this.mc.player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        int n = Math.max(1, (int)(d * 0.5));
        for (int i = 0; i < n; ++i) {
            this.mc.world.addParticleClient((ParticleEffect)ParticleTypes.DAMAGE_INDICATOR, this.fakePlayer.getX(), this.fakePlayer.getBodyY(0.5), this.fakePlayer.getZ(), this.mc.world.random.nextGaussian() * 0.1, 0.0, this.mc.world.random.nextGaussian() * 0.1);
        }
    }


    public static final class FakeRemotePlayer
    extends OtherClientPlayerEntity {
        public FakeRemotePlayer(ClientWorld clientWorld, com.mojang.authlib.GameProfile profile) {
            super(clientWorld, profile);
        }
    }
}

