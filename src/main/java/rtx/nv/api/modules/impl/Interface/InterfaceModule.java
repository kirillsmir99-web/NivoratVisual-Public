package rtx.nv.api.modules.impl.Interface;

import java.awt.Color;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ButtonSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.api.ui.theme.ThemeManager;

public final class InterfaceModule extends Module {
    public static final String CLIENT_COLOR_PALETTE = "Палитра";
    public static final String CLIENT_COLOR_ACCENT = "Акцент";

    // Legacy aliases
    public static final String CLIENT_COLOR_THEMES = CLIENT_COLOR_PALETTE;
    public static final String CLIENT_COLOR_CUSTOM = CLIENT_COLOR_ACCENT;

    public static final String GRADIENT_LINE = "Линия";
    public static final String GRADIENT_FLOW = "Поток";
    public static final String GRADIENT_CORNERS = "Углы";

    // Legacy aliases
    public static final String GRADIENT_HORIZONTAL = GRADIENT_LINE;
    public static final String GRADIENT_BLOBS = GRADIENT_FLOW;
    public static final String GRADIENT_SQUARE = GRADIENT_CORNERS;

    public static final String STYLE_GLASS = "Стекло";
    public static final String STYLE_MOSAIC = "Мозаика";
    // Keep source aliases and migrate persisted values through SelectSetting.
    public static final String STYLE_MONOLITH = STYLE_GLASS;
    public static final String STYLE_SHARDS = STYLE_MOSAIC;

    public static final String DRAG_FREE = "Свободный";
    public static final String DRAG_GHOST = "Призрак";

    private static final float THEME_COLOR_ALPHA = 204.0f;
    private static InterfaceModule instance;

    // Раздел 1: Оформление
    private final SeparatorSetting stylingSection = this.register(new SeparatorSetting("Оформление"));
    public final ModeSetting rectStyle = this.register(new ModeSetting("Стиль", "Стиль оформления материала интерфейса.", rtx.nv.ClientEdition.isTrial() ? STYLE_SHARDS : STYLE_MONOLITH, STYLE_MONOLITH, STYLE_SHARDS).visibleWhen(() -> !rtx.nv.ClientEdition.isTrial()));
    public final NumberSetting mosaicScale = this.register(new NumberSetting("Размер осколков", "Размер крупных плавающих элементов под поверхностью панели.", 1.0, 0.4, 2.5, 0.05).visibleWhen(this::isMosaicStyle));
    public final NumberSetting mosaicMorph = this.register(new NumberSetting("Деформация", "Амплитуда медленного изменения формы осколков.", 0.4, 0.0, 1.0, 0.05).visibleWhen(this::isMosaicStyle));
    public final NumberSetting mosaicSpeed = this.register(new NumberSetting("Скорость", "Скорость плавного движения осколков.", 0.7, 0.1, 3.0, 0.05).visibleWhen(this::isMosaicStyle));
    public final NumberSetting mosaicSeam = this.register(new NumberSetting("Толщина швов", "Визуальная толщина стыков между осколками.", 0.04, 0.01, 0.15, 0.005).visibleWhen(this::isMosaicStyle));
    public final NumberSetting mosaicBevel = this.register(new NumberSetting("Фаска", "Тонкий стеклянный блик вдоль скоса граней осколков.", 0.35, 0.0, 1.0, 0.05).visibleWhen(this::isMosaicStyle));
    public final NumberSetting mosaicCellGlow = this.register(new NumberSetting("Свечение швов", "Мягкое свечение вдоль стыков осколков.", 0.20, 0.0, 1.0, 0.05).visibleWhen(this::isMosaicStyle));
    public final NumberSetting textReadability = this.register(new NumberSetting("Читаемость текста", "Увеличение контраста текста и спокойствия фона панелей.", 0.85, 0.0, 1.0, 0.05).visibleWhen(this::isMosaicStyle));
    public final NumberSetting rectCornerRadius = this.register(new NumberSetting("Скругление углов", "Радиус скругления углов общих панелей интерфейса.", 7.0, 2.0, 10.0, 1.0));
    public final NumberSetting rectBackdropBlur = this.register(new NumberSetting("Размытие фона", "Радиус размытия фона за панелями интерфейса.", 18.0, 0.0, 64.0, 1.0));
    public final NumberSetting rectRefractionStrength = this.register(new NumberSetting("Преломление", "Насколько сильно материал искажает размытый фон.", 0.3, 0.0, 0.8, 0.01));
    public final BooleanSetting mosaicBackgroundOnly = this.register(new BooleanSetting("Только на фоне", "Отображать мозаику только на главном фоне интерфейса, отключая её в иконках, настройках, функциях и инвентаре.", true).visibleWhen(this::isMosaicStyle));
    public final ModeSetting gradientStyle = this.register(new ModeSetting("Градиент", "Форма распределения цветов темы: Линия (горизонтальный), Поток (волновой перелив) или Углы (радиальный по углам панели).", GRADIENT_FLOW, GRADIENT_LINE, GRADIENT_FLOW, GRADIENT_CORNERS));
    public final ModeSetting clientColorMode = this.register(new ModeSetting("Цвет клиента", "Источник акцентного цвета клиента: палитра активной темы или акцентные цвета.", CLIENT_COLOR_PALETTE, CLIENT_COLOR_PALETTE, CLIENT_COLOR_ACCENT));
    public final ColorSetting rectColor = this.register(new ColorSetting("Цвет", "Основной оттенок материала и цвет грани.", new Color(-857872385, true)).visibleWhen(this::isCustomClientColor));
    public final BooleanSetting rectUseSecondColor = this.register(new BooleanSetting("Второй цвет", "Включает второй цвет для углового градиента.", false).visibleWhen(this::isCustomClientColor));
    public final ColorSetting rectSecondColor = this.register(new ColorSetting("Цвет 2", "Второй цвет градиента материала.", new Color(-855690602, true)).visibleWhen(() -> this.isCustomClientColor() && this.rectUseSecondColor.getValue()));
    public final BooleanSetting rectColorMovement = this.register(new BooleanSetting("Движение цвета", "Анимирует двухцветный градиент вокруг углов панели.", false).visibleWhen(this::usesSecondClientColor));
    public final NumberSetting rectEdgeStrength = this.register(new NumberSetting("Сила грани", "Насколько сильно цвет грани подмешивается в материал.", 0.18, 0.0, 1.0, 0.01));
    public final NumberSetting rectEdgeSharpness = this.register(new NumberSetting("Резкость грани", "Чем выше значение, тем тоньше и резче блик.", 55.0, 2.0, 100.0, 1.0).visibleWhen(() -> this.rectEdgeStrength.getFloat() > 0.0f));
    public final BooleanSetting rectGlow = this.register(new BooleanSetting("Свечение", "Рисует мягкое свечение вокруг панелей интерфейса.", false));
    public final NumberSetting rectGlowIntensity = this.register(new NumberSetting("Яркость свечения", "Яркость свечения вокруг панелей.", 0.6, 0.0, 2.0, 0.05).visibleWhen(this.rectGlow::getValue));
    public final NumberSetting rectGlowRadius = this.register(new NumberSetting("Радиус свечения", "Насколько далеко расходится свечение.", 15.0, 15.0, 70.0, 1.0).visibleWhen(this.rectGlow::getValue));

    // Раздел 2: Элементы
    private final SeparatorSetting elementsSection = this.register(new SeparatorSetting("Элементы"));
    public final BooleanSetting hudIcons = this.register(new BooleanSetting("Иконки", "Показывает иконку справа в заголовке элементов HUD, а название сдвигает влево.", true));

    // Раздел 3: Анимации
    private final SeparatorSetting animationsSection = this.register(new SeparatorSetting("Анимации"));
    public final BooleanSetting waveEdgeEnabled = this.register(new BooleanSetting("Живая кромка", "Мягкая органичная волнистая деформация по внешнему контуру стеклянных поверхностей.", true));
    public final NumberSetting waveEdgeIntensity = this.register(new NumberSetting("Интенсивность", "Изменяет выраженность эффекта.", 0.70, 0.0, 1.0, 0.05).visibleWhen(this.waveEdgeEnabled::getValue));
    public final NumberSetting waveEdgeSize = this.register(new NumberSetting("Размер волны", "Изменяет высоту и размер выпуклостей.", 0.50, 0.0, 1.0, 0.05).visibleWhen(this.waveEdgeEnabled::getValue));
    public final NumberSetting waveEdgeDensity = this.register(new NumberSetting("Плотность", "Изменяет количество волн по краю.", 0.50, 0.0, 1.0, 0.05).visibleWhen(this.waveEdgeEnabled::getValue));
    public final BooleanSetting waveEdgeMotion = this.register(new BooleanSetting("Движение", "Плавное перемещение волн вдоль контура.", true).visibleWhen(this.waveEdgeEnabled::getValue));
    public final NumberSetting waveEdgeSpeed = this.register(new NumberSetting("Скорость", "Изменяет скорость движения текстуры.", 0.50, 0.0, 1.0, 0.05).visibleWhen(() -> this.waveEdgeEnabled.getValue() && this.waveEdgeMotion.getValue()));
    public final NumberSetting waveEdgeGlow = this.register(new NumberSetting("Свечение", "Изменяет яркость света на вершинах волн.", 0.35, 0.0, 1.0, 0.05).visibleWhen(this.waveEdgeEnabled::getValue));
    public final BooleanSetting waveEdgeInterface = this.register(new BooleanSetting("Интерфейс", "Применение живой кромки к главным окнам и панелям интерфейса.", true).visibleWhen(this.waveEdgeEnabled::getValue));
    public final BooleanSetting waveEdgeHud = this.register(new BooleanSetting("HUD", "Применение живой кромки к виджетам HUD на экране.", true).visibleWhen(this.waveEdgeEnabled::getValue));
    public final BooleanSetting activeModuleWaveEdge = this.register(new BooleanSetting("Анимация активных модулей", "Использовать мягкую живую кромку как основной индикатор активного состояния модулей.", true));
    public final BooleanSetting dragTilt = this.register(new BooleanSetting("Наклон при перетаскивании", "Плавно наклоняет элемент в сторону движения при перетаскивании.", true));

    // Раздел 4: Управление
    private final SeparatorSetting controlSection = this.register(new SeparatorSetting("Управление"));
    public final ModeSetting dragStyle = this.register(new ModeSetting("Перетаскивание", "Свободный режим перемещения элементов HUD.", DRAG_FREE, DRAG_FREE));
    public final ButtonSetting resetHud = this.register(new ButtonSetting("Сброс расположения HUD", "Возвращает все виджеты на исходные позиции экрана.")
        .label("Сбросить")
        .onClick(() -> rtx.nv.api.drags.DragSystem.get().resetAllPositions()));

    // Совместимость
    public final BooleanSetting liveEdgeEnabled = waveEdgeEnabled;
    public final NumberSetting liveEdgeIntensity = waveEdgeIntensity;
    public final NumberSetting liveEdgeSize = waveEdgeSize;
    public final NumberSetting liveEdgeDensity = waveEdgeDensity;
    public final BooleanSetting liveEdgeMotion = waveEdgeMotion;
    public final NumberSetting liveEdgeSpeed = waveEdgeSpeed;
    public final NumberSetting liveEdgeGlow = waveEdgeGlow;
    public final BooleanSetting liveEdgeInterface = waveEdgeInterface;
    public final BooleanSetting liveEdgeHud = waveEdgeHud;
    public final BooleanSetting moduleLiveEdge = activeModuleWaveEdge;
    public final BooleanSetting dragJitter = new BooleanSetting("Тряска при перетаскивании", "", false).visibleWhen(() -> false);
    public final BooleanSetting dragWaves = new BooleanSetting("Волны при перетаскивании", "", false).visibleWhen(() -> false);

    public InterfaceModule() {
        super("Interface", "Общий визуальный стиль для всех элементов интерфейса.", Category.DISPLAY);
        instance = this;
    }

    public static InterfaceModule getInstance() {
        return instance;
    }

    private static int lerpWhite(int n, float f) {
        float f2 = Math.max(0.0f, Math.min(1.0f, f));
        int n2 = Math.round(255.0f + (float)((n >> 16 & 0xFF) - 255) * f2);
        int n3 = Math.round(255.0f + (float)((n >> 8 & 0xFF) - 255) * f2);
        int n4 = Math.round(255.0f + (float)((n & 0xFF) - 255) * f2);
        return n2 << 16 | n3 << 8 | n4;
    }

    public int clientPrimaryColorOpaque() {
        return 0xFF000000 | this.clientPrimaryColor() & 0xFFFFFF;
    }

    public boolean usesSecondClientColor() {
        return this.isThemeClientColor() || this.rectUseSecondColor.getValue();
    }

    public int clientSecondaryColorOpaque() {
        return 0xFF000000 | this.clientSecondaryColor() & 0xFFFFFF;
    }

    public boolean clientColorMovement() {
        return this.usesSecondClientColor() && this.rectColorMovement.getValue();
    }

    public int clientPrimaryColor() {
        return ClientAccent.gradientA(204.0f);
    }

    public boolean isThemeClientColor() {
        return ThemeManager.isLiveOverrideActive() || this.clientColorMode.is(CLIENT_COLOR_PALETTE) || this.clientColorMode.is("Темы");
    }

    public int gradientStyleId() {
        if (this.gradientStyle.is(GRADIENT_FLOW) || this.gradientStyle.is("Жидкие пятна") || this.gradientStyle.is("Flow")) {
            return 1;
        }
        return (this.gradientStyle.is(GRADIENT_CORNERS) || this.gradientStyle.is("По квадрату") || this.gradientStyle.is("Corners")) ? 2 : 0;
    }

    public int[] clientPalette() {
        int[] nArray;
        if (this.isThemeClientColor() && (nArray = ThemeManager.blendedPalette()) != null && nArray.length > 0) {
            return nArray;
        }
        if (this.rectUseSecondColor.getValue()) {
            return new int[]{this.rectColor.getColor() & 0xFFFFFF, this.rectSecondColor.getColor() & 0xFFFFFF};
        }
        return new int[]{this.rectColor.getColor() & 0xFFFFFF};
    }


    public int clientSecondaryColor() {
        return ClientAccent.gradientB(204.0f);
    }

    public boolean isCustomClientColor() {
        return this.clientColorMode.is(CLIENT_COLOR_ACCENT) || this.clientColorMode.is("Свой");
    }

    public boolean isMosaicStyle() {
        if (rtx.nv.ClientEdition.isTrial()) {
            return true;
        }
        return this.rectStyle.is(STYLE_SHARDS) || this.rectStyle.is("Мозаика");
    }

    public boolean isMosaicOnBackgroundOnly() {
        return this.isMosaicStyle() && this.mosaicBackgroundOnly.getValue();
    }

    private float styleTransition = -1.0f;
    private long lastStyleUpdateNs = System.nanoTime();

    public float getStyleTransition() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (float)(now - this.lastStyleUpdateNs) / 1.0E9f);
        this.lastStyleUpdateNs = now;

        float target = this.isMosaicStyle() ? 1.0f : 0.0f;
        if (this.styleTransition < 0.0f) {
            this.styleTransition = target;
            return target;
        }

        // 300 ms smooth transition duration (Section 29)
        float speed = 1.0f - (float) Math.exp(-dt * 12.0f);
        this.styleTransition += (target - this.styleTransition) * speed;
        if (Math.abs(target - this.styleTransition) < 0.002f) {
            this.styleTransition = target;
        }
        return Math.max(0.0f, Math.min(1.0f, this.styleTransition));
    }

    public boolean isWaveEdgeActiveFor(boolean isHud) {
        if (!this.waveEdgeEnabled.getValue()) {
            return false;
        }
        return isHud ? this.waveEdgeHud.getValue() : this.waveEdgeInterface.getValue();
    }

    public boolean isLiveEdgeActiveFor(boolean isHud) {
        return this.isWaveEdgeActiveFor(isHud);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }
}
