package rtx.nv.api.ui.settings.impl;

import java.awt.Color;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.drags.Position;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.theme.AccentGradient;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.animations.Decelerate;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.sounds.Sounds;

public class SelectSetting implements Setting {
    private static final int MAX_VISIBLE = 7;
    private static final float ITEM_H = 12.0f;
    private static final float TEXT_SIZE = 5.5f;
    private final rtx.nv.api.modules.settings.impl.SelectSetting backend;
    private boolean open;
    private final Decelerate dropAnim;
    private float scroll;
    private float scrollTarget;
    private long lastScrollNs;
    private static final float EDGE_FADE = 4.0f;
    private float availableWidth = 120.0f;

    public SelectSetting(rtx.nv.api.modules.settings.impl.SelectSetting selectSetting) {
        this.backend = selectSetting;
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

    private static float clamp(float f, float f2, float f3) {
        return f < f2 ? f2 : (f > f3 ? f3 : f);
    }

    @Override
    public float height() {
        return 16.0f;
    }

    private boolean isSegmented() {
        List<String> list = this.backend.getOptions();
        if (list == null || list.isEmpty() || list.size() > 3) return false;
        float chipWidth = (getSegmentedTotalWidth(availableWidth) - 2.0f) / list.size();
        for (String option : list) {
            if (Fonts.MONTSERRAT_MEDIUM.width(rtx.nv.api.localization.Lang.translateOption(option), TEXT_SIZE) + 8.0f > chipWidth) return false;
        }
        return true;
    }

    private float getSegmentedTotalWidth(float maxWidth) {
        List<String> list = this.backend.getOptions();
        if (list == null || list.isEmpty()) return 50.0f;
        float longest = 0.0f;
        for (String opt : list) {
            longest = Math.max(longest, Fonts.MONTSERRAT_MEDIUM.width(rtx.nv.api.localization.Lang.translateOption(opt), TEXT_SIZE));
        }
        return Math.min(Math.max(0, maxWidth - 12.0f), Math.max(54.0f, Math.min(maxWidth * 0.65f, (longest + 10.0f) * list.size() + 2.0f)));
    }

    private float buttonWidth(float width) {
        String label = rtx.nv.api.localization.Lang.translateOption(this.backend.getSelected());
        return Math.min(Math.max(0, width - 12.0f), Math.max(36.0f, Math.min(width * 0.65f, Fonts.MONTSERRAT_MEDIUM.width(label, TEXT_SIZE) + 16.0f)));
    }

    private static int rgba(int r, int g, int b, float a) {
        int alpha = Math.max(0, Math.min(255, Math.round(a)));
        return (alpha & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    private float getDropX(float x, float width, float dropW) {
        float dropX = x + width - dropW - 4.0f;
        return Math.max(8.0f, Math.min(Position.screenWidth() - dropW - 8.0f, dropX));
    }

    private float getDropY(float y, float dropH) {
        float panelBottom = Math.min(Position.screenHeight() - 8.0f, rtx.nv.api.ui.UI.panelY() + rtx.nv.api.ui.UI.PANEL_H - 6.0f);
        boolean flipUp = (y + 16.0f + 2.0f + dropH > panelBottom) && (y - dropH - 2.0f > 8.0f);
        float resY = flipUp ? (y - dropH - 2.0f) : (y + 16.0f + 2.0f);
        return Math.max(rtx.nv.api.ui.UI.panelY() + 6.0f, Math.min(panelBottom - dropH, resY));
    }

    @Override
    public void renderOverlay(DrawContext drawContext, float f, float f2, float f3, float f4) {
        this.availableWidth = f3;
        if (this.isSegmented()) {
            return;
        }
        float animProgress = this.dropAnim.getOutput().floatValue();
        if (animProgress <= 0.01f) {
            return;
        }
        float effectiveAlpha = animProgress * f4;
        List<String> list = this.backend.getOptions();
        float dropW = this.dropWidth();
        int n = Math.min(list.size(), MAX_VISIBLE);
        float dropH = (float) n * 12.0f + 6.0f;

        float dropX = this.getDropX(f, f3, dropW);
        float dropY = this.getDropY(f2, dropH);

        String currentSelected = this.backend.getSelected();
        boolean hasScroll = list.size() > MAX_VISIBLE;
        this.updateScroll();

        RenderHelper.drawDropBackground(dropX, dropY, dropW, dropH, effectiveAlpha);
        float topClip = dropY + 1.0f;
        float botClip = dropY + dropH - 1.0f;

        for (int i = 0; i < list.size(); ++i) {
            float itemY = dropY + 3.0f + (float) i * 12.0f - this.scroll;
            float itemTop = itemY + 2.0f;
            float itemBot = itemY + 12.0f - 2.0f;
            if (itemBot <= topClip || itemTop >= botClip) continue;

            float itemAlpha = hasScroll ? Math.min(SelectSetting.edgeAlpha(itemTop, topClip, botClip), SelectSetting.edgeAlpha(itemBot, topClip, botClip)) : 1.0f;
            if (itemAlpha <= 0.01f) continue;

            String opt = list.get(i);
            boolean isSel = opt.equals(currentSelected);
            int textCol = isSel ? ClientAccent.accentBright(240.0f * effectiveAlpha * itemAlpha) : rgba(215, 220, 235, 140.0f * effectiveAlpha * itemAlpha);
            String display = RenderHelper.fitText(rtx.nv.api.localization.Lang.translateOption(opt), dropW - 20.0f, TEXT_SIZE);
            Fonts.MONTSERRAT_MEDIUM.draw(display, dropX + 6.0f, itemY + 2.5f, TEXT_SIZE, textCol);

            if (isSel) {
                float pipX = dropX + dropW - (hasScroll ? 8.0f : 6.0f);
                AccentGradient.fillVertical(pipX, itemY + 6.0f - 1.0f, 2.0f, 2.0f, 1.0f, 200.0f * effectiveAlpha * itemAlpha);
            }
        }

        if (hasScroll) {
            float maxScr = this.maxScroll();
            float barX = dropX + dropW - 3.0f;
            float barY = dropY + 2.0f;
            float barH = dropH - 4.0f;
            Render2D.rect(barX, barY, 1.6f, barH, 0.8f, rgba(255, 255, 255, 28.0f * effectiveAlpha));
            float thumbH = Math.max(10.0f, barH * (float) MAX_VISIBLE / (float) list.size());
            float thumbY = barY + (maxScr <= 0.0f ? 0.0f : (this.scroll / maxScr) * (barH - thumbH));
            Render2D.rect(barX, thumbY, 1.6f, thumbH, 0.8f, ClientAccent.accentSoft(190.0f * effectiveAlpha));
        }
    }

    @Override
    public void render(float f, float f2, float f3, float f4) {
        this.availableWidth = f3;
        List<String> list = this.backend.getOptions();
        if (this.isSegmented() && list != null && !list.isEmpty()) {
            // Render Segmented Control
            float totalSegW = this.getSegmentedTotalWidth(f3);
            float segX = f + f3 - totalSegW - 4.0f;
            float segY = f2 + 2.0f;
            float segH = 12.0f;

            // Setting label on left
            float maxLabelW = segX - (f + 6.0f) - 4.0f;
            RenderHelper.drawName(this.backend.getName(), f, f2, maxLabelW, f4);

            // Container pill
            Render2D.rect(segX, segY, totalSegW, segH, 3.0f, rgba(16, 18, 24, 120.0f * f4));
            Render2D.outline(segX, segY, totalSegW, segH, 3.0f, 0.5f, rgba(255, 255, 255, 18.0f * f4));

            float chipW = (totalSegW - 2.0f) / (float) list.size();
            String current = this.backend.getSelected();

            for (int i = 0; i < list.size(); i++) {
                String opt = list.get(i);
                boolean isSel = opt.equals(current);
                float chipX = segX + 1.0f + (float) i * chipW;

                if (isSel) {
                    Render2D.rect(chipX, segY + 1.0f, chipW, segH - 2.0f, 2.5f, ClientAccent.accent(190.0f * f4));
                    Render2D.glow(new BuiltGlow(chipX, segY + 1.0f, chipW, segH - 2.0f, new float[]{2.5f, 2.5f, 2.5f, 2.5f}, ClientAccent.accent(255.0f), 0.25f, 3.0f, f4));
                }

                String displayOpt = rtx.nv.api.localization.Lang.translateOption(opt);
                float textW = Fonts.MONTSERRAT_MEDIUM.width(displayOpt, 5.5f);
                int textCol = isSel ? rgba(255, 255, 255, 245.0f * f4) : rgba(195, 202, 220, 140.0f * f4);
                Fonts.MONTSERRAT_MEDIUM.draw(displayOpt, chipX + (chipW - textW) * 0.5f, segY + 2.5f, 5.5f, textCol);
            }
        } else {
            // Dropdown mode (4+ options)
            String string = this.backend.getSelected();
            String displaySel = rtx.nv.api.localization.Lang.translateOption(string);
            float btnW = buttonWidth(f3);
            float btnX = f + f3 - btnW - 4.0f;
            float btnY = f2 + 2.0f;

            // Label on left
            RenderHelper.drawName(this.backend.getName(), f, f2, btnX - (f + 6.0f) - 4.0f, f4);

            // Dropdown button on right with small chevron
            RenderHelper.drawPanelBg(btnX, btnY, btnW, 12.0f, 3.0f, f4);
            int borderCol = this.open ? ClientAccent.accentBright(180.0f * f4) : rgba(255, 255, 255, 18.0f * f4);
            Render2D.outline(btnX, btnY, btnW, 12.0f, 3.0f, 0.5f, borderCol);

            Fonts.MONTSERRAT_MEDIUM.draw(RenderHelper.fitText(displaySel, btnW - 15.0f, TEXT_SIZE), btnX + 4.5f, btnY + 2.5f, TEXT_SIZE, ClientAccent.accentSoft(220.0f * f4));
            Fonts.NV.msdf(NvIcons.CHEVRON_DOWN, btnX + btnW - 8.5f, btnY + 3.0f, 5.0f, ClientAccent.accentBright(180.0f * f4));
        }
    }

    @Override
    public boolean isVisible() {
        return this.backend.isVisible();
    }

    public void nextOption() {
        List<String> list = this.backend.getOptions();
        if (list == null || list.isEmpty()) return;
        int idx = list.indexOf(this.backend.getSelected());
        int next = (idx + 1) % list.size();
        this.backend.setSelected(list.get(next));
        Sounds.play("buttonclick");
    }

    public void prevOption() {
        List<String> list = this.backend.getOptions();
        if (list == null || list.isEmpty()) return;
        int idx = list.indexOf(this.backend.getSelected());
        int prev = (idx - 1 + list.size()) % list.size();
        this.backend.setSelected(list.get(prev));
        Sounds.play("buttonclick");
    }

    @Override
    public boolean click(float f, float f2, float f3, float f4, float f5) {
        return this.click(f, f2, f3, f4, f5, 0);
    }

    @Override
    public boolean click(float f, float f2, float f3, float f4, float f5, int button) {
        this.availableWidth = f3;
        List<String> list = this.backend.getOptions();
        boolean inRow = f4 >= f && f4 <= f + f3 && f5 >= f2 && f5 <= f2 + this.height();
        if (!inRow) {
            return false;
        }

        if (this.isSegmented() && list != null && !list.isEmpty()) {
            float totalSegW = this.getSegmentedTotalWidth(f3);
            float segX = f + f3 - totalSegW - 4.0f;
            float segY = f2 + 2.0f;
            float segH = 12.0f;
            if (f4 >= segX && f4 <= segX + totalSegW && f5 >= segY && f5 <= segY + segH) {
                float chipW = (totalSegW - 2.0f) / (float) list.size();
                int idx = (int) Math.floor((f4 - (segX + 1.0f)) / chipW);
                if (idx >= 0 && idx < list.size()) {
                    String picked = list.get(idx);
                    if (!picked.equals(this.backend.getSelected())) {
                        this.backend.setSelected(picked);
                        Sounds.play("buttonclick");
                    }
                    return true;
                }
            }
            if (button == 1) {
                this.prevOption();
            } else {
                this.nextOption();
            }
            return true;
        }

        // Dropdown button or row click
        float btnW = buttonWidth(f3);
        float btnX = f + f3 - btnW - 4.0f;
        float btnY = f2 + 2.0f;
        boolean inButton = f4 >= btnX && f4 <= btnX + btnW && f5 >= btnY && f5 <= btnY + 12.0f;

        if (button == 1 || (button == 0 && inButton)) {
            this.open = !this.open;
            Sounds.play(this.open ? "settings_open" : "settings_close");
            this.dropAnim.setDirection(this.open ? Direction.FORWARDS : Direction.BACKWARDS);
            this.dropAnim.counter.resetCounter();
            if (this.open) {
                this.initScrollToSelected();
            }
            return true;
        }

        if (button == 0) {
            this.nextOption();
            return true;
        }

        return false;
    }

    private void updateScroll() {
        long l = System.nanoTime();
        float f = this.lastScrollNs == 0L ? 0.0f : Math.min(0.05f, (float) (l - this.lastScrollNs) / 1.0E9f);
        this.lastScrollNs = l;
        float f2 = this.maxScroll();
        this.scrollTarget = SelectSetting.clamp(this.scrollTarget, 0.0f, f2);
        this.scroll += (this.scrollTarget - this.scroll) * (1.0f - (float) Math.exp(-f * 18.0f));
        if (Math.abs(this.scrollTarget - this.scroll) < 0.05f) {
            this.scroll = this.scrollTarget;
        }
        this.scroll = SelectSetting.clamp(this.scroll, 0.0f, f2);
    }

    private void initScrollToSelected() {
        List<String> list = this.backend.getOptions();
        int n = Math.max(0, list.indexOf(this.backend.getSelected()));
        int n2 = Math.max(0, list.size() - MAX_VISIBLE);
        int n3 = (int) SelectSetting.clamp(n - 3, 0, n2);
        this.scrollTarget = (float) n3 * 12.0f;
        this.scroll = this.scrollTarget;
        this.lastScrollNs = 0L;
    }

    private float dropWidth() {
        float f = 0.0f;
        for (String string : this.backend.getOptions()) {
            f = Math.max(f, Fonts.MONTSERRAT_MEDIUM.width(rtx.nv.api.localization.Lang.translateOption(string), TEXT_SIZE));
        }
        float f2 = f + 18.0f;
        return Math.min(Position.screenWidth() - 16.0f, this.backend.getOptions().size() > MAX_VISIBLE ? f2 + 6.0f : f2);
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
        return !this.isSegmented() && this.open;
    }

    @Override
    public boolean scrollOverlay(float f, float f2, float f3, float f4, float f5, double d) {
        if (this.isSegmented() || !this.open) {
            return false;
        }
        float dropW = this.dropWidth();
        int n = Math.min(this.backend.getOptions().size(), MAX_VISIBLE);
        float dropH = (float) n * 12.0f + 6.0f;
        float dropX = this.getDropX(f, f3, dropW);
        float dropY = this.getDropY(f2, dropH);

        if (f4 < dropX || f4 > dropX + dropW || f5 < dropY || f5 > dropY + dropH) {
            return false;
        }
        float f10 = this.maxScroll();
        if (f10 > 0.0f) {
            this.scrollTarget = SelectSetting.clamp(this.scrollTarget - (float) d * 12.0f, 0.0f, f10);
        }
        return true;
    }

    @Override
    public boolean clickOverlay(float f, float f2, float f3, float f4, float f5) {
        return this.clickOverlay(f, f2, f3, f4, f5, 0);
    }

    @Override
    public boolean clickOverlay(float f, float f2, float f3, float f4, float f5, int button) {
        if (this.isSegmented()) {
            return false;
        }
        float f6 = this.dropAnim.getOutput().floatValue();
        if (!this.open || f6 <= 0.1f) {
            return false;
        }
        List<String> list = this.backend.getOptions();
        float dropW = this.dropWidth();
        int n = Math.min(list.size(), MAX_VISIBLE);
        float dropH = (float) n * 12.0f + 6.0f;

        float dropX = this.getDropX(f, f3, dropW);
        float dropY = this.getDropY(f2, dropH);

        if (f4 < dropX || f4 > dropX + dropW || f5 < dropY + 3.0f || f5 > dropY + dropH - 3.0f) {
            return false;
        }
        if (button == 1) {
            this.closeOverlay(true);
            return true;
        }
        int n2 = (int) Math.floor((f5 - (dropY + 3.0f) + this.scroll) / 12.0f);
        if (n2 >= 0 && n2 < list.size()) {
            this.backend.setSelected(list.get(n2));
            Sounds.play("buttonclick");
            this.closeOverlay(false);
        }
        return true;
    }

    private static float edgeAlpha(float f, float f2, float f3) {
        float f4 = SelectSetting.clamp((f - f2) / 4.0f, 0.0f, 1.0f);
        return Math.min(f4, SelectSetting.clamp((f3 - f) / 4.0f, 0.0f, 1.0f));
    }

    @Override
    public boolean hasOverlay() {
        return !this.isSegmented() && this.dropAnim.getOutput().floatValue() > 0.01f;
    }

    private float maxScroll() {
        return (float) Math.max(0, this.backend.getOptions().size() - MAX_VISIBLE) * 12.0f;
    }

    @Override
    public float preferredWidth() {
        return 6.0f + Fonts.MONTSERRAT_MEDIUM.width(this.backend.getName(), 6.5f) + 6.0f + Fonts.MONTSERRAT_MEDIUM.width(this.backend.getSelected(), 6.0f) + 16.0f + 8.0f;
    }
}

