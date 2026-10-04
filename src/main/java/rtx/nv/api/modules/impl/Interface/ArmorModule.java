package rtx.nv.api.modules.impl.Interface;

import rtx.nv.api.modules.impl.Interface.InterfaceComponentModule;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;

public final class ArmorModule extends InterfaceComponentModule {
    public static final String STYLE_SLOTS = "Ячейки";
    public static final String STYLE_RIBBON = "Лента";
    public static final String STYLE_TRANSPARENT = "Прозрачный";

    public static final String SOURCE_PLAYER = "Игрок";
    public static final String SOURCE_TARGET = "Цель";

    public final ModeSetting styleMode = this.register(new ModeSetting("Стиль", "Оформление отображения брони: Ячейки, Лента или Прозрачный фон.", STYLE_SLOTS, STYLE_SLOTS, STYLE_RIBBON, STYLE_TRANSPARENT));
    public final ModeSetting sourceMode = this.register(new ModeSetting("Источник", "Чью броню отображать: свою или выбранной цели.", SOURCE_PLAYER, SOURCE_PLAYER, SOURCE_TARGET));
    public final BooleanSetting showDurability = this.register(new BooleanSetting("Прочность", "Отображать процент или статус прочности предметов.", true));
    public final BooleanSetting durabilityBar = this.register(new BooleanSetting("Полоска прочности", "Цветная полоска оставшейся прочности под предметом.", true)).visibleWhen(this.showDurability::getValue);

    public ArmorModule() {
        super("Armor", "Отображение надетой брони и её прочности.");
    }

    public boolean isSlotsStyle() {
        return this.styleMode.is(STYLE_SLOTS);
    }

    public boolean isRibbonStyle() {
        return this.styleMode.is(STYLE_RIBBON);
    }

    public boolean isTransparentStyle() {
        return this.styleMode.is(STYLE_TRANSPARENT);
    }

    public boolean isTargetSource() {
        return this.sourceMode.is(SOURCE_TARGET);
    }

    public boolean showDurability() {
        return this.showDurability.getValue();
    }

    public boolean showDurabilityBar() {
        return this.showDurability.getValue() && this.durabilityBar.getValue();
    }
}
