package rtx.nv.api.modules.impl.Interface;

import rtx.nv.api.modules.settings.impl.BooleanSetting;

public final class ScoreboardModule extends InterfaceComponentModule {
    public final BooleanSetting hide = this.register(new BooleanSetting(
        "Скрыть",
        "Полностью скрыть отображение таблицы счёта.",
        false
    ));

    public final BooleanSetting customPosition = this.register(new BooleanSetting(
        "Своя позиция",
        "Разрешить свободное перемещение по экрану.",
        true
    ));

    public ScoreboardModule() {
        super("Scoreboard", "Управление положением и видимостью таблицы счёта серверов: зажмите ЛКМ по таблице при открытом чате или меню для перемещения.");
        this.setEnabled(true);
    }
}
