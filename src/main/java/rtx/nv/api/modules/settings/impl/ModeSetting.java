package rtx.nv.api.modules.settings.impl;
import java.util.function.Supplier;
import rtx.nv.api.modules.settings.impl.SelectSetting;

public class ModeSetting
extends SelectSetting {
    public ModeSetting(String string, String string2, String string3, String ... stringArray) {
        super(string, string2);
        this.value(stringArray);
        this.selected(string3);
    }

    public ModeSetting visibleWhen(Supplier<Boolean> supplier) {
        this.visible(supplier);
        return this;
    }
}

