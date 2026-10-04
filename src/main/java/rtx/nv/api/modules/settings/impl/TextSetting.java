package rtx.nv.api.modules.settings.impl;
import java.util.function.Supplier;
import rtx.nv.api.modules.settings.Setting;

public class TextSetting
extends Setting {
    private String text = "";
    private String placeholder = "";
    private String defaultText = "";
    private boolean defaultCaptured;
    private int minLength = 0;
    private int maxLength = Integer.MAX_VALUE;

    public TextSetting(String string, String string2) {
        super(string, string2);
    }

    public String getValue() {
        return this.text;
    }

    public String getText() {
        return this.text;
    }

    public TextSetting visible(Supplier<Boolean> supplier) {
        this.setVisibilityCondition(supplier);
        return this;
    }

    public TextSetting setText(String string) {
        String string2 = string == null ? "" : string;
        boolean bl = !this.text.equals(string2);
        this.text = string2;
        if (!this.defaultCaptured) {
            this.defaultText = this.text;
            this.defaultCaptured = true;
        }
        if (bl) {
            this.notifyChanged();
        }
        return this;
    }

    public int getMaxLength() {
        return this.maxLength;
    }

    public int getMinLength() {
        return this.minLength;
    }

    public String getDefaultText() {
        return this.defaultText;
    }

    public TextSetting setPlaceholder(String string) {
        this.placeholder = string == null ? "" : string;
        return this;
    }

    public String getPlaceholder() {
        return this.placeholder;
    }

    public TextSetting lengthBounds(int n, int n2) {
        this.minLength = Math.max(0, n);
        this.maxLength = Math.max(this.minLength, n2);
        return this;
    }
}

