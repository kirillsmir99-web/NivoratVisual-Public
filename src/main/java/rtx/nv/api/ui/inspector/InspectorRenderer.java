package rtx.nv.api.ui.inspector;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import rtx.nv.api.drags.Position;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.impl.Interface.ClickGui;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.ui.UI;
import rtx.nv.api.ui.module.ModuleListRenderer;
import rtx.nv.api.ui.pin.PinManager;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.settings.SettingsFactory;
import rtx.nv.api.ui.settings.impl.TextSetting;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.api.ui.theme.ThemeManager;
import rtx.nv.utils.animations.Decelerate;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.sounds.Sounds;

public final class InspectorRenderer {
    public static final float INSPECTOR_WIDTH = 195.0f;
    private static final float HEADER_HEIGHT = 44.0f;
    private static final float PADDING_X = 8.0f;
    private static final float WIDGET_GAP = 5.0f;

    private final Decelerate dockAnim = (Decelerate) new Decelerate().setMs(220).setValue(1.0);
    private Module focusedModule = null;
    private List<Setting> widgets = Collections.emptyList();

    private float scroll = 0.0f;
    private float scrollTarget = 0.0f;
    private float totalContentH = 0.0f;
    private boolean isDraggingScroll = false;
    private float dragGrabY = 0.0f;
    private float scrollHoverT = 0.0f;

    private float closeHoverT = 0.0f;
    private float pinHoverT = 0.0f;

    public InspectorRenderer() {
        this.dockAnim.setDirection(Direction.BACKWARDS);
        this.dockAnim.counter.setTime(System.currentTimeMillis() - 10000L);
    }

    public Module getFocusedModule() {
        return this.focusedModule;
    }

    public boolean isOpen() {
        return this.dockAnim.getOutput().floatValue() > 0.005f;
    }

    public float dockProgress() {
        return this.dockAnim.getOutput().floatValue();
    }

    public float dockWidth() {
        return INSPECTOR_WIDTH * this.dockProgress();
    }

    public void open(Module module) {
        if (module == null) {
            return;
        }
        if (this.focusedModule != module) {
            this.focusedModule = module;
            this.widgets = SettingsFactory.build(module);
            this.scroll = 0.0f;
            this.scrollTarget = 0.0f;
        }
        this.dockAnim.setDirection(Direction.FORWARDS);
        try {
            Sounds.play("module_settings_open");
        } catch (Throwable ignored) {
        }
    }

    public void close() {
        if (this.focusedModule == null && this.dockAnim.getDirection() == Direction.BACKWARDS) {
            return;
        }
        TextSetting.unfocusAll();
        this.closeOverlays();
        this.dockAnim.setDirection(Direction.BACKWARDS);
        try {
            Sounds.play("module_settings_close");
        } catch (Throwable ignored) {
        }
    }

    public void closeOverlays() {
        for (Setting setting : this.widgets) {
            if (setting.isOverlayOpen()) {
                setting.closeOverlay(false);
            }
        }
    }

    public void onCategoryChanged(Module stillVisible) {
        if (stillVisible == null || stillVisible != this.focusedModule) {
            this.close();
        }
    }

    public void onSearchChanged(List<Module> filtered) {
        if (this.focusedModule != null && !filtered.contains(this.focusedModule)) {
            this.close();
        }
    }

    private static int rgba(int r, int g, int b, float a) {
        int alpha = Math.max(0, Math.min(255, Math.round(a)));
        return (alpha & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    public void render(DrawContext drawContext, float x, float y, float w, float h, float alpha, float dt) {
        float progress = this.dockProgress();
        if (progress <= 0.005f || this.focusedModule == null) {
            return;
        }

        float effectiveAlpha = alpha * progress;
        float mouseX = Position.mouseX();
        float mouseY = Position.mouseY();

        // 1. Subtle vertical separator rail on the left
        Render2D.rect(x - 3.5f, y + 6.0f, 1.0f, h - 12.0f, 0.5f, rgba(255, 255, 255, 14.0f * effectiveAlpha));

        // 2. Quiet panel background for Inspector
        RenderHelper.drawPanelBg(x, y, w, h, 0.0f, 12.0f, 12.0f, 0.0f, effectiveAlpha);

        // 3. Header rendering
        this.renderHeader(drawContext, x, y, w, effectiveAlpha, mouseX, mouseY, dt);

        // 4. Settings content clipping and layout
        float bodyY = y + HEADER_HEIGHT + 2.0f;
        float bodyH = h - HEADER_HEIGHT - 6.0f;
        float bodyW = w - PADDING_X * 2.0f;
        float bodyX = x + PADDING_X;

        // Smooth scroll interpolation
        float scrollSpeed = 1.0f - (float) Math.exp(-dt * 14.0f);
        this.scroll += (this.scrollTarget - this.scroll) * scrollSpeed;
        if (Math.abs(this.scrollTarget - this.scroll) < 0.05f) {
            this.scroll = this.scrollTarget;
        }

        Render2D.pushScissor(drawContext, bodyX - 2.0f, bodyY, bodyW + 4.0f, bodyH);

        float curY = bodyY - this.scroll;
        for (Setting setting : this.widgets) {
            if (!setting.isVisible()) {
                continue;
            }
            float itemH = setting.height();
            if (curY + itemH >= bodyY - 10.0f && curY <= bodyY + bodyH + 10.0f) {
                try {
                    setting.render(bodyX, curY, bodyW, effectiveAlpha);
                } catch (Throwable t) {
                    rtx.nv.NV.LOGGER.error("Failed to render inspector setting", t);
                }
            }
            curY += itemH + WIDGET_GAP;
        }

        if (this.widgets.isEmpty()) {
            String empty = rtx.nv.api.localization.Lang.get("ui.inspector.no_settings", "Нет доступных настроек");
            float ew = Fonts.MONTSERRAT_MEDIUM.width(empty, 6.0f);
            Fonts.MONTSERRAT_MEDIUM.draw(empty, bodyX + (bodyW - ew) * 0.5f, bodyY + bodyH * 0.5f - 3.0f, 6.0f, rgba(255, 255, 255, 110.0f * effectiveAlpha));
        }

        this.totalContentH = (curY + this.scroll) - bodyY;
        float maxScroll = Math.max(0.0f, this.totalContentH - bodyH);
        this.scrollTarget = Math.max(0.0f, Math.min(maxScroll, this.scrollTarget));
        this.scroll = Math.max(0.0f, Math.min(maxScroll, this.scroll));

        Render2D.popScissor(drawContext);

        // 5. Scrollbar
        this.renderScrollBar(x, bodyY, w, bodyH, maxScroll, effectiveAlpha, mouseX, mouseY, dt);

        // 6. Overlays (dropdowns, color pickers) with top z-index anchored to each setting row
        float overlayY = bodyY - this.scroll;
        for (Setting setting : this.widgets) {
            if (!setting.isVisible()) {
                continue;
            }
            float itemH = setting.height();
            if (setting.hasOverlay() && setting.isOverlayOpen()) {
                try {
                    setting.renderOverlay(drawContext, bodyX, overlayY, bodyW, effectiveAlpha);
                } catch (Throwable t) {
                    rtx.nv.NV.LOGGER.error("Failed to render inspector overlay", t);
                }
            }
            overlayY += itemH + WIDGET_GAP;
        }
    }

    private void renderHeader(DrawContext drawContext, float x, float y, float w, float alpha, float mouseX, float mouseY, float dt) {
        Module module = this.focusedModule;
        if (module == null) {
            return;
        }

        float wordA = UI.getLangWordAlpha();
        float wordY = UI.getLangWordOffsetY();

        // Subtitle category tag
        String categoryName = module.getCategory() != null ? module.getCategory().getDisplayName().toUpperCase() : "MODULE";
        Fonts.MONTSERRAT_MEDIUM.draw(categoryName, x + PADDING_X + 1.0f, y + 7.5f + wordY, 5.0f, rgba(160, 165, 185, 180.0f * alpha * wordA));

        // Module Title
        String title = module.getDisplayName();
        int titleCol = rgba(255, 255, 255, 245.0f * alpha * wordA);
        Fonts.MONTSERRAT_MEDIUM.draw(title, x + PADDING_X + 1.0f, y + 15.5f + wordY, 7.5f, titleCol);

        // Module Description (zero dash rule, clean and readable with fitText)
        String rawDesc = module.getDescription();
        String desc = rawDesc != null ? rawDesc.replace(" — ", " ").replace(" – ", " ").replace(" - ", " ").replace("—", "").replace("–", "") : "";
        float maxDescW = w - PADDING_X - 66.0f;
        desc = RenderHelper.fitText(desc, maxDescW, 5.5f);
        Fonts.MONTSERRAT_MEDIUM.draw(desc, x + PADDING_X + 1.0f, y + 26.5f + wordY, 5.5f, rgba(180, 185, 205, 190.0f * alpha * wordA));

        // Top right controls: Close ('x'), Pin ('L'), Toggle (pill)
        float closeX = x + w - 16.0f;
        float closeY = y + 8.0f;
        boolean closeHover = mouseX >= closeX - 3.0f && mouseX <= closeX + 10.0f && mouseY >= closeY - 3.0f && mouseY <= closeY + 10.0f;
        this.closeHoverT += ((closeHover ? 1.0f : 0.0f) - this.closeHoverT) * (1.0f - (float) Math.exp(-dt * 16.0f));
        int closeCol = rgba(255, 255, 255, (140.0f + 115.0f * this.closeHoverT) * alpha);
        Fonts.NV.msdf(NvIcons.CLOSE, closeX, closeY, 7.0f, closeCol);

        // Pin Button (NvIcons.PINNED with latch animation)
        float pinX = x + w - 32.0f;
        float pinY = y + 7.5f;
        boolean isPinned = PinManager.isPinned(module);
        float latch = PinManager.latchProgress(module.getName());
        boolean pinHover = mouseX >= pinX - 3.0f && mouseX <= pinX + 10.0f && mouseY >= pinY - 3.0f && mouseY <= pinY + 10.0f;
        this.pinHoverT += ((pinHover ? 1.0f : 0.0f) - this.pinHoverT) * (1.0f - (float) Math.exp(-dt * 16.0f));

        float pinAlpha = Math.max(isPinned ? 230.0f : 70.0f, this.pinHoverT * 210.0f) * alpha;
        int pinCol = isPinned ? ClientAccent.accentBright(pinAlpha) : rgba(255, 255, 255, pinAlpha);
        float pinOffsetY = -1.5f * latch * (1.0f - latch) * 4.0f;
        Fonts.NV.msdf(NvIcons.PINNED, pinX, pinY + pinOffsetY, 7.5f, pinCol);

        // Toggle Pill
        float toggleX = x + w - 58.0f;
        float toggleY = y + 6.5f;
        boolean toggleHover = mouseX >= toggleX && mouseX <= toggleX + ModuleListRenderer.TOGGLE_W && mouseY >= toggleY && mouseY <= toggleY + ModuleListRenderer.TOGGLE_H;
        float enableProg = module.isEnabled() ? 1.0f : 0.0f;
        ModuleListRenderer.drawToggleSwitch(toggleX, toggleY, enableProg, alpha, toggleHover);

        // Subtle header bottom separator
        Render2D.rect(x + PADDING_X, y + HEADER_HEIGHT, w - PADDING_X * 2.0f, 0.6f, 0.3f, rgba(255, 255, 255, 14.0f * alpha));
    }

    private void renderScrollBar(float x, float y, float w, float h, float maxScroll, float alpha, float mouseX, float mouseY, float dt) {
        if (maxScroll <= 1.0f) {
            return;
        }

        float trackX = x + w - 3.5f;
        float trackY = y + 2.0f;
        float trackH = h - 4.0f;

        boolean barHover = mouseX >= trackX - 4.0f && mouseX <= trackX + 5.0f && mouseY >= trackY && mouseY <= trackY + trackH;
        this.scrollHoverT += ((barHover || this.isDraggingScroll ? 1.0f : 0.0f) - this.scrollHoverT) * (1.0f - (float) Math.exp(-dt * 14.0f));

        float barVisibleAlpha = (18.0f + 60.0f * this.scrollHoverT) * alpha;
        Render2D.rect(trackX, trackY, 1.25f, trackH, 0.75f, rgba(255, 255, 255, barVisibleAlpha));

        float thumbRatio = Math.max(0.12f, Math.min(1.0f, trackH / (trackH + maxScroll)));
        float thumbH = Math.max(14.0f, trackH * thumbRatio);
        float thumbY = trackY + (trackH - thumbH) * (this.scroll / maxScroll);

        float thumbAlpha = (70.0f + 160.0f * this.scrollHoverT) * alpha;
        Render2D.rect(trackX, thumbY, 1.5f, thumbH, 0.75f, ClientAccent.accent(thumbAlpha));
    }

    public static float getX() {
        return UI.panelX() + 428.0f;
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (!this.isOpen() || this.focusedModule == null) {
            return false;
        }

        float x = getX();
        float y = UI.panelY() + UI.CONTENT_Y_OFFSET;
        float w = INSPECTOR_WIDTH;
        float h = UI.CONTENT_HEIGHT;

        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) {
            return false;
        }

        // 1. Overlay click check (anchored to each setting row)
        float bodyY = y + HEADER_HEIGHT + 2.0f;
        float bodyH = h - HEADER_HEIGHT - 6.0f;
        float bodyW = w - PADDING_X * 2.0f;
        float bodyX = x + PADDING_X;

        Setting openOverlaySetting = null;
        for (Setting setting : this.widgets) {
            if (setting.isVisible() && setting.hasOverlay() && setting.isOverlayOpen()) {
                openOverlaySetting = setting;
                break;
            }
        }
        if (openOverlaySetting != null) {
            float overlayClickY = bodyY - this.scroll;
            for (Setting setting : this.widgets) {
                if (!setting.isVisible()) continue;
                if (setting == openOverlaySetting) {
                    if (openOverlaySetting.clickOverlay(bodyX, overlayClickY, bodyW, mouseX, mouseY, button)) {
                        return true;
                    }
                    // Clicked outside the open overlay: dismiss it cleanly without clicking through to underlying widgets!
                    openOverlaySetting.closeOverlay(true);
                    return true;
                }
                overlayClickY += setting.height() + WIDGET_GAP;
            }
        }

        // 2. Header clicks
        if (mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
            float closeX = x + w - 16.0f;
            float closeY = y + 8.0f;
            if (mouseX >= closeX - 4.0f && mouseX <= closeX + 12.0f && mouseY >= closeY - 4.0f && mouseY <= closeY + 12.0f) {
                this.close();
                return true;
            }

            float pinX = x + w - 32.0f;
            float pinY = y + 7.5f;
            if (mouseX >= pinX - 4.0f && mouseX <= pinX + 12.0f && mouseY >= pinY - 4.0f && mouseY <= pinY + 12.0f) {
                PinManager.toggle(this.focusedModule);
                return true;
            }

            float toggleX = x + w - 58.0f;
            float toggleY = y + 6.5f;
            if (mouseX >= toggleX && mouseX <= toggleX + ModuleListRenderer.TOGGLE_W && mouseY >= toggleY && mouseY <= toggleY + ModuleListRenderer.TOGGLE_H) {
                this.focusedModule.toggle();
                return true;
            }
            return true;
        }

        // 3. Settings items click
        if (mouseY > y + HEADER_HEIGHT && mouseY <= y + h) {
            float curY = bodyY - this.scroll;
            for (Setting setting : this.widgets) {
                if (!setting.isVisible()) {
                    continue;
                }
                float itemH = setting.height();
                if (mouseY >= curY && mouseY <= curY + itemH) {
                    boolean wasOpen = setting.isOverlayOpen();
                    if (setting.click(bodyX, curY, bodyW, mouseX, mouseY, button)) {
                        if (!wasOpen && setting.isOverlayOpen()) {
                            for (Setting other : this.widgets) {
                                if (other != setting && other.hasOverlay() && other.isOverlayOpen()) {
                                    other.closeOverlay(false);
                                }
                            }
                        }
                        return true;
                    }
                }
                curY += itemH + WIDGET_GAP;
            }
        }

        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!this.isOpen() || this.focusedModule == null) {
            return false;
        }

        float x = getX();
        float y = UI.panelY() + UI.CONTENT_Y_OFFSET;
        float w = INSPECTOR_WIDTH;
        float h = UI.CONTENT_HEIGHT;

        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
            // Check overlay scroll
            float bodyY = y + HEADER_HEIGHT + 2.0f;
            float bodyH = h - HEADER_HEIGHT - 6.0f;
            float bodyW = w - PADDING_X * 2.0f;
            float bodyX = x + PADDING_X;

            float overlayScrollY = bodyY - this.scroll;
            for (Setting setting : this.widgets) {
                if (!setting.isVisible()) {
                    continue;
                }
                float itemH = setting.height();
                if (setting.hasOverlay() && setting.isOverlayOpen()) {
                    if (setting.scrollOverlay(bodyX, overlayScrollY, bodyW, (float) mouseX, (float) mouseY, amount)) {
                        return true;
                    }
                }
                overlayScrollY += itemH + WIDGET_GAP;
            }

            float maxScroll = Math.max(0.0f, this.totalContentH - bodyH);
            this.scrollTarget = Math.max(0.0f, Math.min(maxScroll, this.scrollTarget - (float) amount * 18.0f));
            return true;
        }

        return false;
    }

    public void mouseReleased(int button) {
        if (!this.isOpen()) {
            return;
        }
        this.isDraggingScroll = false;
        for (Setting setting : this.widgets) {
            setting.releaseDrag();
        }
    }

    public boolean keyPressed(KeyInput input) {
        if (!this.isOpen() || this.widgets.isEmpty()) {
            return false;
        }
        for (Setting setting : this.widgets) {
            if (setting instanceof rtx.nv.api.ui.settings.impl.BindSetting bindSetting) {
                if (bindSetting.isListening()) {
                    bindSetting.setKey(input.key());
                    return true;
                }
            }
            if (setting instanceof TextSetting textSetting) {
                if (textSetting.isFocused() && textSetting.typeKey(input.key())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean charTyped(CharInput input) {
        if (!this.isOpen() || this.widgets.isEmpty()) {
            return false;
        }
        for (Setting setting : this.widgets) {
            if (setting instanceof TextSetting textSetting) {
                if (textSetting.isFocused()) {
                    textSetting.typeChar(input.codepoint());
                    return true;
                }
            }
        }
        return false;
    }
}
