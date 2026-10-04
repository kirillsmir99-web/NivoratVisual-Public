package rtx.nv.api.modules.impl.Visuals;

import java.awt.Color;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.post.fogblur.FogBlurRenderer;

public class FogBlur extends Module {
    public static final String COLOR_DEFAULT = "Стандартный";
    public static final String COLOR_CLIENT = "Клиент";
    public static final String COLOR_CUSTOM = "Свой";

    private static FogBlur instance;

    private final SeparatorSetting distanceSeparator = this.register(new SeparatorSetting("Дистанция и плотность"));
    private final SliderSetting fogStart = this.register(new SliderSetting(
        "Начало",
        "Дистанция в блоках до начала появления тумана."
    ).range(0.0f, 150.0f).increment(1.0f).setValue(10.0f));
    private final SliderSetting fogEnd = this.register(new SliderSetting(
        "Дальность",
        "Дистанция в блоках до максимальной плотности тумана."
    ).range(10.0f, 300.0f).increment(5.0f).setValue(64.0f));
    private final SliderSetting fogDensity = this.register(new SliderSetting(
        "Плотность",
        "Коэффициент плотности тумана."
    ).range(0.2f, 2.5f).increment(0.05f).setValue(1.0f));

    private final SeparatorSetting colorSeparator = this.register(new SeparatorSetting("Оформление"));
    private final ModeSetting colorMode = this.register(new ModeSetting(
        "Режим цвета",
        "Источник цветовой гаммы тумана.",
        COLOR_DEFAULT,
        COLOR_DEFAULT,
        COLOR_CLIENT,
        COLOR_CUSTOM
    ));
    private final ColorSetting customColor = this.register(new ColorSetting(
        "Свой цвет",
        "Пользовательский оттенок тумана.",
        new Color(190, 215, 255)
    ).visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM)));
    private final SliderSetting colorAlpha = this.register(new SliderSetting(
        "Сила цвета",
        "Интенсивность окрашивания тумана."
    ).range(0.1f, 1.0f).increment(0.05f).setValue(0.75f).visible(() -> !this.colorMode.is(COLOR_DEFAULT)));

    private final SeparatorSetting envSeparator = this.register(new SeparatorSetting("Среда и измерения"));
    private final BooleanSetting waterFog = this.register(new BooleanSetting(
        "Туман в воде",
        "Применять настройки тумана при нахождении под водой.",
        false
    ));
    private final BooleanSetting lavaFog = this.register(new BooleanSetting(
        "Туман в лаве",
        "Применять настройки тумана при погружении в лаву.",
        false
    ));

    private final SeparatorSetting blurSeparator = this.register(new SeparatorSetting("Пост-размытие"));
    private final BooleanSetting blurPass = this.register(new BooleanSetting(
        "Размытие глубины",
        "Эффект оптического размытия дальнего плана.",
        false
    ));
    private final SliderSetting blurStrength = this.register(new SliderSetting(
        "Сила размытия",
        "Радиус размытия дальних объектов."
    ).range(1.0f, 15.0f).increment(1.0f).setValue(5.0f).visible(this.blurPass::getValue));

    public FogBlur() {
        super("Fog Blur", "Настройка дистанции плотности и цвета игрового тумана", Category.VISUALS);
        instance = this;
    }

    public static FogBlur getInstance() {
        if (instance == null) {
            instance = ModuleManager.get().get(FogBlur.class);
        }
        return instance;
    }

    @Override
    protected void onDisable() {
        FogBlurRenderer.clear();
    }

    public float getEffectiveFogStart() {
        float start = this.fogStart.getFloat();
        float end = this.fogEnd.getFloat();
        if (start >= end - 2.0f) {
            start = Math.max(0.0f, end - 2.0f);
        }
        return start;
    }

    public float getEffectiveFogEnd() {
        float start = this.getEffectiveFogStart();
        float end = Math.max(start + 2.0f, this.fogEnd.getFloat());
        float density = Math.max(0.1f, this.fogDensity.getFloat());
        return start + (end - start) / density;
    }

    public boolean shouldApplyFog(Camera camera) {
        if (!this.isEnabled() || camera == null) {
            return false;
        }
        CameraSubmersionType submersion = camera.getSubmersionType();
        if (submersion == CameraSubmersionType.WATER && !this.waterFog.getValue()) {
            return false;
        }
        if (submersion == CameraSubmersionType.LAVA && !this.lavaFog.getValue()) {
            return false;
        }
        if (submersion == CameraSubmersionType.POWDER_SNOW) {
            return false;
        }
        return true;
    }

    public boolean hasCustomFogDistance() {
        return this.isEnabled();
    }

    public boolean hasCustomFogColor(Camera camera) {
        if (!this.shouldApplyFog(camera)) {
            return false;
        }
        return !this.colorMode.is(COLOR_DEFAULT);
    }

    public int getCustomFogColor() {
        int base;
        if (this.colorMode.is(COLOR_CLIENT)) {
            base = ClientAccent.accentOpaque();
        } else {
            base = this.customColor.getColor();
        }
        float a = this.colorAlpha.getFloat();
        return ColorUtil.multAlpha(base, a);
    }

    public void onAfterTranslucent(Framebuffer framebuffer) {
        if (!this.isEnabled() || !this.blurPass.getValue()) {
            return;
        }
        if (this.mc.player == null || this.mc.world == null || this.mc.gameRenderer == null) {
            return;
        }
        if (FogBlurRenderer.isDisabledAfterError()) {
            return;
        }
        int color = this.getCustomFogColor();
        float strength = this.colorAlpha.getFloat();
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        FogBlurRenderer.setBlurTint(r, g, b, strength * 0.4f);
        FogBlurRenderer.apply(
            framebuffer,
            this.blurStrength.getFloat(),
            Math.max(1.0f, this.getEffectiveFogStart()),
            this.getEffectiveFogEnd(),
            0.52f,
            2
        );
    }
}
