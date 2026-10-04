package rtx.nv.api.ui.settings.impl;
import java.awt.Color;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.theme.AccentGradient;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.animations.Decelerate;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.sounds.Sounds;

public class BoolSetting
implements Setting {
    private final BooleanSetting backend;
    private final Decelerate anim;
    public static final float PW = 19.1f;
    public static final float PH = 10.4f;
    public static final float PR = 4.8f;
    public static final float PAD = 4.0f;

    public BoolSetting(BooleanSetting booleanSetting) {
        this.backend = booleanSetting;
        this.anim = new Decelerate();
        this.anim.setMs(120);
        this.anim.setValue(1.0);
        this.anim.setDirection(booleanSetting.getValue() ? Direction.FORWARDS : Direction.BACKWARDS);
        this.anim.counter.setTime(System.currentTimeMillis() - 10000L);
    }

    @Override
    public String name() {
        return this.backend.getName();
    }

    public boolean getValue() {
        return this.backend.getValue();
    }

    public void setValue(boolean bl) {
        this.backend.setValue(bl);
    }

    @Override
    public float height() {
        return 16.0f;
    }

    @Override
    public void render(float f, float f2, float f3, float f4) {
        float f5 = f + f3 - 19.1f - 4.0f;
        float f6 = f2 + 2.8000002f;
        RenderHelper.drawName(this.backend.getName(), f, f2, f5 - (f + 6.0f) - 4.0f, f4);
        this.anim.setDirection(this.backend.getValue() ? Direction.FORWARDS : Direction.BACKWARDS);
        float f7 = this.anim.getOutput().floatValue();
        BoolSetting.drawToggle(f5, f6, f7, f4);
    }

    private static int rgba(int n, int n2, int n3, float f) {
        int n4 = Math.max(0, Math.min(255, Math.round(f)));
        if (n4 <= 0) {
            return 0;
        }
        return new Color(n, n2, n3, n4).getRGB();
    }

    @Override
    public boolean isVisible() {
        return this.backend.isVisible();
    }

    @Override
    public boolean click(float x, float y, float width, float mouseX, float mouseY) {
        return this.click(x, y, width, mouseX, mouseY, 0);
    }

    @Override
    public boolean click(float x, float y, float width, float mouseX, float mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + this.height()) {
            if (button == 0 || button == 1) {
                this.backend.toggle();
                Sounds.play("buttonclick");
                return true;
            }
        }
        return false;
    }

    public static void drawToggle(float f, float f2, float f3, float f4, float f5, float f6) {
        float f7 = f4 * 0.4615385f;
        int n = BoolSetting.rgba(96, 99, 105, 165.0f * f6 * (1.0f - f5));
        int n2 = BoolSetting.rgba(78, 81, 87, 165.0f * f6 * (1.0f - f5));
        int n3 = BoolSetting.rgba(120, 123, 129, 150.0f * f6);
        int n4 = ColorUtil.lerpColor(n3, ClientAccent.accentBright(150.0f * f6), f5);
        if (f5 < 0.999f) {
            Render2D.rect(f + 0.5f, f2 + 0.5f, f3 - 1.0f, f4 - 1.0f, f7, n, n2, n2, n);
        }
        if (f5 > 0.001f) {
            AccentGradient.fillHorizontal(f + 0.5f, f2 + 0.5f, f3 - 1.0f, f4 - 1.0f, f7, 175.0f * f6 * f5);
        }
        Render2D.outline(f, f2, f3, f4, f7, 0.5f, n4);
        float f8 = f4 * 0.8076923f;
        float f9 = f4 * 0.125f;
        float f10 = f + f9;
        float f11 = f + f3 - f8 - f9;
        float f12 = f10 + (f11 - f10) * f5;
        float f13 = f2 + (f4 - f8) * 0.5f;
        int n5 = BoolSetting.rgba(239, 252, 255, 255.0f * f6);
        int n6 = BoolSetting.rgba(242, 250, 255, 255.0f * f6);
        Render2D.rect(f12, f13, f8, f8, f8 * 0.5f, n5, n5, n6, n5);
        Render2D.outline(f12, f13, f8, f8, f8 * 0.5f, 0.5f, BoolSetting.rgba(255, 255, 255, 210.0f * f6));
    }

    public static void drawToggle(float f, float f2, float f3, float f4) {
        BoolSetting.drawToggle(f, f2, 19.1f, 10.4f, f3, f4);
    }

    @Override
    public float preferredWidth() {
        return 6.0f + Fonts.MONTSERRAT_MEDIUM.width(this.backend.getName(), 6.5f) + 6.0f + 19.1f + 4.0f;
    }
}

