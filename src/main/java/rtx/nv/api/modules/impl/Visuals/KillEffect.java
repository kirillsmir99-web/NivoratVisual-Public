package rtx.nv.api.modules.impl.Visuals;

import rtx.nv.api.events.EventHandler;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import rtx.nv.api.drags.Position;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.player.AttackEntityEvent;
import rtx.nv.api.events.impl.render.DrawEvent;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.impl.Visuals.killeffect.KillEffectDeathMemoryTracker;
import rtx.nv.api.modules.impl.Visuals.killeffect.KillEffectEasing;
import rtx.nv.api.modules.impl.Visuals.killeffect.KillEffectParticleSystem;
import rtx.nv.api.modules.impl.Visuals.killeffect.KillEffectScanRenderer;
import rtx.nv.api.modules.impl.Visuals.killeffect.KillEffectSoundQueue;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.MultiSelectSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.others.RenderCompatibility;
import rtx.nv.utils.render.post.killdistortion.KillDistortionRenderer;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.sounds.SoundManager;
import rtx.nv.utils.storage.friend.FriendUtils;
import rtx.nv.utils.time.StopWatch;

public final class KillEffect extends Module {
    public static final String MODE_IMPULSE = "Импульс";
    public static final String MODE_STAR_DECAY = "Звёздный распад";
    public static final String MODE_SCAN = "Сканирование";

    private static final Identifier VIGNETTE_TEXTURE = Identifier.of("nv", "textures/effects/frag/lightarroundscreen_alpha.png");
    private static final String TARGET_PLAYERS = "Игроки";
    private static final String TARGET_FRIENDS = "Друзья";
    private static final String TARGET_MOBS = "Мобы";
    private static final String TARGET_ANIMALS = "Животные";
    private static final String COLOR_CLIENT = "Клиент";
    private static final String COLOR_CUSTOM = "Свой";
    private static final int DARK_SECOND_COLOR = new Color(16, 16, 16, 75).getRGB();

    private final KillEffectSoundQueue soundQueue = new KillEffectSoundQueue();
    private final KillEffectParticleSystem particleSystem = new KillEffectParticleSystem();
    private final StopWatch effectTimer = new StopWatch();
    private final Map<Integer, Long> recentlyAttacked = new HashMap<>();
    private final KillEffectDeathMemoryTracker deathMemoryTracker = new KillEffectDeathMemoryTracker(2500L, this::handleRememberedDeath);

    private int lastKilledEntityId = -1;
    private long lastKillTimestamp = 0L;

    private final SeparatorSetting modeSeparator = this.register(new SeparatorSetting("Режим"));
    private final ModeSetting effectMode = this.register(new ModeSetting("Режим", "Стиль эффекта убийства.", MODE_IMPULSE, MODE_IMPULSE, MODE_STAR_DECAY, MODE_SCAN));
    private final SliderSetting effectSize = this.register(new SliderSetting("Размер", "Масштаб зоны эффекта убийства.").range(0.5f, 3.0f).increment(0.1f).setValue(1.2f));
    private final SliderSetting effectDuration = this.register(new SliderSetting("Длительность", "Длительность эффекта убийства в секундах.").range(0.5f, 5.0f).increment(0.1f).setValue(1.5f));
    private final SliderSetting particleCount = this.register(new SliderSetting("Количество элементов", "Количество частиц и элементов эффекта.").range(10.0f, 100.0f).increment(2.0f).setValue(36.0f));
    private final SliderSetting intensity = this.register(new SliderSetting("Интенсивность", "Сила визуального проявления эффекта.").range(0.2f, 2.5f).increment(0.1f).setValue(1.0f));

    private final SeparatorSetting colorsSeparator = this.register(new SeparatorSetting("Цвета"));
    private final ModeSetting colorMode = this.register(new ModeSetting("Режим цвета", "Режим окрашивания эффекта убийства.", COLOR_CLIENT, COLOR_CLIENT, COLOR_CUSTOM));
    private final BooleanSetting useSecondColor = this.register(new BooleanSetting("Второй цвет", "Использовать второй оттенок для градиента.", false));
    private final ColorSetting customColor = this.register(new ColorSetting("Цвет", "Основной цвет эффекта убийства.", new Color(-50116, true)));
    private final ColorSetting customSecondColor = this.register(new ColorSetting("Цвет 2", "Второй цвет эффекта убийства.", new Color(ColorUtil.lerpColor(-50116, DARK_SECOND_COLOR, 0.7f), true)));

    private final SeparatorSetting targetsSeparator = this.register(new SeparatorSetting("Цели"));
    private final MultiSelectSetting effectTargets = this.register(new MultiSelectSetting("Цели эффекта", "Какие сущности запускают эффект убийства.")
        .value(TARGET_PLAYERS, TARGET_FRIENDS, TARGET_MOBS, TARGET_ANIMALS)
        .selected(TARGET_PLAYERS, TARGET_FRIENDS, TARGET_MOBS, TARGET_ANIMALS));

    private final SeparatorSetting effectsSeparator = this.register(new SeparatorSetting("Эффекты"));
    private final BooleanSetting spikes = this.register(new BooleanSetting("Шипы", "Показывает накладку из шипов по краям экрана.", true));
    private final BooleanSetting changeSaturation = this.register(new BooleanSetting("Менять насыщенность", "Кратковременно снижает насыщенность мира во время эффекта.", false));
    private final BooleanSetting zoomEffect = this.register(new BooleanSetting("Приближение", "Мягко приближает и возвращает камеру при убийстве.", false));
    private final BooleanSetting cameraShake = this.register(new BooleanSetting("Тряска камеры", "Добавляет легкую тряску камеры при убийстве.", false));
    private final BooleanSetting distortion = this.register(new BooleanSetting("Искажение", "Ударная стеклянная волна от места убийства.", false));

    private boolean animate;
    private float activeEffectSpeedMultiplier = 1.0f;
    private final StopWatch cameraTimer = new StopWatch();
    private boolean cameraAnimate;
    private static final long CAMERA_EFFECT_MS = 650L;
    private static final long DISTORTION_MS = 900L;
    private final StopWatch distortionTimer = new StopWatch();
    private boolean distortionAnimate;
    private Vec3d distortionCenter = Vec3d.ZERO;

    public KillEffect() {
        super("Kill Effect", "Эффект сканирования и распада при победе над целью.", Category.VISUALS);
        this.useSecondColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM));
        this.customColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM));
        this.customSecondColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM) && this.useSecondColor.getValue());
    }

    public static KillEffect getInstance() {
        return ModuleManager.get().get(KillEffect.class);
    }

    public static KillEffect getInstanceIfReady() {
        try {
            return KillEffect.getInstance();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void resetState() {
        this.animate = false;
        this.cameraAnimate = false;
        this.distortionAnimate = false;
        this.activeEffectSpeedMultiplier = 1.0f;
        this.soundQueue.clear();
        this.particleSystem.clear();
        this.recentlyAttacked.clear();
        this.deathMemoryTracker.clear();
        this.lastKilledEntityId = -1;
        this.lastKillTimestamp = 0L;
        KillEffectScanRenderer.clear();
        KillDistortionRenderer.clear();
    }

    @Override
    protected void onDisable() {
        this.resetState();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!event.isPost()) return;
        if (this.mc.player == null || this.mc.world == null) {
            this.resetState();
            return;
        }
        this.deathMemoryTracker.tick();
        this.particleSystem.tick();
        this.soundQueue.tick();
    }

    @EventHandler
    public void onAttack(AttackEntityEvent event) {
        if (this.mc.player == null || event.isSynthetic()) return;
        Entity target = event.getTarget();
        if (target instanceof LivingEntity living && living != this.mc.player) {
            this.recentlyAttacked.put(living.getId(), System.currentTimeMillis());
            this.deathMemoryTracker.remember(living, true);
            this.pruneAttackMemory();
        }
    }

    private int accent() {
        int c1;
        int c2;
        if (this.colorMode.is(COLOR_CLIENT)) {
            InterfaceModule iface = InterfaceModule.getInstance();
            if (iface != null) {
                c1 = iface.clientPrimaryColorOpaque();
                c2 = iface.usesSecondClientColor() ? iface.clientSecondaryColorOpaque() : c1;
            } else {
                c1 = c2 = -50116;
            }
        } else {
            c1 = this.customColor.getColorOpaque();
            c2 = this.useSecondColor.getValue() ? this.customSecondColor.getColorOpaque() : c1;
        }
        if (c1 == c2) {
            return opaque(c1);
        }
        return opaque(fade(c1, c2));
    }

    private void scheduleCharmSounds() {
        long step = Math.max(1L, (long)(150L / this.intensity.getFloat()));
        this.soundQueue.schedule(SoundManager.FRAG_EFFECT_PULSE, 1.0f, 0L);
        this.soundQueue.schedule(SoundManager.FRAG_EFFECT_KNOCK_MAIN, 1.0f, step);
        this.soundQueue.schedule(SoundManager.FRAG_EFFECT_SPARKS_COLLISION, 0.2f, step * 2L);
        this.soundQueue.schedule(SoundManager.FRAG_EFFECT_ECHO_MAIN, 0.6f, step * 3L);
    }

    private void handleDeath(LivingEntity livingEntity, DamageSource damageSource) {
        if (this.mc.player == null || this.mc.world == null || livingEntity == null || livingEntity == this.mc.player) {
            return;
        }
        if (!this.localPlayerGotKill(livingEntity, damageSource)) {
            return;
        }
        if (!this.matchesEffectTarget(livingEntity)) {
            return;
        }
        this.recentlyAttacked.remove(livingEntity.getId());
        this.deathMemoryTracker.forget(livingEntity.getId());
        this.onTrackedKill(livingEntity);
    }

    private void drawVignette(DrawEvent event) {
        if (!this.spikes.isValue()) return;
        long duration = this.scaleDuration(600L);
        float progress = this.animate ? Math.min((float)this.effectTimer.elapsedTime() / (float)duration, 1.0f) : 0.0f;
        if (progress <= 0.0f || progress >= 1.0f) return;

        float sin = (float) Math.sin(progress * Math.PI);
        float quint = KillEffectEasing.quintOut(sin);
        float inv = 1.0f - quint;
        float w = Position.screenWidth();
        float h = Position.screenHeight();
        float ow = w * 0.75f * inv;
        float oh = h * 0.75f * inv;

        int color = ColorUtil.withAlpha(ColorUtil.lerpColor(lighten(this.accent(), 0.22f), -1, progress), Math.round(150.0f * sin * this.intensity.getFloat()));
        Render2D.beginFrame(event.getGraphics());
        Render2D.image(VIGNETTE_TEXTURE.toString(), -ow, -oh, w + ow * 2.0f, h + oh * 2.0f, 0.0f, color);
        Render2D.flush();
    }

    private void renderDistortion(WorldRenderEvent event) {
        if (!this.distortion.isValue() || !this.distortionAnimate || KillDistortionRenderer.isDisabledAfterError()) return;
        float p = Math.min((float)this.distortionTimer.elapsedTime() / 900.0f, 1.0f);
        if (p >= 1.0f) {
            this.distortionAnimate = false;
            return;
        }
        if (this.mc.player == null || event.getCamera() == null) return;
        Framebuffer fb = this.mc.getFramebuffer();
        if (fb == null || fb.textureWidth <= 0 || fb.textureHeight <= 0) return;

        Matrix4f posMat = event.getPositionMatrix();
        Matrix4f projMat = event.getProjectionMatrix();
        if (posMat == null || projMat == null) return;

        Vec3d cam = event.getCamera().getCameraPos();
        Vector4f vec = new Vector4f((float)(this.distortionCenter.x - cam.x), (float)(this.distortionCenter.y - cam.y), (float)(this.distortionCenter.z - cam.z), 1.0f);
        posMat.transform(vec);
        projMat.transform(vec);
        if (vec.w <= 1.0e-4f) return;

        float cx = vec.x / vec.w * 0.5f + 0.5f;
        float cy = vec.y / vec.w * 0.5f + 0.5f;
        float cz = vec.z / vec.w * 0.5f + 0.5f;
        float aspect = (float) fb.textureWidth / (float) Math.max(1, fb.textureHeight);
        float radius = KillEffectEasing.sineInOut(p) * 1.4f * this.effectSize.getFloat();
        float width = (0.14f - 0.05f * p) * this.intensity.getFloat();
        float strength = (float) Math.sin(p * Math.PI) * 0.055f * this.intensity.getFloat();

        float[] params = new float[]{cx, cy, cz, aspect, radius, width, strength, p * 6.0f};
        KillDistortionRenderer.apply(fb, params);
    }

    private void pruneAttackMemory() {
        long cutoff = System.currentTimeMillis() - 2500L;
        this.recentlyAttacked.values().removeIf(t -> t < cutoff);
    }

    public static float getKillZoomFovScale() {
        KillEffect killEffect = KillEffect.getInstanceIfReady();
        if (killEffect == null || !killEffect.isEnabled() || !killEffect.zoomEffect.isValue()) return 1.0f;
        float env = killEffect.cameraEnvelope();
        if (env <= 0.0f) return 1.0f;
        float zoom = 15.0f * env * killEffect.intensity.getFloat();
        return Math.clamp(1.0f - zoom / 70.0f, 0.5f, 1.0f);
    }

    private boolean localPlayerGotKill(LivingEntity livingEntity, DamageSource damageSource) {
        ClientPlayerEntity player = this.mc.player;
        if (player == null) return false;
        Long lastAtk = this.recentlyAttacked.get(livingEntity.getId());
        if (lastAtk != null && System.currentTimeMillis() - lastAtk <= 2500L) {
            return true;
        }
        if (livingEntity.getPrimeAdversary() == player) {
            return true;
        }
        return damageSource != null && damageSource.getAttacker() == player;
    }

    private long scaleDuration(long duration) {
        return Math.max(1L, (long) Math.round((float) duration / Math.clamp(this.activeEffectSpeedMultiplier, 0.25f, 2.0f)));
    }

    public static float getKillShakeDegrees() {
        KillEffect killEffect = KillEffect.getInstanceIfReady();
        if (killEffect == null || !killEffect.isEnabled() || !killEffect.cameraShake.isValue()) return 0.0f;
        float p = killEffect.cameraProgress();
        if (p <= 0.0f) return 0.0f;
        float t = (float) killEffect.cameraTimer.elapsedTime() / 1000.0f;
        float shake = (float)(Math.sin(t * 26.0) * 0.65 + Math.sin(t * 16.5) * 0.35);
        float decay = (1.0f - p) * (1.0f - p);
        return shake * 2.0f * decay * killEffect.intensity.getFloat();
    }

    private void restartEffect(float speed) {
        this.animate = true;
        this.activeEffectSpeedMultiplier = Math.clamp(speed, 0.25f, 2.0f);
        this.effectTimer.reset();
    }

    private boolean matchesEffectTarget(LivingEntity livingEntity) {
        if (livingEntity == null || livingEntity == this.mc.player) return false;
        if (livingEntity instanceof PlayerEntity player) {
            if (FriendUtils.isFriend(player.getName().getString())) {
                return this.effectTargets.isSelected(TARGET_FRIENDS);
            }
            return this.effectTargets.isSelected(TARGET_PLAYERS);
        }
        if (livingEntity instanceof AnimalEntity) {
            return this.effectTargets.isSelected(TARGET_ANIMALS);
        }
        if (livingEntity instanceof MobEntity) {
            return this.effectTargets.isSelected(TARGET_MOBS);
        }
        return false;
    }

    private void handleRememberedDeath(LivingEntity livingEntity) {
        if (!this.matchesEffectTarget(livingEntity)) return;
        this.recentlyAttacked.remove(livingEntity.getId());
        this.onTrackedKill(livingEntity);
    }

    private float cameraProgress() {
        if (!this.cameraAnimate) return 0.0f;
        float p = Math.min((float)this.cameraTimer.elapsedTime() / 650.0f, 1.0f);
        if (p >= 1.0f) {
            this.cameraAnimate = false;
        }
        return p;
    }

    private float cameraEnvelope() {
        float p = this.cameraProgress();
        return p <= 0.0f ? 0.0f : (float) Math.sin(p * Math.PI);
    }

    private long effectDurationMs() {
        return (long) (this.effectDuration.getFloat() * 1000.0f);
    }

    private void onTrackedKill(LivingEntity livingEntity) {
        if (!this.matchesEffectTarget(livingEntity)) return;
        if (this.mc.player == null || this.mc.player.isDead()) return;

        long now = System.currentTimeMillis();
        if (livingEntity.getId() == this.lastKilledEntityId && now - this.lastKillTimestamp < 1500L) {
            return;
        }
        this.lastKilledEntityId = livingEntity.getId();
        this.lastKillTimestamp = now;

        Vec3d pos = livingEntity.getEyePos();
        this.restartEffect(1.0f);

        String mode = this.effectMode.getValue();
        int count = (int) this.particleCount.getFloat();
        float size = this.effectSize.getFloat();
        float power = this.intensity.getFloat();

        if (MODE_IMPULSE.equals(mode)) {
            this.particleSystem.spawnImpulse(pos, count, size, power);
        } else if (MODE_STAR_DECAY.equals(mode)) {
            this.particleSystem.spawnStarDecay(pos, count, size, power);
        } else {
            KillEffectScanRenderer.ping(pos, 1.0f);
            this.particleSystem.spawnBurst(pos, 480, 900, 1.9f * size, count, 0.012f);
        }

        this.scheduleCharmSounds();
        if (this.zoomEffect.isValue() || this.cameraShake.isValue()) {
            this.cameraAnimate = true;
            this.cameraTimer.reset();
        }
        if (this.distortion.isValue()) {
            this.distortionCenter = pos;
            this.distortionAnimate = true;
            this.distortionTimer.reset();
        }
    }

    public static float getWorldSaturationMultiplier() {
        KillEffect killEffect = KillEffect.getInstanceIfReady();
        if (killEffect == null || !killEffect.isEnabled() || !killEffect.changeSaturation.isValue()) return 1.0f;
        float elapsed = (float) killEffect.effectTimer.elapsedTime();
        float total = (float) killEffect.effectDurationMs();
        if (!killEffect.animate || elapsed >= total) return 1.0f;
        float p = elapsed / total;
        float sin = (float) Math.sin(p * Math.PI) * killEffect.intensity.getFloat();
        return Math.clamp(1.0f - sin * 0.7f, 0.0f, 1.0f);
    }

    private static int red(int n) { return n >>> 16 & 0xFF; }
    private static int green(int n) { return n >>> 8 & 0xFF; }
    private static int blue(int n) { return n & 0xFF; }
    private static int alpha(int n) { return n >>> 24 & 0xFF; }

    private static int lighten(int n, float f) {
        float f2 = Math.clamp(f, 0.0f, 1.0f);
        int r = red(n); int g = green(n); int b = blue(n); int a = alpha(n);
        return ColorUtil.rgba(Math.clamp((long)Math.round(r + (255 - r) * f2), 0, 255), Math.clamp((long)Math.round(g + (255 - g) * f2), 0, 255), Math.clamp((long)Math.round(b + (255 - b) * f2), 0, 255), a);
    }

    private static int darken(int n, float f) {
        float f2 = 1.0f - Math.clamp(f, 0.0f, 1.0f);
        return ColorUtil.rgba(Math.clamp((long)Math.round(red(n) * f2), 0, 255), Math.clamp((long)Math.round(green(n) * f2), 0, 255), Math.clamp((long)Math.round(blue(n) * f2), 0, 255), alpha(n));
    }

    private static int fade(int n, int n2) {
        int n3 = (int)(System.currentTimeMillis() / 8L % 360L);
        n3 = n3 >= 180 ? 360 - n3 : n3;
        return ColorUtil.lerpColor(n, n2, (float)n3 / 180.0f);
    }

    private static int opaque(int n) { return 0xFF000000 | n & 0xFFFFFF; }

    private int c1() { return lighten(this.accent(), 0.45f); }
    private int c2() { return lighten(this.accent(), 0.18f); }
    private int c3() { return darken(this.accent(), 0.18f); }
    private int c4() { return lighten(this.accent(), 0.6f); }
    private int c5() { return lighten(this.accent(), 0.7f); }
    private int c6() { return ColorUtil.lerpColor(this.accent(), -1, 0.28f); }
    private int c7() { return darken(this.accent(), 0.08f); }
    private int c8() { return darken(this.accent(), 0.35f); }

    @EventHandler
    public void onDraw(DrawEvent drawEvent) {
        this.drawVignette(drawEvent);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent worldRenderEvent) {
        if (!this.isEnabled() || RenderCompatibility.shouldDisableFragEffectScanShader() || KillEffectScanRenderer.isDisabledAfterError()) {
            return;
        }
        if (MODE_SCAN.equals(this.effectMode.getValue())) {
            KillEffectScanRenderer.render(worldRenderEvent, this.activeEffectSpeedMultiplier, this.c1(), this.c2(), this.c3(), this.c4(), this.c5(), this.c6(), this.c7(), this.c8());
        }
        this.renderDistortion(worldRenderEvent);
    }

    public static void notifyEntityDied(LivingEntity livingEntity, DamageSource damageSource) {
        KillEffect killEffect = KillEffect.getInstanceIfReady();
        if (killEffect == null || !killEffect.isEnabled()) {
            return;
        }
        killEffect.handleDeath(livingEntity, damageSource);
    }
}
