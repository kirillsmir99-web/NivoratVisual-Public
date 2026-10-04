package rtx.nv.api.ui.settings.impl;
import java.awt.Color;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.sounds.Sounds;

public class TextSetting
implements Setting {
    private static final float FIELD_W = 92.0f;
    private static final float FONT_SIZE = 6.0f;
    private static TextSetting focused;
    private final rtx.nv.api.modules.settings.impl.TextSetting backend;

    public TextSetting(rtx.nv.api.modules.settings.impl.TextSetting textSetting) {
        this.backend = textSetting;
    }

    @Override
    public String name() {
        return this.backend.getName();
    }

    public void typeChar(int n) {
        if (Character.isISOControl(n)) {
            return;
        }
        String string = this.backend.getValue();
        if (string.length() >= this.backend.getMaxLength()) {
            return;
        }
        this.backend.setText(string + new String(Character.toChars(n)));
        Sounds.play("search_typing");
    }

    public boolean typeKey(int n) {
        switch (n) {
            case 259: {
                String string = this.backend.getValue();
                if (!string.isEmpty()) {
                    this.backend.setText(string.substring(0, string.length() - 1));
                    Sounds.play("search_typing");
                }
                return true;
            }
            case 256: 
            case 257: 
            case 335: {
                focused = null;
                Sounds.play("buttonclick");
                return true;
            }
        }
        return false;
    }

    @Override
    public float height() {
        return 16.0f;
    }

    @Override
    public void render(float f, float f2, float f3, float f4) {
        String string;
        boolean bl;
        float f5 = 12.0f;
        float fieldW = Math.min(84.0f, Math.max(56.0f, f3 * 0.44f));
        float f6 = f + f3 - fieldW - 4.0f;
        float f7 = f2 + (16.0f - f5) * 0.5f;
        boolean bl2 = this.isFocused();
        RenderHelper.drawName(this.backend.getName(), f, f2, f6 - (f + 6.0f) - 4.0f, f4);
        Render2D.rect(f6, f7, fieldW, f5, 3.0f, new Color(0, 0, 0, (int)(60.0f * f4)).getRGB());
        if (bl2) {
            Render2D.outline(f6, f7, fieldW, f5, 3.0f, 0.6f, ClientAccent.accentBright(190.0f * f4));
        }
        boolean bl3 = bl = (string = this.backend.getValue()) == null || string.isEmpty();
        String string2 = bl ? (this.backend.getPlaceholder().isEmpty() ? "..." : this.backend.getPlaceholder()) : string;
        float f8 = fieldW - 10.0f;
        String string3 = string2;
        while (string3.length() > 1 && Fonts.MONTSERRAT_MEDIUM.width(string3, 6.0f) > f8) {
            string3 = string3.substring(1);
        }
        int n = bl ? new Color(255, 255, 255, (int)(110.0f * f4)).getRGB() : ClientAccent.accentSoft(220.0f * f4);
        Fonts.MONTSERRAT_MEDIUM.draw(string3, f6 + 5.0f, f7 + 2.5f, 6.0f, n);
        if (bl2 && System.currentTimeMillis() / 500L % 2L == 0L) {
            float f9 = f6 + 5.0f + (bl ? 0.0f : Fonts.MONTSERRAT_MEDIUM.width(string3, 6.0f)) + 0.5f;
            Render2D.rect(f9, f7 + 2.0f, 0.8f, f5 - 4.0f, 0.0f, ClientAccent.accentBright(220.0f * f4));
        }
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
            boolean was = this.isFocused();
            focused = was ? null : this;
            Sounds.play("buttonclick");
            return true;
        }
        if (this.isFocused()) {
            focused = null;
        }
        return false;
    }

    public boolean isFocused() {
        return focused == this;
    }

    public static void unfocusAll() {
        focused = null;
    }

    public static boolean isAnyFocused() {
        return focused != null;
    }

    @Override
    public float preferredWidth() {
        return 6.0f + Fonts.MONTSERRAT_MEDIUM.width(this.backend.getName(), 6.5f) + 6.0f + 92.0f + 8.0f;
    }
}

