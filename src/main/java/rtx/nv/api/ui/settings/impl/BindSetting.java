package rtx.nv.api.ui.settings.impl;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.utils.key.KeyBind;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.sounds.Sounds;

public class BindSetting
implements Setting {
    private final rtx.nv.api.modules.settings.impl.BindSetting backend;
    private boolean listening;

    public BindSetting(rtx.nv.api.modules.settings.impl.BindSetting bindSetting) {
        this.backend = bindSetting;
    }

    @Override
    public String name() {
        return this.backend.getName();
    }

    public void setKey(int n) {
        if (n == 256) {
            this.backend.setKey(0);
        } else {
            this.backend.setKey(n);
        }
        this.listening = false;
        Sounds.play("buttonclick");
    }

    @Override
    public float height() {
        return 16.0f;
    }

    @Override
    public void render(float f, float f2, float f3, float f4) {
        String string = this.listening ? "Press key..." : new KeyBind(this.backend.getKey()).getDisplayName();
        float f5 = Fonts.MONTSERRAT_MEDIUM.width(string, 6.0f) + 10.0f;
        float f6 = f + f3 - f5 - 4.0f;
        float f7 = f2 + 2.0f;
        RenderHelper.drawName(this.backend.getName(), f, f2, f6 - (f + 6.0f) - 4.0f, f4);
        RenderHelper.drawBtn(f6, f7, f5, 12.0f, string, f4);
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
            if (button == 1) {
                this.backend.setKey(0);
                this.listening = false;
                Sounds.play("buttonclick");
                return true;
            }
            this.listening = !this.listening;
            Sounds.play("buttonclick");
            return true;
        }
        return false;
    }

    public void setListening(boolean bl) {
        this.listening = bl;
    }

    public boolean isListening() {
        return this.listening;
    }

    @Override
    public float preferredWidth() {
        String string = new KeyBind(this.backend.getKey()).getDisplayName();
        return 6.0f + Fonts.MONTSERRAT_MEDIUM.width(this.backend.getName(), 6.5f) + 6.0f + Fonts.MONTSERRAT_MEDIUM.width(string, 6.0f) + 10.0f + 8.0f;
    }
}

