package rtx.nv.api.modules.settings.impl;
import java.util.function.Supplier;
import rtx.nv.api.modules.settings.impl.MultiSelectSetting;

public class MultiModeSetting
extends MultiSelectSetting {
    public MultiModeSetting(String string, String string2, String[] stringArray, String ... stringArray2) {
        super(string, string2);
        this.value(stringArray);
        this.selected(stringArray2);
    }

    public MultiModeSetting visibleWhen(Supplier<Boolean> supplier) {
        this.visible(supplier);
        return this;
    }

    @Override
    public boolean isSelected(String string) {
        return this.is(string);
    }
}

