package rtx.nv.api.ui.module;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import rtx.nv.api.drags.Position;
import rtx.nv.api.localization.LocalizationManager;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.restrict.Server;
import rtx.nv.api.modules.restrict.ServerRestrictions;
import rtx.nv.api.ui.ScrollBar;
import rtx.nv.api.ui.UI;
import rtx.nv.api.ui.pin.PinManager;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.settings.SettingsFactory;
import rtx.nv.api.ui.settings.impl.BindSetting;
import rtx.nv.api.ui.settings.impl.TextSetting;
import rtx.nv.api.ui.theme.AccentGradient;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.api.ui.theme.ThemeManager;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.animations.Decelerate;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.key.KeyBind;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.api.ui.GuiDebugRenderer;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.others.RoundedScissor;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.glass.BuiltGlass;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.utils.sounds.Sounds;

public final class ModuleListRenderer {
    private static final float FADE_OUT_DURATION = 0.15f;
    private static final int ROW_FADE_MS = 60;
    private static final int QUICK_ROW_FADE_MS = 40;
    private static final float SLIDE_PX = 6.0f;
    private static final float CONTENT_Y_OFFSET = 5.0f;
    public static final float HEADER_OFFSET = 26.0f;
    public static final float CARD_H = 36.0f;
    private static final float CARD_RADIUS = 7.0f;
    private static final float CARD_GAP = 6.0f;
    private static final float CARD_PAD = 6.0f;
    private static final float PAD_X = 8.0f;
    private static final float NAME_SIZE = rtx.nv.api.ui.UiTokens.TEXT_SIZE + 0.5f;
    private static final float NAME_TOP = 6.5f;
    private static final float DESC_SIZE = rtx.nv.api.ui.UiTokens.SECONDARY_SIZE;
    private static final float DESC_TOP = 19.5f;

    public static final float TOGGLE_W = 18.0f;
    public static final float TOGGLE_H = 10.0f;
    public static final float TOGGLE_RADIUS = 5.0f;
    public static final float TOGGLE_RIGHT_PAD = 8.0f;
    public static final float TOGGLE_TOP = 13.0f;
    private static final int TOGGLE_ANIM_MS = 160;

    private static final float GEAR_SIZE = 10.0f;
    private static final float PIN_SIZE = 10.0f;

    private final EnumMap<Category, List<Module>> moduleCache = new EnumMap<>(Category.class);
    private int moduleCacheSize = -1;
    private LocalizationManager.Language moduleCacheLanguage = null;
    private final Map<String, Decelerate> enableAnims = new HashMap<>();
    private final Map<String, Float> hoverAnims = new HashMap<>();
    private final Map<String, Float> gearHoverAnims = new HashMap<>();
    private final Map<String, Float> pinHoverAnims = new HashMap<>();
    private final Map<String, Float> edgeActivations = new HashMap<>();
    private long lastFrameTimeNs = System.nanoTime();
    private final Map<String, List<Setting>> moduleSettings = new HashMap<>();

    private float scroll = 0.0f;
    private float scrollTarget = 0.0f;
    private float contentH = 0.0f;
    private Category lastRendered = null;
    private final Map<Integer, Decelerate> rowAppearAnims = new HashMap<>();
    private int appearFadeMs = 220;
    private long appearBaseMs;
    private boolean appearInitialFrame;
    private final ScrollBar scrollBar = new ScrollBar();
    private boolean transitioning = false;
    private boolean quickAppear = false;
    private List<Module> fadingOut = Collections.emptyList();
    private float fadeOutTime = 0.0f;
    private String focusName;
    private long focusUntilMs;
    private boolean focusScrollPending;
    private float focusDimT;

    public static final int MAX_BLUR_CARDS = 64;
    private final float[] cardBlurRects = new float[384];
    private int cardBlurCount;
    private float cardBlurMaxPhase;
    private boolean appearComposite;
    private int lastModulesSig;

    public void render(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, Category category, List<Module> list) {
        float f9 = f + UI.contentXOff();
        float f10 = f2 + 5.0f + HEADER_OFFSET;
        float f11 = f3 - UI.contentInset();
        float f12 = ModuleListRenderer.colW(f11);
        float f13 = f9 + CARD_PAD;
        float f14 = f13 + f12 + CARD_GAP;
        float f15 = UI.CONTENT_HEIGHT - HEADER_OFFSET;

        float f16 = 1.0f - (float) Math.exp(-f6 * 14.0f);
        this.scroll += (this.scrollTarget - this.scroll) * f16;
        if (Math.abs(this.scrollTarget - this.scroll) < 0.05f) {
            this.scroll = this.scrollTarget;
        }

        if (this.transitioning) {
            if (this.fadeOutTime > 0.0f) {
                this.fadeOutTime -= f6;
                float f17 = f4 * Math.max(0.0f, this.fadeOutTime / 0.15f);
                this.renderCards(drawContext, f9, f10, f11, f12, f13, f14, f15, f17, f5, f6, this.fadingOut, false);
            }
            return;
        }

        int n = ModuleListRenderer.modulesSignature(list);
        boolean bl2 = this.appearComposite && (category != this.lastRendered || n != this.lastModulesSig || this.scrollBar.isDragging() || Math.abs(this.scrollTarget - this.scroll) > 0.05f || this.hasUnfinishedAppear());
        this.lastModulesSig = n;
        if (category != this.lastRendered) {
            this.lastRendered = category;
            this.scroll = 0.0f;
            this.scrollTarget = 0.0f;
            this.startRowAnims(ModuleListRenderer.rowCount(list.size()));
        }

        this.contentH = this.totalContentH(list, f12);
        float f18 = Math.max(0.0f, this.contentH - f15);
        this.scroll = Math.max(0.0f, Math.min(this.scroll, f18));
        this.scrollTarget = Math.max(0.0f, Math.min(this.scrollTarget, f18));

        int n2 = this.focusName != null ? ModuleListRenderer.indexOfModule(list, this.focusName) : -1;
        boolean bl = n2 >= 0 && System.currentTimeMillis() < this.focusUntilMs;
        if (this.focusScrollPending && n2 >= 0) {
            float cardY = CARD_PAD + (float) (n2 / 2) * (CARD_H + CARD_GAP);
            this.scrollTarget = Math.max(0.0f, Math.min(f18, cardY - (f15 - CARD_H) * 0.5f));
            this.focusScrollPending = false;
        }

        this.focusDimT += ((bl ? 1.0f : 0.0f) - this.focusDimT) * (1.0f - (float) Math.exp(-f6 * 8.0f));
        if (!bl && this.focusDimT < 0.01f && !this.focusScrollPending) {
            this.focusName = null;
        }

        float sbX = f + f3 - 7.5f;
        float cr = RenderHelper.effectiveCornerRadius(12.0f, f11, 280.0f);
        float inset = Math.max(0.0f, RenderHelper.cornerEdgeInset(cr, 1.0f) + 1.5f - 3.0f);
        float sbY = f10 + 3.0f;
        float sbH = f15 - 6.0f - inset;
        float f22 = this.scrollBar.render(sbX, sbY, sbH, f15, this.contentH, this.scroll, f4);
        if (this.scrollBar.isDragging()) {
            this.scroll = f22;
            this.scrollTarget = f22;
        }

        if (bl2) {
            GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor) drawContext).nv_getGuiRenderState();
            guiRenderState.createNewRootLayer();
            guiRenderState.applyBlur();
            UI.markCardStratum();
        }

        this.renderCards(drawContext, f9, f10, f11, f12, f13, f14, f15, f4, f5, f6, list, true);
        this.appearInitialFrame = false;
    }

    private void renderCards(DrawContext drawContext, float f, float f2, float f3, float colW, float leftColX, float rightColX, float viewH, float alpha, float f9, float dt, List<Module> list, boolean appear) {
        float overflowX = 6.0f;
        float topOverflow = 5.0f;
        float maxBotY = UI.panelY() + UI.PANEL_H - 2.0f;
        float scissorY = f2 - topOverflow;
        float scissorH = Math.max(0.0f, maxBotY - scissorY);
        Render2D.pushScissor(drawContext, f - overflowX, scissorY, f3 + overflowX * 2.0f, scissorH);
        RoundedScissor.push(drawContext, f - overflowX, scissorY, f3 + overflowX * 2.0f, scissorH, 0.0f, 0.0f, 10.0f, 0.0f);

        float mouseX = Position.mouseX();
        float mouseY = Position.mouseY();
        boolean uiOpen = UI.isOpen();
        boolean mouseInView = uiOpen && mouseX >= f && mouseX <= f + f3 && mouseY >= f2 && mouseY <= f2 + viewH;
        float animSpeed = uiOpen ? 1.0f - (float) Math.exp(-dt * 16.0f) : 0.0f;

        for (int i = 0; i < list.size(); ++i) {
            CardLayout layout = this.getCardLayout(list, i, f, f2, f3, viewH, alpha, appear);
            if (layout == null || !layout.visible) {
                continue;
            }

            Module module = layout.module;
            float cardX = layout.cardX;
            float cardY = layout.cardY;
            float finalAlpha = layout.alpha;

            if (GuiDebugRenderer.ENABLED) {
                GuiDebugRenderer.recordRect("card_" + module.getName(), layout.cardX, layout.cardY, layout.cardW, layout.cardH, true);
            }

            boolean hovered = mouseInView && layout.containsCard(mouseX, mouseY);
            float hoverProgress = this.hoverAnims.getOrDefault(module.getName(), 0.0f);
            hoverProgress += ((hovered ? 1.0f : 0.0f) - hoverProgress) * animSpeed;
            this.hoverAnims.put(module.getName(), hoverProgress);

            boolean bl6 = layout.bl6;
            boolean bl7 = layout.bl7;
            float f29 = layout.f29;
            float f30 = layout.f30;
            float f32 = layout.f32;

            // Motion blur / Kawase blur rect registration
            if (bl6) {
                float f20 = 0.85f + 0.15f * f29;
                if (bl7 && this.cardBlurCount < 64) {
                    float topY = Math.max(cardY, f2);
                    float botY = Math.min(cardY + CARD_H, f2 + viewH);
                    if (botY - topY > 0.5f) {
                        int n4 = this.cardBlurCount * 6;
                        this.cardBlurRects[n4] = cardX;
                        this.cardBlurRects[n4 + 1] = topY;
                        this.cardBlurRects[n4 + 2] = colW;
                        this.cardBlurRects[n4 + 3] = botY - topY;
                        this.cardBlurRects[n4 + 4] = f30;
                        this.cardBlurRects[n4 + 5] = f32;
                        ++this.cardBlurCount;
                        this.cardBlurMaxPhase = Math.max(this.cardBlurMaxPhase, f32);
                    }
                }
                float midX = cardX + colW * 0.5f;
                float midY = cardY + CARD_H * 0.5f;
                drawContext.getMatrices().pushMatrix();
                drawContext.getMatrices().translate(midX, midY);
                drawContext.getMatrices().scale(f20, f20);
                drawContext.getMatrices().translate(-midX, -midY);
            }

            Decelerate enableAnim = this.enableAnims.computeIfAbsent(module.getName(), k -> {
                Decelerate d = (Decelerate) new Decelerate().setMs(TOGGLE_ANIM_MS).setValue(1.0);
                d.setDirection(Direction.BACKWARDS);
                d.counter.setTime(System.currentTimeMillis() - 10000L);
                return d;
            });
            enableAnim.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
            float enableProg = enableAnim.getOutput().floatValue();

            float currentAct = this.edgeActivations.computeIfAbsent(module.getName(), k -> module.isEnabled() ? 1.0f : 0.0f);
            float targetAct = module.isEnabled() ? 1.0f : 0.0f;
            float rate = targetAct > currentAct ? (1.0f / 0.260f) : (1.0f / 0.210f);
            float step = dt * rate;
            if (currentAct < targetAct) {
                currentAct = Math.min(targetAct, currentAct + step);
            } else if (currentAct > targetAct) {
                currentAct = Math.max(targetAct, currentAct - step);
            }
            this.edgeActivations.put(module.getName(), currentAct);

            InterfaceModule interfaceModule = InterfaceModule.getInstance();
            boolean useModuleWaveEdge = interfaceModule == null || interfaceModule.activeModuleWaveEdge.getValue();

            // 1. Card Background with Active Wave Edge indicator (replaces legacy side bars)
            int cardBg = ModuleListRenderer.rgba(18, 20, 27, (135.0f + 25.0f * hoverProgress) * finalAlpha);
            if (useModuleWaveEdge) {
                float cardSeed = (float) Math.abs(module.getName().hashCode() % 10000);
                int activeCardTint = ClientAccent.gradientA(45.0f * currentAct * finalAlpha);
                int finalCardBg = ColorUtil.lerpColor(cardBg, activeCardTint, currentAct * 0.45f);

                float userIntensity = interfaceModule != null ? interfaceModule.waveEdgeIntensity.getFloat() : 0.70f;
                float userSize = interfaceModule != null ? interfaceModule.waveEdgeSize.getFloat() : 0.50f;
                float userDensity = interfaceModule != null ? interfaceModule.waveEdgeDensity.getFloat() : 0.50f;
                boolean userMotion = interfaceModule == null || interfaceModule.waveEdgeMotion.getValue();
                float userSpeed = interfaceModule != null ? interfaceModule.waveEdgeSpeed.getFloat() : 0.50f;
                float userGlow = interfaceModule != null ? interfaceModule.waveEdgeGlow.getFloat() : 0.25f;

                int accentA = ClientAccent.gradientA(230.0f * currentAct * finalAlpha);
                int accentB = ClientAccent.gradientB(230.0f * currentAct * finalAlpha);

                BuiltGlass cardGlass = new BuiltGlass(
                    cardX, cardY, colW, CARD_H,
                    CARD_RADIUS, CARD_RADIUS, CARD_RADIUS, CARD_RADIUS,
                    finalCardBg, finalAlpha,
                    25.0f, accentA,
                    0.85f, true,
                    0.25f + 0.35f * currentAct,
                    0.10f, 0.5f, 0.0f
                ).withBlurRadius(14.0f)
                 .withSecondColor(accentB, 0.0f)
                 .withWaveEdge(
                     true,
                     currentAct,
                     userIntensity,
                     userSize,
                     userDensity,
                     userMotion,
                     userSpeed,
                     Math.max(userGlow, 0.35f),
                     cardSeed,
                     1
                 );
                Render2D.glass(cardGlass);
            } else {
                Render2D.rect(cardX, cardY, colW, CARD_H, CARD_RADIUS, cardBg);
            }

            // 2. Focus Flow (subtle physical glass highlight tracking cursor relative position)
            if (hoverProgress > 0.01f) {
                float u = Math.max(0.0f, Math.min(1.0f, (mouseX - cardX) / colW));
                float v = Math.max(0.0f, Math.min(1.0f, (mouseY - cardY) / CARD_H));
                Render2D.rect(cardX + 2.0f + u * 4.0f, cardY + 2.0f + v * 3.0f, colW - 8.0f, CARD_H - 8.0f, CARD_RADIUS - 1.0f, ModuleListRenderer.rgba(255, 255, 255, 3.5f * hoverProgress * finalAlpha));
            }

            // 3. Card Outline (subtle neutral outline receding completely when Live Edge activates)
            float outlineFade = useModuleWaveEdge ? (1.0f - currentAct) : 1.0f;
            if (outlineFade > 0.02f) {
                int idleOutline = ModuleListRenderer.rgba(255, 255, 255, (12.0f + 14.0f * hoverProgress) * finalAlpha * outlineFade);
                int activeOutlineA = ClientAccent.gradientA(75.0f * finalAlpha * outlineFade);
                int activeOutlineB = ClientAccent.gradientB(75.0f * finalAlpha * outlineFade);
                int outlineA = ColorUtil.lerpColor(idleOutline, activeOutlineA, enableProg);
                int outlineB = ColorUtil.lerpColor(idleOutline, activeOutlineB, enableProg);
                Render2D.outline(cardX, cardY, colW, CARD_H, CARD_RADIUS, 0.6f, outlineA, outlineB, outlineB, outlineA);
            }

            // 5. Keybind Badge (if bound)
            KeyBind keyBind = module.getBind();
            float badgeW = 0.0f;
            if (keyBind.isBound()) {
                String bindLabel = ModuleListRenderer.badgeLabel(keyBind);
                float bw = Math.max(10.0f, Fonts.MONTSERRAT_MEDIUM.width(bindLabel, 5.5f) + 6.0f);
                float bx = cardX + PAD_X + 2.0f;
                float by = cardY + 6.0f;
                Render2D.rect(bx, by, bw, 8.5f, 2.5f, ModuleListRenderer.rgba(255, 255, 255, 18.0f * finalAlpha));
                Render2D.outline(bx, by, bw, 8.5f, 2.5f, 0.5f, ModuleListRenderer.rgba(255, 255, 255, 32.0f * finalAlpha));
                float tw = Fonts.MONTSERRAT_MEDIUM.width(bindLabel, 5.5f);
                Fonts.MONTSERRAT_MEDIUM.draw(bindLabel, bx + (bw - tw) * 0.5f, by + 1.25f, 5.5f, ModuleListRenderer.rgba(255, 255, 255, 210.0f * finalAlpha));
                badgeW = bw + 5.0f;
            }

            // 6. Title with soft MSDF shadow (RenderHelper.fitText prevents overlapping pin / gear)
            float titleX = cardX + PAD_X + 2.0f + badgeW;
            float rightBound = layout.pinX;
            float maxTitleW = Math.max(10.0f, (rightBound - 6.0f) - titleX);
            String fittedTitle = RenderHelper.fitText(Fonts.MONTSERRAT_MEDIUM, module.getDisplayName(), maxTitleW, NAME_SIZE);
            float wordA = UI.getLangWordAlpha();
            float wordY = UI.getLangWordOffsetY();
            int titleShadow = ModuleListRenderer.rgba(0, 0, 0, 102.0f * finalAlpha * wordA);
            Fonts.MONTSERRAT_MEDIUM.draw(fittedTitle, titleX + 0.5f, cardY + NAME_TOP + 0.5f + wordY, NAME_SIZE, titleShadow);
            int titleCol = ModuleListRenderer.rgba(241, 244, 252, (230.0f + 25.0f * enableProg) * finalAlpha * wordA);
            Fonts.MONTSERRAT_MEDIUM.draw(fittedTitle, titleX, cardY + NAME_TOP + wordY, NAME_SIZE, titleCol);

            // 7. Single clean line description (Zero Dash Rule, secondary text brightness boost + soft shadow)
            String rawDesc = module.getDescription();
            String desc = rawDesc != null ? rawDesc.replace(" \u2014 ", " ").replace(" \u2013 ", " ").replace(" - ", " ") : "";
            float maxDescW = colW - (layout.hasSettings ? 46.0f : 28.0f);
            String fittedDesc = RenderHelper.fitText(Fonts.MONTSERRAT_MEDIUM, desc, maxDescW, DESC_SIZE);
            int descShadow = ModuleListRenderer.rgba(0, 0, 0, 90.0f * finalAlpha * wordA);
            Fonts.MONTSERRAT_MEDIUM.draw(fittedDesc, cardX + PAD_X + 2.0f + 0.5f, cardY + DESC_TOP + 0.5f + wordY, DESC_SIZE, descShadow);
            int descCol = ModuleListRenderer.rgba(183, 193, 213, (225.0f + 25.0f * hoverProgress) * finalAlpha * wordA);
            Fonts.MONTSERRAT_MEDIUM.draw(fittedDesc, cardX + PAD_X + 2.0f, cardY + DESC_TOP + wordY, DESC_SIZE, descCol);

            // 8. Right side controls: Gear (if settings exist) and Pin
            if (layout.hasSettings) {
                float gearHoverT = this.gearHoverAnims.getOrDefault(module.getName(), 0.0f);
                boolean gearHover = layout.hitSettings(mouseX, mouseY);
                gearHoverT += ((gearHover ? 1.0f : 0.0f) - gearHoverT) * animSpeed;
                this.gearHoverAnims.put(module.getName(), gearHoverT);

                boolean isFocusedInInspector = UI.INSTANCE.getInspector().isOpen() && UI.INSTANCE.getInspector().getFocusedModule() == module;
                int gearCol = isFocusedInInspector
                    ? ClientAccent.accentBright(240.0f * finalAlpha)
                    : ModuleListRenderer.rgba(255, 255, 255, (100.0f + 140.0f * gearHoverT) * finalAlpha);
                Fonts.NV.msdf(NvIcons.SETTINGS, layout.gearX, layout.gearY, layout.gearW, gearCol);
            }

            // Pin Button
            boolean isPinned = PinManager.isPinned(module);
            float latch = PinManager.latchProgress(module.getName());
            boolean pinHover = layout.hitPin(mouseX, mouseY);
            float pinHoverT = this.pinHoverAnims.getOrDefault(module.getName(), 0.0f);
            pinHoverT += ((pinHover ? 1.0f : 0.0f) - pinHoverT) * animSpeed;
            this.pinHoverAnims.put(module.getName(), pinHoverT);

            float pinAlpha = Math.max(isPinned ? 240.0f : 0.0f, pinHoverT * 180.0f) * finalAlpha;
            if (pinAlpha > 1.0f) {
                int pinCol = isPinned ? ClientAccent.accentBright(pinAlpha) : ModuleListRenderer.rgba(255, 255, 255, pinAlpha);
                float pinBounce = -1.5f * latch * (1.0f - latch) * 4.0f;
                Fonts.NV.msdf(NvIcons.PINNED, layout.pinX, layout.pinY + pinBounce, layout.pinW, pinCol);
            }

            if (bl6) {
                drawContext.getMatrices().popMatrix();
            }
        }

        RoundedScissor.pop();
        Render2D.popScissor(drawContext);
    }

    public static void drawToggleSwitch(float toggleX, float toggleY, float enableProgress, float alpha, boolean hovered) {
    }

    public static final class CardLayout {
        public final int index;
        public final Module module;
        public final float cardX;
        public final float cardY;
        public final float cardW;
        public final float cardH;
        public final float toggleX;
        public final float toggleY;
        public final float toggleW;
        public final float toggleH;
        public final float gearX;
        public final float gearY;
        public final float gearW;
        public final float gearH;
        public final float pinX;
        public final float pinY;
        public final float pinW;
        public final float pinH;
        public final float alpha;
        public final boolean visible;
        public final float rowProg;
        public final float f29;
        public final float f30;
        public final float f32;
        public final boolean bl6;
        public final boolean bl7;
        public final boolean hasSettings;

        public CardLayout(int index, Module module, float cardX, float cardY, float cardW, float cardH,
                          float toggleX, float toggleY, float toggleW, float toggleH,
                          float gearX, float gearY, float gearW, float gearH,
                          float pinX, float pinY, float pinW, float pinH,
                          float alpha, boolean visible,
                          float rowProg, float f29, float f30, float f32, boolean bl6, boolean bl7,
                          boolean hasSettings) {
            this.index = index;
            this.module = module;
            this.cardX = cardX;
            this.cardY = cardY;
            this.cardW = cardW;
            this.cardH = cardH;
            this.toggleX = toggleX;
            this.toggleY = toggleY;
            this.toggleW = toggleW;
            this.toggleH = toggleH;
            this.gearX = gearX;
            this.gearY = gearY;
            this.gearW = gearW;
            this.gearH = gearH;
            this.pinX = pinX;
            this.pinY = pinY;
            this.pinW = pinW;
            this.pinH = pinH;
            this.alpha = alpha;
            this.visible = visible;
            this.rowProg = rowProg;
            this.f29 = f29;
            this.f30 = f30;
            this.f32 = f32;
            this.bl6 = bl6;
            this.bl7 = bl7;
            this.hasSettings = hasSettings;
        }

        public boolean containsCard(float mouseX, float mouseY) {
            if (!this.visible || this.alpha < 0.01f) {
                return false;
            }
            return mouseX >= this.cardX && mouseX <= this.cardX + this.cardW && mouseY >= this.cardY && mouseY <= this.cardY + this.cardH;
        }

        public boolean hitToggle(float mouseX, float mouseY) {
            return false;
        }

        public boolean hitSettings(float mouseX, float mouseY) {
            if (!this.visible || this.alpha < 0.01f || !this.hasSettings) {
                return false;
            }
            return mouseX >= this.gearX - 4.0f && mouseX <= this.gearX + this.gearW + 4.0f
                && mouseY >= this.gearY - 4.0f && mouseY <= this.gearY + this.gearH + 4.0f;
        }

        public boolean hitPin(float mouseX, float mouseY) {
            if (!this.visible || this.alpha < 0.01f) {
                return false;
            }
            return mouseX >= this.pinX - 4.0f && mouseX <= this.pinX + this.pinW + 4.0f
                && mouseY >= this.pinY - 4.0f && mouseY <= this.pinY + this.pinH + 4.0f;
        }
    }

    public CardLayout getCardLayout(List<Module> list, int i, float contentX, float contentY, float contentW, float viewH, float alpha, boolean appear) {
        if (list == null || i < 0 || i >= list.size()) {
            return null;
        }
        Module module = list.get(i);
        float colW = ModuleListRenderer.colW(contentW);
        int col = i % 2;
        int row = i / 2;
        float cardX = col == 0 ? contentX + CARD_PAD : contentX + CARD_PAD + colW + CARD_GAP;
        float baseCardY = contentY + CARD_PAD + (float) row * (CARD_H + CARD_GAP) - this.scroll;

        float rowProg = appear ? this.rowAppear(row) : 1.0f;
        float f29 = Math.min(1.0f, rowProg / 0.6f);
        float f30 = f29 * f29;
        float f31 = rowProg < 0.6f ? 0.0f : (rowProg - 0.6f) / 0.4f;
        float f32 = 1.0f - f31 * f31 * (3.0f - 2.0f * f31);
        float cardY = baseCardY + (1.0f - f29) * 6.0f;

        float itemAlpha = alpha;
        if (this.focusDimT > 0.01f && !module.getName().equals(this.focusName)) {
            itemAlpha *= 1.0f - 0.75f * this.focusDimT;
        }
        EnumSet<Server> enumSet = ServerRestrictions.current();
        if (ServerRestrictions.isBlockedBy(module, enumSet)) {
            itemAlpha *= 0.35f;
        }

        boolean bl6 = rowProg < 0.999f;
        boolean bl7 = this.appearComposite && bl6;
        float finalAlpha = itemAlpha * (!bl6 || bl7 ? 1.0f : f30);

        boolean inView = (cardY + CARD_H >= contentY - 5.0f) && (cardY <= contentY + viewH + 5.0f);
        boolean visible = inView && rowProg >= 0.001f && finalAlpha > 0.01f && !this.transitioning;

        boolean hasSettings = module != null && !module.getSettings().all().isEmpty();
        float gearW = 10.0f;
        float gearH = 10.0f;
        float gearX = cardX + colW - 9.0f - gearW;
        float gearY = cardY + (CARD_H - gearH) * 0.5f;

        float pinW = 10.0f;
        float pinH = 10.0f;
        float pinX = hasSettings ? (gearX - 14.0f) : (cardX + colW - 9.0f - pinW);
        float pinY = cardY + (CARD_H - pinH) * 0.5f;

        return new CardLayout(i, module, cardX, cardY, colW, CARD_H,
            0.0f, 0.0f, 0.0f, 0.0f,
            gearX, gearY, gearW, gearH,
            pinX, pinY, pinW, pinH,
            finalAlpha, visible, rowProg, f29, f30, f32, bl6, bl7, hasSettings);
    }

    public float[] cardRect(List<Module> list, int i, float contentX, float contentY, float contentW) {
        CardLayout layout = this.getCardLayout(list, i, contentX, contentY, contentW, UI.CONTENT_HEIGHT - HEADER_OFFSET, 1.0f, true);
        if (layout == null || !layout.visible) {
            return null;
        }
        return new float[]{layout.cardX, layout.cardY, layout.cardW, layout.cardH};
    }

    public boolean hitToggle(float mouseX, float mouseY, float cardX, float cardY, float colW) {
        return false;
    }

    public boolean hitPin(float mouseX, float mouseY, float cardX, float cardY, float colW) {
        float gearX = cardX + colW - 9.0f - 10.0f;
        float pinX = gearX - 14.0f;
        float pinY = cardY + (CARD_H - 10.0f) * 0.5f;
        return mouseX >= pinX - 4.0f && mouseX <= pinX + 14.0f
            && mouseY >= pinY - 4.0f && mouseY <= pinY + 14.0f;
    }

    public boolean hitSettingsIcon(float mouseX, float mouseY, float cardX, float cardY, float colW, Module module) {
        if (module == null || module.getSettings().all().isEmpty()) return false;
        float gearX = cardX + colW - 9.0f - 10.0f;
        float gearY = cardY + (CARD_H - 10.0f) * 0.5f;
        return mouseX >= gearX - 4.0f && mouseX <= gearX + 14.0f
            && mouseY >= gearY - 4.0f && mouseY <= gearY + 14.0f;
    }

    public float baseCardHeight(Module module, float f) {
        return CARD_H;
    }

    public float totalCardHeight(Module module, float f) {
        return CARD_H;
    }

    private float totalContentH(List<Module> list, float f) {
        int rows = (list.size() + 1) / 2;
        if (rows <= 0) {
            return 16.0f;
        }
        return CARD_PAD * 2.0f + (float) rows * (CARD_H + CARD_GAP) - CARD_GAP;
    }

    public static float colW(float contentW) {
        return Math.max(80.0f, (contentW - CARD_PAD * 2.0f - CARD_GAP) * 0.5f);
    }

    private static int rowCount(int count) {
        return (count + 1) / 2;
    }

    public static char iconChar(Category category) {
        return NvIcons.charForCategory(category);
    }

    private static String badgeLabel(KeyBind keyBind) {
        String string = keyBind.getDisplayName();
        return string == null || string.isEmpty() ? "?" : string;
    }

    private static int clamp255(float f) {
        return Math.max(0, Math.min(255, Math.round(f)));
    }

    private static int rgba(int n, int n2, int n3, float f) {
        int n4 = ModuleListRenderer.clamp255(f);
        if (n4 <= 0) {
            return 0;
        }
        return new Color(n, n2, n3, n4).getRGB();
    }

    private static Decelerate createAnim(int n) {
        Decelerate decelerate = (Decelerate) new Decelerate().setMs(n).setValue(1.0);
        decelerate.setDirection(Direction.BACKWARDS);
        decelerate.counter.setTime(System.currentTimeMillis() - 10000L);
        return decelerate;
    }

    public void startRowAnims(int rows) {
        this.rowAppearAnims.clear();
        this.appearFadeMs = this.quickAppear ? QUICK_ROW_FADE_MS : ROW_FADE_MS;
        this.appearBaseMs = System.currentTimeMillis();
        this.appearInitialFrame = true;
        this.scroll = 0.0f;
        this.scrollTarget = 0.0f;
        for (int i = 0; i < rows; ++i) {
            Decelerate anim = (Decelerate) new Decelerate().setMs(this.appearFadeMs).setValue(1.0);
            anim.counter.setTime(this.appearBaseMs);
            anim.setDirection(Direction.FORWARDS);
            this.rowAppearAnims.put(i, anim);
        }
    }

    private float rowAppear(int n) {
        Decelerate decelerate = this.rowAppearAnims.get(n);
        if (decelerate == null) {
            long l = this.appearInitialFrame ? this.appearBaseMs : System.currentTimeMillis();
            decelerate = (Decelerate) new Decelerate().setMs(this.appearFadeMs).setValue(1.0);
            decelerate.counter.setTime(l + (long) n * 15L);
            decelerate.setDirection(Direction.FORWARDS);
            this.rowAppearAnims.put(n, decelerate);
        }
        return decelerate.getOutput().floatValue();
    }

    private boolean hasUnfinishedAppear() {
        for (Decelerate decelerate : this.rowAppearAnims.values()) {
            if (decelerate.getOutput().floatValue() < 0.999f) {
                return true;
            }
        }
        return false;
    }

    public void invalidateCache() {
        this.moduleCache.clear();
    }

    public List<Module> getModules(Category category) {
        if (category == Category.PINNED) {
            return PinManager.getPinnedModules();
        }
        ModuleManager manager = ModuleManager.get();
        if (manager == null || manager.getAll().isEmpty()) return Collections.emptyList();
        LocalizationManager.Language language = LocalizationManager.getCurrentLanguage();
        if (moduleCacheSize != manager.getAll().size() || moduleCacheLanguage != language) {
            moduleCache.clear();
            moduleCacheSize = manager.getAll().size();
            moduleCacheLanguage = language;
        }
        return moduleCache.computeIfAbsent(category, k -> {
            List<Module> list = new ArrayList<>();
            for (Module m : manager.getAll()) {
                if (m.getCategory() == category) {
                    list.add(m);
                }
            }
            list.sort(Comparator.comparing(Module::getDisplayName, String.CASE_INSENSITIVE_ORDER));
            return list;
        });
    }

    public void scroll(double d, float f) {
        float maxScroll = Math.max(0.0f, this.contentH - f);
        this.scrollTarget = Math.max(0.0f, Math.min(maxScroll, this.scrollTarget - (float) d * 18.0f));
    }

    public boolean scrollbarGrab(float f, float f2) {
        return this.scrollBar.tryGrab(f, f2);
    }

    public void scrollbarRelease() {
        this.scrollBar.release();
    }

    public boolean scrollbarDragging() {
        return this.scrollBar.isDragging();
    }

    public float currentScroll() {
        return this.scroll;
    }

    public void resetScroll() {
        this.scroll = 0.0f;
        this.scrollTarget = 0.0f;
    }

    public void resetCardBlur() {
        this.cardBlurCount = 0;
        this.cardBlurMaxPhase = 0.0f;
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

    public void setAppearComposite(boolean bl) {
        this.appearComposite = bl;
    }

    public void focusModule(String string) {
        this.focusName = string;
        this.focusUntilMs = System.currentTimeMillis() + 5000L;
        this.focusScrollPending = true;
    }

    public boolean isTransitioning() {
        return this.transitioning;
    }

    public void beginFadeOut(Category category) {
        if (category == null) {
            return;
        }
        this.fadingOut = this.getModules(category);
        this.fadeOutTime = FADE_OUT_DURATION;
        this.transitioning = true;
    }

    public boolean isFadeOutDone() {
        return this.transitioning && this.fadeOutTime <= 0.0f;
    }

    public void finishTransition() {
        this.transitioning = false;
        this.fadingOut = Collections.emptyList();
        this.fadeOutTime = 0.0f;
        this.scroll = 0.0f;
        this.scrollTarget = 0.0f;
        this.lastRendered = null;
    }

    // Compatibility methods for Inspector and Popups
    public boolean isExpanded(Module module) {
        return UI.INSTANCE.getInspector().isOpen() && UI.INSTANCE.getInspector().getFocusedModule() == module;
    }

    public boolean isExpanded(String name) {
        return UI.INSTANCE.getInspector().isOpen() && UI.INSTANCE.getInspector().getFocusedModule() != null
            && UI.INSTANCE.getInspector().getFocusedModule().getName().equals(name);
    }

    public void toggleExpand(Module module) {
        if (module != null) {
            if (this.isExpanded(module)) {
                UI.INSTANCE.getInspector().close();
            } else {
                UI.INSTANCE.getInspector().open(module);
            }
        }
    }

    public void releaseAllDrags() {
        this.scrollBar.release();
        UI.INSTANCE.getInspector().mouseReleased(0);
    }

    public boolean mouseBind(int button) {
        return false;
    }

    public boolean middleClick(float mouseX, float mouseY, List<Module> list, float contentX, float contentY, float contentW) {
        return false;
    }

    public boolean clickOverlays(float mouseX, float mouseY, List<Module> list, float contentX, float contentY, float contentW) {
        return false;
    }

    public boolean scrollOverlays(float mouseX, float mouseY, double amount, List<Module> list, float contentX, float contentY, float contentW) {
        return false;
    }

    public boolean clickSettings(Module module, float cardX, float cardY, float colW, float mouseX, float mouseY, int button) {
        if (module != null) {
            UI.INSTANCE.getInspector().open(module);
            return true;
        }
        return false;
    }

    public boolean keyPressed(KeyInput input) {
        return UI.INSTANCE.getInspector().keyPressed(input);
    }

    public boolean charTyped(CharInput input) {
        return UI.INSTANCE.getInspector().charTyped(input);
    }

    public boolean isAnyWidgetFocused() {
        return false;
    }

    public void warmup() {
        for (rtx.nv.api.modules.Category cat : rtx.nv.api.modules.Category.values()) {
            this.getModules(cat);
        }
        for (rtx.nv.api.modules.Module m : rtx.nv.api.modules.ModuleManager.get().getAll()) {
            this.enableAnims.computeIfAbsent(m.getName(), k -> {
                rtx.nv.utils.animations.Decelerate d = (rtx.nv.utils.animations.Decelerate) new rtx.nv.utils.animations.Decelerate().setMs(TOGGLE_ANIM_MS).setValue(1.0);
                d.setDirection(m.isEnabled() ? rtx.nv.utils.animations.Direction.FORWARDS : rtx.nv.utils.animations.Direction.BACKWARDS);
                d.counter.setTime(System.currentTimeMillis() - 10000L);
                return d;
            });
            this.edgeActivations.put(m.getName(), m.isEnabled() ? 1.0f : 0.0f);
        }
    }

    private static int indexOfModule(List<Module> list, String string) {
        for (int i = 0; i < list.size(); ++i) {
            if (list.get(i).getName().equals(string)) {
                return i;
            }
        }
        return -1;
    }

    private static int modulesSignature(List<Module> list) {
        int n = list.size();
        if (!list.isEmpty()) {
            n = n * 31 + list.get(0).getName().hashCode();
            n = n * 31 + list.get(list.size() - 1).getName().hashCode();
        }
        return n;
    }
}
