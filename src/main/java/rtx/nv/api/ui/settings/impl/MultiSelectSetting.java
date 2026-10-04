package rtx.nv.api.ui.settings.impl;

import java.awt.Color;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.theme.AccentGradient;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.animations.Decelerate;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.sounds.Sounds;

public class MultiSelectSetting implements Setting {
    private static final int MAX_VISIBLE = 7;
    private static final float ITEM_H = 12.0f;
    private static final float PAD = 3.0f;
    private static final float TEXT_SIZE = 5.5f;
    private static final float EDGE_FADE = 4.0f;

    private final rtx.nv.api.modules.settings.impl.MultiSelectSetting backend;
    private boolean open;
    private final Decelerate dropAnim;
    private float scroll;
    private float scrollTarget;
    private long lastScrollNs;

    public MultiSelectSetting(rtx.nv.api.modules.settings.impl.MultiSelectSetting multiSelectSetting) {
        this.backend = multiSelectSetting;
        this.dropAnim = new Decelerate();
        this.dropAnim.setMs(200);
        this.dropAnim.setValue(1.0);
        this.dropAnim.setDirection(Direction.BACKWARDS);
        this.dropAnim.counter.setTime(System.currentTimeMillis() - 10000L);
    }

    @Override
    public String name() {
        return this.backend.getName();
    }

    private static float clamp(float f, float min, float max) {
        return f < min ? min : (f > max ? max : f);
    }

    private String label() {
        String fmt = rtx.nv.api.localization.Lang.get("ui.multiselect.format", "%d of %d");
        return String.format(fmt, this.backend.getSelected().size(), this.backend.getOptions().size());
    }

    @Override
    public float height() {
        return 16.0f;
    }

    private float dropWidth() {
        float f = 0.0f;
        for (String string : this.backend.getOptions()) {
            String opt = rtx.nv.api.localization.Lang.translateOption(string);
            f = Math.max(f, Fonts.MONTSERRAT_MEDIUM.width(opt, TEXT_SIZE));
        }
        float f2 = f + 20.0f;
        return this.backend.getOptions().size() > MAX_VISIBLE ? f2 + 6.0f : f2;
    }

    private float maxScroll() {
        return (float) Math.max(0, this.backend.getOptions().size() - MAX_VISIBLE) * ITEM_H;
    }

    private static float edgeAlpha(float f, float f2, float f3) {
        float f4 = clamp((f - f2) / EDGE_FADE, 0.0f, 1.0f);
        return Math.min(f4, clamp((f3 - f) / EDGE_FADE, 0.0f, 1.0f));
    }

    private void updateScroll() {
        long now = System.nanoTime();
        float dt = this.lastScrollNs == 0L ? 0.0f : Math.min(0.05f, (float) (now - this.lastScrollNs) / 1.0E9f);
        this.lastScrollNs = now;
        float max = this.maxScroll();
        this.scrollTarget = clamp(this.scrollTarget, 0.0f, max);
        this.scroll += (this.scrollTarget - this.scroll) * (1.0f - (float) Math.exp(-dt * 18.0f));
        if (Math.abs(this.scrollTarget - this.scroll) < 0.05f) {
            this.scroll = this.scrollTarget;
        }
        this.scroll = clamp(this.scroll, 0.0f, max);
    }

    private float getDropX(float x, float width, float dropW) {
        float dropX = x + width - dropW - 4.0f;
        return Math.max(8.0f, Math.min(rtx.nv.api.drags.Position.screenWidth() - dropW - 8.0f, dropX));
    }

    private float getDropY(float y, float dropH) {
        float panelBottom = Math.min(rtx.nv.api.drags.Position.screenHeight() - 8.0f, rtx.nv.api.ui.UI.panelY() + rtx.nv.api.ui.UI.PANEL_H - 6.0f);
        boolean flipUp = (y + 16.0f + 2.0f + dropH > panelBottom) && (y - dropH - 2.0f > 8.0f);
        float resY = flipUp ? (y - dropH - 2.0f) : (y + 16.0f + 2.0f);
        return Math.max(rtx.nv.api.ui.UI.panelY() + 6.0f, Math.min(panelBottom - dropH, resY));
    }

    @Override
    public void renderOverlay(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float animProgress = this.dropAnim.getOutput().floatValue();
        if (animProgress <= 0.01f) {
            return;
        }
        float alphaMult = animProgress * f4;
        List<String> list = this.backend.getOptions();
        float dropW = this.dropWidth();
        int visibleCount = Math.min(list.size(), MAX_VISIBLE);
        float dropH = (float) visibleCount * ITEM_H + PAD * 2.0f;
        float dropX = this.getDropX(f, f3, dropW);
        float dropY = this.getDropY(f2, dropH);
        boolean hasScroll = list.size() > MAX_VISIBLE;

        this.updateScroll();
        RenderHelper.drawDropBackground(dropX, dropY, dropW, dropH, alphaMult);

        float topEdge = dropY + 1.0f;
        float botEdge = dropY + dropH - 1.0f;

        for (int i = 0; i < list.size(); ++i) {
            float itemY = dropY + PAD + (float) i * ITEM_H - this.scroll;
            float topY = itemY + 2.0f;
            float botY = itemY + ITEM_H - 2.0f;
            if (botY <= topEdge || topY >= botEdge) {
                continue;
            }
            float itemAlpha = hasScroll ? Math.min(edgeAlpha(topY, topEdge, botEdge), edgeAlpha(botY, topEdge, botEdge)) : 1.0f;
            if (itemAlpha <= 0.01f) {
                continue;
            }
            String option = list.get(i);
            boolean isSelected = this.backend.isSelected(option);
            int textCol = isSelected ? ClientAccent.accentSoft(230.0f * alphaMult * itemAlpha) : new Color(255, 255, 255, (int) (140.0f * alphaMult * itemAlpha)).getRGB();
            Fonts.MONTSERRAT_MEDIUM.draw(rtx.nv.api.localization.Lang.translateOption(option), dropX + 6.0f, itemY + 2.5f, TEXT_SIZE, textCol);
            if (!isSelected) {
                continue;
            }
            AccentGradient.fillVertical(dropX + dropW - (float) (hasScroll ? 10 : 8), itemY + ITEM_H * 0.5f - 1.0f, 2.0f, 2.0f, 1.0f, 200.0f * alphaMult * itemAlpha);
        }

        if (hasScroll) {
            float maxScr = this.maxScroll();
            float barX = dropX + dropW - 3.5f;
            float barY = dropY + 2.0f;
            float barH = dropH - 4.0f;
            Render2D.rect(barX, barY, 1.6f, barH, 0.8f, new Color(255, 255, 255, (int) (28.0f * alphaMult)).getRGB());
            float thumbH = Math.max(10.0f, barH * (float) MAX_VISIBLE / (float) list.size());
            float thumbY = barY + (maxScr <= 0.0f ? 0.0f : (this.scroll / maxScr) * (barH - thumbH));
            Render2D.rect(barX, thumbY, 1.6f, thumbH, 0.8f, ClientAccent.accentSoft(190.0f * alphaMult));
        }
    }

    @Override
    public void render(float f, float f2, float f3, float f4) {
        String string = this.label();
        float btnW = Fonts.MONTSERRAT_MEDIUM.width(string, 6.0f) + 10.0f;
        float btnX = f + f3 - btnW - 4.0f;
        float btnY = f2 + 2.0f;
        RenderHelper.drawName(this.backend.getName(), f, f2, btnX - (f + 6.0f) - 4.0f, f4);
        RenderHelper.drawBtn(btnX, btnY, btnW, 12.0f, string, f4);
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
            this.open = !this.open;
            Sounds.play(this.open ? "settings_open" : "settings_close");
            this.dropAnim.setDirection(this.open ? Direction.FORWARDS : Direction.BACKWARDS);
            this.dropAnim.counter.resetCounter();
            if (this.open) {
                this.lastScrollNs = 0L;
            }
            return true;
        }
        return false;
    }

    @Override
    public void closeOverlay() {
        this.closeOverlay(true);
    }

    @Override
    public void closeOverlay(boolean playSound) {
        if (this.open) {
            this.open = false;
            if (playSound) {
                Sounds.play("settings_close");
            }
            this.dropAnim.setDirection(Direction.BACKWARDS);
            this.dropAnim.counter.resetCounter();
        }
    }

    @Override
    public boolean isOverlayOpen() {
        return this.open;
    }

    @Override
    public boolean scrollOverlay(float f, float f2, float f3, float f4, float f5, double d) {
        if (!this.open) {
            return false;
        }
        float dropW = this.dropWidth();
        int visibleCount = Math.min(this.backend.getOptions().size(), MAX_VISIBLE);
        float dropH = (float) visibleCount * ITEM_H + PAD * 2.0f;
        float dropX = this.getDropX(f, f3, dropW);
        float dropY = this.getDropY(f2, dropH);
        if (f4 < dropX || f4 > dropX + dropW || f5 < dropY || f5 > dropY + dropH) {
            return false;
        }
        float maxScr = this.maxScroll();
        if (maxScr > 0.0f) {
            this.scrollTarget = clamp(this.scrollTarget - (float) d * ITEM_H, 0.0f, maxScr);
        }
        return true;
    }

    @Override
    public boolean clickOverlay(float f, float f2, float f3, float f4, float f5) {
        return this.clickOverlay(f, f2, f3, f4, f5, 0);
    }

    @Override
    public boolean clickOverlay(float f, float f2, float f3, float f4, float f5, int button) {
        float animProgress = this.dropAnim.getOutput().floatValue();
        if (!this.open || animProgress <= 0.1f) {
            return false;
        }
        List<String> list = this.backend.getOptions();
        float dropW = this.dropWidth();
        int visibleCount = Math.min(list.size(), MAX_VISIBLE);
        float dropH = (float) visibleCount * ITEM_H + PAD * 2.0f;
        float dropX = this.getDropX(f, f3, dropW);
        float dropY = this.getDropY(f2, dropH);
        if (f4 < dropX || f4 > dropX + dropW || f5 < dropY + PAD || f5 > dropY + dropH - PAD) {
            return false;
        }
        if (button == 1) {
            this.closeOverlay(true);
            return true;
        }
        int idx = (int) Math.floor((f5 - (dropY + PAD) + this.scroll) / ITEM_H);
        if (idx >= 0 && idx < list.size()) {
            this.backend.toggle(list.get(idx));
            Sounds.play("buttonclick");
        }
        return true;
    }

    @Override
    public boolean hasOverlay() {
        return this.dropAnim.getOutput().floatValue() > 0.01f;
    }

    @Override
    public float preferredWidth() {
        return 6.0f + Fonts.MONTSERRAT_MEDIUM.width(this.backend.getName(), 6.5f) + 6.0f + Fonts.MONTSERRAT_MEDIUM.width(this.label(), 6.0f) + 10.0f + 8.0f;
    }
}
