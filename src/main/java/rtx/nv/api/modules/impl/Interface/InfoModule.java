package rtx.nv.api.modules.impl.Interface;

import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;

public final class InfoModule extends InterfaceComponentModule {
    private static InfoModule instance;

    public final BooleanSetting background = this.register(new BooleanSetting(
        "Фон",
        "Отображать стеклянную подложку под текстом.",
        true
    ));

    public final BooleanSetting dynamicColors = this.register(new BooleanSetting(
        "Цветные индикаторы",
        "Подсвечивать значения TPS и пинга в зависимости от качества.",
        true
    ));

    public final BooleanSetting showBps = this.register(new BooleanSetting(
        "Скорость BPS",
        "Отображать скорость передвижения игрока.",
        true
    ));

    public final BooleanSetting showNether = this.register(new BooleanSetting(
        "Калькулятор ада",
        "Динамический пересчет координат между обычным миром и адом.",
        true
    ));

    public final NumberSetting pillHeight = this.register(new NumberSetting(
        "Высота плашки",
        "Высота плашек инфо (регулировка компактности).",
        13.0, 10.0, 18.0, 0.5
    ));

    public final NumberSetting scale = this.register(new NumberSetting(
        "Масштаб",
        "Общий масштаб плашек инфо.",
        1.0, 0.7, 1.5, 0.05
    ));

    public InfoModule() {
        super("Info", "Статичная сводка на экране (bps, координаты, пинг, tps).");
        instance = this;
    }

    public static InfoModule getInstance() {
        return instance;
    }
}
