package rtx.nv.api.ui.theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import rtx.nv.api.config.ConfigManager;
import rtx.nv.api.drags.Position;
import rtx.nv.api.localization.Lang;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.ui.UI;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.animations.Decelerate;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.sounds.Sounds;

public final class ThemesRenderer {
    private static final float FADE_OUT_DURATION = 0.15f;
    private static final int ROW_FADE_MS = 380;
    private static final int QUICK_ROW_FADE_MS = 320;
    private static final float SLIDE_PX = 6.0f;
    private static final float CARD_H = 44.0f;
    private static final float CONTENT_Y_OFFSET = 5.0f;
    private static final float CONTENT_HEIGHT = 280.0f;
    private static final float HEADER_BAR_H = 20.0f;

    private final ThemeEditorRenderer editor = new ThemeEditorRenderer();
    private final Map<String, Decelerate> selectAnims = new HashMap<>();
    private final Map<String, Float> hoverAnims = new HashMap<>();
    private final Map<Integer, Decelerate> rowAppearAnims = new HashMap<>();
    private int appearFadeMs = 320;
    private long appearBaseMs;
    private boolean appearInitialFrame;
    private boolean transitioning = false;
    private float fadeOutTime = 0.0f;
    private float scroll;
    private float scrollTarget;
    private float contentH;
    public static final int MAX_BLUR_CARDS = 32;
    private final float[] cardBlurRects = new float[192];
    private int cardBlurCount;
    private float cardBlurMaxPhase;
    private boolean appearComposite;

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    public ThemeEditorRenderer getEditor() {
        return this.editor;
    }

    public List<ITheme> getAllThemes() {
        if (rtx.nv.ClientEdition.isTrial()) {
            List<ITheme> list = new ArrayList<>();
            list.add(Theme.NIVORA);
            return list;
        }
        List<ITheme> list = new ArrayList<>();
        list.addAll(CustomThemeManager.getCustomThemes());
        Collections.addAll(list, Theme.values());
        return list;
    }

    public void open() {
        this.open(false);
    }

    public void open(boolean bl) {
        this.rowAppearAnims.clear();
        this.scroll = 0.0f;
        this.scrollTarget = 0.0f;
        this.appearFadeMs = bl ? 320 : 380;
        this.appearBaseMs = System.currentTimeMillis();
        this.appearInitialFrame = true;
    }

    public void render(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        this.render(drawContext, f, f2, f3, f4, 1.0f, f5);
    }

    public void render(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        float f7 = f + UI.contentXOff();
        float f8 = f2 + 5.0f;
        float f9 = f3 - UI.contentInset();
        float f16 = UI.CONTENT_HEIGHT;

        if (this.editor.isOpen()) {
            f4 *= 0.95f;
        }

        float fSpeed = 1.0f - (float)Math.exp(-f6 * 14.0f);
        this.scroll += (this.scrollTarget - this.scroll) * fSpeed;
        if (Math.abs(this.scrollTarget - this.scroll) < 0.05f) {
            this.scroll = this.scrollTarget;
        }
        if (this.appearComposite && !this.transitioning && this.hasAppearWork()) {
            GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
            guiRenderState.createNewRootLayer();
            guiRenderState.applyBlur();
            UI.markCardStratum();
        }
        this.renderCards(drawContext, f, f2, f3, f4, f5, f6, true);
        this.appearInitialFrame = false;
    }

    public boolean click(float f, float f2, float f3, float f4, float f5) {
        if (this.editor.isOpen()) {
            if (this.editor.mouseClicked(f4, f5, 0)) {
                return true;
            }
            if (this.editor.getDraft().isDirty()) {
                this.editor.promptLeaveCategory(null);
                return true;
            }
        }
        if (this.transitioning) {
            return false;
        }
        float f6 = f + UI.contentXOff();
        float f7 = f2 + 5.0f;
        float f8 = f3 - UI.contentInset();
        float f9 = 3.0f;
        float f10 = 4.0f;
        float f11 = (f8 - f9 - f10 * 2.0f) * 0.5f;
        float f12 = f6 + f10;
        float f13 = f12 + f11 + f9;
        if (f4 < f6 || f4 > f6 + f8 || f5 < f7 || f5 > f7 + UI.CONTENT_HEIGHT) {
            return false;
        }

        if (!rtx.nv.ClientEdition.isTrial()) {
            // Header bar button: [ + Create Theme ]
            float btnW = 84.0f;
            float btnH = 16.0f;
            float btnX = f6 + f8 - btnW - 6.0f;
            float btnY = f7 + 3.0f;
            if (f4 >= btnX && f4 <= btnX + btnW && f5 >= btnY && f5 <= btnY + btnH) {
                this.editor.openNew();
                return true;
            }
        }

        float cardsStartY = f7 + HEADER_BAR_H;
        List<ITheme> allThemes = this.getAllThemes();
        for (int i = 0; i < allThemes.size(); ++i) {
            ITheme th = allThemes.get(i);
            int n = i % 2;
            int n2 = i / 2;
            float f14 = n == 0 ? f12 : f13;
            float f15 = cardsStartY + f10 + (float)n2 * (CARD_H + f9) - this.scroll;
            if (!(f4 >= f14) || !(f4 <= f14 + f11) || !(f5 >= f15) || !(f5 <= f15 + CARD_H)) continue;

            // Check action buttons on custom themes
            if (th.isCustom()) {
                CustomTheme ct = (CustomTheme) th;
                // Delete button ('x')
                float delX = f14 + f11 - 10.0f;
                float delY = f15 + 6.0f;
                if (f4 >= delX - 2.0f && f4 <= delX + 8.0f && f5 >= delY - 2.0f && f5 <= delY + 10.0f) {
                    CustomThemeManager.delete(ct);
                    ConfigManager.markDirty();
                    try { Sounds.play("click"); } catch (Throwable ignored) {}
                    return true;
                }
                // Edit button ('f')
                float editX = f14 + f11 - 20.0f;
                float editY = f15 + 6.0f;
                if (f4 >= editX - 2.0f && f4 <= editX + 8.0f && f5 >= editY - 2.0f && f5 <= editY + 10.0f) {
                    this.editor.openEdit(ct);
                    return true;
                }
            } else if (!rtx.nv.ClientEdition.isTrial()) {
                // Duplicate button on built-in theme
                float dupX = f14 + f11 - 10.0f;
                float dupY = f15 + 6.0f;
                if (th != ThemeManager.currentTheme() && f4 >= dupX - 2.0f && f4 <= dupX + 8.0f && f5 >= dupY - 2.0f && f5 <= dupY + 10.0f) {
                    CustomTheme copy = CustomThemeManager.duplicate(th);
                    if (copy != null) {
                        this.editor.openEdit(copy);
                    }
                    return true;
                }
            }

            ThemeManager.set(th);
            InterfaceModule interfaceModule = InterfaceModule.getInstance();
            if (interfaceModule != null) {
                interfaceModule.clientColorMode.setSelected(InterfaceModule.CLIENT_COLOR_PALETTE);
            }
            ConfigManager.markDirty();
            return true;
        }
        return false;
    }

    private static Decelerate createAnim(int n) {
        Decelerate decelerate = (Decelerate)new Decelerate().setMs(n).setValue(1.0);
        decelerate.setDirection(Direction.BACKWARDS);
        decelerate.counter.setTime(System.currentTimeMillis() - 10000L);
        return decelerate;
    }

    public void scroll(double d, float f) {
        float f2 = Math.max(0.0f, this.contentH - f + 8.0f);
        this.scrollTarget = ThemesRenderer.clamp(this.scrollTarget - (float)d * 18.0f, 0.0f, f2);
    }

    public boolean isTransitioning() {
        return this.transitioning;
    }

    public float cardBlurMaxPhase() {
        return this.cardBlurMaxPhase;
    }

    public float[] cardBlurRects() {
        return this.cardBlurRects;
    }

    public int cardBlurCount() {
        return this.cardBlurCount;
    }

    public float currentScroll() {
        return this.scroll;
    }

    public void resetCardBlur() {
        this.cardBlurCount = 0;
        this.cardBlurMaxPhase = 0.0f;
    }

    public void setAppearComposite(boolean bl) {
        this.appearComposite = bl;
    }

    public void finishTransition() {
        this.transitioning = false;
        this.fadeOutTime = 0.0f;
    }

    public void resetScroll() {
        this.scroll = 0.0f;
        this.scrollTarget = 0.0f;
    }

    private boolean hasAppearWork() {
        if (this.appearInitialFrame) {
            return true;
        }
        if (Math.abs(this.scrollTarget - this.scroll) > 0.05f) {
            return true;
        }
        for (Decelerate decelerate : this.rowAppearAnims.values()) {
            if (!(decelerate.getOutput().floatValue() < 0.999f)) continue;
            return true;
        }
        return false;
    }

    private void renderScrollBar(float f, float f2, float f3, float f4, float f5, float f6) {
        if (f5 <= 0.5f) {
            return;
        }
        float f7 = 3.0f;
        float f8 = f + f3 + 0.25f;
        float f9 = f2 + f7;
        float f10 = f4 - f7 * 2.0f;
        float f11 = ThemesRenderer.clamp(f4 / Math.max(this.contentH, f4), 0.0f, 1.0f);
        float f12 = ThemesRenderer.clamp(f10 * f11, 12.0f, f10);
        float f13 = Math.max(0.0f, f10 - f12);
        float f14 = ThemesRenderer.clamp(this.scroll / Math.max(f5, 1.0f), 0.0f, 1.0f);
        float f15 = f9 + f13 * f14;
        Render2D.rect(f8, f9, 1.25f, f10, 1.0f, ThemeManager.rgba(0xFFFFFF, 18.0f * f6));
        AccentGradient.fillVertical(f8, f15, 1.25f, f12, 1.0f, 165.0f * f6);
    }

    public boolean isFadeOutDone() {
        return this.transitioning && this.fadeOutTime <= 0.0f;
    }

    private void renderCards(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, boolean bl) {
        int n;
        float f7 = f + UI.contentXOff();
        float f8 = f2 + 5.0f;
        float f9 = f3 - UI.contentInset();
        float f10 = 3.0f;
        float f11 = 4.0f;
        float f12 = 4.5f;
        float f13 = (f9 - f10 - f11 * 2.0f) * 0.5f;
        float f14 = f7 + f11;
        float f15 = f14 + f13 + f10;
        float f16 = UI.CONTENT_HEIGHT;

        float f17 = Position.mouseX();
        float f18 = Position.mouseY();

        // 1. Header Bar: Title + [ + Create Theme ] button
        float btnW = 84.0f;
        float btnH = 16.0f;
        float btnX = f7 + f9 - btnW - 6.0f;
        float btnY = f8 + 3.0f;
        boolean btnHover = f17 >= btnX && f17 <= btnX + btnW && f18 >= btnY && f18 <= btnY + btnH;

        String headerTitle = Lang.get("category.themes", "Темы");
        Fonts.MONTSERRAT_MEDIUM.draw(headerTitle, f7 + 6.0f, f8 + 4.5f, 7.5f, ThemeManager.rgba(0xFFFFFF, 230.0f * f4));

        if (!rtx.nv.ClientEdition.isTrial()) {
            Render2D.rect(btnX, btnY, btnW, btnH, 3.5f, ClientAccent.accent((btnHover ? 210.0f : 160.0f) * f4));
            Render2D.outline(btnX, btnY, btnW, btnH, 3.5f, 0.6f, ThemeManager.rgba(0xFFFFFF, 35.0f * f4));
            String createStr = Lang.get("theme.editor.create", "Создать тему");
            float addIconSize = 5.5f;
            Fonts.NV.msdf(NvIcons.ADD, btnX + 6.0f, btnY + 5.0f, addIconSize, ThemeManager.rgba(0xFFFFFF, 255.0f * f4));
            Fonts.MONTSERRAT_MEDIUM.draw(rtx.nv.api.ui.settings.RenderHelper.fitText(Fonts.MONTSERRAT_MEDIUM, createStr, btnW - 22.0f, 5.5f), btnX + 15.0f, btnY + 4.5f, 5.5f, ThemeManager.rgba(0xFFFFFF, 255.0f * f4));
        }

        // 2. Cards Scissor View
        float cardsStartY = f8 + HEADER_BAR_H;
        float cardsAreaH = f16 - HEADER_BAR_H;

        Render2D.pushScissor(drawContext, f7, cardsStartY, f9, cardsAreaH);
        float f19 = (1.0f - f5) * 8.0f;
        boolean bl2 = UI.isOpen();
        boolean bl3 = bl2 && f17 >= f7 && f17 <= f7 + f9 && f18 >= cardsStartY && f18 <= cardsStartY + cardsAreaH;
        float f20 = bl2 ? 1.0f - (float)Math.exp(-f6 * 16.0f) : 0.0f;

        List<ITheme> themeList = this.getAllThemes();
        for (n = 0; n < themeList.size(); ++n) {
            float f21;
            float f22;
            float f23;
            ITheme theme2 = themeList.get(n);
            int n2 = n % 2;
            int n3 = n / 2;
            float f24 = n2 == 0 ? f14 : f15;
            float f25 = cardsStartY + f11 + (float)n3 * (CARD_H + f10) + f19 - this.scroll;
            boolean bl4 = bl3 && f17 >= f24 && f17 <= f24 + f13 && f18 >= f25 && f18 <= f25 + CARD_H;
            float f26 = this.hoverAnims.getOrDefault(theme2.id(), Float.valueOf(0.0f)).floatValue();
            f26 += ((bl4 ? 1.0f : 0.0f) - f26) * f20;
            this.hoverAnims.put(theme2.id(), Float.valueOf(f26));
            if (f25 + CARD_H < cardsStartY - 5.0f || f25 > cardsStartY + cardsAreaH + 5.0f) continue;
            float f27 = f23 = bl ? this.rowAppear(n3) : 1.0f;
            if (f23 < 0.001f) continue;
            float f28 = Math.min(1.0f, f23 / 0.6f);
            float f29 = f28 * f28;
            float f30 = f23 < 0.6f ? 0.0f : (f23 - 0.6f) / 0.4f;
            float f31 = 1.0f - f30 * f30 * (3.0f - 2.0f * f30);
            f25 += (1.0f - f28) * 6.0f;
            boolean bl5 = f23 < 0.999f;
            boolean bl6 = this.appearComposite && bl5;
            float f32 = f4 * (!bl5 || bl6 ? 1.0f : f29);
            if (bl6 && this.cardBlurCount < 32) {
                int n4 = this.cardBlurCount * 6;
                this.cardBlurRects[n4] = f24;
                this.cardBlurRects[n4 + 1] = f25;
                this.cardBlurRects[n4 + 2] = f13;
                this.cardBlurRects[n4 + 3] = CARD_H;
                this.cardBlurRects[n4 + 4] = f29;
                this.cardBlurRects[n4 + 5] = f31;
                ++this.cardBlurCount;
                this.cardBlurMaxPhase = Math.max(this.cardBlurMaxPhase, f31);
            }
            Decelerate decelerate = this.selectAnims.computeIfAbsent(theme2.id(), theme -> ThemesRenderer.createAnim(220));
            decelerate.setDirection(theme2 == ThemeManager.currentTheme() ? Direction.FORWARDS : Direction.BACKWARDS);
            f22 = decelerate.getOutput().floatValue();

            // Background card with hover brightness boost
            Render2D.rect(f24, f25, f13, CARD_H, f12, ThemeManager.rgba(0, (40.0f + 20.0f * f26) * f32));
            if (f26 > 0.01f) {
                Render2D.rect(f24, f25, f13, CARD_H, f12, ThemeManager.rgba(0xFFFFFF, 9.0f * f26 * f32));
            }

            // Outline with hover boost and smooth fade when selected
            if ((f21 = (14.0f + 14.0f * f26) * (1.0f - f22) * f32) > 0.5f) {
                Render2D.outline(f24, f25, f13, CARD_H, f12, f12, f12, f12, 0.6f, ThemeManager.rgba(0xFFFFFF, f21));
            }

            // Selected state accent indicator bar & glowing border
            if (f22 > 0.01f) {
                int n5 = theme2.gradientA();
                int n6 = theme2.gradientB();
                int n7 = ThemeManager.mix(ThemeManager.rgba(n5, 255.0f), ThemeManager.rgba(n6, 255.0f), 0.5f) & 0xFFFFFF;
                Render2D.rect(f24 + 2.0f, f25 + 4.0f, 1.5f, CARD_H - 8.0f, 6.0f, ThemeManager.rgba(n5, 210.0f * f22 * f32), ThemeManager.rgba(n5, 210.0f * f22 * f32), ThemeManager.rgba(n6, 210.0f * f22 * f32), ThemeManager.rgba(n6, 210.0f * f22 * f32));
                Render2D.outline(f24, f25, f13, CARD_H, f12, f12, f12, f12, 0.7f, ThemeManager.rgba(n5, 120.0f * f22 * f32), ThemeManager.rgba(n7, 120.0f * f22 * f32), ThemeManager.rgba(n6, 120.0f * f22 * f32), ThemeManager.rgba(n7, 120.0f * f22 * f32));
            }

            // Theme display name
            float f34 = f24 + 8.0f + f22 * 2.5f;
            float maxTitleW = Math.max(20.0f, f13 - (theme2.isCustom() ? 48.0f : (rtx.nv.ClientEdition.isTrial() ? 28.0f : 38.0f)) - 8.0f - f22 * 2.5f);
            String fittedTitle = rtx.nv.api.ui.settings.RenderHelper.fitText(Fonts.MONTSERRAT_MEDIUM, theme2.displayName(), maxTitleW, 7.5f);
            Fonts.MONTSERRAT_MEDIUM.draw(fittedTitle, f34, f25 + 5.5f, 7.5f, ThemeManager.rgba(0xFFFFFF, (210.0f + 40.0f * f26 + 15.0f * f22) * f32));

            // Description text
            String descriptor = Lang.get("theme." + theme2.id().toLowerCase(java.util.Locale.ROOT) + ".desc", "");
            if (descriptor.isEmpty()) {
                descriptor = theme2.profile() != null ? Lang.get("theme.profile." + theme2.profile().id, theme2.profile().displayName) : "Default";
                if (theme2.isCustom()) {
                    descriptor += " (" + Lang.get("theme.editor.custom_tag", "Пользовательская") + ")";
                }
            }
            float maxDescW = Math.max(20.0f, f13 - 16.0f - f22 * 2.5f);
            String fittedDesc = rtx.nv.api.ui.settings.RenderHelper.fitText(Fonts.MONTSERRAT_MEDIUM, descriptor, maxDescW, 5.0f);
            Fonts.MONTSERRAT_MEDIUM.draw(fittedDesc, f34, f25 + 16.5f, 5.0f, ThemeManager.rgba(0x9E9E9E, (155.0f + 35.0f * f26) * f32));

            // Action icons at top right
            if (theme2.isCustom()) {
                // Delete button
                float delX = f24 + f13 - 10.0f;
                float delY = f25 + 6.5f;
                boolean delHover = f17 >= delX - 2.0f && f17 <= delX + 8.0f && f18 >= delY - 2.0f && f18 <= delY + 10.0f;
                Fonts.NV.msdf(NvIcons.DELETE, delX, delY, 6.0f, ThemeManager.rgba(0xFFFFFF, (delHover ? 255.0f : 120.0f) * f32));

                // Edit button
                float editX = f24 + f13 - 20.0f;
                float editY = f25 + 6.5f;
                boolean editHover = f17 >= editX - 2.0f && f17 <= editX + 8.0f && f18 >= editY - 2.0f && f18 <= editY + 10.0f;
                Fonts.NV.msdf(NvIcons.SETTINGS, editX, editY, 6.0f, ThemeManager.rgba(0xFFFFFF, (editHover ? 255.0f : 120.0f) * f32));
            } else if (f22 > 0.01f) {
                // Selected indicator checkmark
                float checkX = f24 + f13 - 11.0f;
                Fonts.NV.msdf(NvIcons.CHECK, checkX, f25 + 6.5f, 6.0f, ThemeManager.rgba(theme2.accentBrightRgb(), 230.0f * f22 * f32));
            } else if (!rtx.nv.ClientEdition.isTrial() && f26 > 0.1f) {
                // Duplicate icon on hover
                float dupX = f24 + f13 - 11.0f;
                boolean dupHover = f17 >= dupX - 2.0f && f17 <= dupX + 8.0f && f18 >= f25 + 4.5f && f18 <= f25 + 16.5f;
                Fonts.NV.msdf(NvIcons.DUPLICATE, dupX, f25 + 6.5f, 6.0f, ThemeManager.rgba(0xFFFFFF, (dupHover ? 220.0f : 110.0f) * f26 * f32));
            }

            // 4-stop continuous gradient preview strip & samples
            int[] nArray = theme2.palette();
            if (nArray != null && nArray.length >= 2) {
                float dotsX = f24 + f13 - (theme2.isCustom() ? 44.0f : (rtx.nv.ClientEdition.isTrial() ? 24.0f : 34.0f));
                for (int d = 0; d < Math.min(3, nArray.length); ++d) {
                    Render2D.rect(dotsX + (float)d * 6.0f, f25 + 7.5f, 3.5f, 3.5f, 1.75f, ThemeManager.rgba(nArray[d], (190.0f + 50.0f * f26) * f32));
                }

                float barX = f24 + 8.0f;
                float barY = f25 + CARD_H - 11.0f;
                float barW = f13 - 16.0f;
                float barH = 5.0f;
                int segments = nArray.length - 1;
                float segW = barW / (float)segments;
                for (int s = 0; s < segments; ++s) {
                    float sx = barX + (float)s * segW;
                    int col1 = ThemeManager.rgba(nArray[s], (220.0f + 35.0f * f26) * f32);
                    int col2 = ThemeManager.rgba(nArray[s + 1], (220.0f + 35.0f * f26) * f32);
                    Render2D.rect(sx, barY, segW + 0.5f, barH, (s == 0 || s == segments - 1) ? 1.5f : 0.0f, col1, col2, col2, col1);
                }
                Render2D.outline(barX, barY, barW, barH, 2.0f, 0.5f, ColorUtil.rgba(255, 255, 255, Math.round(22.0f * f32)));
            }
        }
        n = (themeList.size() + 1) / 2;
        this.contentH = (float)n * (CARD_H + f10) - f10;
        float f40 = Math.max(0.0f, this.contentH - cardsAreaH + f11 * 2.0f);
        this.scrollTarget = ThemesRenderer.clamp(this.scrollTarget, 0.0f, f40);
        this.scroll = ThemesRenderer.clamp(this.scroll, 0.0f, f40);
        Render2D.popScissor(drawContext);
        this.renderScrollBar(f7, cardsStartY, f9, cardsAreaH, f40, f4);
    }

    public void beginFadeOut() {
        this.fadeOutTime = 0.15f;
        this.transitioning = true;
    }

    private float rowAppear(int n) {
        Decelerate decelerate = this.rowAppearAnims.get(n);
        if (decelerate == null) {
            long l = this.appearInitialFrame ? this.appearBaseMs : System.currentTimeMillis();
            decelerate = (Decelerate)new Decelerate().setMs(this.appearFadeMs).setValue(1.0);
            decelerate.counter.setTime(l);
            this.rowAppearAnims.put(n, decelerate);
        }
        float f = decelerate.getOutput().floatValue();
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    public boolean keyPressed(KeyInput input) {
        if (this.editor.isOpen()) {
            return this.editor.keyPressed(input.key());
        }
        return false;
    }

    public boolean charTyped(CharInput input) {
        if (this.editor.isOpen()) {
            return this.editor.charTyped((char) input.codepoint());
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (this.editor.isOpen()) {
            this.editor.mouseReleased(mouseX, mouseY, button);
        }
    }
}
