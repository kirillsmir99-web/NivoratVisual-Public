package rtx.nv.api.ui;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.profile.Profile;
import rtx.nv.api.profile.Role;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import rtx.nv.api.localization.LocalizationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import rtx.nv.IMinecraft;
import rtx.nv.api.drags.Position;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.input.MouseButtonEvent;
import rtx.nv.api.events.impl.input.MouseButtonEvent.Action;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.ClickGui;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareCloseState;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareLocalSnapshot;
import rtx.nv.api.modules.impl.Utils.guishare.GuiSharePopupRow;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareThemeState;
import rtx.nv.api.modules.restrict.Server;
import rtx.nv.api.modules.restrict.ServerRestrictions;
import rtx.nv.api.ui.BaseScreen;
import rtx.nv.api.ui.BindPopup;
import rtx.nv.api.ui.SettingsPopup;
import rtx.nv.api.ui.inspector.InspectorRenderer;
import rtx.nv.api.ui.pin.PinManager;
import rtx.nv.api.ui.module.DiscordAvatar;
import rtx.nv.api.ui.module.ModuleListRenderer;
import rtx.nv.api.ui.module.SearchField;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.settings.impl.BindSetting;
import rtx.nv.api.ui.settings.impl.TextSetting;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.api.ui.theme.AccentGradient;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.api.ui.theme.ThemeManager;
import rtx.nv.api.ui.theme.ThemesRenderer;
import rtx.nv.api.ui.theme.ThemeEditorRenderer;
import rtx.nv.api.ui.window.GuiShatterAnimation;
import rtx.nv.api.ui.window.WorldGuiCloseAnimation;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.animations.Decelerate;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.animations.GuiMotionAnimation;
import rtx.nv.utils.discord.rpc.DiscordRPCManager;
import rtx.nv.utils.key.KeyBind;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.post.guilayerblur.GuiCapture;
import rtx.nv.utils.render.post.guilayerblur.GuiCapture.Source;
import rtx.nv.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import rtx.nv.utils.render.post.guimotionblur.GuiMotionBlurRenderer;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.blur.BlurFramebuffer;
import rtx.nv.utils.render.render2d.gif.GifRenderer;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;
import rtx.nv.utils.sounds.Sounds;

public class UI
extends BaseScreen
implements GuiCapture.Source {
    public static final UI INSTANCE = new UI();
    public static final float PANEL_W = 430.0f;
    public static final float PANEL_H = 290.0f;
    public static final float DEFAULT_SIDEBAR_W = 90.0f;
    public static final float MIN_SIDEBAR_W = 38.0f;
    public static final float MAX_SIDEBAR_W = 145.0f;
    public static final float SIDEBAR_W = DEFAULT_SIDEBAR_W;

    public static float customSidebarW = DEFAULT_SIDEBAR_W;
    public static boolean isDraggingSidebar = false;
    private static float sidebarDragGrabX = 0.0f;
    private float splitterHoverT = 0.0f;

    public static float sidebarW() {
        return Math.max(MIN_SIDEBAR_W, Math.min(MAX_SIDEBAR_W, customSidebarW));
    }

    public static float contentXOff() {
        return sidebarW() + 8.0f;
    }

    public static float contentInset() {
        return contentXOff() + 5.0f;
    }

    private static float customPanelX = -1.0f;
    private static float customPanelY = -1.0f;
    private static boolean isDraggingPanel = false;
    private static float dragGrabX = 0.0f;
    private static float dragGrabY = 0.0f;

    public static float panelW() {
        float dock = 0.0f;
        if (INSTANCE != null) {
            if (INSTANCE.inspector != null && INSTANCE.inspector.isOpen()) {
                dock = INSTANCE.inspector.dockWidth();
            } else if (INSTANCE.themesRenderer != null && INSTANCE.themesRenderer.getEditor() != null && INSTANCE.themesRenderer.getEditor().isOpen()) {
                dock = INSTANCE.themesRenderer.getEditor().dockWidth();
            }
        }
        return PANEL_W + dock;
    }

    public static float panelX() {
        float curW = panelW();
        float preview = previewWidth();
        float defaultX = Math.max(8.0f + preview, (Position.screenWidth() - curW - preview) * .5f + preview);
        if (customPanelX < 0.0f) {
            return defaultX;
        }
        float maxX = Math.max(8.0f, Position.screenWidth() - curW - 8.0f);
        return Math.max(8.0f + preview, Math.min(maxX, customPanelX));
    }

    private static float previewWidth() {
        if (INSTANCE == null || !INSTANCE.inspector.isOpen()) return 0;
        var focused = INSTANCE.inspector.getFocusedModule();
        boolean hasPreview = (focused instanceof rtx.nv.api.modules.impl.Visuals.Wings)
            || (focused instanceof rtx.nv.api.modules.impl.Visuals.CustomPet);
        return hasPreview ? (WingPreviewRenderer.WIDTH + 8) * INSTANCE.inspector.dockProgress() : 0;
    }

    public static float layoutWidth() {
        return panelW() + previewWidth();
    }

    public static float panelY() {
        float baseY;
        float defaultY = Math.max(8.0f, Position.screenHeight() / 2.0f - PANEL_H / 2.0f);
        if (customPanelY < 0.0f) {
            baseY = defaultY;
        } else {
            float maxY = Math.max(8.0f, Position.screenHeight() - PANEL_H - 8.0f);
            baseY = Math.max(8.0f, Math.min(maxY, customPanelY));
        }

        return baseY;
    }

    public static final float CONTENT_X_OFF = DEFAULT_SIDEBAR_W + 8.0f;
    public static final float CONTENT_INSET = CONTENT_X_OFF + 5.0f;
    public static final float CONTENT_HEIGHT = 280.0f;
    public static final float CONTENT_Y_OFFSET = 5.0f;
    public static final float HEADER_H = 22.0f;
    public static final float HEADER_OFFSET = 26.0f;
    private static final Category[] MAIN_CATEGORIES = rtx.nv.ClientEdition.isTrial()
        ? new Category[]{Category.VISUALS, Category.DISPLAY}
        : new Category[]{Category.VISUALS, Category.DISPLAY, Category.MEDIA, Category.UTILS};
    private static final Category[] SYSTEM_CATEGORIES = new Category[]{Category.PINNED, Category.THEMES};
    private static final float CAT_COL_TOP = 31.0f;
    private static final float CAT_HEADER_H = 20.0f;
    private static final float CAT_SUB_GAP = 3.0f;
    private static final float CAT_SUB_ROW_H = 17.0f;
    private static final float CAT_OTHERS_GAP = 8.0f;
    private static final float CAT_OTHER_ROW_H = 17.0f;
    private static Category lastSelectedCategory = Category.VISUALS;
    private Category targetCategory = Category.VISUALS;
    private Category contentCategory = Category.VISUALS;
    private String oldSubText = Category.VISUALS.getDisplayName();
    private String newSubText = Category.VISUALS.getDisplayName();
    private final Map<Category, Decelerate> categoryAnims = new EnumMap<Category, Decelerate>(Category.class);
    private final Decelerate subTextAnim = UI.createAnim(300);
    private final Decelerate modulesHeaderAnim = UI.createAnim(200);
    private boolean subTextAnimDone = true;
    private static final float CATEGORY_FADE_SEC = 0.15f;
    private float categoryT = 1.0f;
    private float themesRowT = 1.0f;
    private final ModuleListRenderer moduleList = new ModuleListRenderer();
    private final InspectorRenderer inspector = new InspectorRenderer();
    private final SearchField search = new SearchField();
    private final BindPopup bindPopup = new BindPopup();
    private final SettingsPopup settingsPopup = new SettingsPopup();
    private final ThemesRenderer themesRenderer = new ThemesRenderer();
    private float lastAboutResetPosBtnX, lastAboutResetPosBtnY, lastAboutResetPosBtnW, lastAboutResetPosBtnH;
    private float lastAboutResetSidebarBtnX, lastAboutResetSidebarBtnY, lastAboutResetSidebarBtnW, lastAboutResetSidebarBtnH;
    private float lastAboutResetThemeBtnX, lastAboutResetThemeBtnY, lastAboutResetThemeBtnW, lastAboutResetThemeBtnH;

    private final rtx.nv.utils.animations.SmoothAnimation langAnim = new rtx.nv.utils.animations.SmoothAnimation();
    private final rtx.nv.utils.animations.SmoothAnimation langCrossfadeAnim = new rtx.nv.utils.animations.SmoothAnimation();
    private final rtx.nv.utils.animations.SmoothAnimation langWordFade = new rtx.nv.utils.animations.SmoothAnimation();
    private boolean langAnimInitialized = false;
    private static float activeWordAlpha = 1.0f;
    private static float activeWordOffsetY = 0.0f;

    public static float getLangWordAlpha() {
        return activeWordAlpha;
    }

    public static float getLangWordOffsetY() {
        return activeWordOffsetY;
    }

    public InspectorRenderer getInspector() {
        return this.inspector;
    }
    private final GuiMotionAnimation screenAnim = new GuiMotionAnimation();
    private long lastNs = System.nanoTime();
    private static Screen pendingAfterClose;
    private static boolean motionBlurPending;
    private static float motionBlurOpacity;
    private static float motionBlurRadius;
    private static float motionBlurScale;
    private static float motionBlurOriginX;
    private static float motionBlurOriginY;
    private static int motionBlurX;
    private static int motionBlurY;
    private static int motionBlurW;
    private static int motionBlurH;
    private static int motionBlurSrcX;
    private static int motionBlurSrcY;
    private static int motionBlurSrcW;
    private static int motionBlurSrcH;
    private static final float[] motionBlurMask;
    private static final float CARD_BLUR_MAX_RADIUS = 22.0f;
    private static boolean cardStratumMarked;
    private static boolean cardBlurPending;
    private static final float[] cardBlurMask;
    private static int cardBlurMaskCount;
    private static boolean cardCaptureStaged;
    private static boolean cardBlurCaptured;
    private static boolean panelSplitMarked;
    private static boolean vanillaBlurRequested;
    private static boolean popupStratumMarked;
    private static final float POPUP_BLUR_GUI_RADIUS = 9.0f;
    private static final float[] popupBlurMask;
    private static int popupBlurMaskCount;
    private static boolean popupLayerBlurWanted;
    private static boolean popupBlurWanted;
    private static boolean popupBlurStaged;
    private static boolean popupBlurCaptured;
    private static boolean popupBlurPending;
    private static float popupBlurRadius;
    private static int popupBlurX;
    private static int popupBlurY;
    private static int popupBlurW;
    private static int popupBlurH;
    private static float popupBlurOriginX;
    private static float popupBlurOriginY;
    private static float guiCaptureScale;
    private static float guiCaptureBlurMainPx;
    private final Decelerate placeholderAnim = UI.createAnim(200);
    private float parallaxX;
    private float parallaxY;
    private float lastCameraYaw = Float.NaN;
    private float lastCameraPitch;
    private static final String RU_LAYOUT = "\u0439\u0446\u0443\u043a\u0435\u043d\u0433\u0448\u0449\u0437\u0445\u044a\u0444\u044b\u0432\u0430\u043f\u0440\u043e\u043b\u0434\u0436\u044d\u044f\u0447\u0441\u043c\u0438\u0442\u044c\u0431\u044e\u0451";
    private static final String EN_LAYOUT = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`";

    private UI() {
        super((Text)Text.literal((String)"UI"));
        this.placeholderAnim.setDirection(Direction.FORWARDS);
        EventBus.get().subscribe(this);
    }

    static {
        motionBlurMask = new float[12];
        cardBlurMask = new float[396];
        popupBlurMask = new float[18];
        guiCaptureScale = 1.0f;
    }

    public static boolean isOpen() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        return minecraftClient != null && minecraftClient.currentScreen == INSTANCE;
    }

    public static boolean isPanelActive() {
        if (isOpen()) {
            return true;
        }
        return INSTANCE != null && INSTANCE.screenAnim.isClosing();
    }

    private static int color(int n, int n2, int n3, int n4, float f) {
        int n5 = Math.max(0, Math.min(255, Math.round((float)n4 * f)));
        if (n5 <= 0) {
            return 0;
        }
        return n5 << 24 | (n & 255) << 16 | (n2 & 255) << 8 | n3 & 255;
    }



    public void warmupRender() {
        this.moduleList.warmup();
    }

    public static void closeInto(Screen screen) {
        pendingAfterClose = screen;
        UI uI = INSTANCE;
        WorldGuiCloseAnimation.cancel();
        GuiShatterAnimation.cancel();
        uI.screenAnim.snapClosed();
        uI.releaseAllDrags();
        uI.settingsPopup.close();
        uI.inspector.close();
        uI.search.collapse();
        if (MinecraftClient.getInstance().currentScreen == uI) {
            MinecraftClient.getInstance().setScreen(screen);
        }
    }

    private static Decelerate createAnim(int n) {
        Decelerate decelerate = (Decelerate)new Decelerate().setMs(n).setValue(1.0);
        decelerate.setDirection(Direction.BACKWARDS);
        decelerate.counter.setTime(System.currentTimeMillis() - 10000L);
        return decelerate;
    }

    private static char iconChar(Category category) {
        return rtx.nv.utils.render.fonts.NvIcons.charForCategory(category);
    }

    private static boolean ctrlHeld() {
        long l = IMinecraft.mc.getWindow().getHandle();
        return GLFW.glfwGetKey((long)l, (int)341) == 1 || GLFW.glfwGetKey((long)l, (int)345) == 1;
    }

    @EventHandler
    private void onRawMouse(MouseButtonEvent mouseButtonEvent) {
        if (MinecraftClient.getInstance().currentScreen != this) {
            return;
        }
        if (mouseButtonEvent.action != MouseButtonEvent.Action.PRESS || mouseButtonEvent.button != 2) {
            return;
        }
        float f = PANEL_W;
        float f3 = panelX();
        float f4 = panelY();
        float f7 = f3 + contentXOff();
        float f8 = f4 + CONTENT_Y_OFFSET + HEADER_OFFSET;
        float f9 = f - contentInset();
        if (this.isModuleView() && this.moduleList.middleClick(Position.mouseX(), Position.mouseY(), this.filteredModules(this.contentCategory), f7, f8, f9)) {
            mouseButtonEvent.cancel();
            return;
        }
        Module module = this.moduleAtCursor();
        if (module != null) {
            this.bindPopup.open(module, Position.mouseX(), Position.mouseY());
            mouseButtonEvent.cancel();
        }
    }

    public void close() {
        WorldGuiCloseAnimation.cancel();
        GuiShatterAnimation.cancel();
        this.screenAnim.snapClosed();
        this.releaseAllDrags();
        this.settingsPopup.close();
        this.inspector.close();
        this.search.collapse();
        Sounds.play("gui_close");
        if (MinecraftClient.getInstance().currentScreen == this) {
            MinecraftClient.getInstance().setScreen(null);
        }
    }

    public boolean keyPressed(KeyInput input) {
        int n;
        Setting setting;
        if (this.screenAnim.isClosing()) {
            ClickGui clickGui = ModuleManager.get().get(ClickGui.class);
            int n2 = (clickGui != null && clickGui.getBind().getCode() > 0) ? clickGui.getBind().getCode() : 344;
            if (input.key() == n2 || input.key() == 344) {
                pendingAfterClose = null;
                WorldGuiCloseAnimation.reverse();
                GuiShatterAnimation.gather(WorldGuiCloseAnimation.isReversing() ? WorldGuiCloseAnimation.remainingNanos() : 0L);
                this.screenAnim.resumeOpening();
                Sounds.play("gui_open");
            }
            return true;
        }
        if (this.themesRenderer.getEditor().isOpen()) {
            if (this.themesRenderer.getEditor().keyPressed(input.key())) {
                return true;
            }
        }
        if (this.bindPopup.isOpen()) {
            if (this.bindPopup.keyPressed(input.key())) {
                return true;
            }
            if (input.key() == 256) {
                this.bindPopup.close();
            }
            return true;
        }
        if (this.contentCategory == Category.THEMES && this.themesRenderer.keyPressed(input)) {
            return true;
        }
        if (this.inspector.isOpen() && this.inspector.keyPressed(input)) {
            return true;
        }
        if (this.moduleList.keyPressed(input)) {
            return true;
        }
        if (this.search.isTyping() && this.search.keyPressed(input)) {
            return true;
        }
        ClickGui clickGui = ModuleManager.get().get(ClickGui.class);
        n = (clickGui != null && clickGui.getBind().getCode() > 0) ? clickGui.getBind().getCode() : 344;
        if (input.key() == 256) {
            if (this.inspector.isOpen()) {
                this.inspector.close();
                return true;
            }
            this.close();
            return true;
        }
        if (input.key() == n || input.key() == 344) {
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    public void removed() {
        PetPreviewRenderer.reset();
        if (!this.screenAnim.isClosing()) {
            this.screenAnim.snapClosed();
            GuiShatterAnimation.cancel();
            this.settingsPopup.close();
            this.inspector.close();
            this.search.collapse();
        }
        ThemeManager.clearLiveOverride();
        this.releaseAllDrags();
        super.removed();
    }

    protected void init() {
        pendingAfterClose = null;
        BaseScreen.dropClosingOverlay();
        GuiCapture.bind(this);
        if (!this.search.hasText()) {
            this.search.collapse();
        }
        Category start = this.resolveStartCategory();
        if (start == null) start = Category.VISUALS;
        this.targetCategory = start;
        this.contentCategory = start;
        lastSelectedCategory = start;
        savedStartCategoryName = start.name();
        this.oldSubText = start.getDisplayName();
        this.newSubText = start.getDisplayName();
        this.subTextAnim.setValue(1.0);
        this.subTextAnim.setDirection(Direction.FORWARDS);
        this.subTextAnimDone = true;
        this.categoryT = 1.0f;
        for (Category category4 : Category.values()) {
            this.getCategoryAnim(category4).setValue(category4 == start ? 1.0 : 0.0);
            this.getCategoryAnim(category4).setDirection(category4 == start ? Direction.FORWARDS : Direction.BACKWARDS);
        }
        this.modulesHeaderAnim.setValue(UI.isMainCategory(start) ? 1.0 : 0.0);
        this.modulesHeaderAnim.setDirection(UI.isMainCategory(start) ? Direction.FORWARDS : Direction.BACKWARDS);
        if (this.screenAnim.isClosing()) {
            WorldGuiCloseAnimation.reverse();
            GuiShatterAnimation.gather(WorldGuiCloseAnimation.isReversing() ? WorldGuiCloseAnimation.remainingNanos() : 0L);
            this.screenAnim.resumeOpening();
        } else {
            WorldGuiCloseAnimation.cancel();
            GuiShatterAnimation.cancel();
            this.screenAnim.startOpening();
        }
        this.lastNs = System.nanoTime();
    }

    public boolean mouseClicked(Click click, boolean doubled) {
        if (!this.screenAnim.canInteract()) {
            return true;
        }
        if (this.themesRenderer.getEditor().isOpen()) {
            if (this.themesRenderer.getEditor().mouseClicked(Position.mouseX(), Position.mouseY(), click.button())) {
                return true;
            }
            Category category = this.categoryButtonAt(panelX(), panelY(), Position.mouseX(), Position.mouseY());
            if (category != null && click.button() == 0) {
                this.themesRenderer.getEditor().promptLeaveCategory(category);
                return true;
            }
            float f3 = panelX();
            float f4 = panelY();
            float f5 = Position.mouseX();
            float f6 = Position.mouseY();
            float thX = f3 + contentXOff();
            float thY = f4 + CONTENT_Y_OFFSET;
            float thW = PANEL_W - contentInset();
            float thH = CONTENT_HEIGHT;
            if (f5 >= thX && f5 <= thX + thW && f6 >= thY && f6 <= thY + thH) {
                if (this.contentCategory == Category.THEMES && click.button() == 0) {
                    if (this.themesRenderer.click(f3, f4, PANEL_W, f5, f6)) {
                        return true;
                    }
                }
            }
            return true;
        }
        float f = PANEL_W;
        float f2 = PANEL_H;
        float f3 = panelX();
        float f4 = panelY();
        float f5 = Position.mouseX();
        float f6 = Position.mouseY();

        boolean isShift = false;
        if (MinecraftClient.getInstance().getWindow() != null) {
            long winHandle = MinecraftClient.getInstance().getWindow().getHandle();
            isShift = GLFW.glfwGetKey(winHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                      GLFW.glfwGetKey(winHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        }

        if (isShift) {
            if (click.button() == 0) {
                float px = panelX();
                float py = panelY();
                if (f5 >= px - 20.0f && f5 <= px + panelW() + 20.0f && f6 >= py - 20.0f && f6 <= py + PANEL_H + 20.0f) {
                    isDraggingPanel = true;
                    dragGrabX = f5 - px;
                    dragGrabY = f6 - py;
                    return true;
                }
            } else if (click.button() == 2) {
                customPanelX = -1.0f;
                customPanelY = -1.0f;
                isDraggingPanel = false;
                return true;
            }
        }

        // Telegram-style resizable sidebar splitter rail hit test
        float railX = f3 + 5.0f + sidebarW() + 1.5f;
        float railY = f4 + 5.0f;
        float railH = PANEL_H - 10.0f;
        if (f5 >= railX - 4.0f && f5 <= railX + 4.0f && f6 >= railY && f6 <= railY + railH) {
            if (click.button() == 2 || doubled) {
                customSidebarW = DEFAULT_SIDEBAR_W;
                isDraggingSidebar = false;
                Sounds.play("select_category");
                return true;
            }
            if (click.button() == 0) {
                isDraggingSidebar = true;
                sidebarDragGrabX = f5 - (f3 + 5.0f + customSidebarW);
                return true;
            }
        }

        if (this.moduleList.mouseBind(click.button())) {
            return true;
        }
        if (this.bindPopup.isOpen() && this.bindPopup.mouseBind(click.button())) {
            return true;
        }
        if (this.bindPopup.isOpen() && click.button() == 0) {
            this.bindPopup.click(f5, f6);
            return true;
        }
        if (this.inspector.isOpen() && this.inspector.getFocusedModule() instanceof rtx.nv.api.modules.impl.Visuals.Wings) {
            float prevX = f3 - WingPreviewRenderer.WIDTH - 8.0f;
            float prevY = f4 + CONTENT_Y_OFFSET;
            if (WingPreviewRenderer.mouseClicked(f5, f6, click.button(), doubled, prevX, prevY, CONTENT_HEIGHT)) {
                return true;
            }
        } else if (this.inspector.isOpen() && this.inspector.getFocusedModule() instanceof rtx.nv.api.modules.impl.Visuals.CustomPet) {
            float prevX = f3 - WingPreviewRenderer.WIDTH - 8.0f;
            float prevY = f4 + CONTENT_Y_OFFSET;
            if (PetPreviewRenderer.mouseClicked(f5, f6, click.button(), doubled, prevX, prevY, CONTENT_HEIGHT)) {
                return true;
            }
        }
        if (this.inspector.isOpen() && this.inspector.mouseClicked(f5, f6, click.button())) {
            return true;
        }
        if (this.isModuleView() && this.search.mouseClicked(f5, f6, click.button())) {
            return true;
        }
        if (this.contentCategory == Category.THEMES && click.button() == 0 && this.themesRenderer.click(f3, f4, f, f5, f6)) {
            return true;
        }
        if (this.contentCategory == Category.ABOUT && click.button() == 0 && this.clickAboutCategory(f5, f6)) {
            return true;
        }
        if (click.button() == 0 || click.button() == 1) {
            boolean bl = this.isModuleView();
            if (bl && this.moduleList.scrollbarGrab(f5, f6)) {
                return true;
            }
            float f7 = f3 + contentXOff();
            float f8 = f4 + CONTENT_Y_OFFSET + HEADER_OFFSET;
            float f9 = f - contentInset();
            float f10 = CONTENT_HEIGHT - HEADER_OFFSET;
            if (bl) {
                List<Module> list = this.filteredModules(this.contentCategory);
                if (this.moduleList.clickOverlays(f5, f6, list, f7, f8, f9)) {
                    return true;
                }
                if (f5 >= f7 && f5 <= f7 + f9 && f6 >= f8 && f6 <= f8 + f10) {
                    for (int i = 0; i < list.size(); ++i) {
                        Module module = list.get(i);
                        ModuleListRenderer.CardLayout layout = this.moduleList.getCardLayout(list, i, f7, f8, f9, f10, 1.0f, true);
                        if (layout == null || !layout.visible) continue;
                        if (!layout.containsCard(f5, f6)) continue;

                        float baseH = this.moduleList.baseCardHeight(module, layout.cardW);
                        if (f6 > layout.cardY + baseH) {
                            this.moduleList.clickSettings(module, layout.cardX, layout.cardY, layout.cardW, f5, f6, click.button());
                            return true;
                        }

                        if (this.search.hasText() && UI.ctrlHeld()) {
                            Category category = module.getCategory();
                            this.moduleList.focusModule(module.getName());
                            if (this.targetCategory != category) {
                                Sounds.play("select_category");
                                this.selectCategory(category);
                            } else {
                                this.search.setText("");
                                this.search.blur();
                            }
                            return true;
                        }

                        if (click.button() == 1) {
                            if (!module.getSettings().all().isEmpty()) {
                                if (this.inspector.getFocusedModule() == module && this.inspector.isOpen()) {
                                    this.inspector.close();
                                } else {
                                    this.inspector.open(module);
                                }
                            }
                            return true;
                        }

                        if (click.button() == 0) {
                            if (layout.hitPin(f5, f6)) {
                                PinManager.toggle(module);
                                return true;
                            }
                            if (layout.hitSettings(f5, f6)) {
                                if (this.inspector.getFocusedModule() == module && this.inspector.isOpen()) {
                                    this.inspector.close();
                                } else {
                                    this.inspector.open(module);
                                }
                                return true;
                            }
                            module.toggle();
                            return true;
                        }
                        return true;
                    }
                }
            }
            if (click.button() == 1) {
                return true;
            }
        }

        // Language toggle button hit test in header (directly to the left of profile)
        if (this.isModuleView() && click.button() == 0) {
            float f7 = f3 + contentXOff();
            float f8 = f4 + CONTENT_Y_OFFSET;
            float f9 = f - contentInset();
            float f10 = 6.0f;
            float f14 = 16.0f;
            float f15 = f7 + f9 - f10 - f14;
            String profName = UI.profileName();
            String profRole = UI.profileRole();
            float f19 = Fonts.MONTSERRAT_MEDIUM.width(profName, 6.5f);
            float f20 = Fonts.MONTSERRAT_MEDIUM.width(profRole, 5.0f);
            float f21 = f15 - 5.0f;
            float profileLeft = f21 - Math.max(f19, f20);

            float langW = 44.0f;
            float langH = 14.0f;
            float langX = profileLeft - langW - 8.0f;
            float langY = f8 + (HEADER_H - langH) * 0.5f;

            if (f5 >= langX && f5 <= langX + langW && f6 >= langY && f6 <= langY + langH) {
                boolean isCurrentlyRu = rtx.nv.api.localization.LocalizationManager.getCurrentLanguage() == rtx.nv.api.localization.LocalizationManager.Language.RU;
                rtx.nv.api.localization.LocalizationManager.setLanguage(isCurrentlyRu ? rtx.nv.api.localization.LocalizationManager.Language.EN : rtx.nv.api.localization.LocalizationManager.Language.RU);
                this.moduleList.invalidateCache();
                double targetBadge = isCurrentlyRu ? 1.0 : 0.0;
                this.langAnim.run(targetBadge, 0.28, rtx.nv.utils.animations.Easings.CUBIC_OUT, false);
                this.langWordFade.set(0.12);
                this.langWordFade.run(1.0, 0.32, rtx.nv.utils.animations.Easings.CUBIC_OUT, false);
                Sounds.play("select_category");
                return true;
            }
        }

        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        Category category = this.categoryButtonAt(f3, f4, f5, f6);
        if (category != null) {
            Sounds.play("select_category");
            this.selectCategory(category);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.screenAnim.canInteract()) {
            return true;
        }
        if (this.bindPopup.isOpen()) {
            this.bindPopup.close();
            return true;
        }
        double d = Position.mouseX();
        double d2 = Position.mouseY();

        float f2 = panelX();
        float f3 = panelY();
        float prevX = f2 - WingPreviewRenderer.WIDTH - 8.0f;
        float prevY = f3 + CONTENT_Y_OFFSET;
        if (this.inspector.isOpen() && this.inspector.getFocusedModule() instanceof rtx.nv.api.modules.impl.Visuals.Wings) {
            if (WingPreviewRenderer.mouseScrolled(d, d2, verticalAmount, prevX, prevY, CONTENT_HEIGHT)) {
                return true;
            }
        } else if (this.inspector.isOpen() && this.inspector.getFocusedModule() instanceof rtx.nv.api.modules.impl.Visuals.CustomPet) {
            if (PetPreviewRenderer.mouseScrolled(d, d2, verticalAmount, prevX, prevY, CONTENT_HEIGHT)) {
                return true;
            }
        }
        if (this.inspector.isOpen() && this.inspector.mouseScrolled(d, d2, verticalAmount)) {
            return true;
        }
        float f = PANEL_W;
        float f4 = f2 + contentXOff();
        float f5 = f3 + CONTENT_Y_OFFSET;
        float f6 = f - contentInset();
        float f7 = CONTENT_HEIGHT;
        if (this.themesRenderer.getEditor().isOpen()) {
            if (this.themesRenderer.getEditor().mouseScrolled(d, d2, verticalAmount)) {
                return true;
            }
        }
        if (d >= (double)f4 && d <= (double)(f4 + f6) && d2 >= (double)f5 && d2 <= (double)(f5 + f7)) {
            if (this.contentCategory == Category.THEMES) {
                this.themesRenderer.scroll(verticalAmount, f7);
            } else if (this.contentCategory == Category.ABOUT) {
                // about page is static
            } else {
                float f8 = f3 + CONTENT_Y_OFFSET + HEADER_OFFSET;
                float f9 = f - contentInset();
                if (!this.moduleList.scrollOverlays((float)d, (float)d2, verticalAmount, this.filteredModules(this.contentCategory), f4, f8, f9)) {
                    this.moduleList.scroll(verticalAmount, f7 - HEADER_OFFSET);
                }
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (this.screenAnim.isClosing()) return true;
        if (WingPreviewRenderer.mouseDragged(deltaX, deltaY, click.button())) return true;
        if (PetPreviewRenderer.mouseDragged(deltaX, deltaY, click.button())) return true;
        if (this.themesRenderer.getEditor().mouseDragged(Position.mouseX(), Position.mouseY(), click.button())) return true;
        return super.mouseDragged(click, deltaX, deltaY);
    }

    public boolean mouseReleased(Click click) {
        if (this.screenAnim.isClosing()) {
            return true;
        }
        WingPreviewRenderer.mouseReleased(click.button());
        PetPreviewRenderer.mouseReleased(click.button());
        if (this.themesRenderer.getEditor().isOpen()) {
            this.themesRenderer.getEditor().mouseReleased(click.x(), click.y(), click.button());
            return true;
        }
        if (click.button() == 0) {
            isDraggingPanel = false;
            isDraggingSidebar = false;
            this.moduleList.releaseAllDrags();
            this.bindPopup.releaseDrag();
        }
        this.inspector.mouseReleased(click.button());
        this.moduleList.scrollbarRelease();
        this.search.mouseReleased(click.button());
        this.themesRenderer.mouseReleased(click.x(), click.y(), click.button());
        return super.mouseReleased(click);
    }

    public boolean charTyped(CharInput input) {
        if (this.screenAnim.isClosing()) {
            return true;
        }
        if (this.themesRenderer.getEditor().isOpen()) {
            if (this.themesRenderer.getEditor().charTyped((char) input.codepoint())) {
                return true;
            }
        }
        if (this.contentCategory == Category.THEMES && this.themesRenderer.charTyped(input)) {
            return true;
        }
        if (this.bindPopup.isOpen()) {
            return true;
        }
        if (this.inspector.isOpen() && this.inspector.charTyped(input)) {
            return true;
        }
        if (this.moduleList.charTyped(input)) {
            return true;
        }
        if (this.search.isTyping() && this.search.charTyped(input)) {
            return true;
        }
        return super.charTyped(input);
    }

    public static boolean isSearchTyping() {
        return UI.INSTANCE.search.isTyping();
    }

    public static boolean isInputActive() {
        if (!isOpen()) {
            return false;
        }
        if (INSTANCE.themesRenderer != null && INSTANCE.themesRenderer.getEditor().isOpen()) {
            return true;
        }
        if (isSearchTyping()) {
            return true;
        }
        if (INSTANCE.moduleList.isAnyWidgetFocused()) {
            return true;
        }
        if (INSTANCE.bindPopup != null && INSTANCE.bindPopup.isOpen()) {
            return true;
        }
        return false;
    }

    public static boolean guiCaptureActive() {
        boolean bl;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (!GuiCapture.isBound(INSTANCE)) {
            return false;
        }
        boolean bl2 = bl = WorldGuiCloseAnimation.isActive() && !WorldGuiCloseAnimation.isFinished() && (minecraftClient == null || minecraftClient.currentScreen == null || WorldGuiCloseAnimation.isReversing());
        boolean bl3 = UI.INSTANCE.screenAnim.isClosing() && (WorldGuiCloseAnimation.isActive() ? bl : !UI.INSTANCE.screenAnim.isCloseFinished());
        return !(!UI.isOpen() && !bl3 || !UI.INSTANCE.screenAnim.isAnimating() && !bl);
    }

    public static GuiShareLocalSnapshot shareSnapshot() {
        UI uI = INSTANCE;
        if (!UI.isOpen()) {
            return null;
        }
        Category category = uI.targetCategory;
        ArrayList<String> arrayList = new ArrayList<String>();
        for (Module module : uI.filteredModules(category)) {
            if (!module.isEnabled()) continue;
            arrayList.add(module.getName());
        }
        Object object = null;
        try {
            object = DiscordAvatar.currentUrl();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        float f = panelX();
        float f2 = panelY();
        return new GuiShareLocalSnapshot(category == null ? "" : category.name(), 0, 0.0f, uI.moduleList.currentScroll(), (Position.mouseX() - f) / PANEL_W, (Position.mouseY() - f2) / PANEL_H, uI.settingsPopup.shareModuleName(), uI.settingsPopup.shareScroll(), (uI.settingsPopup.shareX() - f) / PANEL_W, (uI.settingsPopup.shareY() - f2) / PANEL_H, GuiSharePopupRow.capture(uI.settingsPopup.shareModule(), uI.settingsPopup.widgets()), uI.search.getText(), arrayList, UI.profileRole(), (String)(object == null ? "" : object), GuiShareThemeState.capture(), ThemeManager.current().name(), uI.themesRenderer.currentScroll());
    }

    public static GuiShareCloseState shareCloseState() {
        if (!WorldGuiCloseAnimation.isActive() || WorldGuiCloseAnimation.isFinished()) {
            return GuiShareCloseState.IDLE;
        }
        float[] fArray = GuiShatterAnimation.beginRect();
        return new GuiShareCloseState(true, 1.0f - WorldGuiCloseAnimation.progress(), GuiShatterAnimation.local().lastProgress(), WorldGuiCloseAnimation.screenScale(), GuiShatterAnimation.seed(), fArray[0], fArray[1], fArray[2], fArray[3], fArray[4], fArray[5], fArray[6]);
    }

    private void releaseAllDrags() {
        isDraggingPanel = false;
        isDraggingSidebar = false;
        this.moduleList.releaseAllDrags();
        this.settingsPopup.releaseDrags();
        this.moduleList.scrollbarRelease();
    }

    public static float guiCaptureScale() {
        return guiCaptureScale;
    }

    public static boolean isSettingsPopupVisible() {
        return false;
    }

    public void openModuleSettings(Module module) {
        if (module == null) {
            return;
        }
        this.selectCategory(module.getCategory());
        this.search.setText("");
        this.search.blur();
        this.moduleList.focusModule(module.getName());
        this.inspector.open(module);
    }

    private static final class SearchCacheKey {
        private final String query;
        private final Category category;
        private final LocalizationManager.Language language;
        private final EnumSet<Server> restrictions;
        private final int moduleCount;

        SearchCacheKey(String query, Category category, LocalizationManager.Language language, EnumSet<Server> restrictions, int moduleCount) {
            this.query = query;
            this.category = category;
            this.language = language;
            this.restrictions = restrictions == null ? EnumSet.noneOf(Server.class) : EnumSet.copyOf(restrictions);
            this.moduleCount = moduleCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SearchCacheKey that = (SearchCacheKey) o;
            return moduleCount == that.moduleCount &&
                   Objects.equals(query, that.query) &&
                   category == that.category &&
                   language == that.language &&
                   Objects.equals(restrictions, that.restrictions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(query, category, language, restrictions, moduleCount);
        }
    }

    private SearchCacheKey lastSearchKey = null;
    private List<Module> lastSearchResults = Collections.emptyList();

    private List<Module> filteredModules(Category category) {
        String query = this.search.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            if (category == Category.PINNED) {
                return PinManager.getPinnedModules();
            }
            return this.moduleList.getModules(category);
        }

        LocalizationManager.Language lang = LocalizationManager.getCurrentLanguage();
        EnumSet<Server> enumSet = ServerRestrictions.current();
        int moduleSize = ModuleManager.get() != null ? ModuleManager.get().getAll().size() : 0;
        SearchCacheKey key = new SearchCacheKey(query, category, lang, enumSet, moduleSize);
        if (key.equals(this.lastSearchKey) && this.lastSearchResults != null) {
            return this.lastSearchResults;
        }

        String qEn = UI.layoutNormalize(query);
        String qRu = UI.toRuLayout(query);
        String qClean = query.replace(" ", "");
        String qEnClean = qEn.replace(" ", "");
        String qRuClean = qRu.replace(" ", "");
        String[] words = query.split("\\s+");

        ArrayList<Module> arrayList = new ArrayList<Module>();
        List<Module> pool = ModuleManager.get().getAll();

        for (Module module : pool) {
            if (ServerRestrictions.isHiddenBy(module, enumSet)) continue;
            if (this.matchesSearch(module, query, qEn, qRu, qClean, qEnClean, qRuClean, words)) {
                arrayList.add(module);
            }
        }

        arrayList.sort((m1, m2) -> {
            boolean exact1 = m1.getName().equalsIgnoreCase(query) || m1.getDisplayName().equalsIgnoreCase(query);
            boolean exact2 = m2.getName().equalsIgnoreCase(query) || m2.getDisplayName().equalsIgnoreCase(query);
            if (exact1 != exact2) return exact1 ? -1 : 1;

            boolean starts1 = m1.getName().toLowerCase(Locale.ROOT).startsWith(query) || m1.getDisplayName().toLowerCase(Locale.ROOT).startsWith(query);
            boolean starts2 = m2.getName().toLowerCase(Locale.ROOT).startsWith(query) || m2.getDisplayName().toLowerCase(Locale.ROOT).startsWith(query);
            if (starts1 != starts2) return starts1 ? -1 : 1;

            return m1.getDisplayName().compareToIgnoreCase(m2.getDisplayName());
        });

        this.lastSearchKey = key;
        this.lastSearchResults = Collections.unmodifiableList(arrayList);
        return this.lastSearchResults;
    }

    private boolean matchesSearch(Module module, String q, String qEn, String qRu, String qClean, String qEnClean, String qRuClean, String[] words) {
        String name = module.getName().toLowerCase(Locale.ROOT);
        String dispName = module.getDisplayName().toLowerCase(Locale.ROOT);
        String nameClean = name.replace(" ", "");
        String dispClean = dispName.replace(" ", "");

        if (name.contains(q) || dispName.contains(q) ||
            name.contains(qEn) || dispName.contains(qEn) ||
            name.contains(qRu) || dispName.contains(qRu) ||
            nameClean.contains(qClean) || dispClean.contains(qClean) ||
            nameClean.contains(qEnClean) || dispClean.contains(qEnClean) ||
            nameClean.contains(qRuClean) || dispClean.contains(qRuClean)) {
            return true;
        }

        String desc = module.getDescription();
        if (desc != null && !desc.isEmpty()) {
            String descLower = desc.toLowerCase(Locale.ROOT);
            if (descLower.contains(q) || descLower.contains(qEn) || descLower.contains(qRu)) {
                return true;
            }
        }

        if (words.length > 1) {
            boolean allWords = true;
            String combined = (name + " " + dispName + " " + (desc != null ? desc : "")).toLowerCase(Locale.ROOT);
            for (String w : words) {
                if (w.isEmpty()) continue;
                String wEn = UI.layoutNormalize(w);
                String wRu = UI.toRuLayout(w);
                if (!combined.contains(w) && !combined.contains(wEn) && !combined.contains(wRu)) {
                    allWords = false;
                    break;
                }
            }
            if (allWords) return true;
        }

        for (rtx.nv.api.modules.settings.Setting s : module.getSettings().all()) {
            String sName = s.getName().toLowerCase(Locale.ROOT);
            String sDisp = s.getDisplayName().toLowerCase(Locale.ROOT);
            if (sName.contains(q) || sDisp.contains(q) || sName.contains(qEn) || sDisp.contains(qEn) || sName.contains(qRu) || sDisp.contains(qRu)) {
                return true;
            }
            String sDesc = s.getDescription();
            if (sDesc != null && !sDesc.isEmpty()) {
                String sDescLower = sDesc.toLowerCase(Locale.ROOT);
                if (sDescLower.contains(q) || sDescLower.contains(qEn) || sDescLower.contains(qRu)) {
                    return true;
                }
            }
            if (s instanceof rtx.nv.api.modules.settings.impl.SelectSetting ss) {
                for (String opt : ss.getOptions()) {
                    String optLower = opt.toLowerCase(Locale.ROOT);
                    String optDisp = rtx.nv.api.localization.Lang.translateOption(opt).toLowerCase(Locale.ROOT);
                    if (optLower.contains(q) || optDisp.contains(q) || optLower.contains(qEn) || optDisp.contains(qEn) || optLower.contains(qRu) || optDisp.contains(qRu)) {
                        return true;
                    }
                }
            } else if (s instanceof rtx.nv.api.modules.settings.impl.MultiSelectSetting mss) {
                for (String opt : mss.getOptions()) {
                    String optLower = opt.toLowerCase(Locale.ROOT);
                    String optDisp = rtx.nv.api.localization.Lang.translateOption(opt).toLowerCase(Locale.ROOT);
                    if (optLower.contains(q) || optDisp.contains(q) || optLower.contains(qEn) || optDisp.contains(qEn) || optLower.contains(qRu) || optDisp.contains(qRu)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean isSettingsOpenFor(String string) {
        return UI.isOpen() && UI.INSTANCE.moduleList.isExpanded(string);
    }

    public static boolean consumePopupStratumMark() {
        boolean bl = popupStratumMarked;
        popupStratumMarked = false;
        return bl;
    }

    public static boolean consumePopupBlurCapture() {
        if (!popupBlurWanted) {
            return false;
        }
        popupBlurWanted = false;
        popupBlurCaptured = true;
        return true;
    }

    public static boolean consumePopupLayerCapture() {
        if (!popupLayerBlurWanted) {
            return false;
        }
        popupLayerBlurWanted = false;
        popupBlurCaptured = true;
        return true;
    }

    public static void requestVanillaBlurAtSplit() {
        vanillaBlurRequested = true;
    }

    public static boolean consumeVanillaBlurRequest() {
        boolean bl = vanillaBlurRequested;
        vanillaBlurRequested = false;
        return bl;
    }

    private static boolean isMainCategory(Category category) {
        if (category == null) {
            return false;
        }
        if (category == Category.PINNED) {
            return true;
        }
        for (Category category2 : MAIN_CATEGORIES) {
            if (category2 != category) continue;
            return true;
        }
        return false;
    }

    public static void applyMainCompositeAtSplit() {
        if (!motionBlurPending) {
            return;
        }
        motionBlurPending = false;
        cardBlurPending = false;
        GuiMotionBlurRenderer.applyWithCopy(motionBlurOpacity, motionBlurRadius, motionBlurX, motionBlurY, motionBlurW, motionBlurH, motionBlurMask, 2, motionBlurSrcX, motionBlurSrcY, motionBlurSrcW, motionBlurSrcH, motionBlurScale, motionBlurOriginX, motionBlurOriginY);
    }

    private void stagePopupBlur() {
        if (!popupBlurStaged) {
            return;
        }
        popupBlurStaged = false;
        int n = 1;
        if (this.settingsPopup.writeBlurRect(popupBlurMask, n * 6)) {
            ++n;
        }
        if (this.bindPopup.writeBlurRect(popupBlurMask, n * 6)) {
            ++n;
        }
        if (n <= 1) {
            return;
        }
        float f = 0.0f;
        for (int i = 1; i < n; ++i) {
            f = Math.max(f, popupBlurMask[i * 6 + 5]);
        }
        if (f <= 0.004f) {
            return;
        }
        float f2 = Render2DCoordinateSpace.designGuiScale();
        float f3 = Math.max(0.5f, 9.0f * f2 * f);
        float f4 = Float.MAX_VALUE;
        float f5 = Float.MAX_VALUE;
        float f6 = -3.4028235E38f;
        float f7 = -3.4028235E38f;
        for (int i = 1; i < n; ++i) {
            int n2 = i * 6;
            float f8 = popupBlurMask[n2 + 5] > 0.004f ? f3 : 0.0f;
            float f9 = (popupBlurMask[n2] + this.parallaxX) * f2 - f8;
            float f10 = (popupBlurMask[n2 + 1] + this.parallaxY) * f2 - f8;
            float f11 = popupBlurMask[n2 + 2] * f2 + f8 * 2.0f;
            float f12 = popupBlurMask[n2 + 3] * f2 + f8 * 2.0f;
            UI.popupBlurMask[n2] = f9;
            UI.popupBlurMask[n2 + 1] = f10;
            UI.popupBlurMask[n2 + 2] = f11;
            UI.popupBlurMask[n2 + 3] = f12;
            f4 = Math.min(f4, f9);
            f5 = Math.min(f5, f10);
            f6 = Math.max(f6, f9 + f11);
            f7 = Math.max(f7, f10 + f12);
        }
        float f13 = 24.0f + f3;
        UI.popupBlurMask[0] = f4 - f13;
        UI.popupBlurMask[1] = f5 - f13;
        UI.popupBlurMask[2] = f6 - f4 + f13 * 2.0f;
        UI.popupBlurMask[3] = f7 - f5 + f13 * 2.0f;
        UI.popupBlurMask[4] = -1.0f;
        UI.popupBlurMask[5] = 0.0f;
        popupBlurMaskCount = n;
        float f14 = f13 + f3 * 2.0f + 8.0f;
        popupBlurRadius = f3;
        popupBlurX = Math.round(f4 - f14);
        popupBlurY = Math.round(f5 - f14);
        popupBlurW = Math.round(f6 - f4 + f14 * 2.0f);
        popupBlurH = Math.round(f7 - f5 + f14 * 2.0f);
        popupBlurOriginX = (f4 + f6) * 0.5f;
        popupBlurOriginY = (f5 + f7) * 0.5f;
        popupBlurPending = true;
    }

    private void stageCardBlur(float f, float f2, float f3) {
        float f4;
        float f5;
        float[] fArray;
        float f6;
        int n;
        if (motionBlurPending || !cardCaptureStaged) {
            return;
        }
        float f7 = f + contentXOff();
        float f8 = f3 - contentInset();
        if (this.moduleList.cardBlurCount() > 0) {
            n = this.moduleList.cardBlurCount();
            f6 = this.moduleList.cardBlurMaxPhase();
            fArray = this.moduleList.cardBlurRects();
            f5 = f2 + CONTENT_Y_OFFSET + HEADER_OFFSET;
            f4 = CONTENT_HEIGHT - HEADER_OFFSET;
        } else {
            n = this.themesRenderer.cardBlurCount();
            f6 = this.themesRenderer.cardBlurMaxPhase();
            fArray = this.themesRenderer.cardBlurRects();
            f5 = f2 + CONTENT_Y_OFFSET;
            f4 = CONTENT_HEIGHT;
        }
        if (n <= 0 || f6 <= 0.003f) {
            return;
        }
        float f9 = Render2DCoordinateSpace.designGuiScale();
        float f10 = f7 * f9;
        float f11 = f5 * f9;
        float f12 = f8 * f9;
        float f13 = f4 * f9;
        float f14 = 48.0f;
        UI.cardBlurMask[0] = f10 - f14;
        UI.cardBlurMask[1] = f11 - f14;
        UI.cardBlurMask[2] = f12 + f14 * 2.0f;
        UI.cardBlurMask[3] = f13 + f14 * 2.0f;
        UI.cardBlurMask[4] = -1.0f;
        UI.cardBlurMask[5] = 0.0f;
        int n2 = 1;
        for (int i = 0; i < n; ++i) {
            int n3 = i * 6;
            int n4 = n2 * 6;
            UI.cardBlurMask[n4] = fArray[n3] * f9;
            UI.cardBlurMask[n4 + 1] = fArray[n3 + 1] * f9;
            UI.cardBlurMask[n4 + 2] = fArray[n3 + 2] * f9;
            UI.cardBlurMask[n4 + 3] = fArray[n3 + 3] * f9;
            UI.cardBlurMask[n4 + 4] = fArray[n3 + 4];
            UI.cardBlurMask[n4 + 5] = fArray[n3 + 5] / f6;
            ++n2;
        }
        cardBlurMaskCount = n2;
        motionBlurOpacity = 1.0f;
        motionBlurRadius = Math.max(0.5f, 22.0f * f6);
        float f15 = f14 + motionBlurRadius * 2.0f + 8.0f;
        motionBlurX = Math.round(f10 - f15);
        motionBlurY = Math.round(f11 - f15);
        motionBlurW = Math.round(f12 + f15 * 2.0f);
        motionBlurH = Math.round(f13 + f15 * 2.0f);
        motionBlurScale = 1.0f;
        motionBlurOriginX = (f7 + f8 * 0.5f) * f9;
        motionBlurOriginY = (f5 + f4 * 0.5f) * f9;
        cardBlurPending = true;
    }

    public static boolean motionBlurCapturePending() {
        return motionBlurPending;
    }

    private static void beginShatter() {
        if (GuiShatterAnimation.resume()) {
            return;
        }
        float f = Render2DCoordinateSpace.designGuiScale();
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        float f2 = interfaceModule != null && interfaceModule.rectGlow.getValue() ? interfaceModule.rectGlowRadius.getFloat() : 0.0f;
        float f3 = (f2 + 14.0f) * f;
        float f4 = PANEL_W * f;
        float f5 = PANEL_H * f;
        float f6 = Position.screenWidth() * f;
        float f7 = Position.screenHeight() * f;
        float sx = panelX() * f;
        float sy = panelY() * f;
        GuiShatterAnimation.begin(sx, sy, f4, f5, f3, f6, f7);
    }

    public static boolean consumeCardStratumMark() {
        boolean bl = cardStratumMarked;
        cardStratumMarked = false;
        if (bl) {
            cardBlurCaptured = true;
        }
        return bl;
    }

    public static boolean popupLayerCapturePending() {
        return popupLayerBlurWanted;
    }

    public static float motionBlurCaptureRadius() {
        return motionBlurRadius;
    }

    public static void markPopupLayerCapture() {
        popupLayerBlurWanted = true;
    }

    public static boolean consumePanelSplitMark() {
        boolean bl = panelSplitMarked;
        panelSplitMarked = false;
        return bl;
    }

    public static void markPopupStratum(boolean bl, boolean bl2) {
        popupStratumMarked = true;
        popupBlurWanted = bl;
        popupBlurStaged = bl2;
        popupBlurCaptured = false;
    }

    private static String profileRole() {
        Role role = Profile.getRole();
        if (role == null || role == Role.USER) {
            return "NV";
        }
        return role.name();
    }

    public static void markCardStratum() {
        cardStratumMarked = true;
        cardCaptureStaged = true;
    }

    public static void dropPendingBlurs() {
        cardStratumMarked = false;
        panelSplitMarked = false;
        vanillaBlurRequested = false;
        popupStratumMarked = false;
        popupLayerBlurWanted = false;
        popupBlurWanted = false;
        popupBlurStaged = false;
        popupBlurCaptured = false;
        popupBlurPending = false;
        cardBlurPending = false;
        cardCaptureStaged = false;
        cardBlurCaptured = false;
    }

    public static float guiShatterProgress() {
        float f = WorldGuiCloseAnimation.isActive() ? WorldGuiCloseAnimation.progress() : UI.INSTANCE.screenAnim.closeProgress();
        return GuiShatterAnimation.progress(f);
    }

    @Override
    public float shatterProgress() {
        return UI.guiShatterProgress();
    }

    private void swapContentCategory(Category category) {
        this.moduleList.finishTransition();
        this.themesRenderer.finishTransition();
        this.contentCategory = category;
        if (category == Category.THEMES) {
            this.themesRenderer.open(false);
        }
        if (this.contentCategory == null) {
            this.contentCategory = Category.VISUALS;
        }
    }

    private void renderModuleHeader(DrawContext drawContext, float f, float f2, float f3, float f4, Category category, float f5, float f6) {
        float f7 = f + contentXOff();
        float f8 = f2 + CONTENT_Y_OFFSET + f6;
        float f9 = f3 - contentInset();
        float f10 = 6.0f;
        float f11 = 14.0f;
        float f12 = HEADER_H;
        float f13 = f8 + (f12 - f11) * 0.5f;
        float f14 = 16.0f;
        float f15 = f7 + f9 - f10 - f14;
        float f16 = f8 + (f12 - f14) * 0.5f;
        this.renderDiscordAvatar(drawContext, f15, f16, f14, f4);
        String string = UI.profileName();
        String string2 = UI.profileRole();
        float f17 = 6.5f;
        float f18 = 5.0f;
        float f19 = Fonts.MONTSERRAT_MEDIUM.width(string, f17);
        float f20 = Fonts.MONTSERRAT_MEDIUM.width(string2, f18);
        float f21 = f15 - 5.0f;
        float f22 = f8 + (f12 - (f17 + 1.0f + f18)) * 0.5f;
        Fonts.MONTSERRAT_MEDIUM.draw(string, f21 - f19, f22, f17, UI.color(255, 255, 255, 230, f4));
        Fonts.MONTSERRAT_MEDIUM.draw(string2, f21 - f20, f22 + f17 + 1.0f, f18, ClientAccent.accentSoft(195.0f * f4));
        float profileLeft = f21 - Math.max(f19, f20);

        // Thematic Language toggle button with smooth animated sliding badge between RU and EN
        float langW = 44.0f;
        float langH = 14.0f;
        float langX = profileLeft - langW - 8.0f;
        float langY = f8 + (f12 - langH) * 0.5f;

        float mouseX = Position.mouseX();
        float mouseY = Position.mouseY();
        boolean hoverLang = mouseX >= langX && mouseX <= langX + langW && mouseY >= langY && mouseY <= langY + langH;
        boolean isRu = rtx.nv.api.localization.LocalizationManager.getCurrentLanguage() == rtx.nv.api.localization.LocalizationManager.Language.RU;
        double targetBadge = isRu ? 0.0 : 1.0;

        if (!this.langAnimInitialized) {
            this.langAnim.set(targetBadge);
            this.langAnimInitialized = true;
        } else if (Math.abs(this.langAnim.getToValue() - targetBadge) > 0.001) {
            this.langAnim.run(targetBadge, 0.28, rtx.nv.utils.animations.Easings.CUBIC_OUT, false);
        }
        this.langAnim.update();
        this.langCrossfadeAnim.update();
        this.langWordFade.update();
        float wf = (float) this.langWordFade.get();
        if (wf <= 0.001f || this.langWordFade.isFinished()) {
            activeWordAlpha = 1.0f;
            activeWordOffsetY = 0.0f;
        } else {
            activeWordAlpha = Math.max(0.12f, Math.min(1.0f, wf));
            activeWordOffsetY = (1.0f - activeWordAlpha) * -2.5f;
        }
        float t = (float) this.langAnim.get();

        Render2D.rect(langX, langY, langW, langH, 3.5f, ThemeManager.rgba(0, (55.0f + (hoverLang ? 25.0f : 0.0f)) * f4));
        Render2D.outline(langX, langY, langW, langH, 3.5f, 0.6f, hoverLang ? ClientAccent.accent(160.0f * f4) : ThemeManager.rgba(0xFFFFFF, (18.0f + (hoverLang ? 16.0f : 0.0f)) * f4));
        if (hoverLang) {
            Render2D.glow(new BuiltGlow(langX, langY, langW, langH, new float[]{3.5f, 3.5f, 3.5f, 3.5f}, ClientAccent.accent(255.0f), 0.25f, 3.5f, f4));
        }

        // Thematic Language vector glyph
        float iconSize = 7.5f;
        int iconCol = hoverLang ? ClientAccent.accentBright(255.0f * f4) : ClientAccent.accentSoft(210.0f * f4);
        Fonts.NV.msdf(NvIcons.LANGUAGE, langX + 3.5f, langY + (langH - iconSize) * 0.5f, iconSize, iconCol);

        // Thin separator
        Render2D.rect(langX + 13.0f, langY + 3.0f, 0.6f, langH - 6.0f, 0.3f, ThemeManager.rgba(0xFFFFFF, 22.0f * f4));

        // Symmetrical 2-slot toggle track for RU and EN
        float trackX = langX + 15.0f;
        float trackW = langW - 16.5f;
        float slotW = trackW * 0.5f;
        float badgeW = slotW - 1.0f;
        float badgeH = langH - 3.0f;
        float badgeX = trackX + 0.5f + t * slotW;
        float badgeY = langY + 1.5f;

        Render2D.rect(badgeX, badgeY, badgeW, badgeH, 2.5f, ClientAccent.accent(175.0f * f4));
        Render2D.glow(new BuiltGlow(badgeX, badgeY, badgeW, badgeH, new float[]{2.5f, 2.5f, 2.5f, 2.5f}, ClientAccent.accent(255.0f), 0.35f, 3.0f, f4));

        // Perfectly centered RU and EN labels with smooth brightness cross-fade
        float textY = langY + (langH - 5.0f) * 0.5f - 0.2f;
        float ruW = Fonts.MONTSERRAT_MEDIUM.width("RU", 5.0f);
        float enW = Fonts.MONTSERRAT_MEDIUM.width("EN", 5.0f);
        float ruX = trackX + (slotW - ruW) * 0.5f;
        float enX = trackX + slotW + (slotW - enW) * 0.5f;

        int ruCol = UI.color(255, 255, 255, Math.round(140.0f + 115.0f * (1.0f - t)), f4);
        int enCol = UI.color(255, 255, 255, Math.round(140.0f + 115.0f * t), f4);
        Fonts.MONTSERRAT_MEDIUM.draw("RU", ruX, textY, 5.0f, ruCol);
        Fonts.MONTSERRAT_MEDIUM.draw("EN", enX, textY, 5.0f, enCol);

        float searchX = f7 + 6.0f;
        float maxSearchW = Math.max(60.0f, langX - searchX - 10.0f);
        float targetExpandedW = Math.min(120.0f, maxSearchW);
        this.search.render(drawContext, searchX, f13, targetExpandedW, f11, f4, Position.mouseX(), Position.mouseY(), f5);
    }

    @Override
    public float captureScale() {
        return guiCaptureScale;
    }

    private void renderSplitterRail(float panelX, float panelY, float panelH, float alpha, float dt) {
        float railX = panelX + 5.0f + sidebarW() + 1.5f;
        float railY = panelY + 5.0f;
        float railH = panelH - 10.0f;

        boolean overSplitter = Position.mouseX() >= railX - 4.0f && Position.mouseX() <= railX + 4.0f
                && Position.mouseY() >= railY && Position.mouseY() <= railY + railH;

        float targetHover = (overSplitter || isDraggingSidebar) ? 1.0f : 0.0f;
        float hoverSpeed = 1.0f - (float) Math.exp(-dt * 16.0f);
        this.splitterHoverT += (targetHover - this.splitterHoverT) * hoverSpeed;

        if (this.splitterHoverT > 0.01f) {
            Render2D.rect(railX - 0.5f, railY + 6.0f, 1.0f, railH - 12.0f, 0.5f, ClientAccent.accent(30.0f * this.splitterHoverT * alpha));

            float handleW = 2.0f;
            float handleH = 22.0f;
            float handleX = railX - handleW * 0.5f;
            float handleY = railY + (railH - handleH) * 0.5f;
            int handleCol = ClientAccent.accentBright((130.0f + 125.0f * this.splitterHoverT) * alpha);
            Render2D.rect(handleX, handleY, handleW, handleH, 1.0f, handleCol);
            Render2D.glow(new BuiltGlow(handleX, handleY, handleW, handleH, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, ClientAccent.accent(255.0f), 0.35f, 4.0f, this.splitterHoverT * alpha));
        }
    }

    private void renderCategoryPanel(float f, float f2, float f3, float f4) {
        if (this.themesRenderer.getEditor().isOpen()) {
            f4 *= 0.55f;
        }
        float sidebarX = f;
        float sidebarW = sidebarW();
        boolean isCompact = sidebarW < 58.0f;
        boolean isExpanded = sidebarW > 105.0f;
        boolean isRu = rtx.nv.api.localization.LocalizationManager.getCurrentLanguage() == rtx.nv.api.localization.LocalizationManager.Language.RU;

        float headerY = f2 + 3.0f;
        float headerH = HEADER_H;
        RenderHelper.drawPanelBg(sidebarX, headerY, sidebarW, headerH, 12.0f, 0.0f, 0.0f, 0.0f, f4);
        float headerCenterY = headerY + headerH * 0.5f;

        if (isCompact) {
            float logoSize = 13.0f;
            float logoX = sidebarX + (sidebarW - logoSize) * 0.5f;
            BrandMark.draw(logoX, headerCenterY - logoSize * .5f, logoSize, f4);
        } else if (isExpanded) {
            float logoH = 13.0f;
            float fontH = 8.5f;
            float logoGap = 5.0f;
            float logoStartX = sidebarX + 8.0f;
            BrandMark.draw(logoStartX, headerCenterY - logoH * .5f, logoH, f4);
            Fonts.MONTSERRAT_BOLD.msdf("NV", logoStartX + logoH + logoGap, headerCenterY - fontH * 0.5f + 0.5f, fontH, UI.color(255, 255, 255, 255, f4));

            String tag = "CLIENT";
            float tagW = Fonts.MONTSERRAT_MEDIUM.width(tag, 5.0f);
            float badgeW = tagW + 7.0f;
            float badgeH = 10.0f;
            float badgeX = sidebarX + sidebarW - badgeW - 6.0f;
            float badgeY = headerCenterY - badgeH * 0.5f;
            Render2D.rect(badgeX, badgeY, badgeW, badgeH, 3.0f, UI.color(255, 255, 255, 14, f4));
            Fonts.MONTSERRAT_MEDIUM.draw(tag, badgeX + 3.5f, badgeY + 2.0f, 5.0f, ClientAccent.accentSoft(220.0f * f4));
        } else {
            float logoH = 14.0f;
            float fontH = 8.5f;
            float logoGap = 6.0f;
            float textW = Fonts.MONTSERRAT_BOLD.width("NV", fontH);
            float logoTotalW = logoH + logoGap + textW;
            float logoStartX = sidebarX + (sidebarW - logoTotalW) * 0.5f;
            BrandMark.draw(logoStartX, headerCenterY - logoH * .5f, logoH, f4);
            Fonts.MONTSERRAT_BOLD.msdf("NV", logoStartX + logoH + logoGap, headerCenterY - fontH * 0.5f + 0.5f, fontH, UI.color(255, 255, 255, 255, f4));
        }

        float modulesSectionY = headerY + headerH + 6.0f;
        float modulesHeaderCenterY = modulesSectionY + 8.0f;
        float headerAnim = this.modulesHeaderAnim.getOutput().floatValue();
        if (headerAnim > 0.004f) {
            Render2D.rect(sidebarX + 4.0f, modulesSectionY + 1.0f, sidebarW - 8.0f, 14.0f, 5.0f, UI.color(255, 255, 255, Math.round(16.0f * headerAnim), f4));
        }
        float modHeaderX = sidebarX + 8.0f;
        float modIconSize = 8.5f;
        int modIconCol = ClientAccent.accentBright((200.0f + 55.0f * headerAnim) * f4);

        if (isCompact) {
            Fonts.NV.msdf(NvIcons.MODULES, sidebarX + (sidebarW - modIconSize) * 0.5f, modulesHeaderCenterY - modIconSize * 0.5f, modIconSize, modIconCol);
        } else if (isExpanded) {
            Fonts.NV.msdf(NvIcons.MODULES, modHeaderX, modulesHeaderCenterY - modIconSize * 0.5f, modIconSize, modIconCol);
            Fonts.MONTSERRAT_MEDIUM.draw(rtx.nv.api.localization.Lang.get("category.modules", "Modules"), modHeaderX + modIconSize + 5.0f, modulesHeaderCenterY - 4.0f + 0.5f, 8.0f, UI.color(255, 255, 255, Math.round(215.0f + 40.0f * headerAnim), f4));

            long enabledCount = ModuleManager.get().getAll().stream().filter(Module::isEnabled).count();
            String actText = enabledCount + (isRu ? " вкл." : " on");
            float actW = Fonts.MONTSERRAT_MEDIUM.width(actText, 5.0f);
            float actBadgeW = actW + 7.0f;
            float actBadgeH = 10.0f;
            float actBadgeX = sidebarX + sidebarW - actBadgeW - 6.0f;
            float actBadgeY = modulesHeaderCenterY - actBadgeH * 0.5f;
            Render2D.rect(actBadgeX, actBadgeY, actBadgeW, actBadgeH, 3.0f, ClientAccent.accent(30.0f * f4));
            Fonts.MONTSERRAT_MEDIUM.draw(actText, actBadgeX + 3.5f, actBadgeY + 2.0f, 5.0f, ClientAccent.accentBright(240.0f * f4));
        } else {
            Fonts.NV.msdf(NvIcons.MODULES, modHeaderX, modulesHeaderCenterY - modIconSize * 0.5f, modIconSize, modIconCol);
            Fonts.MONTSERRAT_MEDIUM.draw(rtx.nv.api.localization.Lang.get("category.modules", "Modules"), modHeaderX + modIconSize + 5.0f, modulesHeaderCenterY - 4.0f + 0.5f, 8.0f, UI.color(255, 255, 255, Math.round(215.0f + 40.0f * headerAnim), f4));
        }

        float catStartY = modulesSectionY + 18.0f;
        for (int i = 0; i < MAIN_CATEGORIES.length; ++i) {
            Category category = MAIN_CATEGORIES[i];
            float itemY = catStartY + (float)i * (CAT_SUB_ROW_H + CAT_SUB_GAP);
            float itemCenterY = itemY + CAT_SUB_ROW_H * 0.5f;
            float anim = this.getCategoryAnim(category).getOutput().floatValue();

            if (anim > 0.01f) {
                float indH = 13.0f * anim;
                float indW = 2.5f;
                float indX = sidebarX + (isCompact ? 2.0f : 3.0f);
                float indY = itemCenterY - indH * 0.5f;
                Render2D.rect(indX, indY, indW, indH, 1.25f, ClientAccent.accentBright(255.0f * anim * f4));
                Render2D.glow(new BuiltGlow(indX, indY, indW, indH, new float[]{1.25f, 1.25f, 1.25f, 1.25f}, ClientAccent.accent(255.0f), 0.45f, 5.0f, anim * f4));
            }

            int textAlpha = Math.min(255, 140 + Math.round(anim * 115.0f));
            int textCol = UI.color(255, 255, 255, textAlpha, f4);
            float iconSize = 8.5f;
            int iconCol = anim > 0.01f ? ClientAccent.accentBright((float)textAlpha * f4) : ClientAccent.icon(140.0f * f4);
            int moduleCount = category == Category.PINNED ? PinManager.getPinnedCount() : (category == Category.THEMES ? (rtx.nv.api.ui.theme.Theme.values().length + rtx.nv.api.ui.theme.CustomThemeManager.getCustomThemes().size()) : this.moduleList.getModules(category).size());

            if (isCompact) {
                float iconX = sidebarX + (sidebarW - iconSize) * 0.5f;
                Fonts.NV.msdf(NvIcons.forCategory(category), iconX, itemCenterY - iconSize * 0.5f, iconSize, iconCol);
                if (category == Category.PINNED && moduleCount > 0) {
                    float dotSize = 3.5f;
                    Render2D.rect(iconX + iconSize - 1.0f, itemCenterY - iconSize * 0.5f - 1.0f, dotSize, dotSize, dotSize * 0.5f, ClientAccent.accentBright(255.0f * f4));
                }
            } else if (isExpanded) {
                float iconX = sidebarX + 9.0f;
                Fonts.NV.msdf(NvIcons.forCategory(category), iconX, itemCenterY - iconSize * 0.5f, iconSize, iconCol);

                String badgeText = moduleCount + (category == Category.PINNED ? (isRu ? " закр." : " pin") : (category == Category.THEMES ? (isRu ? " тем" : " themes") : (isRu ? " мод." : " mods")));
                float bTextW = Fonts.MONTSERRAT_MEDIUM.width(badgeText, 5.0f);
                float badgeW = bTextW + 8.0f;
                float badgeH = 11.0f;
                float badgeX = sidebarX + sidebarW - 7.0f - badgeW;
                float badgeY = itemCenterY - badgeH * 0.5f;
                drawSidebarLabel(category.getDisplayName(), iconX + iconSize + 5.0f, itemCenterY, badgeX - iconX - iconSize - 11.0f, textCol);
                int bgCol = anim > 0.01f ? ClientAccent.accent(35.0f * anim * f4) : UI.color(255, 255, 255, 12, f4);
                Render2D.rect(badgeX, badgeY, badgeW, badgeH, 3.5f, bgCol);
                int countCol = anim > 0.01f ? ClientAccent.accentBright(240.0f * f4) : UI.color(255, 255, 255, 160, f4);
                Fonts.MONTSERRAT_MEDIUM.draw(badgeText, badgeX + 4.0f, badgeY + 2.5f, 5.0f, countCol);
            } else {
                float iconX = sidebarX + 9.0f;
                Fonts.NV.msdf(NvIcons.forCategory(category), iconX, itemCenterY - iconSize * 0.5f, iconSize, iconCol);

                String count = String.valueOf(moduleCount);
                float countW = Fonts.MONTSERRAT_MEDIUM.width(count, 5.5f);
                drawSidebarLabel(category.getDisplayName(), iconX + iconSize + 5.0f, itemCenterY, sidebarX + sidebarW - 13.0f - countW - iconX - iconSize - 5.0f, textCol);
                Fonts.MONTSERRAT_MEDIUM.draw(count, sidebarX + sidebarW - 8.0f - countW, itemCenterY - 2.5f, 5.5f, UI.color(255, 255, 255, Math.round(80.0f + 80.0f * anim), f4));
            }
        }

        // Pinned Bottom System Categories (Settings, About) with Subtle Separator Line
        float sidebarBottom = f2 + PANEL_H - 10.0f;
        float systemStartY = sidebarBottom - (float)SYSTEM_CATEGORIES.length * (CAT_OTHER_ROW_H + CAT_SUB_GAP) - 4.0f;
        float dividerY = systemStartY - 6.0f;
        float divPad = isCompact ? 5.0f : 8.0f;
        Render2D.rect(sidebarX + divPad, dividerY, sidebarW - divPad * 2.0f, 0.6f, 0.3f, UI.color(255, 255, 255, 18, f4));

        for (int i = 0; i < SYSTEM_CATEGORIES.length; ++i) {
            Category category = SYSTEM_CATEGORIES[i];
            float rowVisibility = 1.0f;
            float itemY = systemStartY + (float)i * (CAT_OTHER_ROW_H + CAT_SUB_GAP);
            float itemCenterY = itemY + CAT_OTHER_ROW_H * 0.5f;
            float themesAnim = this.getCategoryAnim(category).getOutput().floatValue();

            if (themesAnim > 0.01f) {
                float indH = 13.0f * themesAnim;
                float indW = 2.5f;
                float indX = sidebarX + (isCompact ? 2.0f : 3.0f);
                float indY = itemCenterY - indH * 0.5f;
                Render2D.rect(indX, indY, indW, indH, 1.25f, ClientAccent.accentBright(255.0f * themesAnim * f4 * rowVisibility));
                Render2D.glow(new BuiltGlow(indX, indY, indW, indH, new float[]{1.25f, 1.25f, 1.25f, 1.25f}, ClientAccent.accent(255.0f), 0.45f, 5.0f, themesAnim * f4 * rowVisibility));
            }

            int n6 = Math.round((float)Math.min(255, 140 + Math.round(themesAnim * 115.0f)) * rowVisibility);
            int n7 = UI.color(255, 255, 255, n6, f4);
            float iconSize = 8.5f;
            int iconCol = themesAnim > 0.01f ? ClientAccent.accentBright((float)n6 * f4) : UI.color(255, 255, 255, n6, f4);

            if (isCompact) {
                float iconX = sidebarX + (sidebarW - iconSize) * 0.5f;
                Fonts.NV.msdf(NvIcons.forCategory(category), iconX, itemCenterY - iconSize * 0.5f, iconSize, iconCol);
            } else if (isExpanded) {
                float iconX = sidebarX + 9.0f;
                Fonts.NV.msdf(NvIcons.forCategory(category), iconX, itemCenterY - iconSize * 0.5f, iconSize, iconCol);
                Fonts.MONTSERRAT_MEDIUM.draw(category.getDisplayName(), iconX + iconSize + 5.0f, itemCenterY - 3.5f + 0.5f + activeWordOffsetY, 7.0f, ColorUtil.multAlpha(n7, activeWordAlpha));

                String badgeText = "v1.0";
                float bTextW = Fonts.MONTSERRAT_MEDIUM.width(badgeText, 5.0f);
                float badgeW = bTextW + 8.0f;
                float badgeH = 11.0f;
                float badgeX = sidebarX + sidebarW - 7.0f - badgeW;
                float badgeY = itemCenterY - badgeH * 0.5f;
                int bgCol = themesAnim > 0.01f ? ClientAccent.accent(35.0f * themesAnim * f4) : UI.color(255, 255, 255, 12, f4);
                Render2D.rect(badgeX, badgeY, badgeW, badgeH, 3.5f, bgCol);
                int countCol = themesAnim > 0.01f ? ClientAccent.accentBright(240.0f * f4) : UI.color(255, 255, 255, 160, f4);
                Fonts.MONTSERRAT_MEDIUM.draw(badgeText, badgeX + 4.0f, badgeY + 2.5f, 5.0f, countCol);
            } else {
                float iconX = sidebarX + 9.0f;
                Fonts.NV.msdf(NvIcons.forCategory(category), iconX, itemCenterY - iconSize * 0.5f, iconSize, iconCol);
                Fonts.MONTSERRAT_MEDIUM.draw(category.getDisplayName(), iconX + iconSize + 5.0f, itemCenterY - 3.5f + 0.5f + activeWordOffsetY, 7.0f, ColorUtil.multAlpha(n7, activeWordAlpha));
            }
        }
    }

    private static void drawSidebarLabel(String text, float x, float centerY, float maxWidth, int color) {
        float width = Fonts.MONTSERRAT_MEDIUM.width(text, 7.0f);
        float size = Math.max(6.2f, Math.min(7.0f, 7.0f * Math.max(1, maxWidth) / Math.max(1, width)));
        String fitted = rtx.nv.api.ui.settings.RenderHelper.fitText(Fonts.MONTSERRAT_MEDIUM, text, Math.max(1, maxWidth), size);
        int finalColor = ColorUtil.multAlpha(color, activeWordAlpha);
        Fonts.MONTSERRAT_MEDIUM.draw(fitted, x, centerY - size * .5f + .5f + activeWordOffsetY, size, finalColor);
    }

    public static String categoryIconPath(Category category) {
        if (category == null) return "nv:textures/icons/modules.png";
        return switch (category) {
            case PINNED -> "nv:textures/icons/pinned.png";
            case VISUALS -> "nv:textures/icons/visuals.png";
            case DISPLAY -> "nv:textures/icons/interface.png";
            case UTILS -> "nv:textures/icons/utils.png";
            case THEMES -> "nv:textures/icons/themes.png";
            case MEDIA -> "nv:textures/icons/music.png";
            case ABOUT -> "nv:textures/icons/info.png";
            default -> "nv:textures/icons/modules.png";
        };
    }

    private void updateCameraParallax() {
        ClientPlayerEntity clientPlayerEntity = IMinecraft.mc.player;
        if (clientPlayerEntity == null) {
            this.lastCameraYaw = Float.NaN;
            this.parallaxX = 0.0f;
            this.parallaxY = 0.0f;
            return;
        }
        float f = clientPlayerEntity.getYaw();
        float f2 = clientPlayerEntity.getPitch();
        if (Float.isNaN(this.lastCameraYaw)) {
            this.lastCameraYaw = f;
            this.lastCameraPitch = f2;
        }
        float f3 = MathHelper.wrapDegrees((float)(f - this.lastCameraYaw));
        float f4 = f2 - this.lastCameraPitch;
        this.lastCameraYaw = f;
        this.lastCameraPitch = f2;
        if (!this.screenAnim.isClosing()) {
            this.parallaxX = 0.0f;
            this.parallaxY = 0.0f;
            return;
        }
        double d = Math.max(30.0, (double)((Integer)IMinecraft.mc.options.getFov().getValue()).intValue());
        float f5 = (float)((double)Position.screenHeight() / d);
        this.parallaxX -= f3 * f5;
        this.parallaxY -= f4 * f5;
    }


    @Override
    public boolean captureActive() {
        return UI.guiCaptureActive();
    }

    public static float guiCaptureBlurRadius() {
        return guiCaptureBlurMainPx;
    }

    private void renderNoResults(float f, float f2, float f3, float f4) {
        float f5 = f + contentXOff();
        float f6 = f3 - contentInset();
        float f7 = f2 + CONTENT_Y_OFFSET + HEADER_OFFSET;
        float f8 = CONTENT_HEIGHT - HEADER_OFFSET;
        String string = rtx.nv.api.localization.Lang.get("ui.search.not_found", "Ничего не найдено");
        float f9 = 6.5f;
        float f10 = Fonts.MONTSERRAT_MEDIUM.width(string, f9);
        Fonts.MONTSERRAT_MEDIUM.draw(string, f5 + (f6 - f10) * 0.5f, f7 + f8 * 0.5f - 3.0f, f9, UI.color(255, 255, 255, 110, f4));
    }

    private void renderEmptyPinned(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = f + contentXOff();
        float f6 = f2 + CONTENT_Y_OFFSET + HEADER_OFFSET;
        float f7 = PANEL_W - contentInset();
        float f8 = CONTENT_HEIGHT - HEADER_OFFSET;
        float centerY = f6 + f8 * 0.5f;

        float iconSize = 22.0f;
        float iconX = f5 + (f7 - iconSize) * 0.5f;
        Fonts.NV.msdf(NvIcons.PINNED, iconX, centerY - 28.0f, iconSize, ClientAccent.accentBright(220.0f * f4));

        String title = rtx.nv.api.localization.Lang.get("ui.pinned.empty.title", "Нет закреплённых модулей");
        float titleSize = 7.0f;
        float titleW = Fonts.MONTSERRAT_MEDIUM.width(title, titleSize);
        Fonts.MONTSERRAT_MEDIUM.draw(title, f5 + (f7 - titleW) * 0.5f, centerY, titleSize, UI.color(255, 255, 255, 225, f4));

        String sub = rtx.nv.api.localization.Lang.get("ui.pinned.empty.desc", "Нажмите на булавку в карточке чтобы закрепить модуль здесь");
        float subSize = 5.5f;
        float subW = Fonts.MONTSERRAT_MEDIUM.width(sub, subSize);
        Fonts.MONTSERRAT_MEDIUM.draw(sub, f5 + (f7 - subW) * 0.5f, centerY + 11.0f, subSize, UI.color(255, 255, 255, 115, f4));
    }

    @Override
    protected void renderScreen(DrawContext drawContext, int n, int n2, float f) {
        if (isDraggingPanel) {
            if (MinecraftClient.getInstance().getWindow() != null &&
                GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) != GLFW.GLFW_PRESS) {
                isDraggingPanel = false;
            } else {
                float mouseX = Position.mouseX();
                float mouseY = Position.mouseY();
                customPanelX = mouseX - dragGrabX;
                customPanelY = mouseY - dragGrabY;
            }
        }
        this.renderPanel(drawContext);
    }

    private void renderModuleHeaderPanel(float f, float f2, float f3, float f4, float f5) {
        RenderHelper.drawPanelBg(f, f2, f3, f4, 0.0f, 12.0f, 0.0f, 0.0f, f5);
    }

    private boolean isModuleView() {
        return this.contentCategory != null
            && this.contentCategory != Category.THEMES
            && this.contentCategory != Category.ABOUT;
    }

    private static String savedStartCategoryName = null;

    public static void setStartCategoryName(String name) {
        savedStartCategoryName = name;
    }

    public static String getLastCategoryName() {
        if (INSTANCE != null && INSTANCE.contentCategory != null) {
            return INSTANCE.contentCategory.name();
        }
        return savedStartCategoryName != null ? savedStartCategoryName : Category.VISUALS.name();
    }

    private Category resolveStartCategory() {
        if (savedStartCategoryName != null) {
            try {
                Category cat = Category.valueOf(savedStartCategoryName);
                if (cat != null && cat != Category.ABOUT) {
                    return cat;
                }
            } catch (Exception ignored) {}
        }
        if (lastSelectedCategory != null && lastSelectedCategory != Category.ABOUT) {
            return lastSelectedCategory;
        }
        return Category.VISUALS;
    }

    private void selectCategory(Category category) {
        if (category == null || category == Category.ABOUT) {
            category = Category.VISUALS;
        }
        if (category == this.targetCategory && this.contentCategory == category) {
            this.search.collapse();
            if (this.inspector.isOpen()) {
                this.inspector.close();
            }
            this.moduleList.resetScroll();
            this.getCategoryAnim(category).setValue(1.0);
            this.getCategoryAnim(category).setDirection(Direction.FORWARDS);
            return;
        }
        this.search.collapse();
        this.settingsPopup.close();
        if (this.inspector.isOpen()) {
            this.inspector.close();
        }
        this.moduleList.resetScroll();
        this.targetCategory = category;
        lastSelectedCategory = category;
        savedStartCategoryName = category.name();
        rtx.nv.api.config.ConfigManager.markDirty();
        this.oldSubText = this.newSubText;
        this.newSubText = category.getDisplayName();
        this.subTextAnim.setDirection(Direction.FORWARDS);
        this.subTextAnim.counter.resetCounter();
        this.subTextAnimDone = false;
        for (Category category4 : Category.values()) {
            Decelerate cAnim = this.getCategoryAnim(category4);
            cAnim.setValue(1.0);
            cAnim.setDirection(category4 == category ? Direction.FORWARDS : Direction.BACKWARDS);
        }
        this.modulesHeaderAnim.setDirection(UI.isMainCategory(category) ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    private void updateCategoryCrossfade(float f) {
        if (this.targetCategory == this.contentCategory) {
            if (this.contentCategory != null && this.categoryT < 1.0f) {
                this.categoryT = Math.min(1.0f, this.categoryT + f / 0.15f);
            }
            return;
        }
        if (this.contentCategory == null) {
            this.swapContentCategory(this.targetCategory);
            this.categoryT = 0.0f;
        } else {
            this.categoryT = Math.max(0.0f, this.categoryT - f / 0.15f);
            if (this.categoryT <= 0.0f) {
                this.categoryT = 0.0f;
                this.swapContentCategory(this.targetCategory);
            }
        }
    }

    public static void flushMotionBlur() {
        cardStratumMarked = false;
        panelSplitMarked = false;
        vanillaBlurRequested = false;
        popupStratumMarked = false;
        popupLayerBlurWanted = false;
        popupBlurWanted = false;
        popupBlurStaged = false;
        cardCaptureStaged = false;
        boolean bl = popupBlurCaptured;
        popupBlurCaptured = false;
        boolean bl2 = cardBlurCaptured;
        cardBlurCaptured = false;
        if (motionBlurPending) {
            motionBlurPending = false;
            cardBlurPending = false;
            popupBlurPending = false;
            GuiMotionBlurRenderer.applyWithCopy(motionBlurOpacity, motionBlurRadius, motionBlurX, motionBlurY, motionBlurW, motionBlurH, motionBlurMask, 2, motionBlurSrcX, motionBlurSrcY, motionBlurSrcW, motionBlurSrcH, motionBlurScale, motionBlurOriginX, motionBlurOriginY);
            return;
        }
        if (popupBlurPending && bl) {
            popupBlurPending = false;
            cardBlurPending = false;
            GuiMotionBlurRenderer.applyWithCopy(1.0f, popupBlurRadius, popupBlurX, popupBlurY, popupBlurW, popupBlurH, popupBlurMask, popupBlurMaskCount, popupBlurX, popupBlurY, popupBlurW, popupBlurH, 1.0f, popupBlurOriginX, popupBlurOriginY, true);
            return;
        }
        popupBlurPending = false;
        if (cardBlurPending && bl2) {
            cardBlurPending = false;
            GuiMotionBlurRenderer.applyWithCopy(motionBlurOpacity, motionBlurRadius, motionBlurX, motionBlurY, motionBlurW, motionBlurH, cardBlurMask, cardBlurMaskCount, motionBlurX, motionBlurY, motionBlurW, motionBlurH, 1.0f, motionBlurOriginX, motionBlurOriginY);
        }
    }

    @Override
    public float captureBlurRadius() {
        return guiCaptureBlurMainPx;
    }

    private Decelerate getCategoryAnim(Category category2) {
        return this.categoryAnims.computeIfAbsent(category2, category -> UI.createAnim(200));
    }

    private void renderPanel(DrawContext drawContext) {
        float f;
        boolean bl;
        float f2;
        this.screenAnim.updateFrame();
        this.updateCameraParallax();
        boolean bl2 = WorldGuiCloseAnimation.isDetachedRender();
        if (bl2) {
            this.parallaxX = 0.0f;
            this.parallaxY = 0.0f;
        }
        if (this.screenAnim.isCloseFinished() && !bl2) {
            if (MinecraftClient.getInstance().currentScreen == this) {
                this.screenAnim.snapClosed();
                MinecraftClient.getInstance().setScreen(null);
            }
            return;
        }
        float f3 = this.screenAnim.scale();
        float f4 = Math.max(Math.abs(this.parallaxX), Math.abs(this.parallaxY));
        if (f4 > 0.5f) {
            f2 = Math.min(1.0f, f4 / 20.0f);
            f3 += (1.0f - f3) * f2;
        }
        guiCaptureScale = f3;
        f2 = bl2 ? WorldGuiCloseAnimation.blurRadius() : this.screenAnim.blurRadius();
        guiCaptureBlurMainPx = Math.max(f2, GuiShatterAnimation.blurRadius()) * Render2DCoordinateSpace.designGuiScale();
        float f5 = this.screenAnim.alpha();
        float f6 = bl2 ? 1.0f : f5;
        float f7 = panelW();
        float f8 = PANEL_H;
        float f9 = panelX();
        float f10 = panelY();
        Render2D.rect(-10.0f, -10.0f, Position.screenWidth() + 20.0f, Position.screenHeight() + 20.0f, 0.0f, UI.color(0, 0, 0, 60, f5));
        if (UI.guiCaptureActive()) {
            Render2D.flush();
            Render2D.beginFrame(drawContext);
            GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
            guiRenderState.createNewRootLayer();
            guiRenderState.applyBlur();
        }
        this.moduleList.resetCardBlur();
        this.themesRenderer.resetCardBlur();
        drawContext.getMatrices().pushMatrix();
        if (this.parallaxX != 0.0f || this.parallaxY != 0.0f) {
            drawContext.getMatrices().translate(this.parallaxX, this.parallaxY);
        }
        if (isDraggingSidebar) {
            float targetW = (float)Position.mouseX() - (f9 + 5.0f) - sidebarDragGrabX;
            customSidebarW = Math.max(MIN_SIDEBAR_W, Math.min(MAX_SIDEBAR_W, targetW));
        }
        RectUtil.drawClientRect(f9, f10, f7, f8, 12.0f, f6, 6.0f);
        float f11 = 12.0f;
        RenderHelper.drawPanelBg(f9 + 5.0f, f10 + 5.0f, sidebarW(), f8 - 10.0f, f11, 0.0f, 0.0f, f11, f6);
        long l = System.nanoTime();
        float f12 = Math.min(0.1f, (float)(l - this.lastNs) / 1.0E9f);
        this.lastNs = l;
        this.themesRowT = 1.0f;
        this.renderCategoryPanel(f9 + 5.0f, f10 + 2.0f, f8, f6);

        this.renderSplitterRail(f9, f10, f8, f6, f12);

        float mainW = PANEL_W - contentInset();
        float rightRadius = (this.inspector.isOpen() || this.themesRenderer.getEditor().isOpen()) ? 0.0f : f11;
        RenderHelper.drawPanelBg(f9 + contentXOff(), f10 + CONTENT_Y_OFFSET, mainW, CONTENT_HEIGHT, 0.0f, rightRadius, rightRadius, 0.0f, f6);
        this.updateCategoryCrossfade(f12);
        float f14 = f6 * this.categoryT;
        float f15 = this.screenAnim.isClosing() ? 1.0f : this.categoryT;
        boolean bl3 = !UI.guiCaptureActive() && !this.bindPopup.isVisible() && !this.settingsPopup.isVisible();
        this.moduleList.setAppearComposite(bl3);
        this.themesRenderer.setAppearComposite(bl3);
        if (f14 > 0.01f && this.contentCategory != null) {
            if (this.contentCategory == Category.THEMES) {
                this.themesRenderer.render(drawContext, f9, f10, PANEL_W, f14, f15, f12);
            } else if (this.contentCategory == Category.ABOUT) {
                this.renderAboutCategory(drawContext, f9, f10, PANEL_W, f14, f15, f12);
            } else {
                List<Module> list = this.filteredModules(this.contentCategory);
                if (this.contentCategory == Category.PINNED && list.isEmpty() && !this.search.hasText()) {
                    this.renderEmptyPinned(drawContext, f9, f10, PANEL_W, f14);
                } else {
                    this.moduleList.render(drawContext, f9, f10, PANEL_W, f14, f15, f12, this.contentCategory, list);
                    if (list.isEmpty() && this.search.hasText()) {
                        this.renderNoResults(f9, f10, PANEL_W, f14);
                    }
                }
            }
        }
        float f16 = this.modulesHeaderAnim.getOutput().floatValue();
        boolean bl4 = bl = this.contentCategory != null && this.contentCategory != Category.THEMES && this.contentCategory != Category.ABOUT;
        if (bl && f6 > 0.01f && f16 > 0.004f) {
            f = f9 + contentXOff();
            float f17 = f10 + CONTENT_Y_OFFSET;
            float f18 = mainW;
            float f19 = HEADER_H;
            float f20 = f6 * f16;
            this.renderModuleHeaderPanel(f, f17, f18, f19, f20);
            Render2D.pushScissor(drawContext, f, f17, f18, f19);
            this.renderModuleHeader(drawContext, f9, f10, PANEL_W, f20, this.contentCategory, f12, 0.0f);
            Render2D.popScissor(drawContext);
        }


        if (this.inspector.isOpen()) {
            if (this.inspector.getFocusedModule() instanceof rtx.nv.api.modules.impl.Visuals.Wings) {
                WingPreviewRenderer.render(drawContext, f9 - WingPreviewRenderer.WIDTH - 8,
                    f10 + CONTENT_Y_OFFSET, CONTENT_HEIGHT, f6 * this.inspector.dockProgress());
            } else if (this.inspector.getFocusedModule() instanceof rtx.nv.api.modules.impl.Visuals.CustomPet pet) {
                PetPreviewRenderer.render(drawContext, f9 - WingPreviewRenderer.WIDTH - 8,
                    f10 + CONTENT_Y_OFFSET, CONTENT_HEIGHT, f6 * this.inspector.dockProgress(), pet);
            } else {
                PetPreviewRenderer.reset();
            }
            float inspX = InspectorRenderer.getX();
            float inspW = InspectorRenderer.INSPECTOR_WIDTH;
            Render2D.pushScissor(drawContext, inspX - 1.0f, f10 + CONTENT_Y_OFFSET, this.inspector.dockWidth() + 2.0f, CONTENT_HEIGHT);
            this.inspector.render(drawContext, inspX, f10 + CONTENT_Y_OFFSET, inspW, CONTENT_HEIGHT, f6, f12);
            Render2D.popScissor(drawContext);
        }

        if (this.themesRenderer.getEditor().isOpen()) {
            float dockX = f9 + PANEL_W;
            float dockW = this.themesRenderer.getEditor().dockWidth();
            Render2D.pushScissor(drawContext, dockX - 1.0f, f10 + CONTENT_Y_OFFSET, dockW + 2.0f, CONTENT_HEIGHT);
            this.themesRenderer.getEditor().renderInspector(drawContext, dockX, f10 + CONTENT_Y_OFFSET, rtx.nv.api.ui.theme.ThemeEditorRenderer.INSPECTOR_WIDTH, CONTENT_HEIGHT, f6, f12);
            Render2D.popScissor(drawContext);
        }
        boolean bl5 = this.settingsPopup.isVisible();
        boolean bl6 = this.bindPopup.isVisible();
        if (bl5 || bl6) {
            boolean bl7;
            boolean bl8 = this.settingsPopup.blurPhase() > 0.004f;
            boolean bl9 = this.bindPopup.blurPhase() > 0.004f;
            boolean bl10 = bl7 = !UI.guiCaptureActive() && bl5 && bl6 && bl9 && !bl8;
            if (!UI.guiCaptureActive()) {
                GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
                guiRenderState.createNewRootLayer();
                guiRenderState.applyBlur();
                UI.markPopupStratum((bl8 || bl9) && !bl7, bl8 || bl9);
            }
            if (bl5) {
                this.settingsPopup.render(drawContext, f6);
            }
            if (bl7) {
                UI.markPopupLayerCapture();
                GuiLayerBlurRenderer.markPopupBoundary(drawContext);
            }
            if (bl6) {
                this.bindPopup.render(drawContext, f6);
            }
        }
        drawContext.getMatrices().popMatrix();
        if (this.themesRenderer.getEditor().isOpen()) {
            this.themesRenderer.getEditor().renderOverlays(drawContext, f6);
        }
        if (!UI.guiCaptureActive()) {
            this.stagePopupBlur();
            this.stageCardBlur(f9, f10, f7);
        }
    }

    private static String layoutNormalize(String string) {
        StringBuilder stringBuilder = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            int n = RU_LAYOUT.indexOf(c);
            stringBuilder.append(n >= 0 ? EN_LAYOUT.charAt(n) : c);
        }
        return stringBuilder.toString();
    }

    private static String toRuLayout(String string) {
        StringBuilder stringBuilder = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            int n = EN_LAYOUT.indexOf(c);
            stringBuilder.append(n >= 0 ? RU_LAYOUT.charAt(n) : c);
        }
        return stringBuilder.toString();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void renderClosingPanelOverHud(DrawContext drawContext) {
        boolean bl;
        UI uI = INSTANCE;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        WorldGuiCloseAnimation.updateFrame();
        if (GuiCapture.isBound(uI) && WorldGuiCloseAnimation.isReversing() && WorldGuiCloseAnimation.isFinished()) {
            WorldGuiCloseAnimation.cancel();
            GuiShatterAnimation.cancel();
            if (!uI.screenAnim.isClosing()) {
                uI.screenAnim.snapOpen();
            }
        }
        boolean bl2 = GuiCapture.isBound(uI);
        if (minecraftClient.currentScreen != null) {
            if (minecraftClient.currentScreen != uI) {
                if (bl2 && WorldGuiCloseAnimation.isActive()) {
                    WorldGuiCloseAnimation.cancel();
                }
                if (uI.screenAnim.isClosing()) {
                    uI.screenAnim.snapClosed();
                }
                pendingAfterClose = null;
            }
            return;
        }
        if (bl2 && WorldGuiCloseAnimation.isActive() && (minecraftClient.world == null || minecraftClient.options.hudHidden || WorldGuiCloseAnimation.isFinished() || WorldGuiCloseAnimation.surfaceChanged() || !GuiLayerBlurRenderer.available())) {
            WorldGuiCloseAnimation.cancel();
            if (uI.screenAnim.isClosing()) {
                uI.screenAnim.snapClosed();
                if (pendingAfterClose != null) {
                    Screen screen = pendingAfterClose;
                    pendingAfterClose = null;
                    minecraftClient.setScreen(screen);
                }
            }
            return;
        }
        boolean bl3 = bl2 && WorldGuiCloseAnimation.isActive();
        boolean bl4 = bl = uI.screenAnim.isClosing() && (bl3 || !uI.screenAnim.isCloseFinished());
        if (!bl) {
            if (uI.screenAnim.isClosing() && pendingAfterClose != null) {
                Screen screen = pendingAfterClose;
                pendingAfterClose = null;
                uI.screenAnim.snapClosed();
                minecraftClient.setScreen(screen);
            }
            return;
        }
        Render2D.beginFrame(drawContext);
        if (bl3) {
            BlurFramebuffer.beginWorldScope();
        }
        try {
            uI.renderPanel(drawContext);
        }
        finally {
            BlurFramebuffer.endWorldScope();
        }
        Render2D.flush();
        GuiLayerBlurRenderer.markPanelEnd(drawContext);
    }


    private void renderDiscordAvatar(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = f3 * 0.5f;
        int n = Math.max(0, Math.min(255, Math.round(f4 * 255.0f))) << 24 | 0xFFFFFF;
        String string = DiscordAvatar.texture();
        if (string != null && Render2D.imageReady(string)) {
            Render2D.image(string, f, f2, f3, f3, f5, n);
            return;
        }
        Render2D.rect(f, f2, f3, f3, f5, UI.color(0, 0, 0, 90, f4));
        Render2D.outline(f, f2, f3, f3, f5, 0.8f, UI.color(255, 255, 255, 30, f4));
        Fonts.NV.msdf(NvIcons.PROFILE, f + (f3 - 8.0f) * 0.5f, f2 + (f3 - 8.0f) * 0.5f, 8.0f, UI.color(255, 255, 255, 160, f4));
    }

    private Module moduleAtCursor() {
        if (!this.isModuleView()) {
            return null;
        }
        float f = PANEL_W;
        float f2 = panelX();
        float f3 = panelY();
        float f4 = Position.mouseX();
        float f5 = Position.mouseY();
        float f6 = f2 + contentXOff();
        float f7 = f3 + CONTENT_Y_OFFSET + HEADER_OFFSET;
        float f8 = f - contentInset();
        float f9 = CONTENT_HEIGHT - HEADER_OFFSET;
        if (f4 < f6 || f4 > f6 + f8 || f5 < f7 || f5 > f7 + f9) {
            return null;
        }
        List<Module> list = this.filteredModules(this.contentCategory);
        for (int i = 0; i < list.size(); ++i) {
            ModuleListRenderer.CardLayout layout = this.moduleList.getCardLayout(list, i, f6, f7, f8, f9, 1.0f, true);
            if (layout == null || !layout.visible) continue;
            if (layout.containsCard(f4, f5)) {
                return list.get(i);
            }
        }
        return null;
    }

    private Category categoryButtonAt(float f, float f2, float f3, float f4) {
        float f5;
        float f6 = f + 5.0f;
        float f7 = sidebarW();
        float f8 = f6;
        float f9 = f6 + f7;
        float f10 = f2 + 2.0f;
        float headerY = f10 + 3.0f;
        float modulesSectionY = headerY + HEADER_H + 6.0f;
        float catStartY = modulesSectionY + 18.0f;

        // Clicking on "Modules" section header selects VISUALS
        if (f3 >= f8 && f3 <= f9 && f4 >= modulesSectionY && f4 < catStartY) {
            return Category.VISUALS;
        }

        for (int i = 0; i < MAIN_CATEGORIES.length; ++i) {
            f5 = catStartY + (float)i * (CAT_SUB_ROW_H + CAT_SUB_GAP);
            if (f3 >= f8 && f3 <= f9 && f4 >= f5 - 1.5f && f4 <= f5 + CAT_SUB_ROW_H + 1.5f) {
                return MAIN_CATEGORIES[i];
            }
        }
        float sidebarBottom = f10 + PANEL_H - 10.0f;
        float systemStartY = sidebarBottom - (float)SYSTEM_CATEGORIES.length * (CAT_OTHER_ROW_H + CAT_SUB_GAP) - 4.0f;
        for (int i = 0; i < SYSTEM_CATEGORIES.length; ++i) {
            float f14 = systemStartY + (float)i * (CAT_OTHER_ROW_H + CAT_SUB_GAP);
            if (f3 >= f8 && f3 <= f9 && f4 >= f14 - 1.5f && f4 <= f14 + CAT_OTHER_ROW_H + 1.5f) {
                return SYSTEM_CATEGORIES[i];
            }
        }
        return null;
    }

    public void selectCategoryFromWorkspace(Category target) {
        if (target != null && target != this.targetCategory) {
            Sounds.play("select_category");
            this.selectCategory(target);
        }
    }

    private static String profileName() {
        String string = Profile.getUsername();
        if (string != null && !string.isBlank()) {
            return string;
        }
        String string2 = DiscordRPCManager.username();
        if (string2 != null && !string2.isBlank()) {
            return string2;
        }
        try {
            if (IMinecraft.mc.getSession() != null && IMinecraft.mc.getSession().getUsername() != null && !IMinecraft.mc.getSession().getUsername().isBlank()) {
                return IMinecraft.mc.getSession().getUsername();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return "Player";
    }



    private void renderAboutCategory(DrawContext drawContext, float px, float py, float panelW, float alpha, float pageT, float dt) {
        float enterY = (1.0f - pageT) * 6.0f;
        float compAlpha = alpha * Math.min(1.0f, pageT * 1.25f);
        float contentX = px + contentXOff();
        float contentY = py + CONTENT_Y_OFFSET + enterY;
        alpha = compAlpha;
        float contentW = panelW - contentInset();
        float contentH = CONTENT_HEIGHT;

        float mouseX = Position.mouseX();
        float mouseY = Position.mouseY();

        // 1. Header Card (Glassmorphic banner)
        float headerCardH = 46.0f;
        Render2D.rect(contentX + 6.0f, contentY + 6.0f, contentW - 12.0f, headerCardH, 6.0f, ThemeManager.rgba(0, 45.0f * alpha));
        Render2D.outline(contentX + 6.0f, contentY + 6.0f, contentW - 12.0f, headerCardH, 6.0f, 0.6f, ThemeManager.rgba(0xFFFFFF, 18.0f * alpha));

        // Icon + Title
        BrandMark.draw(contentX + 16.0f, contentY + 16.0f, 16.0f, alpha);
        Fonts.MONTSERRAT_MEDIUM.draw("Nivorat Visual", contentX + 38.0f, contentY + 14.0f, 10.0f, UI.color(255, 255, 255, 245, alpha));

        // Version badge
        String ver = rtx.nv.ClientEdition.isTrial() ? "v1.0.1 Free (Fabric 1.21.11)" : "v1.0.1 (Fabric 1.21.11)";
        float verW = Fonts.MONTSERRAT_MEDIUM.width(ver, 5.0f);
        float verX = contentX + contentW - 18.0f - verW - 8.0f;
        float verY = contentY + 15.0f;
        Render2D.rect(verX, verY, verW + 8.0f, 11.0f, 3.0f, ThemeManager.rgba(0, 50.0f * alpha));
        Render2D.outline(verX, verY, verW + 8.0f, 11.0f, 3.0f, 0.5f, ClientAccent.accent(140.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw(ver, verX + 4.0f, verY + 2.5f, 5.0f, ClientAccent.accentBright(230.0f * alpha));

        // Tagline (Zero Dash Rule)
        Fonts.MONTSERRAT_MEDIUM.draw("Твой взгляд на Minecraft", contentX + 38.0f, contentY + 28.0f, 6.0f, UI.color(180, 185, 205, 210, alpha));

        // 2. Feature highlights card
        float card2Y = contentY + 6.0f + headerCardH + 6.0f;
        float card2H = 138.0f;
        Render2D.rect(contentX + 6.0f, card2Y, contentW - 12.0f, card2H, 6.0f, ThemeManager.rgba(0, 40.0f * alpha));
        Render2D.outline(contentX + 6.0f, card2Y, contentW - 12.0f, card2H, 6.0f, 0.6f, ThemeManager.rgba(0xFFFFFF, 16.0f * alpha));

        Fonts.MONTSERRAT_MEDIUM.draw("Что умеет NV", contentX + 14.0f, card2Y + 8.0f, 7.0f, UI.color(255, 255, 255, 235, alpha));
        Render2D.rect(contentX + 14.0f, card2Y + 19.0f, contentW - 28.0f, 0.5f, 0.25f, ThemeManager.rgba(0xFFFFFF, 14.0f * alpha));

        String[] feats = rtx.nv.ClientEdition.isTrial() ? new String[]{
            "HUD: удобное расположение нужной информации",
            "Мозаика: стильный материал интерфейса по умолчанию",
            "Ватермарк: фиксированный фирменный статус",
            "Темы: подборка официальных цветовых палитр",
            "Оптимизация и плавные анимации",
            "Разработчик: Virion (@virionDEV)",
            "Сделано для комфортной игры"
        } : new String[]{
            "Эффекты мира и сражений",
            "HUD: нужная информация рядом",
            "Музыка: плеер и слова песни",
            "Питомцы: маленький спутник в игре",
            "Темы: твои цвета и материалы",
            "Разработчик: Virion (@virionDEV)",
            "Сделано для удобной игры"
        };
        float curF = card2Y + 26.0f;
        for (String feat : feats) {
            Render2D.rect(contentX + 15.0f, curF + 3.0f, 3.0f, 3.0f, 1.5f, ClientAccent.accent(200.0f * alpha));
            Fonts.MONTSERRAT_MEDIUM.draw(rtx.nv.api.ui.settings.RenderHelper.fitText(Fonts.MONTSERRAT_MEDIUM, feat, contentW - 40.0f, 6.0f), contentX + 23.0f, curF, 6.0f, UI.color(200, 205, 220, 215, alpha));
            curF += 15.5f;
        }

        // 3. Quick Actions Row (Buttons)
        float btnRowY = card2Y + card2H + 7.0f;
        float btnH = 20.0f;
        float gap = 5.0f;
        float btnW = (contentW - 12.0f - gap * 2.0f) / 3.0f;

        // Button 1: Reset Position
        this.lastAboutResetPosBtnX = contentX + 6.0f;
        this.lastAboutResetPosBtnY = btnRowY;
        this.lastAboutResetPosBtnW = btnW;
        this.lastAboutResetPosBtnH = btnH;
        boolean hov1 = mouseX >= lastAboutResetPosBtnX && mouseX <= lastAboutResetPosBtnX + btnW && mouseY >= btnRowY && mouseY <= btnRowY + btnH;
        Render2D.rect(lastAboutResetPosBtnX, btnRowY, btnW, btnH, 4.0f, ThemeManager.rgba(0, (hov1 ? 75.0f : 45.0f) * alpha));
        Render2D.outline(lastAboutResetPosBtnX, btnRowY, btnW, btnH, 4.0f, 0.6f, hov1 ? ClientAccent.accent(180.0f * alpha) : ThemeManager.rgba(0xFFFFFF, 18.0f * alpha));
        Fonts.NV.msdf(NvIcons.ASPECT_RATIO, lastAboutResetPosBtnX + 6.0f, btnRowY + 5.5f, 7.5f, hov1 ? ClientAccent.accentBright(240.0f * alpha) : UI.color(255, 255, 255, 170, alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("Сброс позиции", lastAboutResetPosBtnX + 19.0f, btnRowY + 5.5f, 5.5f, UI.color(255, 255, 255, hov1 ? 255 : 210, alpha));

        // Button 2: Reset Sidebar Width
        this.lastAboutResetSidebarBtnX = lastAboutResetPosBtnX + btnW + gap;
        this.lastAboutResetSidebarBtnY = btnRowY;
        this.lastAboutResetSidebarBtnW = btnW;
        this.lastAboutResetSidebarBtnH = btnH;
        boolean hov2 = mouseX >= lastAboutResetSidebarBtnX && mouseX <= lastAboutResetSidebarBtnX + btnW && mouseY >= btnRowY && mouseY <= btnRowY + btnH;
        Render2D.rect(lastAboutResetSidebarBtnX, btnRowY, btnW, btnH, 4.0f, ThemeManager.rgba(0, (hov2 ? 75.0f : 45.0f) * alpha));
        Render2D.outline(lastAboutResetSidebarBtnX, btnRowY, btnW, btnH, 4.0f, 0.6f, hov2 ? ClientAccent.accent(180.0f * alpha) : ThemeManager.rgba(0xFFFFFF, 18.0f * alpha));
        Fonts.NV.msdf(NvIcons.SCALE, lastAboutResetSidebarBtnX + 6.0f, btnRowY + 5.5f, 7.5f, hov2 ? ClientAccent.accentBright(240.0f * alpha) : UI.color(255, 255, 255, 170, alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("Ширина меню", lastAboutResetSidebarBtnX + 19.0f, btnRowY + 5.5f, 5.5f, UI.color(255, 255, 255, hov2 ? 255 : 210, alpha));

        // Button 3: Reset Theme
        this.lastAboutResetThemeBtnX = lastAboutResetSidebarBtnX + btnW + gap;
        this.lastAboutResetThemeBtnY = btnRowY;
        this.lastAboutResetThemeBtnW = btnW;
        this.lastAboutResetThemeBtnH = btnH;
        boolean hov3 = mouseX >= lastAboutResetThemeBtnX && mouseX <= lastAboutResetThemeBtnX + btnW && mouseY >= btnRowY && mouseY <= btnRowY + btnH;
        Render2D.rect(lastAboutResetThemeBtnX, btnRowY, btnW, btnH, 4.0f, ThemeManager.rgba(0, (hov3 ? 75.0f : 45.0f) * alpha));
        Render2D.outline(lastAboutResetThemeBtnX, btnRowY, btnW, btnH, 4.0f, 0.6f, hov3 ? ClientAccent.accent(180.0f * alpha) : ThemeManager.rgba(0xFFFFFF, 18.0f * alpha));
        Fonts.NV.msdf(NvIcons.THEMES, lastAboutResetThemeBtnX + 6.0f, btnRowY + 5.5f, 7.5f, hov3 ? ClientAccent.accentBright(240.0f * alpha) : UI.color(255, 255, 255, 170, alpha));
        Fonts.MONTSERRAT_MEDIUM.draw("Сброс темы", lastAboutResetThemeBtnX + 19.0f, btnRowY + 5.5f, 5.5f, UI.color(255, 255, 255, hov3 ? 255 : 210, alpha));
    }

    public boolean clickAboutCategory(float mouseX, float mouseY) {
        if (mouseX >= this.lastAboutResetPosBtnX && mouseX <= this.lastAboutResetPosBtnX + this.lastAboutResetPosBtnW &&
            mouseY >= this.lastAboutResetPosBtnY && mouseY <= this.lastAboutResetPosBtnY + this.lastAboutResetPosBtnH) {
            customPanelX = -1.0f;
            customPanelY = -1.0f;
            isDraggingPanel = false;
            try { Sounds.play("select_category"); } catch (Throwable ignored) {}
            return true;
        }
        if (mouseX >= this.lastAboutResetSidebarBtnX && mouseX <= this.lastAboutResetSidebarBtnX + this.lastAboutResetSidebarBtnW &&
            mouseY >= this.lastAboutResetSidebarBtnY && mouseY <= this.lastAboutResetSidebarBtnY + this.lastAboutResetSidebarBtnH) {
            customSidebarW = DEFAULT_SIDEBAR_W;
            isDraggingSidebar = false;
            try { Sounds.play("select_category"); } catch (Throwable ignored) {}
            return true;
        }
        if (mouseX >= this.lastAboutResetThemeBtnX && mouseX <= this.lastAboutResetThemeBtnX + this.lastAboutResetThemeBtnW &&
            mouseY >= this.lastAboutResetThemeBtnY && mouseY <= this.lastAboutResetThemeBtnY + this.lastAboutResetThemeBtnH) {
            ThemeManager.set(rtx.nv.api.ui.theme.Theme.NIVORA);
            try { Sounds.play("select_category"); } catch (Throwable ignored) {}
            return true;
        }
        return false;
    }
}

