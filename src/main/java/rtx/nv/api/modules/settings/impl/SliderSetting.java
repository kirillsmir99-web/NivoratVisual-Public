package rtx.nv.api.modules.settings.impl;
import java.util.function.Supplier;
import rtx.nv.api.modules.settings.Setting;

public class SliderSetting
extends Setting {
    private float value;
    private float min;
    private float max;
    private float increment;
    private boolean integer;
    private boolean rangeConfigured;
    private float defaultValue;
    private boolean defaultCaptured;

    public SliderSetting(String string, String string2) {
        super(string, string2);
    }

    public int getInt() {
        return Math.round(this.value);
    }

    public float getFloat() {
        return this.value;
    }

    public float getValue() {
        return this.value;
    }

    private float clamp(float f) {
        if (!Float.isFinite(f)) return this.defaultCaptured ? this.defaultValue : (this.rangeConfigured ? Math.min(this.min, this.max) : 0.0f);
        float f2;
        float f3;
        float f4 = f;
        if (this.rangeConfigured) {
            f3 = Math.min(this.min, this.max);
            f2 = Math.max(this.min, this.max);
            f4 = Math.max(f3, Math.min(f2, f4));
        }
        if (this.increment > 0.0f) {
            f3 = this.rangeConfigured ? this.min : 0.0f;
            f4 = f3 + (float)Math.round((f4 - f3) / this.increment) * this.increment;
        }
        if (this.integer) {
            f4 = Math.round(f4);
        }
        if (this.rangeConfigured) {
            f3 = Math.min(this.min, this.max);
            f2 = Math.max(this.min, this.max);
            f4 = Math.max(f3, Math.min(f2, f4));
        }
        return f4;
    }

    public SliderSetting increment(float f) {
        this.increment = Math.max(0.0f, f);
        this.value = this.clamp(this.value);
        return this;
    }

    public SliderSetting increment(int n) {
        return this.increment((float)n);
    }

    public SliderSetting setValue(float f) {
        float f2 = this.clamp(f);
        boolean bl = Float.compare(this.value, f2) != 0;
        this.value = f2;
        if (!this.defaultCaptured) {
            this.defaultValue = this.value;
            this.defaultCaptured = true;
        }
        if (bl) {
            this.notifyChanged();
        }
        return this;
    }

    public float getDefaultValue() {
        return this.defaultValue;
    }

    public SliderSetting range(int n, int n2) {
        this.integer = true;
        this.min = n;
        this.max = n2;
        this.rangeConfigured = true;
        if (this.increment <= 0.0f) {
            this.increment = 1.0f;
        }
        this.value = this.clamp(this.value);
        return this;
    }

    public SliderSetting range(float f, float f2) {
        this.min = f;
        this.max = f2;
        this.rangeConfigured = true;
        this.value = this.clamp(this.value);
        return this;
    }

    public boolean isInteger() {
        return this.integer;
    }

    public SliderSetting visible(Supplier<Boolean> supplier) {
        this.setVisibilityCondition(supplier);
        return this;
    }

    public float getMax() {
        return this.max;
    }

    public float getMin() {
        return this.min;
    }

    public float getProgress() {
        if (!this.rangeConfigured || this.max == this.min) {
            return 0.0f;
        }
        return (this.value - this.min) / (this.max - this.min);
    }

    public float getIncrement() {
        return this.increment;
    }
}

