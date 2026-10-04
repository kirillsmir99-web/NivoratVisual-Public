package rtx.nv.api.modules.impl.Visuals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.player.AttackEntityEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.post.hitbubbles.HitBubblesRenderer;
import rtx.nv.utils.render.render2d.ClientPalette;

public final class HitBubbles extends Module {
    public static final String FORM_BUBBLE = "Пузырь";
    public static final String FORM_LENS = "Линза";
    public static final String FORM_PULSE = "Импульсное кольцо";

    public static final String COLOR_CLIENT = "Клиент";
    public static final String COLOR_CUSTOM = "Свой";

    private static final int DARK_SECOND_COLOR = new Color(16, 16, 16, 75).getRGB();
    private static HitBubbles instance;

    private final SeparatorSetting shapeSeparator = this.register(new SeparatorSetting("Форма и физика"));
    private final ModeSetting form = this.register(new ModeSetting(
        "Форма",
        "Визуальный тип эффекта в точке удара.",
        FORM_BUBBLE,
        FORM_BUBBLE,
        FORM_LENS,
        FORM_PULSE
    ));
    private final NumberSetting size = this.register(new NumberSetting(
        "Размер",
        "Радиус эффекта как доля экрана.",
        0.35,
        0.15,
        0.70,
        0.05
    ));
    private final NumberSetting duration = this.register(new NumberSetting(
        "Длительность",
        "Время жизни эффекта в миллисекундах.",
        700.0,
        200.0,
        1800.0,
        50.0
    ));
    private final NumberSetting expansion = this.register(new NumberSetting(
        "Расширение",
        "Скорость раскрытия волны от эпицентра.",
        1.0,
        0.4,
        2.5,
        0.1
    ));
    private final NumberSetting fade = this.register(new NumberSetting(
        "Затухание",
        "Плавность растворения эффекта со временем.",
        1.0,
        0.4,
        2.5,
        0.1
    ));
    private final NumberSetting strength = this.register(new NumberSetting(
        "Сила",
        "Степень преломления пространства.",
        1.0,
        0.2,
        3.0,
        0.1
    ));

    private final SeparatorSetting warpSeparator = this.register(new SeparatorSetting("Искривление"));
    private final BooleanSetting warp = this.register(new BooleanSetting(
        "Турбулентность",
        "Добавляет волнистое искажение внутри области эффекта.",
        false
    ));
    private final NumberSetting warpStrength = this.register(new NumberSetting(
        "Сила турбулентности",
        "Сила внутреннего волнистого искривления.",
        0.5,
        0.1,
        2.0,
        0.1
    ).visibleWhen(this.warp::getValue));
    private final NumberSetting saturation = this.register(new NumberSetting(
        "Насыщенность",
        "Насыщенность цвета внутри области эффекта.",
        0.0,
        -1.0,
        1.0,
        0.05
    ));

    private final SeparatorSetting tintSeparator = this.register(new SeparatorSetting("Подкрашивание"));
    private final BooleanSetting tint = this.register(new BooleanSetting(
        "Подкрашивать цветом",
        "Подкрашивать область волны в точке удара.",
        false
    ));
    private final NumberSetting tintStrength = this.register(new NumberSetting(
        "Сила цвета",
        "Интенсивность окрашивания волны.",
        55.0,
        0.0,
        100.0,
        1.0
    ).visibleWhen(this.tint::getValue));
    private final ModeSetting colorMode = this.register(new ModeSetting(
        "Режим цвета",
        "Источник оттенка подкраски.",
        COLOR_CLIENT,
        COLOR_CLIENT,
        COLOR_CUSTOM
    ));
    private final BooleanSetting useSecondColor = this.register(new BooleanSetting(
        "Второй цвет",
        "Использовать второй цвет для градиентного перелива.",
        false
    ));
    private final ColorSetting customColor = this.register(new ColorSetting(
        "Свой цвет",
        "Основной цвет подкраски.",
        new Color(255, 255, 255, 255)
    ));
    private final ColorSetting customSecondColor = this.register(new ColorSetting(
        "Цвет 2",
        "Второй цвет подкраски.",
        new Color(ColorUtil.lerpColor(-1, DARK_SECOND_COLOR, 0.7f), true)
    ));

    private final List<Ripple> ripples = new ArrayList<>();

    public HitBubbles() {
        super("Hit Bubbles", "Искажает пространство волной в месте удара", Category.VISUALS);
        instance = this;
        this.colorMode.visibleWhen(this::colorsApply);
        this.useSecondColor.visibleWhen(() -> this.colorsApply() && this.colorMode.is(COLOR_CUSTOM));
        this.customColor.visibleWhen(() -> this.colorsApply() && this.colorMode.is(COLOR_CUSTOM));
        this.customSecondColor.visibleWhen(() -> this.colorsApply() && this.colorMode.is(COLOR_CUSTOM) && this.useSecondColor.getValue());
    }

    public static HitBubbles getInstance() {
        HitBubbles hitBubbles = ModuleManager.get().get(HitBubbles.class);
        return hitBubbles != null ? hitBubbles : instance;
    }

    private static void putColor(float[] fArray, int n, int n2) {
        fArray[n] = (float)(n2 >> 16 & 0xFF) / 255.0f;
        fArray[n + 1] = (float)(n2 >> 8 & 0xFF) / 255.0f;
        fArray[n + 2] = (float)(n2 & 0xFF) / 255.0f;
    }

    private static int fade(int n, int n2, int n3, int n4) {
        int n5 = (int)((System.currentTimeMillis() / (long)Math.max(1, n) + (long)n2) % 360L);
        n5 = n5 >= 180 ? 360 - n5 : n5;
        return ColorUtil.lerpColor(n3, n4, (float)n5 / 180.0f);
    }

    @Override
    protected void onDisable() {
        this.ripples.clear();
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        Entity entity = event.getTarget();
        if (entity == null) {
            return;
        }
        Vec3d pos = entity.getEntityPos().add(0.0, (double)entity.getHeight() * 0.5, 0.0);
        this.ripples.add(new Ripple(pos, System.currentTimeMillis()));
        while (this.ripples.size() > 16) {
            this.ripples.remove(0);
        }
    }

    private int getColor(int offset, float alpha) {
        int c1;
        int c2;
        if (this.colorMode.is(COLOR_CLIENT)) {
            int[] palette = ClientPalette.colors();
            if (palette != null && palette.length >= 2) {
                return ColorUtil.multAlpha(paletteFade(8, offset, palette), alpha);
            }
            InterfaceModule ui = InterfaceModule.getInstance();
            c1 = ui != null ? ui.clientPrimaryColorOpaque() : -1;
            c2 = ui != null && ui.usesSecondClientColor() ? ui.clientSecondaryColorOpaque() : c1;
        } else {
            c1 = this.customColor.getColor();
            c2 = this.useSecondColor.getValue() ? this.customSecondColor.getColor() : this.customColor.getColor();
        }
        if (c1 == c2) {
            return ColorUtil.multAlpha(c1, alpha);
        }
        return ColorUtil.multAlpha(fade(8, offset, c1, c2), alpha);
    }

    public void onAfterWorld(Framebuffer framebuffer, Matrix4f viewMatrix, Matrix4f projMatrix, Camera camera) {
        if (framebuffer == null || camera == null || this.ripples.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        float totalDuration = Math.max(1.0f, this.duration.getFloat());
        float maxRadius = this.size.getFloat();
        float spreadWidth = MathHelper.clamp(maxRadius * 0.4f, 0.06f, 0.22f);
        float baseAmp = 0.018f * this.strength.getFloat();
        float aspect = (float)framebuffer.textureWidth / (float)Math.max(1, framebuffer.textureHeight);
        Vec3d camPos = camera.getCameraPos();
        float[] params = new float[152];
        int activeCount = 0;

        float expRate = this.expansion.getFloat();
        float fadePow = this.fade.getFloat();

        Iterator<Ripple> iterator = this.ripples.iterator();
        while (iterator.hasNext()) {
            Ripple ripple = iterator.next();
            float progress = (float)(now - ripple.spawnMs) / totalDuration;
            if (progress >= 1.0f) {
                iterator.remove();
                continue;
            }
            if (activeCount >= 16) continue;

            Vector4f vec = new Vector4f((float)(ripple.pos.x - camPos.x), (float)(ripple.pos.y - camPos.y), (float)(ripple.pos.z - camPos.z), 1.0f);
            viewMatrix.transform(vec);
            projMatrix.transform(vec);
            if (vec.w <= 1.0E-4f) continue;

            float screenX = vec.x / vec.w * 0.5f + 0.5f;
            float screenY = vec.y / vec.w * 0.5f + 0.5f;
            if (screenX < -0.5f || screenX > 1.5f || screenY < -0.5f || screenY > 1.5f) continue;

            float fadeIn = MathHelper.clamp(progress / 0.08f, 0.0f, 1.0f);
            float fadeOut = MathHelper.clamp((1.0f - progress) * (1.0f / Math.max(0.1f, fadePow)), 0.0f, 1.0f);
            float envelope = fadeIn * fadeOut;
            float currAmp = baseAmp * envelope;

            float currentRadius = MathHelper.clamp(progress * expRate, 0.0f, 1.0f) * maxRadius;

            int offset = 24 + activeCount * 8;
            params[offset] = screenX;
            params[offset + 1] = screenY;
            params[offset + 2] = currentRadius;
            params[offset + 3] = spreadWidth;
            params[offset + 4] = currAmp;
            params[offset + 5] = envelope;
            params[offset + 6] = maxRadius;
            ++activeCount;
        }

        if (activeCount == 0) {
            return;
        }

        float formId = 0.0f;
        if (this.form.is(FORM_LENS)) {
            formId = 1.0f;
        } else if (this.form.is(FORM_PULSE)) {
            formId = 2.0f;
        }

        params[0] = activeCount;
        params[1] = aspect;
        params[2] = (float)(now % 100000L) / 1000.0f;
        params[3] = this.warp.getValue() ? 0.01f * this.warpStrength.getFloat() : 0.0f;
        params[4] = MathHelper.clamp(1.0f + this.saturation.getFloat(), 0.0f, 2.0f);

        params[6] = formId;
        params[7] = fadePow;

        if (this.tint.getValue()) {
            params[5] = MathHelper.clamp(this.tintStrength.getFloat() / 100.0f, 0.0f, 1.0f);
            putColor(params, 8, this.getColor(0, 1.0f));
            putColor(params, 12, this.getColor(90, 1.0f));
            putColor(params, 16, this.getColor(180, 1.0f));
            putColor(params, 20, this.getColor(270, 1.0f));
        }

        HitBubblesRenderer.apply(framebuffer, params);
    }

    private boolean colorsApply() {
        return this.tint.getValue();
    }

    private static int paletteFade(int n, int n2, int[] nArray) {
        int n3 = nArray.length;
        int n4 = (int)((System.currentTimeMillis() / (long)Math.max(1, n) + (long)n2) % 360L);
        float f = (float)n4 / 360.0f * (float)n3;
        int n5 = (int)f % n3;
        int n6 = (n5 + 1) % n3;
        int n7 = nArray[n5] | 0xFF000000;
        int n8 = nArray[n6] | 0xFF000000;
        return ColorUtil.lerpColor(n7, n8, f - (float)Math.floor(f)) | 0xFF000000;
    }

    public static record Ripple(Vec3d pos, long spawnMs) {}
}
