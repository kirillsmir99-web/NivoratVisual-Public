package rtx.nv.api.modules.settings.impl;
import java.util.function.Supplier;
import rtx.nv.api.modules.settings.impl.TextSetting;

public class StringSetting
extends TextSetting {
    public StringSetting(String string, String string2, String string3, int n) {
        super(string, string2);
        this.setText(string3);
        this.lengthBounds(0, n);
    }

    public StringSetting visibleWhen(Supplier<Boolean> supplier) {
        this.visible(supplier);
        return this;
    }
}

