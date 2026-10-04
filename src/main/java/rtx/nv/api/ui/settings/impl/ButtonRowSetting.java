package rtx.nv.api.ui.settings.impl;

import rtx.nv.api.drags.Position;
import rtx.nv.api.modules.settings.impl.ButtonSetting;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.sounds.Sounds;

public class ButtonRowSetting implements Setting {
    public static final float HEIGHT = 18.0f;
    private static final float BTN_H = 12.0f;
    private final ButtonSetting backend;

    public ButtonRowSetting(ButtonSetting backend) {
        this.backend = backend;
    }

    @Override
    public String name() {
        return this.backend.getName();
    }

    @Override
    public float height() {
        return HEIGHT;
    }

    private static int rgba(int r, int g, int b, float a) {
        int alpha = Math.max(0, Math.min(255, Math.round(a)));
        return (alpha & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    private String getDisplayLabel() {
        String label = this.backend.getLabel();
        String fallback = (label == null || label.trim().isEmpty()) ? "Нажать" : label;
        return rtx.nv.api.localization.Lang.get("ui.button." + rtx.nv.api.localization.Lang.toKey(fallback), fallback);
    }

    private float getBtnWidth() {
        String label = this.getDisplayLabel();
        float textW = Fonts.MONTSERRAT_MEDIUM.width(label, 5.5f);
        return Math.max(36.0f, textW + 12.0f);
    }

    @Override
    public void render(float x, float y, float width, float alpha) {
        String label = this.getDisplayLabel();
        float btnW = this.getBtnWidth();
        float btnX = x + width - btnW - 4.0f;
        float btnY = y + (HEIGHT - BTN_H) * 0.5f;

        // Label on left with fitText
        float maxLabelW = btnX - (x + 6.0f) - 4.0f;
        RenderHelper.drawName(this.backend.getName(), x, y, maxLabelW, alpha);

        // Action button on right
        float mouseX = Position.mouseX();
        float mouseY = Position.mouseY();
        boolean hover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + BTN_H;

        int bgCol = rgba(18, 20, 26, (hover ? 160.0f : 110.0f) * alpha);
        Render2D.rect(btnX, btnY, btnW, BTN_H, 3.0f, bgCol);

        int borderCol = hover ? ClientAccent.accentBright(180.0f * alpha) : rgba(255, 255, 255, 22.0f * alpha);
        Render2D.outline(btnX, btnY, btnW, BTN_H, 3.0f, 0.5f, borderCol);

        float textW = Fonts.MONTSERRAT_MEDIUM.width(label, 5.5f);
        int textCol = hover ? ClientAccent.accentBright(245.0f * alpha) : rgba(225, 230, 245, 210.0f * alpha);
        Fonts.MONTSERRAT_MEDIUM.draw(label, btnX + (btnW - textW) * 0.5f, btnY + 2.5f, 5.5f, textCol);
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
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEIGHT) {
            this.backend.click();
            Sounds.play("buttonclick");
            return true;
        }
        return false;
    }

    @Override
    public float preferredWidth() {
        return Fonts.MONTSERRAT_MEDIUM.width(this.backend.getName(), 6.0f) + this.getBtnWidth() + 20.0f;
    }
}
