package rtx.nv.api.ui.theme;

import net.minecraft.client.gui.DrawContext;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.glass.BuiltGlass;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;

/**
 * Authentic 1:1 Scaled Miniature Copy of ClickGUI (LauncherMirrorPreview).
 * Reuses real fonts, icons, Live Wave Edge shader, corner radiuses, and tokens,
 * directly bound to ThemeDraft via PreviewRenderContext without double-alpha bugs.
 * Strictly adheres to the Zero Dash Rule.
 */
public final class LauncherMirrorPreview {

    private static float currentBgModeT = 0.0f;
    private static int prevBgMode = 0;
    private static int targetBgMode = 0;

    private static int rgba(int r, int g, int b, float a) {
        int alpha = Math.max(0, Math.min(255, Math.round(a)));
        return (alpha & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = Math.round(a1 + (a2 - a1) * t);
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void render(DrawContext drawContext, float x, float y, float w, float h, PreviewRenderContext ctx) {
        if (ctx == null || ctx.getAlpha() <= 0.005f) return;

        float alpha = ctx.getAlpha();
        float dt = ctx.getDt();
        int activeMode = ctx.getBgMode();

        // 1. Crossfading Background Modes (0 = World, 1 = Dark, 2 = Light)
        if (activeMode != targetBgMode) {
            prevBgMode = targetBgMode;
            targetBgMode = activeMode;
            currentBgModeT = 0.0f;
        }
        if (currentBgModeT < 1.0f) {
            currentBgModeT = Math.min(1.0f, currentBgModeT + dt * 6.5f);
        }

        renderBackground(x, y, w, h, targetBgMode, prevBgMode, currentBgModeT, alpha);

        // Inner viewport outline
        Render2D.outline(x, y, w, h, 6.0f, 0.6f, rgba(255, 255, 255, (targetBgMode == 2 ? 35.0f : 16.0f) * alpha));

        // 2. Draft Tokens
        int cPrimary = ctx.primaryColor();
        int cSecondary = ctx.secondaryColor();
        int cAccent = ctx.accentColor();
        int cLight = ctx.lightColor();

        ThemeProfile profile = ctx.profile();
        float draftAlpha = ctx.draftAlpha();
        float draftBlur = ctx.draftBlur();
        float draftGlow = ctx.draftGlow();
        float draftRefraction = ctx.draftRefraction();
        float draftEdge = ctx.draftEdgeStrength();
        boolean draftMotion = ctx.draftColorMovement();

        // 3. Mini ClickGUI Geometry
        float availH = h - 8.0f;
        float miniH = Math.min(96.0f, availH);
        float miniW = 268.0f;
        float miniX = x + 6.0f;
        float miniY = y + (h - miniH) * 0.5f;

        // Shell background based on Profile
        int shellBg = ctx.shellBg();
        Render2D.rect(miniX, miniY, miniW, miniH, 5.0f, shellBg);

        // Profile-specific Shell enhancements
        if (profile == ThemeProfile.NEON) {
            Render2D.glow(new BuiltGlow(miniX, miniY, miniW, miniH, new float[]{5.0f, 5.0f, 5.0f, 5.0f}, ctx.accent(255.0f), 0.30f, 4.5f, alpha));
            Render2D.outline(miniX, miniY, miniW, miniH, 5.0f, 0.7f, ctx.accent(160.0f));
        } else {
            Render2D.outline(miniX, miniY, miniW, miniH, 5.0f, 0.6f, rgba(255, 255, 255, 22.0f * alpha));
        }

        // ----------------- MINI SIDEBAR (~16% of mini ClickGUI) -----------------
        float miniSidebarW = 44.0f;
        Render2D.rect(miniX + 1.0f, miniY + 1.0f, miniSidebarW, miniH - 2.0f, 4.0f, 0.0f, 0.0f, 4.0f, rgba(0, 0, 0, 50.0f * alpha));
        Render2D.rect(miniX + miniSidebarW, miniY + 2.0f, 0.6f, miniH - 4.0f, 0.3f, rgba(255, 255, 255, 12.0f * alpha));

        // Mini Logo
        Render2D.image("nv:textures/logo.png", miniX + 4.0f, miniY + 4.0f, 7.0f, 7.0f, rgba(255, 255, 255, 255.0f * alpha));
        Fonts.SMALL_PIXEL.msdf("NV", miniX + 13.0f, miniY + 4.5f, 5.5f, rgba(255, 255, 255, 240.0f * alpha));

        // Categories
        float catStartY = miniY + 16.0f;
        float catRowH = 11.0f;

        // Pinned
        Fonts.NV.msdf(NvIcons.PINNED, miniX + 5.0f, catStartY + 1.5f, 4.5f, rgba(255, 255, 255, 140.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("Закр.", miniX + 12.0f, catStartY + 1.8f, 3.8f, rgba(255, 255, 255, 140.0f * alpha));

        // Visuals (ACTIVE CATEGORY!)
        float cat2Y = catStartY + catRowH;
        Render2D.rect(miniX + 1.5f, cat2Y + 1.5f, 1.5f, 7.0f, 0.75f, ctx.accent(255.0f));
        Render2D.rect(miniX + 3.5f, cat2Y, miniSidebarW - 5.0f, catRowH - 1.0f, 2.5f, ctx.accent(35.0f));
        Fonts.NV.msdf(NvIcons.VISUALS, miniX + 5.0f, cat2Y + 1.5f, 4.5f, ctx.accent(255.0f));
        Fonts.MONTSERRAT_MEDIUM.draw("Визуалы", miniX + 12.0f, cat2Y + 1.8f, 3.8f, 0xFFFFFFFF);

        // Display
        float cat3Y = cat2Y + catRowH;
        Fonts.NV.msdf(NvIcons.DISPLAY, miniX + 5.0f, cat3Y + 1.5f, 4.5f, rgba(255, 255, 255, 140.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("Дисплей", miniX + 12.0f, cat3Y + 1.8f, 3.8f, rgba(255, 255, 255, 140.0f * alpha));

        // Utils
        float cat4Y = cat3Y + catRowH;
        Fonts.NV.msdf(NvIcons.UTILS, miniX + 5.0f, cat4Y + 1.5f, 4.5f, rgba(255, 255, 255, 140.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("Утилиты", miniX + 12.0f, cat4Y + 1.8f, 3.8f, rgba(255, 255, 255, 140.0f * alpha));

        // Themes
        float cat5Y = cat4Y + catRowH;
        Fonts.NV.msdf(NvIcons.THEMES, miniX + 5.0f, cat5Y + 1.5f, 4.5f, rgba(255, 255, 255, 140.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("Темы", miniX + 12.0f, cat5Y + 1.8f, 3.8f, rgba(255, 255, 255, 140.0f * alpha));

        // Bottom system area (About)
        float divY = miniY + miniH - 12.0f;
        Render2D.rect(miniX + 4.0f, divY, miniSidebarW - 8.0f, 0.5f, 0.25f, rgba(255, 255, 255, 14.0f * alpha));
        Fonts.NV.msdf(NvIcons.MORE, miniX + 5.0f, divY + 3.0f, 4.5f, rgba(255, 255, 255, 160.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("О проекте", miniX + 12.0f, divY + 3.2f, 3.8f, rgba(255, 255, 255, 140.0f * alpha));

        // ----------------- MINI MAIN CONTENT (~54% of mini ClickGUI) -----------------
        float contentX = miniX + miniSidebarW + 2.0f;
        float contentW = 148.0f;

        // Mini Header
        Fonts.MONTSERRAT_MEDIUM.draw("Визуалы", contentX + 4.0f, miniY + 3.5f, 4.8f, rgba(255, 255, 255, 235.0f * alpha));

        // Search pill
        float spX = contentX + 38.0f;
        Render2D.rect(spX, miniY + 3.0f, 32.0f, 6.5f, 2.0f, rgba(0, 0, 0, 40.0f * alpha));
        Render2D.outline(spX, miniY + 3.0f, 32.0f, 6.5f, 2.0f, 0.4f, rgba(255, 255, 255, 14.0f * alpha));
        Fonts.NV.msdf(NvIcons.SEARCH, spX + 2.5f, miniY + 4.0f, 3.5f, rgba(255, 255, 255, 160.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("Поиск", spX + 8.5f, miniY + 3.8f, 3.5f, rgba(255, 255, 255, 130.0f * alpha));

        // Profile pill
        float ppX = contentX + contentW - 25.0f;
        Render2D.rect(ppX, miniY + 3.0f, 22.0f, 6.5f, 2.0f, rgba(0, 0, 0, 40.0f * alpha));
        Fonts.NV.msdf(NvIcons.PROFILE, ppX + 2.0f, miniY + 4.0f, 3.5f, ctx.accent(240.0f));
        Fonts.MONTSERRAT_MEDIUM.draw("Virion", ppX + 7.5f, miniY + 3.8f, 3.5f, 0xFFFFFFFF);

        // ----------------- 2x2 MINI MODULE CARDS -----------------
        float cW = (contentW - 9.0f) * 0.5f;
        float cH = (miniH - 18.0f - 4.0f) * 0.5f;

        // Card 1: "Interface" (ACTIVE with authentic BuiltGlass Live Wave Edge!)
        float c1X = contentX + 3.0f;
        float c1Y = miniY + 13.0f;

        BuiltGlass cardGlass = new BuiltGlass(
            c1X, c1Y, cW, cH,
            new float[]{3.0f, 3.0f, 3.0f, 3.0f},
            ctx.primary(75.0f * draftAlpha),
            draftAlpha * alpha,
            draftRefraction * 1.5f,
            ctx.light(110.0f),
            0.25f,
            false,
            0.35f,
            draftRefraction,
            0.0f, 0.0f
        )
        .withBlurRadius(draftBlur)
        .withSecondColor(ctx.secondary(65.0f * draftAlpha), 0.0f)
        .withWaveEdge(
            true,
            1.0f,
            draftEdge,
            0.30f,
            0.42f,
            draftMotion,
            0.20f,
            Math.max(draftGlow, 0.35f),
            17.0f,
            1
        );
        Render2D.glass(cardGlass);

        // Accent glow under active card
        Render2D.glow(new BuiltGlow(c1X, c1Y, cW, cH, new float[]{3.0f, 3.0f, 3.0f, 3.0f}, ctx.accent(255.0f), draftGlow * 0.35f, 3.5f, alpha));
        Render2D.outline(c1X, c1Y, cW, cH, 3.0f, 0.6f, ctx.accent(180.0f));

        Fonts.MONTSERRAT_MEDIUM.draw("Интерфейс", c1X + 4.0f, c1Y + 3.5f, 4.3f, 0xFFFFFFFF);
        Fonts.MONTSERRAT_MEDIUM.draw("Кастомный HUD", c1X + 4.0f, c1Y + 9.5f, 3.2f, rgba(200, 205, 225, 210.0f * alpha));
        Fonts.NV.msdf(NvIcons.SETTINGS, c1X + cW - 7.5f, c1Y + 3.5f, 3.8f, ctx.accent(255.0f));
        Fonts.NV.msdf(NvIcons.PINNED, c1X + cW - 14.5f, c1Y + 3.5f, 3.8f, ctx.accent(255.0f));

        // Card 2: "Target HUD" (DISABLED)
        float c2X = c1X + cW + 3.0f;
        float c2Y = c1Y;
        Render2D.rect(c2X, c2Y, cW, cH, 3.0f, rgba(14, 16, 24, 110.0f * alpha));
        Render2D.outline(c2X, c2Y, cW, cH, 3.0f, 0.5f, rgba(255, 255, 255, 16.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("Target HUD", c2X + 4.0f, c2Y + 3.5f, 4.3f, rgba(255, 255, 255, 170.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("Инфо цели", c2X + 4.0f, c2Y + 9.5f, 3.2f, rgba(160, 165, 185, 160.0f * alpha));
        Fonts.NV.msdf(NvIcons.SETTINGS, c2X + cW - 7.5f, c2Y + 3.5f, 3.8f, rgba(255, 255, 255, 110.0f * alpha));

        // Card 3: "Music HUD" (ACTIVE with accent outline)
        float c3X = c1X;
        float c3Y = c1Y + cH + 3.0f;
        Render2D.rect(c3X, c3Y, cW, cH, 3.0f, ctx.primary(55.0f * draftAlpha));
        Render2D.outline(c3X, c3Y, cW, cH, 3.0f, 0.6f, ctx.accent(150.0f));
        Fonts.MONTSERRAT_MEDIUM.draw("Music HUD", c3X + 4.0f, c3Y + 3.5f, 4.3f, 0xFFFFFFFF);
        Fonts.MONTSERRAT_MEDIUM.draw("Виджет трека", c3X + 4.0f, c3Y + 9.5f, 3.2f, rgba(200, 205, 225, 200.0f * alpha));
        Fonts.NV.msdf(NvIcons.SETTINGS, c3X + cW - 7.5f, c3Y + 3.5f, 3.8f, ctx.accent(240.0f));

        // Card 4: "Click GUI" (DISABLED)
        float c4X = c2X;
        float c4Y = c3Y;
        Render2D.rect(c4X, c4Y, cW, cH, 3.0f, rgba(14, 16, 24, 110.0f * alpha));
        Render2D.outline(c4X, c4Y, cW, cH, 3.0f, 0.5f, rgba(255, 255, 255, 16.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("Click GUI", c4X + 4.0f, c4Y + 3.5f, 4.3f, rgba(255, 255, 255, 170.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("Настройки меню", c4X + 4.0f, c4Y + 9.5f, 3.2f, rgba(160, 165, 185, 160.0f * alpha));

        // ----------------- MINI INSPECTOR DOCK ON RIGHT (~30% of mini ClickGUI) -----------------
        float inspX = contentX + contentW + 2.0f;
        float inspW = miniW - (miniSidebarW + contentW + 4.0f);
        Render2D.rect(inspX - 1.0f, miniY + 3.0f, 0.6f, miniH - 6.0f, 0.3f, rgba(255, 255, 255, 14.0f * alpha));
        Render2D.rect(inspX, miniY + 1.0f, inspW, miniH - 2.0f, 0.0f, 4.0f, 4.0f, 0.0f, rgba(12, 15, 22, 150.0f * alpha));

        // Header
        Fonts.NV.msdf(NvIcons.SETTINGS, inspX + 3.5f, miniY + 4.0f, 3.8f, ctx.accent(240.0f));
        Fonts.MONTSERRAT_MEDIUM.draw("Инспектор", inspX + 9.5f, miniY + 4.0f, 4.0f, 0xFFFFFFFF);
        Render2D.rect(inspX + 3.0f, miniY + 11.5f, inspW - 6.0f, 0.5f, 0.25f, rgba(255, 255, 255, 12.0f * alpha));

        // Setting 1: Toggle Switch "Размытие"
        float set1Y = miniY + 15.0f;
        Fonts.MONTSERRAT_MEDIUM.draw("Размытие", inspX + 4.0f, set1Y + 1.0f, 3.4f, rgba(200, 205, 220, 200.0f * alpha));
        float tX = inspX + inspW - 15.0f;
        Render2D.rect(tX, set1Y, 11.0f, 6.0f, 3.0f, ctx.accent(240.0f));
        Render2D.rect(tX + 5.5f, set1Y + 0.5f, 5.0f, 5.0f, 2.5f, 0xFFFFFFFF);

        // Setting 2: Slider "Сила волн"
        float set2Y = set1Y + 10.0f;
        Fonts.MONTSERRAT_MEDIUM.draw("Сила волн", inspX + 4.0f, set2Y, 3.4f, rgba(200, 205, 220, 200.0f * alpha));
        String valStr = (int) (draftEdge * 100.0f) + "%";
        float vW = Fonts.MONTSERRAT_MEDIUM.width(valStr, 3.2f);
        Fonts.MONTSERRAT_MEDIUM.draw(valStr, inspX + inspW - 4.0f - vW, set2Y, 3.2f, ctx.light(220.0f));

        float slX = inspX + 4.0f;
        float slY = set2Y + 5.5f;
        float slW = inspW - 8.0f;
        Render2D.rect(slX, slY, slW, 2.0f, 1.0f, rgba(255, 255, 255, 18.0f * alpha));
        Render2D.rect(slX, slY, slW * Math.max(0.1f, Math.min(1.0f, draftEdge)), 2.0f, 1.0f, ctx.accent(240.0f));
        Render2D.rect(slX + slW * Math.max(0.1f, Math.min(1.0f, draftEdge)) - 1.25f, slY - 1.0f, 2.5f, 4.0f, 1.0f, 0xFFFFFFFF);

        // Setting 3: Material Indicator
        float set3Y = set2Y + 11.0f;
        Fonts.MONTSERRAT_MEDIUM.draw("Материал", inspX + 4.0f, set3Y, 3.4f, rgba(200, 205, 220, 200.0f * alpha));
        float pW = (inspW - 9.0f) * 0.5f;
        Render2D.rect(slX, set3Y + 5.0f, pW, 8.0f, 2.0f, ctx.accent(40.0f));
        Render2D.outline(slX, set3Y + 5.0f, pW, 8.0f, 2.0f, 0.4f, ctx.accent(180.0f));
        Fonts.MONTSERRAT_MEDIUM.draw("Стекло", slX + 3.0f, set3Y + 6.8f, 3.2f, 0xFFFFFFFF);

        Render2D.rect(slX + pW + 2.0f, set3Y + 5.0f, pW, 8.0f, 2.0f, rgba(0, 0, 0, 30.0f * alpha));
        Render2D.outline(slX + pW + 2.0f, set3Y + 5.0f, pW, 8.0f, 2.0f, 0.4f, rgba(255, 255, 255, 14.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("Матовый", slX + pW + 4.0f, set3Y + 6.8f, 3.2f, rgba(200, 205, 220, 160.0f * alpha));

        // ----------------- 4. RIGHT PART: MINI IN-GAME HUD PREVIEW -----------------
        float hudX = miniX + miniW + 8.0f;
        float hudY = miniY;
        float hudW = Math.max(90.0f, (x + w - 6.0f) - hudX);
        float hudH = miniH;

        Render2D.rect(hudX, hudY, hudW, hudH, 5.0f, rgba(8, 10, 15, 150.0f * alpha));
        Render2D.outline(hudX, hudY, hudW, hudH, 5.0f, 0.5f, rgba(255, 255, 255, 18.0f * alpha));

        // HUD Header
        Fonts.MONTSERRAT_MEDIUM.draw("Игровой HUD", hudX + 6.0f, hudY + 4.0f, 4.3f, rgba(170, 175, 195, 210.0f * alpha));
        Render2D.rect(hudX + hudW - 10.0f, hudY + 5.5f, 3.0f, 3.0f, 1.5f, ctx.accent(255.0f));
        Render2D.glow(new BuiltGlow(hudX + hudW - 10.0f, hudY + 5.5f, 3.0f, 3.0f, new float[]{1.5f, 1.5f, 1.5f, 1.5f}, ctx.accent(255.0f), 0.35f, 2.5f, alpha));

        // HUD Element 1: Mini Watermark with Live Wave Edge!
        float wmX = hudX + 6.0f;
        float wmY = hudY + 12.0f;
        float wmW = Math.min(115.0f, hudW - 12.0f);
        float wmH = 15.0f;

        BuiltGlass wmGlass = new BuiltGlass(
            wmX, wmY, wmW, wmH,
            new float[]{3.0f, 3.0f, 3.0f, 3.0f},
            ctx.primary(70.0f * draftAlpha),
            draftAlpha * alpha,
            draftRefraction,
            ctx.light(100.0f),
            0.25f,
            false,
            0.35f,
            draftRefraction,
            0.0f, 0.0f
        )
        .withBlurRadius(draftBlur)
        .withWaveEdge(
            true,
            1.0f,
            draftEdge,
            0.30f,
            0.42f,
            draftMotion,
            0.20f,
            Math.max(draftGlow, 0.35f),
            55.0f,
            1
        );
        Render2D.glass(wmGlass);

        Fonts.MONTSERRAT_MEDIUM.draw("NIVORAT", wmX + 5.0f, wmY + 4.0f, 4.8f, 0xFFFFFFFF);
        Fonts.MONTSERRAT_MEDIUM.draw("1.21.11", wmX + 34.0f, wmY + 4.2f, 4.0f, ctx.accent(245.0f));
        String fpsStr = "60 FPS";
        float fpsW = Fonts.MONTSERRAT_MEDIUM.width(fpsStr, 3.8f);
        Fonts.MONTSERRAT_MEDIUM.draw(fpsStr, wmX + wmW - fpsW - 4.0f, wmY + 4.5f, 3.8f, rgba(180, 185, 205, 190.0f * alpha));

        // HUD Element 2: Mini Keybinds List
        float kbX = wmX;
        float kbY = wmY + wmH + 4.0f;
        float kbW = wmW;
        float kbH = hudH - 12.0f - wmH - 8.0f;

        Render2D.rect(kbX, kbY, kbW, kbH, 3.0f, rgba(11, 13, 19, 160.0f * alpha));
        Render2D.outline(kbX, kbY, kbW, kbH, 3.0f, 0.5f, rgba(255, 255, 255, 15.0f * alpha));

        // Accent indicator bar on top
        Render2D.rect(kbX + 4.0f, kbY + 1.0f, kbW - 8.0f, 0.7f, 0.35f, ctx.accent(180.0f));

        // Rows (Zero Dash Rule: no dashes in text)
        float r1Y = kbY + 4.0f;
        Fonts.MONTSERRAT_MEDIUM.draw("TargetHUD", kbX + 4.0f, r1Y, 3.5f, rgba(220, 225, 240, 220.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("[M5]", kbX + kbW - 14.0f, r1Y, 3.5f, ctx.accent(240.0f));

        float r2Y = r1Y + 7.5f;
        Fonts.MONTSERRAT_MEDIUM.draw("AutoTotem", kbX + 4.0f, r2Y, 3.5f, rgba(220, 225, 240, 220.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("[V]", kbX + kbW - 11.0f, r2Y, 3.5f, ctx.accent(240.0f));

        float r3Y = r2Y + 7.5f;
        Fonts.MONTSERRAT_MEDIUM.draw("MusicHUD", kbX + 4.0f, r3Y, 3.5f, rgba(220, 225, 240, 220.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("[M]", kbX + kbW - 12.0f, r3Y, 3.5f, ctx.accent(240.0f));
    }

    private static void renderBackground(float x, float y, float w, float h, int targetMode, int prevMode, float t, float alpha) {
        // Mode 0: World, Mode 1: Dark, Mode 2: Light
        int prevC1 = getBgColor1(prevMode, alpha);
        int prevC2 = getBgColor2(prevMode, alpha);
        int prevC3 = getBgColor3(prevMode, alpha);
        int prevC4 = getBgColor4(prevMode, alpha);

        int targetC1 = getBgColor1(targetMode, alpha);
        int targetC2 = getBgColor2(targetMode, alpha);
        int targetC3 = getBgColor3(targetMode, alpha);
        int targetC4 = getBgColor4(targetMode, alpha);

        int c1 = lerpColor(prevC1, targetC1, t);
        int c2 = lerpColor(prevC2, targetC2, t);
        int c3 = lerpColor(prevC3, targetC3, t);
        int c4 = lerpColor(prevC4, targetC4, t);

        Render2D.rect(x, y, w, h, 6.0f, c1, c2, c3, c4);

        // Dot matrix grid
        int dotCol = (targetMode == 2) ? rgba(0, 0, 0, 8.0f * alpha) : rgba(255, 255, 255, 8.0f * alpha);
        for (float gx = x + 16.0f; gx < x + w - 8.0f; gx += 20.0f) {
            for (float gy = y + 12.0f; gy < y + h - 8.0f; gy += 18.0f) {
                Render2D.rect(gx, gy, 1.0f, 1.0f, 0.5f, dotCol);
            }
        }

        // World mode horizon shimmer line
        if (targetMode == 0 || prevMode == 0) {
            float worldT = (targetMode == 0) ? t : (1.0f - t);
            if (worldT > 0.01f) {
                Render2D.rect(x + 4.0f, y + h * 0.45f, w - 8.0f, 1.5f, 0.75f, rgba(255, 255, 255, 14.0f * worldT * alpha));
            }
        }
    }

    private static int getBgColor1(int mode, float alpha) {
        return switch (mode) {
            case 0 -> rgba(12, 16, 24, 190.0f * alpha); // World
            case 1 -> rgba(9, 11, 17, 245.0f * alpha);  // Dark
            case 2 -> rgba(232, 236, 246, 245.0f * alpha); // Light
            default -> rgba(10, 12, 18, 240.0f * alpha);
        };
    }

    private static int getBgColor2(int mode, float alpha) {
        return switch (mode) {
            case 0 -> rgba(20, 26, 38, 190.0f * alpha);
            case 1 -> rgba(16, 20, 30, 245.0f * alpha);
            case 2 -> rgba(220, 225, 238, 245.0f * alpha);
            default -> rgba(16, 20, 30, 240.0f * alpha);
        };
    }

    private static int getBgColor3(int mode, float alpha) {
        return switch (mode) {
            case 0 -> rgba(14, 19, 28, 190.0f * alpha);
            case 1 -> rgba(12, 14, 23, 245.0f * alpha);
            case 2 -> rgba(214, 220, 232, 245.0f * alpha);
            default -> rgba(12, 14, 23, 240.0f * alpha);
        };
    }

    private static int getBgColor4(int mode, float alpha) {
        return switch (mode) {
            case 0 -> rgba(10, 13, 20, 190.0f * alpha);
            case 1 -> rgba(7, 9, 14, 245.0f * alpha);
            case 2 -> rgba(238, 241, 250, 245.0f * alpha);
            default -> rgba(7, 9, 14, 240.0f * alpha);
        };
    }
}
