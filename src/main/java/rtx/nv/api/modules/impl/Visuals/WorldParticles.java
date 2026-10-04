package rtx.nv.api.modules.impl.Visuals;

import rtx.nv.api.events.EventHandler;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3fc;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.impl.Visuals.particles.FadeParticle;
import rtx.nv.api.modules.impl.Visuals.particles.ParticleCollision;
import rtx.nv.api.modules.impl.Visuals.particles.ParticleColors;
import rtx.nv.api.modules.impl.Visuals.particles.ParticleRenderer;
import rtx.nv.api.modules.impl.Visuals.particles.ParticleTexturePicker;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.render2d.ClientPalette;

public final class WorldParticles extends Module {
    private static final int MAX_PARTICLES = 350;
    private static final int DARK_SECOND_COLOR = new Color(16, 16, 16, 75).getRGB();

    private final SeparatorSetting appearanceSeparator = this.register(new SeparatorSetting("Внешний вид"));
    private final ModeSetting display = this.register(new ModeSetting("Отображать", "Выбор текстуры отображаемых частиц.", ParticleTexturePicker.SHOW_ALL, ParticleTexturePicker.worldModeOptions()));
    private final NumberSetting size = this.register(new NumberSetting("Размер", "Масштаб прорисовки частиц.", 140.0, 60.0, 240.0, 10.0));
    private final SliderSetting alpha = this.register(new SliderSetting("Прозрачность", "Общая видимость частиц в мире.").range(0.1f, 1.0f).increment(0.05f).setValue(0.9f));
    private final BooleanSetting scaleWithAlpha = this.register(new BooleanSetting("Скейл", "Масштаб частицы следует за ее прозрачностью.", true));

    private final SeparatorSetting spawnSeparator = this.register(new SeparatorSetting("Появление"));
    private final NumberSetting count = this.register(new NumberSetting("Количество", "Число попыток генерации частиц каждый такт.", 15.0, 1.0, 40.0, 1.0));
    private final NumberSetting range = this.register(new NumberSetting("Радиус спавна", "Горизонтальный радиус появления вокруг игрока.", 35.0, 10.0, 50.0, 1.0));
    private final NumberSetting rangeY = this.register(new NumberSetting("Высота спавна", "Вертикальный разброс появления над игроком.", 20.0, 0.5, 30.0, 0.5));
    private final NumberSetting lifetime = this.register(new NumberSetting("Время жизни", "Длительность существования частицы в миллисекундах.", 800.0, 150.0, 2000.0, 25.0));

    private final SeparatorSetting motionSeparator = this.register(new SeparatorSetting("Движение"));
    private final NumberSetting motionPower = this.register(new NumberSetting("Скорость", "Множитель скорости перемещения частиц.", 1.0, 0.1, 2.0, 0.1));
    private final NumberSetting gravity = this.register(new NumberSetting("Гравитация", "Постоянное ускорение по вертикали.", 0.0, -10.0, 10.0, 1.0));
    private final NumberSetting inclineX = this.register(new NumberSetting("Снос по X", "Боковое смещение полета относительно взгляда.", 0.0, -17.5, 17.5, 0.5));
    private final NumberSetting inclineZ = this.register(new NumberSetting("Снос по Z", "Продольное смещение полета относительно взгляда.", 17.5, -17.5, 17.5, 0.5));
    private final ModeSetting collideMode = this.register(new ModeSetting("При столкновении", "Поведение при контакте с твердыми блоками.", "Отскок", ParticleCollision.MODES));

    private final SeparatorSetting colorSeparator = this.register(new SeparatorSetting("Цвет"));
    private final ModeSetting colorMode = this.register(new ModeSetting("Режим цвета", "Цветовая схема оформления частиц.", "Клиент", "Клиент", "Свой"));
    private final BooleanSetting useSecondColor = this.register(new BooleanSetting("Второй цвет", "Использовать градиент со вторым оттенком.", false));
    private final ColorSetting customColor = this.register(new ColorSetting("Цвет", "Основной цвет частиц.", new Color(255, 255, 255, 255)));
    private final ColorSetting customSecondColor = this.register(new ColorSetting("Цвет 2", "Второй оттенок частиц.", new Color(ColorUtil.lerpColor(-1, DARK_SECOND_COLOR, 0.7f), true)));

    private final List<FadeParticle> particles = new ArrayList<>();
    private final Random random = new Random();
    private final ParticleRenderer renderer = new ParticleRenderer();

    public WorldParticles() {
        super("World Particles", "Окружает игрока парящими атмосферными частицами в мире.", Category.VISUALS);
        this.useSecondColor.visibleWhen(() -> this.colorMode.is("Свой"));
        this.customColor.visibleWhen(() -> this.colorMode.is("Свой"));
        this.customSecondColor.visibleWhen(() -> this.colorMode.is("Свой") && this.useSecondColor.getValue());
    }

    @Override
    protected void onDisable() {
        this.particles.clear();
    }

    @Override
    protected void onEnable() {
        this.particles.clear();
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
        float f = this.motionPower.getFloat();
        float f2 = this.gravity.getFloat() / 80.0f * 0.05f * f;
        float f3 = MathHelper.clamp(this.gravity.getFloat() / 10.0f, 0.0f, 1.0f);
        String string = this.collideMode.getValue();
        float f4 = this.cameraYaw();
        double d = Math.sin(Math.toRadians(f4));
        double d2 = -Math.cos(Math.toRadians(f4));
        double d3 = -Math.sin(Math.toRadians(f4 + 90.0f));
        double d4 = Math.cos(Math.toRadians(f4 + 90.0f));
        double d5 = (d * (double) this.inclineZ.getFloat() / 50.0 + d3 * (double) this.inclineX.getFloat() / 50.0) * 0.05 * (double) f;
        double d6 = (d2 * (double) this.inclineZ.getFloat() / 50.0 + d4 * (double) this.inclineX.getFloat() / 50.0) * 0.05 * (double) f;
        for (FadeParticle fadeParticle2 : this.particles) {
            this.stepParticle(fadeParticle2, f, d5, f2, d6, string, f3);
        }
        this.spawnParticles();
    }

    private float cameraYaw() {
        float f = this.mc.player.getYaw();
        if (this.mc.options.getPerspective().isFrontView()) {
            f += 180.0f;
        }
        return f;
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
        float f = MathHelper.clamp((float) worldRenderEvent.getPartialTicks(), 0.0f, 1.0f);
        float f2 = this.size.getFloat() * 0.012f;
        boolean bl = this.scaleWithAlpha.getValue();
        float globalAlpha = this.alpha.getFloat();
        this.renderer.clear();
        for (FadeParticle fadeParticle : this.particles) {
            float f3 = fadeParticle.alpha01(l) * globalAlpha;
            if (f3 <= 0.004f) continue;
            Vec3d vec3d2 = fadeParticle.renderPos(f);
            int n = this.particleColor(fadeParticle, f3);
            float f4 = bl ? f2 * f3 : f2;
            this.renderer.drawTexture(matrixStack, immediate, fadeParticle.texture, vec3d2, vec3d, quaternionf, f4, fadeParticle.rotationDeg, n, true);
        }
        this.renderer.flush(immediate);
    }

    private void stepParticle(FadeParticle fadeParticle, float f, double d, double d2, double d3, String string, float f2) {
        fadeParticle.beginStep();
        if (!fadeParticle.holdCollide) {
            fadeParticle.posX += fadeParticle.motionX * (double) f;
            fadeParticle.posY += fadeParticle.motionY * (double) f;
            fadeParticle.posZ += fadeParticle.motionZ * (double) f;
            fadeParticle.motionX += d;
            fadeParticle.motionY += d2;
            fadeParticle.motionZ += d3;
        }
        fadeParticle.motionY -= 6.0E-4;
        boolean bl = ParticleCollision.apply(this.mc, fadeParticle, string, f2);
        if (!bl) {
            fadeParticle.posX += fadeParticle.motionX;
            fadeParticle.posY += fadeParticle.motionY;
            fadeParticle.posZ += fadeParticle.motionZ;
        }
    }

    private void spawnParticles() {
        int n = this.count.getInt();
        float f = this.range.getFloat();
        float f2 = Math.max(0.55f, this.rangeY.getFloat());
        float f3 = this.lifetime.getFloat();
        for (int i = 0; i < n && this.particles.size() < MAX_PARTICLES; ++i) {
            double d = this.randomRange(-f, f);
            double d2 = this.randomRange(-f, f);
            double d3 = this.randomRange(0.5, f2);
            Vec3d vec3d = this.mc.player.getEntityPos().add(d, d3, d2);
            if (!this.mc.world.getBlockState(BlockPos.ofFloored((Position) vec3d)).isAir() || !this.isInPlayerView(vec3d)) continue;
            Vec3d vec3d2 = new Vec3d(this.randomRange(-0.04, 0.04), this.randomRange(0.0, 0.05), this.randomRange(-0.04, 0.04));
            float f4 = n <= 1 ? 0.0f : (float) i / (float) (n - 1);
            this.particles.add(new FadeParticle(vec3d, vec3d2, (float) this.randomRange(0.0, 180.0), f3, ParticleTexturePicker.pickWorld(this.display, this.random), f4));
        }
    }

    private boolean isInPlayerView(Vec3d vec3d) {
        Vec3d vec3d2;
        Vec3d vec3d3 = Vec3d.fromPolar(this.mc.player.getPitch(), this.cameraYaw());
        return vec3d3.dotProduct(vec3d2 = vec3d.subtract(this.mc.player.getEntityPos()).normalize()) > 0.1;
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
