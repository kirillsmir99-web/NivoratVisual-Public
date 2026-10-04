package rtx.nv.api.modules.impl.Interface;

import rtx.nv.api.modules.settings.impl.NumberSetting;

public final class InventoryModule extends InterfaceComponentModule {
    private static InventoryModule instance;

    public final NumberSetting scale = this.register(new NumberSetting(
        "Масштаб",
        "Масштаб отображения инвентаря.",
        1.0, 0.5, 2.0, 0.05
    ));

    public InventoryModule() {
        super("Inventory", "Сетка предметов инвентаря с разделительными линиями.");
        instance = this;
    }

    public static InventoryModule getInstance() {
        return instance;
    }

    public float getScale() {
        return this.scale.getFloat();
    }
}
