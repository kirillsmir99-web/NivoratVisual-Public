package rtx.nv.api.modules.impl.Interface;

import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;

public final class HPFocus extends InterfaceComponentModule {
    private static HPFocus instance;

    public final ModeSetting heartStyle = this.register(new ModeSetting(
        "Стиль сердец",
        "Стиль отрисовки сердец в HP Focus.",
        "Ванильные 1:1",
        "Ванильные 1:1", "Векторные Client"
    ));

    public final ModeSetting displayMode = this.register(new ModeSetting(
        "Режим",
        "Условие появления сердец на экране.",
        "При низком HP",
        "При низком HP", "Всегда"
    ));

    public final NumberSetting hpThreshold = this.register(new NumberSetting(
        "Порог HP",
        "Уровень здоровья, при котором отображаются сердца.",
        10.0, 1.0, 20.0, 0.5
    )).visibleWhen(() -> this.displayMode.is("При низком HP"));

    public final NumberSetting scale = this.register(new NumberSetting(
        "Масштаб",
        "Размер отображаемых сердец.",
        1.0, 0.5, 2.0, 0.05
    ));

    public final BooleanSetting showAbsorption = this.register(new BooleanSetting(
        "Поглощение",
        "Отображать золотые сердца поглощения.",
        true
    ));

    public final BooleanSetting damageShake = this.register(new BooleanSetting(
        "Тряска от урона",
        "Динамическая встряска сердец при получении урона.",
        true
    ));

    public HPFocus() {
        super("HPFocus", "Дублирование сердец над худбаром с настройкой порога появления.");
        instance = this;
    }

    public static HPFocus getInstance() {
        return instance;
    }

    public float scale() {
        return this.scale.getFloat();
    }

    public boolean isAlwaysVisible() {
        return this.displayMode.is("Всегда");
    }

    public float hpThresholdHp() {
        return this.hpThreshold.getFloat();
    }
}
