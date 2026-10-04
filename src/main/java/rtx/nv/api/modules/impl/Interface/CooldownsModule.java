package rtx.nv.api.modules.impl.Interface;
import rtx.nv.api.modules.impl.Interface.InterfaceComponentModule;

import rtx.nv.api.modules.settings.impl.BooleanSetting;

public final class CooldownsModule extends InterfaceComponentModule {
    public final BooleanSetting showCombatTag = this.register(new BooleanSetting(
        "Режим боя (КТ)",
        "Отображать кулдаун режима боя в списке перезарядок.",
        true
    ));

    public CooldownsModule() {
        super("Cooldowns", "Перемещаемый список перезарядки предметов.");
    }
}

