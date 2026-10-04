package rtx.nv.api.modules.impl.Interface;

import rtx.nv.api.modules.impl.Interface.InterfaceComponentModule;
import rtx.nv.api.modules.settings.impl.BooleanSetting;

public final class PotionsModule extends InterfaceComponentModule {
    public final BooleanSetting sortMode = this.register(new BooleanSetting(
        "Сортировка по времени",
        "Сортировать эффекты по оставшемуся времени действия.",
        true
    ));

    public PotionsModule() {
        super("Potions", "Перемещаемый список активных эффектов зелий.");
    }

    public boolean isSorted() {
        return this.sortMode.getValue();
    }
}
