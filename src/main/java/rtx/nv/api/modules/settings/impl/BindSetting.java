package rtx.nv.api.modules.settings.impl;
import java.util.function.Supplier;
import rtx.nv.api.modules.settings.Setting;
import rtx.nv.utils.key.KeyBind;

public class BindSetting
extends Setting {
    private int key = -1;
    private int defaultKey = -1;
    private boolean defaultCaptured;
    private BindSetting.Type type = BindSetting.Type.TOGGLE;

    public BindSetting(String string, String string2) {
        super(string, string2);
    }

    public BindSetting(String string, String string2, KeyBind keyBind) {
        this(string, string2);
        if (keyBind != null) {
            this.setKey(keyBind.getCode());
        }
    }

    public KeyBind getValue() {
        return new KeyBind(this.key);
    }

    public int getKey() {
        return this.key;
    }

    public BindSetting.Type getType() {
        return this.type;
    }

    public BindSetting.Type type() {
        return this.type;
    }

    public boolean isBound() {
        return this.key != -1;
    }

    public BindSetting setKey(int n) {
        boolean bl = this.key != n;
        this.key = n;
        if (!this.defaultCaptured) {
            this.defaultKey = n;
            this.defaultCaptured = true;
        }
        if (bl) {
            this.notifyChanged();
        }
        return this;
    }

    public BindSetting visible(Supplier<Boolean> supplier) {
        this.setVisibilityCondition(supplier);
        return this;
    }

    public BindSetting setType(BindSetting.Type type) {
        BindSetting.Type targetType = type == null ? BindSetting.Type.TOGGLE : type;
        if (this.type != targetType) {
            this.type = targetType;
            this.notifyChanged();
        }
        return this;
    }

    public BindSetting visibleWhen(Supplier<Boolean> supplier) {
        return this.visible(supplier);
    }

    public int getDefaultKey() {
        return this.defaultKey;
    }


    public static enum Type {
        HOLD,
        TOGGLE;
    
    }
}

