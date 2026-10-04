package rtx.nv.api.ui.settings.impl;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.drags.Position;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.animations.Decelerate;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.sounds.Sounds;

public class ColorSetting
implements Setting {
    private final rtx.nv.api.modules.settings.impl.ColorSetting backend;
    private float pickerHue;
    private float pickerSat;
    private float pickerVal;
    private float pickerAlpha = 1.0f;
    private float dispHue;
    private float dispSat;
    private float dispVal;
    private float dispAlpha;
    private float svVelX;
    private float svVelY;
    private long lastNs = System.nanoTime();
    private boolean open;
    private int drag;
    private int lastValue = -1;
    private String lastText = null;
    private final Decelerate anim;
    private static final float PAD = 6.0f;
    private static final float SV_W = 70.0f;
    private static final float SV_H = 50.0f;
    private static final float BAR_H = 4.0f;
    private static final float GAP = 4.0f;

    public ColorSetting(rtx.nv.api.modules.settings.impl.ColorSetting colorSetting) {
        this.backend = colorSetting;
        this.anim = new Decelerate();
        this.anim.setMs(220);
        this.anim.setValue(1.0);
        this.anim.setDirection(Direction.BACKWARDS);
        this.anim.counter.setTime(System.currentTimeMillis() - 10000L);
    }

    @Override
    public String name() {
        return this.backend.getName();
    }

    private void commit() {
        int n = Color.HSBtoRGB(this.pickerHue, this.pickerSat, this.pickerVal) & 0xFFFFFF;
        int n2 = Math.round(this.pickerAlpha * 255.0f) & 0xFF;
        this.backend.setColor(n2 << 24 | n);
    }

    @Override
    public float height() {
        return 16.0f;
    }

    private static float clamp01(float f) {
        return f < 0.0f ? 0.0f : (f > 1.0f ? 1.0f : f);
    }

    private float getDropX(float x, float width) {
        float dropW = 82.0f;
        float dropX = x + width - dropW - 4.0f;
        return Math.max(8.0f, Math.min(Position.screenWidth() - dropW - 8.0f, dropX));
    }

    private float getDropY(float y) {
        float dropH = 78.0f;
        float panelBottom = Math.min(Position.screenHeight() - 8.0f, rtx.nv.api.ui.UI.panelY() + rtx.nv.api.ui.UI.PANEL_H - 6.0f);
        boolean flipUp = (y + 16.0f + 2.0f + dropH > panelBottom) && (y - dropH - 2.0f > 8.0f);
        float resY = flipUp ? (y - dropH - 2.0f) : (y + 16.0f + 2.0f);
        return Math.max(rtx.nv.api.ui.UI.panelY() + 6.0f, Math.min(panelBottom - dropH, resY));
    }

    @Override
    public void renderOverlay(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = this.anim.getOutput().floatValue();
        if (f5 <= 0.01f) {
            return;
        }
        if (this.drag != 0) {
            this.updateDrag(Position.mouseX(), Position.mouseY(), f, f2, f3);
        }
        float f6 = f5 * f4;
        long l = System.nanoTime();
        float f7 = Math.min(0.05f, (float)(l - this.lastNs) / 1.0E9f);
        this.lastNs = l;
        float f8 = 1.0f - (float)Math.exp(-f7 * 16.0f);
        this.dispHue += (this.pickerHue - this.dispHue) * f8;
        this.dispAlpha += (this.pickerAlpha - this.dispAlpha) * f8;
        float f9 = 200.0f;
        float f10 = 22.0f;
        this.svVelX += ((this.pickerSat - this.dispSat) * f9 - this.svVelX * f10) * f7;
        this.svVelY += ((this.pickerVal - this.dispVal) * f9 - this.svVelY * f10) * f7;
        this.dispSat += this.svVelX * f7;
        this.dispVal += this.svVelY * f7;
        float f11 = 82.0f;
        float f12 = 78.0f;
        float f13 = this.getDropX(f, f3);
        float f14 = this.getDropY(f2);
        RenderHelper.drawDropBackground(f13, f14, f11, f12, f6);
        float f15 = f13 + 6.0f;
        float f16 = f14 + 6.0f;
        int n = Color.HSBtoRGB(this.pickerHue, 1.0f, 1.0f);
        int n2 = n & 0xFFFFFF | (int)(255.0f * f6) << 24;
        int n3 = (int)(255.0f * f6) << 24 | 0xFFFFFF;
        int n4 = (int)(255.0f * f6) << 24;
        Render2D.rect(f15, f16, 70.0f, 50.0f, 2.0f, n3, n2, n2, n3);
        Render2D.rect(f15, f16, 70.0f, 50.0f, 2.0f, 0, 0, n4, n4);
        float f17 = f15 + 70.0f * this.dispSat;
        float f18 = f16 + 50.0f * (1.0f - this.dispVal);
        int n5 = (int)(220.0f * f6) << 24 | 0xFFFFFF;
        Render2D.outline(f17 - 3.0f, f18 - 3.0f, 5.0f, 5.0f, 3.0f, 1.0f, n5);
        float f19 = f16 + 50.0f + 4.0f;
        this.renderHueBar(f15, f19, 70.0f, 4.0f, f6);
        Render2D.rect(f15 + 70.0f * this.dispHue - 1.0f, f19 - 1.0f, 2.0f, 6.0f, 1.0f, n5);
        float f20 = f19 + 4.0f + 4.0f;
        this.renderAlphaBar(f15, f20, 70.0f, 4.0f, f6);
        Render2D.rect(f15 + 70.0f * this.dispAlpha - 1.0f, f20 - 1.0f, 2.0f, 6.0f, 1.0f, n5);
    }

    private String formatColor(int color) {
        int rgb = color & 0xFFFFFF;
        if (this.lastValue == rgb && this.lastText != null) {
            return this.lastText;
        }
        this.lastValue = rgb;
        this.lastText = String.format("#%06X", rgb);
        return this.lastText;
    }

    @Override
    public void render(float f, float f2, float f3, float f4) {
        int n = this.backend.getColor();
        String string = this.formatColor(n);
        float f5 = Fonts.MONTSERRAT_MEDIUM.width(string, 5.5f);
        float f6 = 8.0f;
        float f7 = f6 + 4.0f + f5 + 10.0f;
        float f8 = f + f3 - f7 - 4.0f;
        float f9 = f2 + 2.0f;
        RenderHelper.drawName(this.backend.getName(), f, f2, f8 - (f + 6.0f) - 4.0f, f4);
        RenderHelper.drawPanelBg(f8, f9, f7, 12.0f, 3.0f, f4);
        int n2 = n & 0xFFFFFF | (int)((float)(n >>> 24 & 0xFF) * f4) << 24;
        Render2D.rect(f8 + 3.0f, f9 + (12.0f - f6) * 0.5f, f6, f6, 2.0f, n2);
        Fonts.MONTSERRAT_MEDIUM.draw(string, f8 + 3.0f + f6 + 4.0f, f9 + 3.0f, 5.5f, ClientAccent.accentSoft(200.0f * f4));
    }

    @Override
    public boolean isVisible() {
        return this.backend.isVisible();
    }

    @Override
    public boolean click(float f, float f2, float f3, float f4, float f5) {
        return this.click(f, f2, f3, f4, f5, 0);
    }

    @Override
    public boolean click(float f, float f2, float f3, float f4, float f5, int button) {
        if (f4 >= f && f4 <= f + f3 && f5 >= f2 && f5 <= f2 + this.height()) {
            if (this.open) {
                this.closeOverlay();
            } else {
                this.openPicker();
            }
            return true;
        }
        return false;
    }

    public int getColor() {
        return this.backend.getColor();
    }

    private void renderHueBar(float f, float f2, float f3, float f4, float f5) {
        Render2D.pickerHue(f, f2, f3, f4, f5);
    }

    private void renderAlphaBar(float f, float f2, float f3, float f4, float f5) {
        int n = Color.HSBtoRGB(this.pickerHue, this.pickerSat, this.pickerVal) & 0xFFFFFF;
        Render2D.pickerAlpha(f, f2, f3, f4, 0xFF000000 | n, f5);
    }

    @Override
    public void releaseDrag() {
        this.drag = 0;
    }

    @Override
    public void closeOverlay() {
        this.closeOverlay(true);
    }

    @Override
    public void closeOverlay(boolean playSound) {
        if (this.open) {
            this.open = false;
            this.drag = 0;
            if (playSound) {
                Sounds.play("settings_close");
            }
            this.anim.setDirection(Direction.BACKWARDS);
            this.anim.counter.resetCounter();
        }
    }

    @Override
    public boolean isOverlayOpen() {
        return this.open;
    }

    @Override
    public boolean clickOverlay(float f, float f2, float f3, float f4, float f5) {
        return this.clickOverlay(f, f2, f3, f4, f5, 0);
    }

    @Override
    public boolean clickOverlay(float f, float f2, float f3, float f4, float f5, int button) {
        float f6 = this.anim.getOutput().floatValue();
        if (!this.open || f6 <= 0.1f) {
            return false;
        }
        float f7 = 82.0f;
        float f8 = 78.0f;
        float f9 = this.getDropX(f, f3);
        float f10 = this.getDropY(f2);
        if (f4 < f9 || f4 > f9 + f7 || f5 < f10 || f5 > f10 + f8) {
            return false;
        }
        if (button == 1) {
            this.closeOverlay();
            return true;
        }
        float f11 = f9 + 6.0f;
        float f12 = f10 + 6.0f;
        float f13 = f12 + 50.0f + 4.0f;
        float f14 = f13 + 4.0f + 4.0f;
        if (f4 >= f11 && f4 <= f11 + 70.0f && f5 >= f12 && f5 <= f12 + 50.0f) {
            this.pickerSat = ColorSetting.clamp01((f4 - f11) / 70.0f);
            this.pickerVal = ColorSetting.clamp01(1.0f - (f5 - f12) / 50.0f);
            this.drag = 1;
            this.commit();
            return true;
        }
        if (f4 >= f11 && f4 <= f11 + 70.0f && f5 >= f13 - 2.0f && f5 <= f13 + 4.0f + 2.0f) {
            this.pickerHue = ColorSetting.clamp01((f4 - f11) / 70.0f);
            this.drag = 2;
            this.commit();
            return true;
        }
        if (f4 >= f11 && f4 <= f11 + 70.0f && f5 >= f14 - 2.0f && f5 <= f14 + 4.0f + 2.0f) {
            this.pickerAlpha = ColorSetting.clamp01((f4 - f11) / 70.0f);
            this.drag = 3;
            this.commit();
            return true;
        }
        return true;
    }

    @Override
    public boolean hasOverlay() {
        return this.anim.getOutput().floatValue() > 0.01f;
    }

    @Override
    public float preferredWidth() {
        float f = Fonts.MONTSERRAT_MEDIUM.width(this.formatColor(this.backend.getColor()), 5.5f);
        return 6.0f + Fonts.MONTSERRAT_MEDIUM.width(this.backend.getName(), 6.5f) + 6.0f + 8.0f + 4.0f + f + 10.0f + 8.0f;
    }

    private void updateDrag(float mouseX, float mouseY, float x, float y, float width) {
        float dropX = this.getDropX(x, width);
        float dropY = this.getDropY(y);
        float f8 = dropX + 6.0f;
        float f9 = dropY + 6.0f;
        if (this.drag == 1) {
            this.pickerSat = ColorSetting.clamp01((mouseX - f8) / 70.0f);
            this.pickerVal = ColorSetting.clamp01(1.0f - (mouseY - f9) / 50.0f);
        } else if (this.drag == 2) {
            this.pickerHue = ColorSetting.clamp01((mouseX - f8) / 70.0f);
        } else if (this.drag == 3) {
            this.pickerAlpha = ColorSetting.clamp01((mouseX - f8) / 70.0f);
        }
        this.commit();
    }

    private void openPicker() {
        this.open = true;
        Sounds.play("settings_open");
        int n = this.backend.getColor();
        float[] fArray = new float[3];
        Color.RGBtoHSB(n >>> 16 & 0xFF, n >>> 8 & 0xFF, n & 0xFF, fArray);
        this.pickerHue = fArray[0];
        this.pickerSat = fArray[1];
        this.pickerVal = fArray[2];
        this.pickerAlpha = (float)(n >>> 24 & 0xFF) / 255.0f;
        this.dispHue = this.pickerHue;
        this.dispSat = this.pickerSat;
        this.dispVal = this.pickerVal;
        this.dispAlpha = this.pickerAlpha;
        this.svVelX = 0.0f;
        this.svVelY = 0.0f;
        this.lastNs = System.nanoTime();
        this.anim.setDirection(Direction.FORWARDS);
        this.anim.counter.resetCounter();
    }
}

