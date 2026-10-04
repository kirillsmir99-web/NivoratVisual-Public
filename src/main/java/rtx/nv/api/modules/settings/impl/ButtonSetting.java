package rtx.nv.api.modules.settings.impl;
import java.util.function.Supplier;
import rtx.nv.api.modules.settings.Setting;

public class ButtonSetting
extends Setting {
    private String label = "";
    private float labelOffsetX;
    private Runnable action = () -> {};

    public ButtonSetting(String string, String string2) {
        super(string, string2);
    }

    public ButtonSetting label(String string) {
        this.label = string == null ? "" : string;
        return this;
    }

    public String getLabel() {
        return this.label;
    }

    public ButtonSetting visible(Supplier<Boolean> supplier) {
        this.setVisibilityCondition(supplier);
        return this;
    }

    public ButtonSetting onClick(Runnable runnable) {
        this.action = runnable == null ? () -> {} : runnable;
        return this;
    }

    public void click() {
        this.action.run();
    }

    public float getLabelOffsetX() {
        return this.labelOffsetX;
    }

    public ButtonSetting labelOffsetX(float f) {
        this.labelOffsetX = f;
        return this;
    }
}

