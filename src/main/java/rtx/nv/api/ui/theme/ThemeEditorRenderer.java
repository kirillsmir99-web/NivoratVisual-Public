package rtx.nv.api.ui.theme;

import java.awt.Color;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rtx.nv.api.config.ConfigManager;
import rtx.nv.api.drags.Position;
import rtx.nv.api.localization.Lang;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.ui.UI;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.utils.animations.Decelerate;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;
import rtx.nv.utils.sounds.Sounds;

/**
 * Right-Attached Theme Inspector for Theme Workspace 4.0 in Nivorat Visual.
 * Integrated into ClickGUI via Adaptive Dock (width 240 px, seamless right border, pushScissor clipping).
 */
public final class ThemeEditorRenderer {
    public static final float INSPECTOR_WIDTH = 240.0f;
    private static final float HEADER_HEIGHT = 32.0f;
    private static final float FOOTER_HEIGHT = 28.0f;
    private static final float PADDING_X = 8.0f;

    private final Decelerate anim = (Decelerate) new Decelerate().setMs(220).setValue(1.0);
    private boolean open = false;

    // Original theme prior to opening editor (used to revert on cancel)
    private ITheme originalTheme;

    // Working theme draft (Single Source of Truth)
    private final ThemeDraft draft = new ThemeDraft();

    // Color picker state
    private float curHue = 0.72f;
    private float curSat = 0.65f;
    private float curVal = 0.85f;
    private boolean saveFailed;
    private float lastDragX = Float.NaN, lastDragY = Float.NaN;
    private int draggingMode = 0; // 0=none, 1=SV, 2=HUE, 3=ALPHA, 4..9=SLIDERS, 10=SCROLLBAR
    private boolean hexFocused = false;
    private String hexInput = "6C72CB";
    private boolean nameFocused = false;

    // Collapsible advanced parameters
    private boolean advancedOpen = false;
    private final Decelerate advancedAnim = (Decelerate) new Decelerate().setMs(220).setValue(1.0);

    // Scroll state
    private float scroll = 0.0f;
    private float scrollTarget = 0.0f;
    private float totalContentH = 0.0f;
    private float lastBodyH = 0.0f;
    private float scrollHoverT = 0.0f;

    // Confirmation dialog for unsaved changes when navigating away
    private boolean pendingLeaveConfirm = false;
    private Category pendingLeaveCategory = null;

    // Tooltip state
    private String pendingTooltipTitle = null;
    private String pendingTooltipDesc = null;
    private int pendingTooltipColor = 0xFFFFFF;

    // Cached coordinates: Fixed Header
    private float lastCloseX, lastCloseY, lastCloseW, lastCloseH;
    private float lastExpX, lastExpY, lastExpW, lastExpH;
    private float lastImpX, lastImpY, lastImpW, lastImpH;

    // Cached coordinates: Fixed Footer
    private float lastCancelX, lastCancelY, lastCancelW, lastCancelH;
    private float lastSaveX, lastSaveY, lastSaveW, lastSaveH;

    // Cached coordinates: Scrollable Content
    private float lastNameX, lastNameY, lastNameW, lastNameH;
    private final float[] lastMatX = new float[4];
    private final float[] lastMatY = new float[4];
    private float matW, matH;
    private final float[] lastColorX = new float[4];
    private final float[] lastColorY = new float[4];
    private float colorCardW, colorCardH;
    private float lastSvX, lastSvY, lastSvW, lastSvH;
    private float lastHueX, lastHueY, lastHueW, lastHueH;
    private float lastAlphaX, lastAlphaY, lastAlphaW, lastAlphaH;
    private float lastHexX, lastHexY, lastHexW, lastHexH;
    private float lastAdvBtnX, lastAdvBtnY, lastAdvBtnW, lastAdvBtnH;
    private final float[] lastSliderX = new float[6];
    private final float[] lastSliderY = new float[6];
    private final float[] lastSliderW = new float[6];
    private final float[] lastSliderH = new float[6];
    private float lastMotionToggleX, lastMotionToggleY, lastMotionToggleW, lastMotionToggleH;

    // Confirmation popup cached coordinates
    private float confX, confY, confW, confH;
    private float confSaveX, confDiscardX, confCancelX, confBtnY, confBtnW, confBtnH;

    private static final ThemeProfile[] MATERIAL_PROFILES = new ThemeProfile[]{
        ThemeProfile.GLASS, ThemeProfile.MATTE, ThemeProfile.VOID, ThemeProfile.NEON
    };

    private static final String[] ROLE_NAMES = new String[]{
        "Основной",
        "Второй",
        "Акцент",
        "Свет"
    };

    private static final String[] ROLE_DESCS = new String[]{
        "Основной цвет стеклянных поверхностей",
        "Используется для переходов и градиентов",
        "Используется для активных элементов и выделения",
        "Используется для бликов и светлых границ"
    };

    public ThemeEditorRenderer() {
        this.anim.setDirection(Direction.BACKWARDS);
        this.anim.counter.setTime(System.currentTimeMillis() - 10000L);
        this.advancedAnim.setDirection(Direction.BACKWARDS);
        this.advancedAnim.counter.setTime(System.currentTimeMillis() - 10000L);
    }

    public boolean isOpen() {
        return this.open || this.dockProgress() > 0.005f;
    }

    public float dockProgress() {
        return this.anim.getOutput().floatValue();
    }

    public float getOpenProgress() {
        return this.dockProgress();
    }

    public float dockWidth() {
        return INSPECTOR_WIDTH * this.dockProgress();
    }

    public boolean isModalOpen() {
        return this.pendingLeaveConfirm;
    }

    public ThemeDraft getDraft() {
        return this.draft;
    }

    public void openNew() {
        if (UI.INSTANCE != null && UI.INSTANCE.getInspector() != null && UI.INSTANCE.getInspector().isOpen()) {
            UI.INSTANCE.getInspector().close();
        }
        this.saveFailed = false;
        this.draggingMode = 0;
        this.nameFocused = false;
        this.hexFocused = false;
        this.lastDragX = Float.NaN;
        this.lastDragY = Float.NaN;
        this.originalTheme = ThemeManager.currentTheme();
        this.draft.resetToNew();
        this.advancedOpen = false;
        this.advancedAnim.setDirection(Direction.BACKWARDS);
        this.pendingLeaveConfirm = false;
        this.pendingLeaveCategory = null;
        this.scroll = 0.0f;
        this.scrollTarget = 0.0f;
        this.syncHsbFromActiveSlot();
        this.open = true;
        this.anim.setDirection(Direction.FORWARDS);
        applyLiveToThemeManager();
        try { Sounds.play("module_settings_open"); } catch (Throwable ignored) {}
    }

    public void openEdit(CustomTheme theme) {
        if (theme == null) return;
        if (UI.INSTANCE != null && UI.INSTANCE.getInspector() != null && UI.INSTANCE.getInspector().isOpen()) {
            UI.INSTANCE.getInspector().close();
        }
        this.saveFailed = false;
        this.draggingMode = 0;
        this.nameFocused = false;
        this.hexFocused = false;
        this.lastDragX = Float.NaN;
        this.lastDragY = Float.NaN;
        this.originalTheme = ThemeManager.currentTheme();
        this.draft.loadFromTheme(theme);
        this.advancedOpen = false;
        this.advancedAnim.setDirection(Direction.BACKWARDS);
        this.pendingLeaveConfirm = false;
        this.pendingLeaveCategory = null;
        this.scroll = 0.0f;
        this.scrollTarget = 0.0f;
        this.syncHsbFromActiveSlot();
        this.open = true;
        this.anim.setDirection(Direction.FORWARDS);
        applyLiveToThemeManager();
        try { Sounds.play("module_settings_open"); } catch (Throwable ignored) {}
    }

    public void applyLiveToThemeManager() {
        if (!this.open) return;
        ThemeManager.setLiveOverride(this.draft.toLiveTheme());
    }

    public void cancelEdit() {
        ThemeManager.clearLiveOverride();
        if (this.originalTheme != null) {
            ThemeManager.set(this.originalTheme);
        }
        this.close();
    }

    public void close() {
        ThemeManager.clearLiveOverride();
        this.open = false;
        this.nameFocused = false;
        this.hexFocused = false;
        this.draggingMode = 0;
        this.pendingLeaveConfirm = false;
        this.anim.setDirection(Direction.BACKWARDS);
        try { Sounds.play("module_settings_close"); } catch (Throwable ignored) {}
    }

    public void promptLeaveCategory(Category target) {
        if (!this.draft.isDirty()) {
            this.cancelEdit();
            if (target != null && UI.INSTANCE != null) {
                UI.INSTANCE.selectCategoryFromWorkspace(target);
            }
            return;
        }
        this.pendingLeaveCategory = target;
        this.pendingLeaveConfirm = true;
        try { Sounds.play("click"); } catch (Throwable ignored) {}
    }

    private void syncHsbFromActiveSlot() {
        int col = this.draft.getActiveColor() & 0xFFFFFF;
        int r = (col >> 16) & 0xFF;
        int g = (col >> 8) & 0xFF;
        int b = col & 0xFF;
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        this.curHue = hsb[0];
        this.curSat = hsb[1];
        this.curVal = hsb[2];
        this.hexInput = String.format(Locale.ROOT, "%06X", col);
    }

    private void updateColorFromHsb() {
        int rgb = Color.HSBtoRGB(this.curHue, this.curSat, this.curVal) & 0xFFFFFF;
        this.draft.setActiveColor(rgb);
        this.hexInput = String.format(Locale.ROOT, "%06X", rgb);
        applyLiveToThemeManager();
    }

    private static int rgba(int r, int g, int b, float a) {
        int alpha = Math.max(0, Math.min(255, Math.round(a)));
        return (alpha & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    /**
     * Renders the Right-Attached Theme Inspector docked to ClickGUI.
     */
    public void renderInspector(DrawContext drawContext, float x, float y, float w, float h, float alpha, float dt) {
        float progress = this.dockProgress();
        if (progress <= 0.005f) return;

        float effectiveAlpha = alpha * progress;
        float mouseX = Position.mouseX();
        float mouseY = Position.mouseY();
        this.pendingTooltipTitle = null;
        this.pendingTooltipDesc = null;

        handleContinuousDragging(mouseX, mouseY);

        // 1. Subtle vertical separator rail on the left
        Render2D.rect(x - 3.5f, y + 6.0f, 1.0f, h - 12.0f, 0.5f, rgba(255, 255, 255, 14.0f * effectiveAlpha));

        // 2. Quiet panel background for Theme Inspector
        RenderHelper.drawPanelBg(x, y, w, h, 0.0f, 12.0f, 12.0f, 0.0f, effectiveAlpha);

        // 3. Fixed Header
        renderFixedHeader(x, y, w, effectiveAlpha, mouseX, mouseY);

        // 4. Fixed Footer
        float footerY = y + h - FOOTER_HEIGHT;
        renderFixedFooter(x, footerY, w, FOOTER_HEIGHT, effectiveAlpha, mouseX, mouseY);

        // 5. Scrollable Content Container
        float bodyY = y + HEADER_HEIGHT + 2.0f;
        float bodyH = h - HEADER_HEIGHT - FOOTER_HEIGHT - 4.0f;
        float bodyW = w - PADDING_X * 2.0f;
        float bodyX = x + PADDING_X;
        this.lastBodyH = bodyH;

        // Smooth scroll interpolation
        float scrollSpeed = 1.0f - (float) Math.exp(-dt * 14.0f);
        this.scroll += (this.scrollTarget - this.scroll) * scrollSpeed;
        if (Math.abs(this.scrollTarget - this.scroll) < 0.05f) {
            this.scroll = this.scrollTarget;
        }

        Render2D.pushScissor(drawContext, bodyX - 2.0f, bodyY, bodyW + 4.0f, bodyH);
        renderScrollableContent(drawContext, bodyX, bodyY, bodyW, bodyH, effectiveAlpha, mouseX, mouseY, dt);
        Render2D.popScissor(drawContext);

        // 6. Scrollbar
        float maxScroll = Math.max(0.0f, this.totalContentH - bodyH);
        renderScrollBar(x, bodyY, w, bodyH, maxScroll, effectiveAlpha, mouseX, mouseY, dt);
    }

    private void renderFixedHeader(float x, float y, float w, float alpha, float mouseX, float mouseY) {
        String title = this.draft.getEditingTheme() != null
            ? Lang.get("theme.workspace.title_edit", "Редактирование темы")
            : Lang.get("theme.workspace.title_new", "Создание темы");

        // Theme Icon & Title
        Fonts.NV.msdf(NvIcons.THEMES, x + PADDING_X + 2.0f, y + 8.5f, 6.5f, ClientAccent.accentBright(240.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw(title, x + PADDING_X + 12.0f, y + 7.5f, 6.5f, rgba(255, 255, 255, 245.0f * alpha));

        // Right action buttons: Close, Export, Import
        this.lastCloseW = 12.0f;
        this.lastCloseH = 12.0f;
        this.lastCloseX = x + w - PADDING_X - this.lastCloseW;
        this.lastCloseY = y + 7.0f;
        boolean closeHov = mouseX >= lastCloseX - 2.0f && mouseX <= lastCloseX + lastCloseW + 2.0f && mouseY >= lastCloseY - 2.0f && mouseY <= lastCloseY + lastCloseH + 2.0f;
        int closeCol = rgba(255, 255, 255, (closeHov ? 255.0f : 140.0f) * alpha);
        Fonts.NV.msdf(NvIcons.CLOSE, lastCloseX + 2.5f, lastCloseY + 2.5f, 6.0f, closeCol);

        this.lastExpW = 12.0f;
        this.lastExpH = 12.0f;
        this.lastExpX = lastCloseX - 16.0f;
        this.lastExpY = y + 7.0f;
        boolean expHov = mouseX >= lastExpX - 2.0f && mouseX <= lastExpX + lastExpW + 2.0f && mouseY >= lastExpY - 2.0f && mouseY <= lastExpY + lastExpH + 2.0f;
        int expCol = rgba(255, 255, 255, (expHov ? 255.0f : 150.0f) * alpha);
        Fonts.NV.msdf(NvIcons.EXPORT, lastExpX + 2.5f, lastExpY + 2.5f, 5.5f, expCol);

        this.lastImpW = 12.0f;
        this.lastImpH = 12.0f;
        this.lastImpX = lastExpX - 16.0f;
        this.lastImpY = y + 7.0f;
        boolean impHov = mouseX >= lastImpX - 2.0f && mouseX <= lastImpX + lastImpW + 2.0f && mouseY >= lastImpY - 2.0f && mouseY <= lastImpY + lastImpH + 2.0f;
        int impCol = rgba(255, 255, 255, (impHov ? 255.0f : 150.0f) * alpha);
        Fonts.NV.msdf(NvIcons.IMPORT, lastImpX + 2.5f, lastImpY + 2.5f, 5.5f, impCol);

        if (expHov) {
            this.pendingTooltipTitle = Lang.get("theme.workspace.export", "Экспорт");
            this.pendingTooltipDesc = "Скопировать палитру в буфер обмена";
            this.pendingTooltipColor = 0xFFFFFF;
        } else if (impHov) {
            this.pendingTooltipTitle = Lang.get("theme.workspace.import", "Импорт");
            this.pendingTooltipDesc = "Вставить палитру из буфера обмена";
            this.pendingTooltipColor = 0xFFFFFF;
        }

        // Header bottom divider
        Render2D.rect(x + PADDING_X, y + HEADER_HEIGHT - 1.0f, w - PADDING_X * 2.0f, 0.6f, 0.3f, rgba(255, 255, 255, 14.0f * alpha));
    }

    private void renderFixedFooter(float x, float footerY, float w, float footerH, float alpha, float mouseX, float mouseY) {
        // Divider above footer
        Render2D.rect(x + PADDING_X, footerY, w - PADDING_X * 2.0f, 0.6f, 0.3f, rgba(255, 255, 255, 14.0f * alpha));

        float btnH = 19.0f;
        float btnY = footerY + (footerH - btnH) * 0.5f;
        float gap = 6.0f;
        float totalBtnW = w - PADDING_X * 2.0f;
        float btnW = (totalBtnW - gap) * 0.5f;

        this.lastCancelX = x + PADDING_X;
        this.lastCancelY = btnY;
        this.lastCancelW = btnW;
        this.lastCancelH = btnH;

        this.lastSaveX = this.lastCancelX + btnW + gap;
        this.lastSaveY = btnY;
        this.lastSaveW = btnW;
        this.lastSaveH = btnH;

        boolean cancHov = mouseX >= lastCancelX && mouseX <= lastCancelX + lastCancelW && mouseY >= lastCancelY && mouseY <= lastCancelY + lastCancelH;
        boolean saveHov = mouseX >= lastSaveX && mouseX <= lastSaveX + lastSaveW && mouseY >= lastSaveY && mouseY <= lastSaveY + lastSaveH;

        // 1. Cancel Button (Launcher glassmorphism style)
        int cancBg = rgba(16, 20, 30, (cancHov ? 200.0f : 140.0f) * alpha);
        Render2D.rect(lastCancelX, btnY, btnW, btnH, 4.0f, cancBg);
        int cancStroke = rgba(255, 255, 255, (cancHov ? 45.0f : 18.0f) * alpha);
        Render2D.outline(lastCancelX, btnY, btnW, btnH, 4.0f, 0.6f, cancStroke);

        String cancelStr = Lang.get("theme.workspace.cancel", "Отмена");
        float cIconSize = 5.0f;
        float cTextW = Fonts.MONTSERRAT_MEDIUM.width(cancelStr, 5.5f);
        float cTotalW = cIconSize + 4.0f + cTextW;
        float cStart = lastCancelX + (btnW - cTotalW) * 0.5f;

        Fonts.NV.msdf(NvIcons.CLOSE, cStart, btnY + (btnH - cIconSize) * 0.5f + 0.5f, cIconSize, rgba(255, 255, 255, (cancHov ? 255.0f : 170.0f) * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw(cancelStr, cStart + cIconSize + 4.0f, btnY + (btnH - 5.5f) * 0.5f + 0.5f, 5.5f, rgba(255, 255, 255, (cancHov ? 255.0f : 190.0f) * alpha));

        // 2. Save Button (Launcher accent button style)
        int sCol = ClientAccent.accent((saveHov ? 245.0f : 200.0f) * alpha);
        Render2D.rect(lastSaveX, btnY, btnW, btnH, 4.0f, sCol);
        Render2D.outline(lastSaveX, btnY, btnW, btnH, 4.0f, 0.6f, rgba(255, 255, 255, (saveHov ? 60.0f : 30.0f) * alpha));
        Render2D.rect(lastSaveX + 2.0f, btnY + 1.5f, btnW - 4.0f, 2.5f, 1.25f, rgba(255, 255, 255, (saveHov ? 35.0f : 18.0f) * alpha));
        Render2D.glow(new BuiltGlow(lastSaveX, btnY, btnW, btnH, new float[]{4.0f, 4.0f, 4.0f, 4.0f}, ClientAccent.accent(255.0f), 0.35f, 4.0f, (saveHov ? 0.9f : 0.45f) * alpha));

        String saveStr = saveFailed ? "Ошибка · повторить" : this.draft.getEditingTheme() == null ? "Создать тему" : this.draft.isDirty() ? "Сохранить" : "Сохранено";
        float sIconSize = 5.0f;
        float sTextW = Fonts.MONTSERRAT_MEDIUM.width(saveStr, 5.5f);
        float sTotalW = sIconSize + 4.0f + sTextW;
        float sStart = lastSaveX + (btnW - sTotalW) * 0.5f;

        Fonts.NV.msdf(NvIcons.CHECK, sStart, btnY + (btnH - sIconSize) * 0.5f + 0.5f, sIconSize, 0xFFFFFFFF);
        Fonts.MONTSERRAT_MEDIUM.draw(saveStr, sStart + sIconSize + 4.0f, btnY + (btnH - 5.5f) * 0.5f + 0.5f, 5.5f, 0xFFFFFFFF);
    }

    private void renderScrollableContent(DrawContext drawContext, float bodyX, float bodyY, float bodyW, float bodyH, float alpha, float mouseX, float mouseY, float dt) {
        float curY = bodyY - this.scroll;

        // ------------------ a) NAME INPUT FIELD ------------------
        Fonts.MONTSERRAT_MEDIUM.draw(Lang.get("theme.workspace.name_label", "Название темы"), bodyX, curY + 1.0f, 5.5f, rgba(160, 165, 185, 190.0f * alpha));
        curY += 8.5f;

        float nameH = 15.0f;
        this.lastNameX = bodyX;
        this.lastNameY = curY;
        this.lastNameW = bodyW;
        this.lastNameH = nameH;

        boolean inpHov = mouseX >= lastNameX && mouseX <= lastNameX + lastNameW && mouseY >= lastNameY && mouseY <= lastNameY + lastNameH;
        Render2D.rect(lastNameX, lastNameY, lastNameW, lastNameH, 3.0f, rgba(0, 0, 0, (nameFocused ? 80.0f : 50.0f) * alpha));
        Render2D.outline(lastNameX, lastNameY, lastNameW, lastNameH, 3.0f, 0.6f, nameFocused ? ClientAccent.accent(210.0f * alpha) : rgba(255, 255, 255, (inpHov ? 35.0f : 18.0f) * alpha));

        String dispName = this.draft.getName().isEmpty() ? Lang.get("theme.workspace.name_placeholder", "Новая тема") : this.draft.getName();
        int txtCol = this.draft.getName().isEmpty() ? rgba(255, 255, 255, 90.0f * alpha) : rgba(255, 255, 255, 240.0f * alpha);
        Fonts.MONTSERRAT_MEDIUM.draw(dispName, lastNameX + 5.0f, lastNameY + 3.8f, 5.5f, txtCol);
        if (this.nameFocused && System.currentTimeMillis() / 450L % 2L == 0L) {
            float cursorX = lastNameX + 5.0f + (this.draft.getName().isEmpty() ? 0.0f : Fonts.MONTSERRAT_MEDIUM.width(dispName, 5.5f)) + 1.0f;
            Render2D.rect(cursorX, lastNameY + 3.0f, 0.8f, 9.0f, 0.0f, ClientAccent.accent(230.0f * alpha));
        }
        curY += nameH + 8.0f;

        // ------------------ b) MATERIAL PROFILE SELECTOR ------------------
        Fonts.MONTSERRAT_MEDIUM.draw(Lang.get("theme.workspace.material", "Материал"), bodyX, curY + 1.0f, 5.5f, rgba(160, 165, 185, 190.0f * alpha));
        curY += 8.5f;

        float matBtnW = (bodyW - 4.0f) * 0.5f;
        float matBtnH = 22.0f;
        this.matW = matBtnW;
        this.matH = matBtnH;

        for (int m = 0; m < MATERIAL_PROFILES.length; ++m) {
            ThemeProfile prof = MATERIAL_PROFILES[m];
            int col = m % 2;
            int row = m / 2;
            float mx = bodyX + (float) col * (matBtnW + 4.0f);
            float my = curY + (float) row * (matBtnH + 4.0f);
            this.lastMatX[m] = mx;
            this.lastMatY[m] = my;

            boolean sel = this.draft.getProfile() == prof;
            boolean hov = mouseX >= mx && mouseX <= mx + matBtnW && mouseY >= my && mouseY <= my + matBtnH;

            int bgCol = switch (prof.id) {
                case "matte" -> rgba(31, 34, 42, (sel ? 190.0f : 120.0f) * alpha);
                case "void" -> rgba(6, 7, 11, (sel ? 230.0f : 170.0f) * alpha);
                case "neon" -> rgba(20, 14, 38, (sel ? 200.0f : 140.0f) * alpha);
                default -> rgba(18, 21, 30, (sel ? 160.0f : 100.0f) * alpha);
            };
            Render2D.rect(mx, my, matBtnW, matBtnH, 3.5f, bgCol);
            if (prof == ThemeProfile.GLASS) {
                Render2D.rect(mx + 2.0f, my + 2.0f, matBtnW - 4.0f, 4.0f, 1.5f, rgba(255, 255, 255, 12.0f * alpha));
            } else if (prof == ThemeProfile.NEON) {
                Render2D.glow(new BuiltGlow(mx, my, matBtnW, matBtnH, new float[]{3.5f, 3.5f, 3.5f, 3.5f}, ClientAccent.accent(255.0f), 0.30f, 3.5f, (sel ? 0.8f : 0.4f) * alpha));
            }

            int strokeCol = sel ? ClientAccent.accent(220.0f * alpha) : rgba(255, 255, 255, (hov ? 40.0f : 18.0f) * alpha);
            Render2D.outline(mx, my, matBtnW, matBtnH, 3.5f, sel ? 0.8f : 0.5f, strokeCol);

            String profName = Lang.get("theme.editor.profile." + prof.id, prof.displayName);
            Fonts.MONTSERRAT_MEDIUM.draw(profName, mx + 6.0f, my + 6.5f, 5.5f, rgba(255, 255, 255, (sel ? 255.0f : 180.0f) * alpha));
            if (sel) {
                Fonts.NV.msdf(NvIcons.CHECK, mx + matBtnW - 10.0f, my + 6.5f, 5.0f, ClientAccent.accentBright(240.0f * alpha));
            }

            if (hov) {
                this.pendingTooltipTitle = profName;
                this.pendingTooltipDesc = switch (prof.id) {
                    case "matte" -> "Плотный бархатистый матовый материал с глубокой тенью";
                    case "void" -> "Сверхтемная обсидиановая поверхность с глубоким контрастом";
                    case "neon" -> "Яркий материал с насыщенным контурным свечением";
                    default -> "Классическое полупрозрачное стекло с мягким бликом";
                };
                this.pendingTooltipColor = sel ? ClientAccent.accent(255.0f) : 0xFFFFFF;
            }
        }
        curY += (matBtnH * 2.0f) + 4.0f + 8.0f;

        // ------------------ c) 4 SEMANTIC COLOR ROWS + INLINE COLOR PICKER ------------------
        Fonts.MONTSERRAT_MEDIUM.draw(Lang.get("theme.workspace.palette", "Цвета темы"), bodyX, curY + 1.0f, 5.5f, rgba(160, 165, 185, 190.0f * alpha));
        curY += 8.5f;

        float colorRowH = 20.0f;
        this.colorCardW = bodyW;
        this.colorCardH = colorRowH;

        int[] palette = this.draft.getPalette();
        for (int c = 0; c < 4; ++c) {
            float cx = bodyX;
            float cy = curY;
            this.lastColorX[c] = cx;
            this.lastColorY[c] = cy;

            boolean sel = this.draft.getActiveSlot() == c;
            boolean hov = mouseX >= cx && mouseX <= cx + bodyW && mouseY >= cy && mouseY <= cy + colorRowH;

            int cardBg = rgba(16, 19, 27, (sel ? 180.0f : (hov ? 120.0f : 80.0f)) * alpha);
            Render2D.rect(cx, cy, bodyW, colorRowH, 3.5f, cardBg);
            int strokeCol = sel ? ClientAccent.accent(220.0f * alpha) : rgba(255, 255, 255, (hov ? 35.0f : 16.0f) * alpha);
            Render2D.outline(cx, cy, bodyW, colorRowH, 3.5f, sel ? 0.8f : 0.5f, strokeCol);

            // Color Swatch
            int curCol = (c < palette.length) ? palette[c] : 0x6C72CB;
            Render2D.rect(cx + 4.0f, cy + 3.5f, 13.0f, 13.0f, 2.5f, ThemeManager.rgba(curCol, 255.0f * alpha));
            Render2D.outline(cx + 4.0f, cy + 3.5f, 13.0f, 13.0f, 2.5f, 0.5f, rgba(255, 255, 255, 40.0f * alpha));

            // Role Name
            String rName = ROLE_NAMES[c];
            Fonts.MONTSERRAT_MEDIUM.draw(rName, cx + 22.0f, cy + 3.5f, 5.5f, rgba(255, 255, 255, (sel ? 255.0f : 200.0f) * alpha));

            // Hex Code
            String hexStr = "#" + String.format(Locale.ROOT, "%06X", curCol & 0xFFFFFF);
            Fonts.MONTSERRAT_MEDIUM.draw(hexStr, cx + 22.0f, cy + 10.5f, 4.8f, ClientAccent.accentSoft(210.0f * alpha));

            // Active indicator
            if (sel) {
                Render2D.rect(cx + bodyW - 3.0f, cy + 3.0f, 1.8f, colorRowH - 6.0f, 0.9f, ClientAccent.accentBright(240.0f * alpha));
                Fonts.NV.msdf(NvIcons.CHECK, cx + bodyW - 12.0f, cy + 5.5f, 5.0f, ClientAccent.accentBright(240.0f * alpha));
            }

            if (hov) {
                this.pendingTooltipTitle = rName;
                this.pendingTooltipDesc = ROLE_DESCS[c];
                this.pendingTooltipColor = curCol;
            }

            curY += colorRowH + 3.0f;

            // Render compact GPU Color Picker directly beneath the active color row!
            if (sel) {
                curY = renderInlineColorPicker(bodyX, curY, bodyW, alpha, mouseX, mouseY);
                curY += 4.0f;
            }
        }

        // ------------------ d) COLLAPSIBLE ADVANCED PARAMETERS SECTION ------------------
        curY += 3.0f;
        this.lastAdvBtnX = bodyX;
        this.lastAdvBtnY = curY;
        this.lastAdvBtnW = bodyW;
        this.lastAdvBtnH = 16.0f;

        boolean advHov = mouseX >= lastAdvBtnX && mouseX <= lastAdvBtnX + lastAdvBtnW && mouseY >= lastAdvBtnY && mouseY <= lastAdvBtnY + lastAdvBtnH;
        Render2D.rect(lastAdvBtnX, lastAdvBtnY, lastAdvBtnW, lastAdvBtnH, 3.0f, rgba(0, 0, 0, (advHov ? 60.0f : 35.0f) * alpha));
        Render2D.outline(lastAdvBtnX, lastAdvBtnY, lastAdvBtnW, lastAdvBtnH, 3.0f, 0.5f, this.advancedOpen ? ClientAccent.accent(180.0f * alpha) : rgba(255, 255, 255, 18.0f * alpha));

        Fonts.NV.msdf(NvIcons.SETTINGS, lastAdvBtnX + 5.0f, lastAdvBtnY + 4.5f, 5.5f, ClientAccent.accentSoft(210.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw(Lang.get("theme.workspace.advanced", "Дополнительно"), lastAdvBtnX + 16.0f, lastAdvBtnY + 4.5f, 5.5f, rgba(255, 255, 255, 210.0f * alpha));
        Fonts.NV.msdf(this.advancedOpen ? NvIcons.CLOSE : NvIcons.CHEVRON_DOWN, lastAdvBtnX + lastAdvBtnW - 11.0f, lastAdvBtnY + 5.0f, 5.0f, rgba(255, 255, 255, 160.0f * alpha));

        curY += lastAdvBtnH + 4.0f;

        if (this.advancedOpen) {
            curY = renderAdvancedSliders(bodyX, curY, bodyW, alpha, mouseX, mouseY);
        }

        this.totalContentH = (curY + this.scroll) - bodyY;
        float maxScroll = Math.max(0.0f, this.totalContentH - bodyH);
        this.scrollTarget = Math.max(0.0f, Math.min(maxScroll, this.scrollTarget));
        this.scroll = Math.max(0.0f, Math.min(maxScroll, this.scroll));
    }

    private float renderInlineColorPicker(float bodyX, float curY, float bodyW, float alpha, float mouseX, float mouseY) {
        // 1. SV 2D Gradient Box (GPU Quads)
        this.lastSvX = bodyX;
        this.lastSvY = curY;
        this.lastSvW = bodyW;
        this.lastSvH = 92.0f;

        int cTL = ColorUtil.rgba(255, 255, 255, Math.round(255.0f * alpha));
        int cTR = ThemeManager.rgba(Color.HSBtoRGB(this.curHue, 1.0f, 1.0f), 255.0f * alpha);
        int cBR = ThemeManager.rgba(0x000000, 255.0f * alpha);
        int cBL = ThemeManager.rgba(0x000000, 255.0f * alpha);
        Render2D.rect(lastSvX, lastSvY, lastSvW, lastSvH, 3.5f, cTL, cTR, cBR, cBL);
        Render2D.outline(lastSvX, lastSvY, lastSvW, lastSvH, 3.5f, 0.5f, rgba(255, 255, 255, 25.0f * alpha));

        // SV Draggable Selector Ring
        float selX = lastSvX + this.curSat * lastSvW;
        float selY = lastSvY + (1.0f - this.curVal) * lastSvH;
        Render2D.rect(selX - 3.5f, selY - 3.5f, 7.0f, 7.0f, 3.5f, rgba(0, 0, 0, 160.0f * alpha));
        Render2D.outline(selX - 3.0f, selY - 3.0f, 6.0f, 6.0f, 3.0f, 0.8f, rgba(255, 255, 255, 255.0f * alpha));

        curY += lastSvH + 4.0f;

        // 2. Horizontal Hue Strip
        this.lastHueX = bodyX;
        this.lastHueY = curY;
        this.lastHueW = bodyW;
        this.lastHueH = 8.0f;

        int HUE_STOPS = 6;
        float segW = lastHueW / (float) HUE_STOPS;
        for (int i = 0; i < HUE_STOPS; i++) {
            float h1 = (float) i / (float) HUE_STOPS;
            float h2 = (float) (i + 1) / (float) HUE_STOPS;
            int col1 = ThemeManager.rgba(Color.HSBtoRGB(h1, 1.0f, 1.0f), 255.0f * alpha);
            int col2 = ThemeManager.rgba(Color.HSBtoRGB(h2, 1.0f, 1.0f), 255.0f * alpha);
            float sx = lastHueX + (float) i * segW;
            float r = (i == 0 || i == HUE_STOPS - 1) ? 2.0f : 0.0f;
            Render2D.rect(sx, lastHueY, segW + 0.5f, lastHueH, r, col1, col2, col2, col1);
        }
        Render2D.outline(lastHueX, lastHueY, lastHueW, lastHueH, 2.5f, 0.5f, rgba(255, 255, 255, 25.0f * alpha));

        // Hue Selector Thumb
        float hueSelX = lastHueX + this.curHue * lastHueW;
        Render2D.rect(hueSelX - 1.5f, lastHueY - 1.0f, 3.0f, lastHueH + 2.0f, 1.5f, rgba(255, 255, 255, 255.0f * alpha));

        curY += lastHueH + 4.0f;

        // 3. Horizontal Alpha Strip (Procedural Checkerboard + Gradient)
        this.lastAlphaX = bodyX;
        this.lastAlphaY = curY;
        this.lastAlphaW = bodyW;
        this.lastAlphaH = 8.0f;

        float chk = 3.0f;
        int aCols = (int) Math.ceil(lastAlphaW / chk);
        int aRows = (int) Math.ceil(lastAlphaH / chk);
        Render2D.rect(lastAlphaX, lastAlphaY, lastAlphaW, lastAlphaH, 2.5f, rgba(200, 200, 200, 255.0f * alpha));
        for (int r = 0; r < aRows; r++) {
            for (int c = 0; c < aCols; c++) {
                if ((r + c) % 2 == 1) {
                    float cx = lastAlphaX + (float) c * chk;
                    float cy = lastAlphaY + (float) r * chk;
                    float cw = Math.min(chk, lastAlphaX + lastAlphaW - cx);
                    float ch = Math.min(chk, lastAlphaY + lastAlphaH - cy);
                    Render2D.rect(cx, cy, cw, ch, 0.0f, rgba(130, 130, 130, 255.0f * alpha));
                }
            }
        }
        int curRgb = this.draft.getActiveColor();
        int transCol = ThemeManager.rgba(curRgb, 0.0f);
        int opaqCol = ThemeManager.rgba(curRgb, 255.0f * alpha);
        Render2D.rect(lastAlphaX, lastAlphaY, lastAlphaW, lastAlphaH, 2.5f, transCol, opaqCol, opaqCol, transCol);
        Render2D.outline(lastAlphaX, lastAlphaY, lastAlphaW, lastAlphaH, 2.5f, 0.5f, rgba(255, 255, 255, 25.0f * alpha));

        // Alpha Selector Thumb
        float aNorm = (float) this.draft.getActiveAlpha() / 255.0f;
        float aSelX = lastAlphaX + aNorm * lastAlphaW;
        Render2D.rect(aSelX - 1.5f, lastAlphaY - 1.0f, 3.0f, lastAlphaH + 2.0f, 1.5f, rgba(255, 255, 255, 255.0f * alpha));

        curY += lastAlphaH + 4.0f;

        // 4. HEX Input Field & RGBA Readout
        this.lastHexX = bodyX;
        this.lastHexY = curY;
        this.lastHexW = 58.0f;
        this.lastHexH = 14.0f;

        boolean hexHov = mouseX >= lastHexX && mouseX <= lastHexX + lastHexW && mouseY >= lastHexY && mouseY <= lastHexY + lastHexH;
        Render2D.rect(lastHexX, lastHexY, lastHexW, lastHexH, 3.0f, rgba(0, 0, 0, (hexFocused ? 80.0f : 50.0f) * alpha));
        Render2D.outline(lastHexX, lastHexY, lastHexW, lastHexH, 3.0f, 0.6f, hexFocused ? ClientAccent.accent(210.0f * alpha) : rgba(255, 255, 255, (hexHov ? 35.0f : 18.0f) * alpha));
        String dispHex = "#" + this.hexInput;
        Fonts.MONTSERRAT_MEDIUM.draw(dispHex, lastHexX + 4.0f, lastHexY + 3.2f, 5.5f, rgba(255, 255, 255, 230.0f * alpha));

        // RGBA Readout (Strict Zero Dash compliant)
        int r = (curRgb >> 16) & 0xFF;
        int g = (curRgb >> 8) & 0xFF;
        int b = curRgb & 0xFF;
        int a = this.draft.getActiveAlpha();
        String rgbaStr = String.format(Locale.ROOT, "R:%d G:%d B:%d A:%d", r, g, b, a);
        Fonts.MONTSERRAT_MEDIUM.draw(rgbaStr, lastHexX + lastHexW + 6.0f, lastHexY + 3.5f, 5.0f, rgba(160, 165, 185, 190.0f * alpha));

        curY += lastHexH;
        return curY;
    }

    private float renderAdvancedSliders(float bodyX, float curY, float bodyW, float alpha, float mouseX, float mouseY) {
        String[] titles = new String[]{
            "Прозрачность",
            "Размытие",
            "Свечение",
            "Преломление",
            "Сила контура",
            "Угол градиента"
        };
        float[] values = new float[]{
            this.draft.getAlpha(),
            this.draft.getBlur() / 32.0f,
            this.draft.getGlow(),
            this.draft.getRefraction(),
            this.draft.getEdgeStrength(),
            this.draft.getGradientAngle() / 360.0f
        };
        String[] displayVals = new String[]{
            (int)(this.draft.getAlpha() * 100.0f) + "%",
            (int)this.draft.getBlur() + " px",
            (int)(this.draft.getGlow() * 100.0f) + "%",
            (int)(this.draft.getRefraction() * 100.0f) + "%",
            (int)(this.draft.getEdgeStrength() * 100.0f) + "%",
            (int)this.draft.getGradientAngle() + "°"
        };

        for (int i = 0; i < 6; i++) {
            Fonts.MONTSERRAT_MEDIUM.draw(titles[i], bodyX, curY + 1.0f, 5.0f, rgba(160, 165, 185, 190.0f * alpha));
            float vw = Fonts.MONTSERRAT_MEDIUM.width(displayVals[i], 4.8f);
            Fonts.MONTSERRAT_MEDIUM.draw(displayVals[i], bodyX + bodyW - vw, curY + 1.0f, 4.8f, ClientAccent.accentSoft(210.0f * alpha));

            float slY = curY + 8.5f;
            float slH = 3.5f;
            this.lastSliderX[i] = bodyX;
            this.lastSliderY[i] = slY;
            this.lastSliderW[i] = bodyW;
            this.lastSliderH[i] = slH;

            // Track
            Render2D.rect(bodyX, slY, bodyW, slH, 1.75f, rgba(255, 255, 255, 22.0f * alpha));

            // Fill
            float fillW = bodyW * Math.max(0.0f, Math.min(1.0f, values[i]));
            Render2D.rect(bodyX, slY, fillW, slH, 1.75f, ClientAccent.accent(220.0f * alpha));

            // Thumb
            float thumbX = bodyX + fillW;
            Render2D.rect(thumbX - 2.5f, slY - 1.5f, 5.0f, 6.5f, 2.5f, rgba(255, 255, 255, 255.0f * alpha));

            curY += 16.0f;
        }

        // Color Motion toggle switch
        Fonts.MONTSERRAT_MEDIUM.draw("Динамика цвета", bodyX, curY + 2.0f, 5.0f, rgba(160, 165, 185, 190.0f * alpha));
        this.lastMotionToggleW = 16.0f;
        this.lastMotionToggleH = 9.0f;
        this.lastMotionToggleX = bodyX + bodyW - this.lastMotionToggleW;
        this.lastMotionToggleY = curY + 1.0f;

        boolean motion = this.draft.isColorMovement();
        if (motion) {
            Render2D.rect(lastMotionToggleX, lastMotionToggleY, lastMotionToggleW, lastMotionToggleH, 4.5f, ClientAccent.accent(230.0f * alpha));
            Render2D.rect(lastMotionToggleX + lastMotionToggleW - 7.5f, lastMotionToggleY + 1.0f, 7.0f, 7.0f, 3.5f, rgba(255, 255, 255, 255.0f * alpha));
        } else {
            Render2D.rect(lastMotionToggleX, lastMotionToggleY, lastMotionToggleW, lastMotionToggleH, 4.5f, rgba(255, 255, 255, 25.0f * alpha));
            Render2D.rect(lastMotionToggleX + 1.0f, lastMotionToggleY + 1.0f, 7.0f, 7.0f, 3.5f, rgba(255, 255, 255, 180.0f * alpha));
        }

        curY += 14.0f;
        return curY;
    }

    private void renderScrollBar(float x, float y, float w, float h, float maxScroll, float alpha, float mouseX, float mouseY, float dt) {
        if (maxScroll <= 1.0f) return;

        float trackX = x + w - 3.5f;
        float trackY = y + 2.0f;
        float trackH = h - 4.0f;

        boolean barHover = mouseX >= trackX - 4.0f && mouseX <= trackX + 5.0f && mouseY >= trackY && mouseY <= trackY + trackH;
        this.scrollHoverT += ((barHover || this.draggingMode == 10 ? 1.0f : 0.0f) - this.scrollHoverT) * (1.0f - (float) Math.exp(-dt * 14.0f));

        float barVisibleAlpha = (18.0f + 60.0f * this.scrollHoverT) * alpha;
        Render2D.rect(trackX, trackY, 1.25f, trackH, 0.75f, rgba(255, 255, 255, barVisibleAlpha));

        float thumbRatio = Math.max(0.12f, Math.min(1.0f, trackH / (trackH + maxScroll)));
        float thumbH = Math.max(14.0f, trackH * thumbRatio);
        float thumbY = trackY + (trackH - thumbH) * (this.scroll / maxScroll);

        float thumbAlpha = (70.0f + 160.0f * this.scrollHoverT) * alpha;
        Render2D.rect(trackX, thumbY, 1.5f, thumbH, 0.75f, ClientAccent.accent(thumbAlpha));
    }

    /**
     * Renders overlays: subtle focus dimming (0-8%), unsaved changes confirmation modal, and tooltips.
     */
    public void renderOverlays(DrawContext drawContext, float alpha) {
        float prog = this.dockProgress();
        if (prog <= 0.004f && !this.open) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();

        // Subtle focus tint (0-8% dimming, exactly 5% tint):
        if (prog > 0.01f) {
            int dimCol = rgba(5, 7, 10, 13.0f * prog * alpha);
            Render2D.rect(0.0f, 0.0f, screenW, screenH, 0.0f, dimCol);
        }

        // Render confirmation dialog for unsaved changes if active
        if (this.pendingLeaveConfirm) {
            float mouseX = Position.mouseX();
            float mouseY = Position.mouseY();
            renderConfirmModal(drawContext, screenW, screenH, alpha, mouseX, mouseY);
        }

        // Render pending tooltip
        if (this.pendingTooltipTitle != null && alpha > 0.1f) {
            float mouseX = Position.mouseX();
            float mouseY = Position.mouseY();
            renderTooltip(this.pendingTooltipTitle, this.pendingTooltipDesc, mouseX, mouseY, alpha);
        }
    }

    public void render(DrawContext drawContext, float x, float y, float w, float h, float alpha, float dt) {
        renderOverlays(drawContext, alpha);
    }

    private void renderTooltip(String title, String desc, float mouseX, float mouseY, float alpha) {
        float tipW = Math.max(Fonts.MONTSERRAT_MEDIUM.width(title, 6.0f), Fonts.MONTSERRAT_MEDIUM.width(desc, 5.0f)) + 14.0f;
        float tipH = 22.0f;
        float tx = Math.min(Position.screenWidth() - tipW - 4.0f, mouseX + 8.0f);
        float ty = Math.min(Position.screenHeight() - tipH - 4.0f, mouseY + 8.0f);

        Render2D.rect(tx, ty, tipW, tipH, 4.0f, rgba(6, 8, 14, 235.0f * alpha));
        Render2D.outline(tx, ty, tipW, tipH, 4.0f, 0.6f, ThemeManager.rgba(this.pendingTooltipColor, 180.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw(title, tx + 6.0f, ty + 3.5f, 6.0f, 0xFFFFFFFF);
        Fonts.MONTSERRAT_MEDIUM.draw(desc, tx + 6.0f, ty + 11.5f, 5.0f, rgba(200, 200, 200, 210.0f * alpha));
    }

    private void renderConfirmModal(DrawContext drawContext, float screenW, float screenH, float alpha, float mouseX, float mouseY) {
        Render2D.rect(0.0f, 0.0f, screenW, screenH, 0.0f, rgba(0, 0, 0, 140.0f * alpha));

        this.confW = 200.0f;
        this.confH = 78.0f;
        this.confX = (screenW - confW) * 0.5f;
        this.confY = (screenH - confH) * 0.5f;

        Render2D.rect(confX, confY, confW, confH, 6.0f, rgba(12, 15, 23, 245.0f * alpha));
        Render2D.outline(confX, confY, confW, confH, 6.0f, 0.7f, ClientAccent.accent(180.0f * alpha));

        Fonts.MONTSERRAT_MEDIUM.draw(Lang.get("theme.workspace.confirm.title", "Сохранить изменения?"), confX + 12.0f, confY + 12.0f, 7.5f, 0xFFFFFFFF);
        Fonts.MONTSERRAT_MEDIUM.draw(Lang.get("theme.workspace.confirm.desc", "В теме есть несохраненные параметры"), confX + 12.0f, confY + 25.0f, 5.5f, rgba(170, 170, 170, 220.0f * alpha));

        this.confBtnW = 54.0f;
        this.confBtnH = 17.0f;
        this.confBtnY = confY + confH - confBtnH - 10.0f;
        this.confSaveX = confX + confW - 10.0f - confBtnW;
        this.confDiscardX = confSaveX - 5.0f - confBtnW;
        this.confCancelX = confDiscardX - 5.0f - confBtnW;

        boolean saveHov = mouseX >= confSaveX && mouseX <= confSaveX + confBtnW && mouseY >= confBtnY && mouseY <= confBtnY + confBtnH;
        boolean discHov = mouseX >= confDiscardX && mouseX <= confDiscardX + confBtnW && mouseY >= confBtnY && mouseY <= confBtnY + confBtnH;
        boolean cancHov = mouseX >= confCancelX && mouseX <= confCancelX + confBtnW && mouseY >= confBtnY && mouseY <= confBtnY + confBtnH;

        // Cancel
        Render2D.rect(confCancelX, confBtnY, confBtnW, confBtnH, 4.0f, rgba(20, 24, 34, (cancHov ? 180.0f : 110.0f) * alpha));
        Render2D.outline(confCancelX, confBtnY, confBtnW, confBtnH, 4.0f, 0.6f, rgba(255, 255, 255, (cancHov ? 45.0f : 18.0f) * alpha));
        String confCancStr = Lang.get("theme.workspace.confirm.cancel", "Отмена");
        float ccw = Fonts.MONTSERRAT_MEDIUM.width(confCancStr, 5.0f);
        Fonts.MONTSERRAT_MEDIUM.draw(confCancStr, confCancelX + (confBtnW - ccw) * 0.5f, confBtnY + 4.5f, 5.0f, rgba(255, 255, 255, (cancHov ? 255.0f : 180.0f) * alpha));

        // Discard
        Render2D.rect(confDiscardX, confBtnY, confBtnW, confBtnH, 4.0f, rgba(20, 24, 34, (discHov ? 180.0f : 110.0f) * alpha));
        Render2D.outline(confDiscardX, confBtnY, confBtnW, confBtnH, 4.0f, 0.6f, rgba(255, 255, 255, (discHov ? 45.0f : 18.0f) * alpha));
        String confDiscStr = Lang.get("theme.workspace.confirm.discard", "Не сохранять");
        float cdw = Fonts.MONTSERRAT_MEDIUM.width(confDiscStr, 5.0f);
        Fonts.MONTSERRAT_MEDIUM.draw(confDiscStr, confDiscardX + (confBtnW - cdw) * 0.5f, confBtnY + 4.5f, 5.0f, rgba(255, 255, 255, (discHov ? 255.0f : 180.0f) * alpha));

        // Save
        Render2D.rect(confSaveX, confBtnY, confBtnW, confBtnH, 4.0f, ClientAccent.accent((saveHov ? 245.0f : 200.0f) * alpha));
        Render2D.outline(confSaveX, confBtnY, confBtnW, confBtnH, 4.0f, 0.6f, rgba(255, 255, 255, (saveHov ? 60.0f : 30.0f) * alpha));
        Render2D.glow(new BuiltGlow(confSaveX, confBtnY, confBtnW, confBtnH, new float[]{4.0f, 4.0f, 4.0f, 4.0f}, ClientAccent.accent(255.0f), 0.35f, 4.0f, (saveHov ? 0.85f : 0.4f) * alpha));
        String confSaveStr = Lang.get("theme.workspace.confirm.save", "Сохранить");
        float csw = Fonts.MONTSERRAT_MEDIUM.width(confSaveStr, 5.0f);
        Fonts.MONTSERRAT_MEDIUM.draw(confSaveStr, confSaveX + (confBtnW - csw) * 0.5f, confBtnY + 4.5f, 5.0f, 0xFFFFFFFF);
    }

    private void handleContinuousDragging(float mouseX, float mouseY) {
        if (this.draggingMode == 0 || !Float.isFinite(mouseX) || !Float.isFinite(mouseY)) return;
        if (mouseX == lastDragX && mouseY == lastDragY) return;
        lastDragX = mouseX; lastDragY = mouseY;
        boolean changed = false;
        if (this.draggingMode == 1 || this.draggingMode == 2) {
            int oldColor = this.draft.getActiveColor();
            if (this.draggingMode == 1 && lastSvW > 0 && lastSvH > 0) {
                this.curSat = clampUnit((mouseX - lastSvX) / lastSvW);
                this.curVal = clampUnit(1f - (mouseY - lastSvY) / lastSvH);
            } else if (this.draggingMode == 2 && lastHueW > 0) {
                this.curHue = clampUnit((mouseX - lastHueX) / lastHueW);
            }
            int rgb = Color.HSBtoRGB(curHue, curSat, curVal) & 0xFFFFFF;
            if ((oldColor & 0xFFFFFF) != rgb) {
                draft.setActiveColor(rgb);
                hexInput = String.format(Locale.ROOT, "%06X", rgb);
                changed = true;
            }
        } else if (this.draggingMode == 3 && lastAlphaW > 0) {
            int alpha = Math.round(clampUnit((mouseX - lastAlphaX) / lastAlphaW) * 255f);
            if (alpha != draft.getActiveAlpha()) { draft.setActiveAlpha(alpha); changed = true; }
        } else if (this.draggingMode >= 4 && this.draggingMode <= 9) {
            int index = draggingMode - 4;
            if (lastSliderW[index] <= 0) return;
            float normalized = clampUnit((mouseX - lastSliderX[index]) / lastSliderW[index]);
            float previous = switch (index) {
                case 0 -> draft.getAlpha(); case 1 -> draft.getBlur() / 32f;
                case 2 -> draft.getGlow(); case 3 -> draft.getRefraction();
                case 4 -> draft.getEdgeStrength(); default -> draft.getGradientAngle() / 360f;
            };
            if (Math.abs(previous - normalized) > .00001f) {
                switch (index) {
                    case 0 -> draft.setAlpha(normalized); case 1 -> draft.setBlur(normalized * 32f);
                    case 2 -> draft.setGlow(normalized); case 3 -> draft.setRefraction(normalized);
                    case 4 -> draft.setEdgeStrength(normalized); case 5 -> draft.setGradientAngle(normalized * 360f);
                }
                changed = true;
            }
        }
        if (changed) { applyLiveToThemeManager(); Sounds.play("slider"); }
    }

    private static float clampUnit(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!isOpen() || button != 0 || draggingMode == 0 || pendingLeaveConfirm) return false;
        handleContinuousDragging((float) mouseX, (float) mouseY);
        return true;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!isOpen() || button != 0) return false;
        lastDragX = Float.NaN; lastDragY = Float.NaN;
        float mouseX = (float) mx;
        float mouseY = (float) my;

        // 1. Confirmation Modal
        if (this.pendingLeaveConfirm) {
            if (mouseX >= confSaveX && mouseX <= confSaveX + confBtnW && mouseY >= confBtnY && mouseY <= confBtnY + confBtnH) {
                if (!saveTheme()) return true;
                this.pendingLeaveConfirm = false;
                Category tgt = this.pendingLeaveCategory;
                this.pendingLeaveCategory = null;
                this.close();
                if (tgt != null && UI.INSTANCE != null) UI.INSTANCE.selectCategoryFromWorkspace(tgt);
                return true;
            }
            if (mouseX >= confDiscardX && mouseX <= confDiscardX + confBtnW && mouseY >= confBtnY && mouseY <= confBtnY + confBtnH) {
                this.pendingLeaveConfirm = false;
                Category tgt = this.pendingLeaveCategory;
                this.pendingLeaveCategory = null;
                this.cancelEdit();
                if (tgt != null && UI.INSTANCE != null) UI.INSTANCE.selectCategoryFromWorkspace(tgt);
                return true;
            }
            if (mouseX >= confCancelX && mouseX <= confCancelX + confBtnW && mouseY >= confBtnY && mouseY <= confBtnY + confBtnH) {
                this.pendingLeaveConfirm = false;
                this.pendingLeaveCategory = null;
                try { Sounds.play("click"); } catch (Throwable ignored) {}
                return true;
            }
            return true; // Eat click inside modal backdrop
        }

        float x = UI.panelX() + UI.PANEL_W;
        float y = UI.panelY() + UI.CONTENT_Y_OFFSET;
        float w = INSPECTOR_WIDTH;
        float h = UI.CONTENT_HEIGHT;

        // Check if click is inside inspector bounds
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) {
            return false;
        }

        // 2. Fixed Header Clicks
        if (mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
            // Close button
            if (mouseX >= lastCloseX - 3.0f && mouseX <= lastCloseX + lastCloseW + 3.0f && mouseY >= lastCloseY - 3.0f && mouseY <= lastCloseY + lastCloseH + 3.0f) {
                if (this.draft.isDirty()) {
                    promptLeaveCategory(null);
                } else {
                    cancelEdit();
                }
                return true;
            }

            // Export button
            if (mouseX >= lastExpX - 3.0f && mouseX <= lastExpX + lastExpW + 3.0f && mouseY >= lastExpY - 3.0f && mouseY <= lastExpY + lastExpH + 3.0f) {
                exportToClipboard();
                return true;
            }

            // Import button
            if (mouseX >= lastImpX - 3.0f && mouseX <= lastImpX + lastImpW + 3.0f && mouseY >= lastImpY - 3.0f && mouseY <= lastImpY + lastImpH + 3.0f) {
                importFromClipboard();
                return true;
            }
            return true;
        }

        // 3. Fixed Footer Clicks
        if (mouseY >= y + h - FOOTER_HEIGHT) {
            // Cancel button
            if (mouseX >= lastCancelX && mouseX <= lastCancelX + lastCancelW && mouseY >= lastCancelY && mouseY <= lastCancelY + lastCancelH) {
                if (this.draft.isDirty()) {
                    promptLeaveCategory(null);
                } else {
                    cancelEdit();
                }
                return true;
            }

            // Save button
            if (mouseX >= lastSaveX && mouseX <= lastSaveX + lastSaveW && mouseY >= lastSaveY && mouseY <= lastSaveY + lastSaveH) {
                if (saveTheme()) close();
                return true;
            }
            return true;
        }

        // 4. Scrollable Body Clicks
        // Name field focus
        if (mouseX >= lastNameX && mouseX <= lastNameX + lastNameW && mouseY >= lastNameY && mouseY <= lastNameY + lastNameH) {
            this.nameFocused = !this.nameFocused;
            this.hexFocused = false;
            return true;
        } else if (this.nameFocused) {
            this.nameFocused = false;
        }

        // Material cards
        for (int m = 0; m < MATERIAL_PROFILES.length; ++m) {
            if (mouseX >= lastMatX[m] && mouseX <= lastMatX[m] + matW && mouseY >= lastMatY[m] && mouseY <= lastMatY[m] + matH) {
                this.draft.setProfile(MATERIAL_PROFILES[m]);
                applyLiveToThemeManager();
                try { Sounds.play("click"); } catch (Throwable ignored) {}
                return true;
            }
        }

        // Color rows
        for (int c = 0; c < 4; ++c) {
            if (mouseX >= lastColorX[c] && mouseX <= lastColorX[c] + colorCardW && mouseY >= lastColorY[c] && mouseY <= lastColorY[c] + colorCardH) {
                this.draft.setActiveSlot(c);
                syncHsbFromActiveSlot();
                try { Sounds.play("click"); } catch (Throwable ignored) {}
                return true;
            }
        }

        // SV Field click / drag
        if (mouseX >= lastSvX && mouseX <= lastSvX + lastSvW && mouseY >= lastSvY && mouseY <= lastSvY + lastSvH) {
            this.draggingMode = 1;
            this.curSat = Math.max(0.0f, Math.min(1.0f, (mouseX - lastSvX) / lastSvW));
            this.curVal = Math.max(0.0f, Math.min(1.0f, 1.0f - (mouseY - lastSvY) / lastSvH));
            updateColorFromHsb();
            return true;
        }

        // Hue horizontal strip click / drag
        if (mouseX >= lastHueX && mouseX <= lastHueX + lastHueW && mouseY >= lastHueY - 2.0f && mouseY <= lastHueY + lastHueH + 2.0f) {
            this.draggingMode = 2;
            this.curHue = Math.max(0.0f, Math.min(1.0f, (mouseX - lastHueX) / lastHueW));
            updateColorFromHsb();
            return true;
        }

        // Alpha horizontal strip click / drag
        if (mouseX >= lastAlphaX && mouseX <= lastAlphaX + lastAlphaW && mouseY >= lastAlphaY - 2.0f && mouseY <= lastAlphaY + lastAlphaH + 2.0f) {
            this.draggingMode = 3;
            float aNorm = Math.max(0.0f, Math.min(1.0f, (mouseX - lastAlphaX) / lastAlphaW));
            this.draft.setActiveAlpha(Math.round(aNorm * 255.0f));
            applyLiveToThemeManager();
            return true;
        }

        // Hex input focus
        if (mouseX >= lastHexX && mouseX <= lastHexX + lastHexW && mouseY >= lastHexY && mouseY <= lastHexY + lastHexH) {
            this.hexFocused = !this.hexFocused;
            this.nameFocused = false;
            return true;
        } else if (this.hexFocused) {
            this.hexFocused = false;
        }

        // Advanced foldout button
        if (mouseX >= lastAdvBtnX && mouseX <= lastAdvBtnX + lastAdvBtnW && mouseY >= lastAdvBtnY && mouseY <= lastAdvBtnY + lastAdvBtnH) {
            this.advancedOpen = !this.advancedOpen;
            this.advancedAnim.setDirection(this.advancedOpen ? Direction.FORWARDS : Direction.BACKWARDS);
            try { Sounds.play("click"); } catch (Throwable ignored) {}
            return true;
        }

        // Advanced sliders
        if (this.advancedOpen) {
            for (int i = 0; i < 6; i++) {
                if (mouseX >= lastSliderX[i] && mouseX <= lastSliderX[i] + lastSliderW[i] && mouseY >= lastSliderY[i] - 3.0f && mouseY <= lastSliderY[i] + lastSliderH[i] + 3.0f) {
                    this.draggingMode = 4 + i;
                    float val = Math.max(0.0f, Math.min(1.0f, (mouseX - lastSliderX[i]) / lastSliderW[i]));
                    switch (i) {
                        case 0 -> this.draft.setAlpha(val);
                        case 1 -> this.draft.setBlur(val * 32.0f);
                        case 2 -> this.draft.setGlow(val);
                        case 3 -> this.draft.setRefraction(val);
                        case 4 -> this.draft.setEdgeStrength(val);
                        case 5 -> this.draft.setGradientAngle(val * 360.0f);
                    }
                    applyLiveToThemeManager();
                    return true;
                }
            }

            // Motion toggle
            if (mouseX >= lastMotionToggleX && mouseX <= lastMotionToggleX + lastMotionToggleW && mouseY >= lastMotionToggleY && mouseY <= lastMotionToggleY + lastMotionToggleH) {
                this.draft.setColorMovement(!this.draft.isColorMovement());
                applyLiveToThemeManager();
                try { Sounds.play("click"); } catch (Throwable ignored) {}
                return true;
            }
        }

        return true;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) this.draggingMode = 0;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isOpen()) return false;
        float x = UI.panelX() + UI.PANEL_W;
        float y = UI.panelY() + UI.CONTENT_Y_OFFSET;
        float w = INSPECTOR_WIDTH;
        float h = UI.CONTENT_HEIGHT;

        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
            // Independent scrolling: never scroll container while dragging color picker or sliders
            if (this.draggingMode != 0) {
                return true;
            }
            float maxScroll = Math.max(0.0f, this.totalContentH - this.lastBodyH);
            this.scrollTarget = Math.max(0.0f, Math.min(maxScroll, this.scrollTarget - (float) amount * 18.0f));
            return true;
        }
        return false;
    }

    public boolean keyPressed(int key) {
        if (!isOpen()) return false;

        if (this.pendingLeaveConfirm) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                this.pendingLeaveConfirm = false;
                this.pendingLeaveCategory = null;
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (!saveTheme()) return true;
                this.pendingLeaveConfirm = false;
                Category tgt = this.pendingLeaveCategory;
                this.pendingLeaveCategory = null;
                this.close();
                if (tgt != null && UI.INSTANCE != null) UI.INSTANCE.selectCategoryFromWorkspace(tgt);
                return true;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (this.nameFocused) {
                this.nameFocused = false;
                return true;
            }
            if (this.hexFocused) {
                this.hexFocused = false;
                return true;
            }
            if (this.draft.isDirty()) {
                promptLeaveCategory(null);
            } else {
                close();
            }
            return true;
        }

        if (this.nameFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !this.draft.getName().isEmpty()) {
                this.draft.setName(this.draft.getName().substring(0, this.draft.getName().length() - 1));
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                this.nameFocused = false;
                return true;
            }
            return true;
        }

        if (this.hexFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !this.hexInput.isEmpty()) {
                this.hexInput = this.hexInput.substring(0, this.hexInput.length() - 1);
                applyHexInput();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                this.hexFocused = false;
                return true;
            }
            return true;
        }

        return false;
    }

    public boolean charTyped(char c) {
        if (!isOpen()) return false;

        if (this.nameFocused) {
            if (this.draft.getName().length() < 24 && !Character.isISOControl(c)) {
                this.draft.setName(this.draft.getName() + c);
            }
            return true;
        }

        if (this.hexFocused) {
            if (this.hexInput.length() < 6) {
                char upper = Character.toUpperCase(c);
                if ((upper >= '0' && upper <= '9') || (upper >= 'A' && upper <= 'F')) {
                    this.hexInput += upper;
                    applyHexInput();
                }
            }
            return true;
        }

        return false;
    }

    private void applyHexInput() {
        if (this.hexInput.length() == 6) {
            try {
                int col = (int) Long.parseLong(this.hexInput, 16);
                this.draft.setActiveColor(col);
                int r = (col >> 16) & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = col & 0xFF;
                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                this.curHue = hsb[0];
                this.curSat = hsb[1];
                this.curVal = hsb[2];
                applyLiveToThemeManager();
            } catch (Throwable ignored) {}
        }
    }

    private boolean saveTheme() {
        String finalName = this.draft.getName().trim();
        if (finalName.isEmpty()) {
            finalName = Lang.get("theme.workspace.name_placeholder", "Новая тема");
        }

        CustomTheme previous = this.draft.getEditingTheme();
        CustomTheme live = (CustomTheme) this.draft.toLiveTheme();
        String id = previous != null ? previous.id() : "custom_" + java.util.UUID.randomUUID();
        CustomTheme target = new CustomTheme(id, finalName, live.profile(), live.palette());
        target.setPaletteAlpha(live.paletteAlpha());
        if (!CustomThemeManager.save(target)) {
            this.saveFailed = true;
            Sounds.play("command_error");
            return false;
        }

        this.saveFailed = false;
        ThemeManager.clearLiveOverride();
        ThemeManager.set(target);
        InterfaceModule im = InterfaceModule.getInstance();
        if (im != null) {
            im.clientColorMode.setSelected(InterfaceModule.CLIENT_COLOR_PALETTE);
        }

        this.draft.setDirty(false);
        this.draft.setEditingTheme(target);
        ConfigManager.markDirty();
        try { Sounds.play("select_category"); } catch (Throwable ignored) {}
        return true;
    }

    private void importFromClipboard() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            String clip = mc.keyboard.getClipboard();
            if (clip == null || clip.isBlank()) return;

            clip = clip.trim().replace("#", "").replace("0x", "").replace(" ", "");
            String[] parts = clip.split("[,;]");
            int[] newPal = this.draft.getPalette().clone();
            int idx = 0;
            for (String p : parts) {
                if (p.length() >= 6 && idx < 4) {
                    try {
                        newPal[idx++] = (int) Long.parseLong(p.substring(0, 6), 16);
                    } catch (Throwable ignored) {}
                }
            }
            if (idx > 0) {
                this.draft.setPalette(newPal);
                syncHsbFromActiveSlot();
                applyLiveToThemeManager();
                Sounds.play("click");
            }
        } catch (Throwable ignored) {}
    }

    private void exportToClipboard() {
        try {
            int[] pal = this.draft.getPalette();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(4, pal.length); i++) {
                if (i > 0) sb.append(", ");
                sb.append(String.format(Locale.ROOT, "#%06X", pal[i] & 0xFFFFFF));
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.keyboard.setClipboard(sb.toString());
            Sounds.play("click");
        } catch (Throwable ignored) {}
    }
}
