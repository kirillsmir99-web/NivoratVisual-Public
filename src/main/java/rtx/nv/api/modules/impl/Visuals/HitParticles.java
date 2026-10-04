package rtx.nv.api.modules.impl.Visuals;

import rtx.nv.api.events.EventHandler;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3fc;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.player.AttackEntityEvent;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.impl.Visuals.particles.FadeParticle;
import rtx.nv.api.modules.impl.Visuals.particles.ParticleCollision;
import rtx.nv.api.modules.impl.Visuals.particles.ParticleColors;
import rtx.nv.api.modules.impl.Visuals.particles.ParticleRenderer;
import rtx.nv.api.modules.impl.Visuals.particles.ParticleTexturePicker;
import rtx.nv.api.modules.impl.Visuals.particles.WorldParticleUtil;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.render2d.ClientPalette;

public final class HitParticles extends Module {
    private static final int MAX_PARTICLES = 300;
    private static final int DARK_SECOND_COLOR = new Color(16, 16, 16, 75).getRGB();
    private static final String DIR_RANDOM = "Случайно";
    private static final String DIR_FROM_TARGET = "От цели";
    private static final String DIR_TO_TARGET = "К цели";
    private static final String DIR_TWIST = "Закручивание";

    private final SeparatorSetting appearanceSeparator = this.register(new SeparatorSetting("Внешний вид"));
    private final ModeSetting display = this.register(new ModeSetting("Отображать", "Выбор текстуры частиц удара.", ParticleTexturePicker.SHOW_ALL, ParticleTexturePicker.hitModeOptions()));
    private final NumberSetting size = this.register(new NumberSetting("Размер", "Масштаб прорисовки частиц.", 50.0, 15.0, 75.0, 1.0));
    private final BooleanSetting scaleWithAlpha = this.register(new BooleanSetting("Скейл", "Масштаб частицы следует за ее прозрачностью.", true));

    private final SeparatorSetting spawnSeparator = this.register(new SeparatorSetting("Появление"));
    private final NumberSetting count = this.register(new NumberSetting("Количество", "Число частиц за один удар.", 4.0, 1.0, 32.0, 1.0));
    private final NumberSetting spread = this.register(new NumberSetting("Разброс", "Радиус рассеивания вокруг точки удара.", 1.5, 0.5, 4.0, 0.5));
    private final ModeSetting spawnDirection = this.register(new ModeSetting("Пресет полета", "Направление разлета частиц при ударе.", DIR_RANDOM, DIR_RANDOM, DIR_FROM_TARGET, DIR_TO_TARGET, DIR_TWIST));
    private final NumberSetting lifetime = this.register(new NumberSetting("Время жизни", "Длительность существования частицы в миллисекундах.", 1200.0, 300.0, 2000.0, 25.0));

    private final SeparatorSetting motionSeparator = this.register(new SeparatorSetting("Движение"));
    private final NumberSetting speed = this.register(new NumberSetting("Скорость", "Начальная скорость разлета частиц.", 0.35, 0.05, 1.0, 0.05));
    private final NumberSetting gravity = this.register(new NumberSetting("Гравитация", "Коэффициент ускорения по вертикали вниз.", 0.2, -0.5, 1.0, 0.05));
    private final ModeSetting collideMode = this.register(new ModeSetting("При столкновении", "Поведение при встрече с блоками.", "Отскок", ParticleCollision.MODES));

    private final SeparatorSetting colorSeparator = this.register(new SeparatorSetting("Цвет"));
    private final ModeSetting colorMode = this.register(new ModeSetting("Режим цвета", "Цветовая схема оформления частиц.", "Клиент", "Клиент", "Свой"));
    private final BooleanSetting useSecondColor = this.register(new BooleanSetting("Второй цвет", "Использовать градиент со вторым оттенком.", false));
    private final ColorSetting customColor = this.register(new ColorSetting("Цвет", "Основной цвет частиц.", new Color(255, 255, 255, 255)));
    private final ColorSetting customSecondColor = this.register(new ColorSetting("Цвет 2", "Второй оттенок частиц.", new Color(ColorUtil.lerpColor(-1, DARK_SECOND_COLOR, 0.7f), true)));

    private final List<FadeParticle> particles = new ArrayList<>();
    private final Random random = new Random();
    private final ParticleRenderer renderer = new ParticleRenderer();

    private int lastTargetId = -1;
    private long lastAttackMs = 0L;

    public HitParticles() {
        super("Hit Particles", "Создает всплеск красивых частиц при нанесении удара по цели.", Category.VISUALS);
        this.useSecondColor.visibleWhen(() -> this.colorMode.is("Свой"));
        this.customColor.visibleWhen(() -> this.colorMode.is("Свой"));
        this.customSecondColor.visibleWhen(() -> this.colorMode.is("Свой") && this.useSecondColor.getValue());
    }

    @Override
    protected void onDisable() {
        this.particles.clear();
        this.lastTargetId = -1;
    }

    @Override
    protected void onEnable() {
        this.particles.clear();
        this.lastTargetId = -1;
    }

    @EventHandler
    private void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPre() || !this.isEnabled()) {
            return;
        }
        if (this.mc.world == null || this.mc.player == null) {
            this.particles.clear();
            return;
        }
        long l = System.currentTimeMillis();
        this.particles.removeIf(fadeParticle -> fadeParticle.isDead(l));
        float f = this.gravity.getFloat();
        String string = this.collideMode.getValue();
        for (FadeParticle fadeParticle2 : this.particles) {
            this.stepParticle(fadeParticle2, f, string);
        }
    }

    @EventHandler
    private void onAttack(AttackEntityEvent attackEntityEvent) {
        if (!this.isEnabled() || this.mc.world == null || this.mc.player == null) {
            return;
        }
        Entity entity = attackEntityEvent.getTarget();
        if (entity == null) {
            return;
        }

        // Debounce to ensure one hit event does not spawn particles multiple times
        long now = System.currentTimeMillis();
        if (entity.getId() == this.lastTargetId && (now - this.lastAttackMs < 80L)) {
            return;
        }
        this.lastTargetId = entity.getId();
        this.lastAttackMs = now;

        int n = this.count.getInt();
        float f = this.lifetime.getFloat();
        float f2 = this.spread.getFloat();
        float f3 = this.speed.getFloat();
        Vec3d vec3d = entity.getEyePos().add(0.0, (double) (-entity.getStandingEyeHeight() / 2.0f), 0.0);
        float f4 = this.spawnDirection.is(DIR_TWIST) ? (this.random.nextBoolean() ? 1.0f : -1.0f) : 0.0f;
        for (int i = 0; i < n && this.particles.size() < MAX_PARTICLES; ++i) {
            Vec3d vec3d2 = WorldParticleUtil.randomSpawnPosition((MinecraftClient) this.mc, (Random) this.random, f2, vec3d);
            if (vec3d2 == null) continue;
            double d = this.randomRange((double) f3 / 1.5, (double) f3 * 1.5);
            Vec3d vec3d3 = this.initialMotion(vec3d, vec3d2, d, f4);
            float f5 = n <= 1 ? 0.0f : (float) i / (float) (n - 1);
            FadeParticle fadeParticle = new FadeParticle(vec3d2, vec3d3, (float) this.randomRange(0.0, 180.0), f, ParticleTexturePicker.pickHit(this.display, this.random), f5);
            fadeParticle.twist = f4;
            this.particles.add(fadeParticle);
        }
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent worldRenderEvent) {
        if (!this.isEnabled() || this.particles.isEmpty() || this.mc.world == null || this.mc.player == null || this.mc.gameRenderer == null) {
            return;
        }
        long l = System.currentTimeMillis();
        MatrixStack matrixStack = worldRenderEvent.getStack();
        VertexConsumerProvider.Immediate immediate = this.mc.getBufferBuilders().getEntityVertexConsumers();
        Vec3d vec3d = this.mc.gameRenderer.getCamera().getCameraPos();
        Quaternionf quaternionf = this.mc.gameRenderer.getCamera().getRotation();
        Vector3fc vector3fc = this.mc.gameRenderer.getCamera().getHorizontalPlane();
        float f = MathHelper.clamp((float) worldRenderEvent.getPartialTicks(), 0.0f, 1.0f);
        float f2 = this.size.getFloat() * 0.012f;
        boolean bl = this.scaleWithAlpha.getValue();
        this.renderer.clear();
        for (FadeParticle fadeParticle : this.particles) {
            float f3 = fadeParticle.alpha01(l);
            if (f3 <= 0.004f) continue;
            Vec3d vec3d2 = fadeParticle.renderPos(f).add(0.0, 0.2, 0.0);
            double d = vec3d2.x - vec3d.x;
            double d2 = vec3d2.y - vec3d.y;
            double d3 = vec3d2.z - vec3d.z;
            if (d * (double) vector3fc.x() + d2 * (double) vector3fc.y() + d3 * (double) vector3fc.z() < (double) (-f2 * 2.0f)) continue;
            int n = this.particleColor(fadeParticle, f3);
            float f4 = bl ? f2 * f3 : f2;
            this.renderer.drawTexture(matrixStack, immediate, fadeParticle.texture, vec3d2, vec3d, quaternionf, f4, fadeParticle.rotationDeg, n, true);
        }
        this.renderer.flush(immediate);
    }

    private void stepParticle(FadeParticle fadeParticle, float f, String string) {
        fadeParticle.beginStep();
        if (fadeParticle.twist != 0.0f && !fadeParticle.holdCollide) {
            double d = 0.09 * (double) fadeParticle.twist;
            double d2 = Math.cos(d);
            double d3 = Math.sin(d);
            double d4 = fadeParticle.motionX * d2 - fadeParticle.motionZ * d3;
            double d5 = fadeParticle.motionX * d3 + fadeParticle.motionZ * d2;
            fadeParticle.motionX = d4;
            fadeParticle.motionZ = d5;
        }
        fadeParticle.motionY -= (double) f / 17.5;
        boolean bl = ParticleCollision.apply(this.mc, fadeParticle, string, 0.1f);
        if (!bl) {
            fadeParticle.posX += fadeParticle.motionX;
            fadeParticle.posY += fadeParticle.motionY;
            fadeParticle.posZ += fadeParticle.motionZ;
        }
    }

    private Vec3d initialMotion(Vec3d vec3d, Vec3d vec3d2, double d, float f) {
        Vec3d vec3d3 = new Vec3d(this.randomRange(-0.06, 0.06), this.randomRange(0.05, 0.15), this.randomRange(-0.06, 0.06));
        if (this.spawnDirection.is(DIR_RANDOM)) {
            return vec3d3.add(this.randomRange(-d, d), this.randomRange(0.0, d), this.randomRange(-d, d));
        }
        Vec3d vec3d4 = vec3d2.subtract(vec3d);
        if (vec3d4.lengthSquared() < 1.0E-4) {
            vec3d4 = new Vec3d(0.0, 1.0, 0.0);
        } else {
            vec3d4 = vec3d4.normalize();
        }
        if (this.spawnDirection.is(DIR_TO_TARGET)) {
            vec3d4 = vec3d4.negate();
        }
        if (this.spawnDirection.is(DIR_TWIST)) {
            Vec3d vec3d5 = new Vec3d(0.0, 1.0, 0.0).crossProduct(vec3d4);
            if (vec3d5.lengthSquared() > 1.0E-4) {
                vec3d4 = vec3d5.normalize().multiply((double) f);
            }
        }
        return vec3d3.add(vec3d4.multiply(d));
    }

    private int particleColor(FadeParticle fadeParticle, float f) {
        int n;
        int n2;
        int n3 = (int) (fadeParticle.gradientT * 360.0f);
        if (this.colorMode.is("Клиент")) {
            int[] nArray = ClientPalette.colors();
            if (nArray != null && nArray.length >= 2) {
                return ColorUtil.multAlpha(ParticleColors.paletteFade(8, n3, nArray), f);
            }
            InterfaceModule interfaceModule = InterfaceModule.getInstance();
            if (interfaceModule != null) {
                n2 = interfaceModule.clientPrimaryColorOpaque();
                n = interfaceModule.usesSecondClientColor() ? interfaceModule.clientSecondaryColorOpaque() : n2;
            } else {
                n = n2 = -1;
            }
        } else {
            n2 = this.customColor.getColor();
            n = this.useSecondColor.getValue() ? this.customSecondColor.getColor() : this.customColor.getColor();
        }
        if (n2 == n) {
            return ColorUtil.multAlpha(n2, f);
        }
        return ColorUtil.multAlpha(ParticleColors.fade(8, n3, n2, n), f);
    }

    private double randomRange(double d, double d2) {
        return d + this.random.nextDouble() * (d2 - d);
    }
}
