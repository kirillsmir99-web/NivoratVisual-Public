package rtx.nv.api.modules.settings.impl;
import java.awt.Color;
import java.util.function.Supplier;
import rtx.nv.api.modules.settings.Setting;

public class ColorSetting
extends Setting {
    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float brightness = 1.0f;
    private float alpha = 1.0f;
    private int defaultColor = -1;
    private boolean defaultCaptured;
    private int cachedRgb;
    private boolean rgbCacheValid;

    public ColorSetting(String string, String string2) {
        super(string, string2);
    }

    public ColorSetting(String string, String string2, Color color) {
        this(string, string2);
        if (color != null) {
            this.setColor(color.getRGB());
        }
    }

    public ColorSetting value(int n) {
        this.setColor(n);
        return this;
    }

    public int getValue() {
        return this.getColor();
    }

    public ColorSetting visible(Supplier<Boolean> supplier) {
        this.setVisibilityCondition(supplier);
        return this;
    }

    public float getHue() {
        return this.hue;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public ColorSetting setHSB(float f, float f2, float f3) {
        int n = this.getColor();
        this.hue = f;
        this.saturation = f2;
        this.brightness = f3;
        this.rgbCacheValid = false;
        if (n != this.getColor()) {
            this.notifyChanged();
        }
        return this;
    }

    public ColorSetting setAlpha(float f) {
        int n = this.getColor();
        this.alpha = Math.max(0.0f, Math.min(1.0f, f));
        if (n != this.getColor()) {
            this.notifyChanged();
        }
        return this;
    }

    public ColorSetting visibleWhen(Supplier<Boolean> supplier) {
        return this.visible(supplier);
    }

    public int getDefaultColor() {
        return this.defaultColor;
    }

    private int rgb() {
        if (!this.rgbCacheValid) {
            this.cachedRgb = Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 0xFFFFFF;
            this.rgbCacheValid = true;
        }
        return this.cachedRgb;
    }

    public ColorSetting setColor(int n) {
        int n2 = this.getColor();
        int n3 = n >>> 24 & 0xFF;
        int n4 = n >>> 16 & 0xFF;
        int n5 = n >>> 8 & 0xFF;
        int n6 = n & 0xFF;
        float[] fArray = Color.RGBtoHSB(n4, n5, n6, null);
        this.hue = fArray[0];
        this.saturation = fArray[1];
        this.brightness = fArray[2];
        this.alpha = (float)n3 / 255.0f;
        this.rgbCacheValid = false;
        if (!this.defaultCaptured) {
            this.defaultColor = n;
            this.defaultCaptured = true;
        }
        if (n2 != this.getColor()) {
            this.notifyChanged();
        }
        return this;
    }

    public int getColor() {
        return Math.round(this.alpha * 255.0f) << 24 | this.rgb();
    }

    public float getSaturation() {
        return this.saturation;
    }

    public float getBrightness() {
        return this.brightness;
    }

    public int getColorOpaque() {
        return this.rgb() | 0xFF000000;
    }
}

