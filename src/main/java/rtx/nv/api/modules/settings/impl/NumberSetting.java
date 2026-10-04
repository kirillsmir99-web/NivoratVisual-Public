package rtx.nv.api.modules.settings.impl;
import java.util.function.Supplier;
import rtx.nv.api.modules.settings.impl.SliderSetting;

public class NumberSetting
extends SliderSetting {
    public NumberSetting(String string, String string2, double d, double d2, double d3, double d4) {
        super(string, string2);
        this.range((float)d2, (float)d3);
        this.increment((float)d4);
        this.setValue((float)d);
    }

    public NumberSetting visibleWhen(Supplier<Boolean> supplier) {
        this.visible(supplier);
        return this;
    }
}

