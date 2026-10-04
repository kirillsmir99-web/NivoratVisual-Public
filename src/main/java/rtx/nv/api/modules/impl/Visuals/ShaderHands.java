package rtx.nv.api.modules.impl.Visuals;

import java.awt.Color;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.post.handsflame.HandsFlameRenderer;
import rtx.nv.utils.render.post.shaderhands.ShaderHandsRenderer;

public final class ShaderHands extends Module {
    private static final int DEFAULT_COLOR = -10785543;
    private static final String COLOR_CLIENT = "Клиент";
    private static final String COLOR_CUSTOM = "Свой";

    private static ShaderHands instance;

    private final SeparatorSetting glassSeparator = this.register(new SeparatorSetting("Стекло"));
    private final BooleanSetting glass = this.register(new BooleanSetting("Стекло", "Стеклянные руки с эффектом преломления мира.", true));
    private final SliderSetting glassSaturation = this.register(new SliderSetting("Сатурация стекла", "Насыщенность отражения мира в руках.").range(0.0f, 3.0f).increment(0.05f).setValue(1.45f).visible(this::isGlassOn));
    private final SliderSetting glassWhite = this.register(new SliderSetting("Яркость стекла", "Подъем яркости отражения.").range(0.0f, 1.0f).increment(0.02f).setValue(0.78f).visible(this::isGlassOn));
    private final SliderSetting glassDistort = this.register(new SliderSetting("Искажение стекла", "Сила искажения отражения.").range(0.0f, 0.05f).increment(0.001f).setValue(0.012f).visible(this::isGlassOn));
    private final SliderSetting glassTint = this.register(new SliderSetting("Подкрас стекла", "Подкраска отражения цветом клиента.").range(0.0f, 1.0f).increment(0.02f).setValue(0.22f).visible(this::isGlassOn));

    private final SeparatorSetting haloSeparator = this.register(new SeparatorSetting("Ореол"));
    private final BooleanSetting halo = this.register(new BooleanSetting("Ореол", "Включает эффект мягкого шейдерного свечения вокруг рук.", true));
    private final SliderSetting intensity = this.register(new SliderSetting("Интенсивность", "Сила свечения вокруг модели рук.").range(0.1f, 3.0f).increment(0.05f).setValue(1.0f).visible(this::isHaloOn));
    private final SliderSetting radius = this.register(new SliderSetting("Радиус", "Дальность распространения ореола.").range(0.2f, 3.0f).increment(0.05f).setValue(1.0f).visible(this::isHaloOn));
    private final SliderSetting softness = this.register(new SliderSetting("Мягкость", "Мягкость размытия и рассеивания ореола.").range(0.1f, 2.0f).increment(0.05f).setValue(0.85f).visible(this::isHaloOn));
    private final SliderSetting opacity = this.register(new SliderSetting("Прозрачность", "Общая видимость эффекта ореола.").range(0.1f, 1.0f).increment(0.05f).setValue(0.9f).visible(this::isHaloOn));
    private final BooleanSetting onlyItems = this.register(new BooleanSetting("Только с предметом", "Отображать ореол только при наличии предмета в руке.", false).visible(this::isHaloOn));

    private final SeparatorSetting colorSeparator = this.register(new SeparatorSetting("Цвет ореола").visible(this::isHaloOn));
    private final ModeSetting colorMode = this.register((ModeSetting) new ModeSetting("Режим цвета", "Цветовая схема ореола.", COLOR_CLIENT, COLOR_CLIENT, COLOR_CUSTOM).visible(this::isHaloOn));
    private final ColorSetting customColor = this.register(new ColorSetting("Цвет", "Пользовательский цвет ореола.", new Color(255, 255, 255, 255)).visible(() -> this.isHaloOn() && this.colorMode.is(COLOR_CUSTOM)));

    public ShaderHands() {
        super("ShaderHands", "Шейдерные руки: стеклянное преломление и мягкий ореол вокруг рук.", Category.VISUALS);
        instance = this;
    }

    public static ShaderHands getInstance() {
        ShaderHands shaderHands = ModuleManager.get().get(ShaderHands.class);
        return shaderHands != null ? shaderHands : instance;
    }

    public static boolean isActive() {
        ShaderHands shaderHands = ShaderHands.getInstance();
        return shaderHands != null && shaderHands.isEnabled();
    }

    public boolean isHaloOn() {
        return this.halo.getValue();
    }

    public boolean isGlassOn() {
        return this.glass.getValue();
    }

    @Override
    protected void onDisable() {
        HandsFlameRenderer.setFlameEnabled(false);
        HandsFlameRenderer.shutdown();
        ShaderHandsRenderer.clear();
    }

    @Override
    protected void onEnable() {
        this.syncFlameRenderer();
    }

    @EventHandler
    private void onTick(TickEvent tickEvent) {
        if (tickEvent.isPre()) {
            this.syncFlameRenderer();
        }
    }

    public static void composite() {
        ShaderHands shaderHands = ShaderHands.getInstance();
        if (shaderHands == null || !shaderHands.isEnabled()) {
            return;
        }
        boolean glassOn = shaderHands.isGlassOn();
        if (!glassOn) {
            return;
        }
        int baseCol = ShaderHands.baseColor();
        int[] dummyGrad = new int[]{baseCol, baseCol, baseCol, baseCol};
        ShaderHandsRenderer.composite(
            baseCol,
            dummyGrad,
            true,
            false,
            0,
            6.0f,
            0.3f,
            1.8f,
            false,
            shaderHands.glassSaturation.getFloat(),
            shaderHands.glassWhite.getFloat(),
            shaderHands.glassDistort.getFloat(),
            shaderHands.glassTint.getFloat()
        );
    }

    private static int baseColor() {
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        return interfaceModule != null ? interfaceModule.clientPrimaryColorOpaque() : DEFAULT_COLOR;
    }

    private void syncFlameRenderer() {
        if (!this.halo.getValue()) {
            HandsFlameRenderer.setFlameEnabled(false);
            return;
        }
        HandsFlameRenderer.setFlameEnabled(true);
        HandsFlameRenderer.configure(
            this.intensity.getFloat(),
            0.0f,
            this.softness.getFloat(),
            this.radius.getFloat(),
            this.opacity.getFloat(),
            this.colorMode.is(COLOR_CUSTOM) ? 1 : 0,
            this.resolveHaloColor(),
            this.onlyItems.getValue(),
            this.colorMode.is(COLOR_CLIENT)
        );
    }

    private int resolveHaloColor() {
        if (this.colorMode.is(COLOR_CUSTOM)) {
            return ColorUtil.withAlpha(this.customColor.getColorOpaque(), 255);
        }
        return ShaderHands.baseColor();
    }

    public static boolean isOldModeActive() {
        ShaderHands shaderHands = ShaderHands.getInstance();
        return shaderHands != null && shaderHands.isEnabled() && shaderHands.isGlassOn();
    }

    public static boolean isNewModeActive() {
        ShaderHands shaderHands = ShaderHands.getInstance();
        return shaderHands != null && shaderHands.isEnabled() && shaderHands.isHaloOn();
    }
}
