package rtx.nv.api.drags;
import rtx.nv.api.events.EventHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import rtx.nv.NV;
import rtx.nv.api.config.ConfigManager;
import rtx.nv.api.drags.DragController;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.drags.HudSettingsPanel;
import rtx.nv.api.drags.MitosisController;
import rtx.nv.api.drags.MitosisController.SplitDraw;
import rtx.nv.api.drags.Position;
import rtx.nv.api.drags.SplitRectComp;
import rtx.nv.api.drags.components.ArmorComp;
import rtx.nv.api.drags.components.ArrayListComp;
import rtx.nv.api.drags.components.CooldownsComp;
import rtx.nv.api.drags.components.HPFocusComp;
import rtx.nv.api.drags.components.HotKeysComp;
import rtx.nv.api.drags.components.InventoryComp;
import rtx.nv.api.drags.components.KeyStrokesComp;
import rtx.nv.api.drags.components.MusicHudComp;
import rtx.nv.api.drags.components.PotionsComp;
import rtx.nv.api.drags.components.ScoreboardComp;
import rtx.nv.api.drags.components.TargetHudComp;
import rtx.nv.api.drags.components.WatermarkComp;
import rtx.nv.api.drags.hud.InfoHud;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.game.CloseScreenEvent;
import rtx.nv.api.events.impl.input.MouseButtonEvent;
import rtx.nv.api.events.impl.input.MouseButtonEvent.Action;
import rtx.nv.api.events.impl.render.HudRenderEvent;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.utils.render.others.LoadingVisualGuard;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.post.hitbubbles.HitBubblesRenderer;
import rtx.nv.utils.render.render2d.Render2D;

public final class DragSystem {
    private static final DragSystem INSTANCE = new DragSystem();
    private final List<Draggable> elements = new ArrayList<Draggable>();
    private final Set<String> renderFailures = new HashSet<String>();
    private Draggable activeDrag = null;

    private DragSystem() {
    }

    public static DragSystem get() {
        return INSTANCE;
    }

    public void register(Draggable draggable) {
        this.elements.add(draggable);
    }

    public void init() {
        this.register(new WatermarkComp());
        this.register(new HotKeysComp());
        this.register(new TargetHudComp());
        this.register(new PotionsComp());
        if (!rtx.nv.ClientEdition.isTrial()) {
            this.register(new CooldownsComp());
        }
        this.register(new ArrayListComp());
        this.register(new ArmorComp());
        this.register(new InventoryComp());
        if (!rtx.nv.ClientEdition.isTrial()) {
            this.register(new HPFocusComp());
            this.register(new KeyStrokesComp());
        }
        this.register(new ScoreboardComp());
        if (!rtx.nv.ClientEdition.isTrial()) {
            this.register(new MusicHudComp());
            this.register(new rtx.nv.api.drags.components.MusicSubtitlesComp());
        }
        new InfoHud();
        ConfigManager.applyActiveDrags();
        EventBus.get().subscribe(this);
    }

    public void unregister(Draggable draggable) {
        this.elements.remove(draggable);
        if (this.activeDrag == draggable) {
            this.activeDrag = null;
        }
    }

    public void resetAllPositions() {
        for (Draggable draggable : this.elements) {
            if (draggable != null) {
                draggable.resetToDefault();
            }
        }
        ConfigManager.markDirty();
    }

    @EventHandler
    public void onCloseScreen(CloseScreenEvent closeScreenEvent) {
        if (this.activeDrag != null) {
            this.activeDrag.getDrag().release();
            this.activeDrag = null;
            ConfigManager.markDirty();
        }
    }

    public void bringToFront(Draggable draggable) {
        int n = this.elements.indexOf(draggable);
        if (n < 0 || n == this.elements.size() - 1) {
            return;
        }
        this.elements.remove(n);
        this.elements.add(draggable);
    }

    public boolean isDragModeActive() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.currentScreen == null) {
            return false;
        }
        return mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof rtx.nv.api.ui.UI;
    }

    private void handleRenderFailure(Draggable draggable, Throwable throwable) {
        try {
            RectUtil.clearSplitOverride();
            Render2D.flush();
        }
        catch (Throwable throwable2) {
            // empty catch block
        }
        String string = draggable.getClass().getName();
        if (this.renderFailures.add(string)) {
            NV.LOGGER.error("[DragSystem] Component {} failed to render and was isolated", (Object)string, (Object)throwable);
        }
    }

    private void releaseInterruptedDrag() {
        boolean bl = false;
        if (this.activeDrag != null) {
            this.activeDrag.getDrag().release();
            this.activeDrag = null;
            bl = true;
        }
        for (Draggable draggable : this.elements) {
            if (!draggable.getDrag().isDragging()) continue;
            draggable.getDrag().release();
            bl = true;
        }
        if (bl) {
            ConfigManager.markDirty();
        }
    }

    public void applyDragDistortion(Framebuffer framebuffer) {
        if (framebuffer == null || !this.isDragModeActive() || framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0) {
            return;
        }
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        if (interfaceModule != null && (interfaceModule.dragStyle.is("\u041e\u0431\u044b\u0447\u043d\u044b\u0439") || !interfaceModule.dragWaves.getValue())) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.getWindow() == null) {
            return;
        }
        float f = Math.max(1.0f, Position.screenWidth());
        float f2 = Math.max(1.0f, Position.screenHeight());
        float f3 = (float)framebuffer.textureWidth / (float)Math.max(1, framebuffer.textureHeight);
        long l = System.currentTimeMillis();
        float f4 = DragController.pulsePhase();
        float f5 = Math.max(0.0f, Math.min(1.0f, f4 / 0.12f));
        float f6 = f5 * (1.0f - f4);
        float f7 = 0.02f * f6;
        float[] fArray = new float[152];
        int n = 0;
        for (Draggable draggable : this.elements) {
            if (n >= 16) break;
            if (!draggable.getDrag().isDragging()) continue;
            float f8 = draggable.width();
            float f9 = draggable.height();
            float f10 = draggable.getDrag().getRenderX() + f8 * 0.5f;
            float f11 = draggable.getDrag().getRenderY() + f9 * 0.5f;
            float f12 = f10 / f;
            float f13 = 1.0f - f11 / f2;
            float f14 = Math.max(f8, f9) * 0.5f / f2;
            float f15 = Math.max(0.04f, Math.min(0.15f, f14 * 0.6f));
            float f16 = f4 * (f14 + 0.1f);
            int n2 = 24 + n * 8;
            fArray[n2] = f12;
            fArray[n2 + 1] = f13;
            fArray[n2 + 2] = f16;
            fArray[n2 + 3] = f15;
            fArray[n2 + 4] = f7;
            fArray[n2 + 5] = f6;
            fArray[n2 + 6] = f14;
            ++n;
        }
        if (n == 0) {
            return;
        }
        fArray[0] = n;
        fArray[1] = f3;
        fArray[2] = (float)(l % 100000L) / 1000.0f;
        fArray[3] = 0.0f;
        fArray[4] = 1.0f;
        HitBubblesRenderer.apply((Framebuffer)framebuffer, (float[])fArray);
    }

    public List<Draggable> getAll() {
        return Collections.unmodifiableList(this.elements);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventHandler
    public void onHud(HudRenderEvent hudRenderEvent) {
        Object object;
        if (LoadingVisualGuard.shouldSuppressHud(MinecraftClient.getInstance())) {
            return;
        }
        RectUtil.setHudRendering(true);
        try {
            DrawContext drawContext = hudRenderEvent.getGraphics();
        HudLayoutCoordinator.beginFrame();
        boolean bl = this.isDragModeActive();
        if (!bl) {
            this.releaseInterruptedDrag();
        }
        float f = Position.mouseX();
        float f2 = Position.mouseY();
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        boolean bl2 = interfaceModule != null && interfaceModule.dragStyle.is("\u041e\u0431\u044b\u0447\u043d\u044b\u0439");
        boolean bl3 = bl2 && interfaceModule.dragTilt.getValue();
        MitosisController mitosisController = MitosisController.get();
        mitosisController.beginFrame();
        mitosisController.stage();
        if (bl) {
            for (Draggable draggable : this.elements) {
                if (!draggable.isInteractive() || mitosisController.isSplitting(draggable)) continue;
                draggable.getDrag().tick(f, f2, draggable.overlayWidth(), draggable.overlayHeight(), bl2);
            }
        }
        for (Draggable draggable : this.elements) {
            draggable.getDrag().updateTilt(f, bl3);
        }
        for (Draggable draggable : this.elements) {
            object = draggable.getDrag();
            if (((DragController)object).isDragging() || mitosisController.isSplitting(draggable)) continue;
            ((DragController)object).applyScreenClamp(draggable.width(), draggable.height());
        }
        for (Draggable draggable : this.elements) {
            if (!draggable.isVisible() || mitosisController.isSplitting(draggable)) continue;
            object = mitosisController.overrideFor(draggable);
            if (object != null) {
                RectUtil.armSplitOverride(((MitosisController.SplitDraw)object).x, ((MitosisController.SplitDraw)object).y, ((MitosisController.SplitDraw)object).width, ((MitosisController.SplitDraw)object).height, ((MitosisController.SplitDraw)object).radius, ((MitosisController.SplitDraw)object).index, ((MitosisController.SplitDraw)object).childX, ((MitosisController.SplitDraw)object).childY, ((MitosisController.SplitDraw)object).childW, ((MitosisController.SplitDraw)object).childH, ((MitosisController.SplitDraw)object).childRadius);
                try {
                    draggable.renderNormal(drawContext);
                    continue;
                }
                catch (Throwable throwable) {
                    this.handleRenderFailure(draggable, throwable);
                    continue;
                }
                finally {
                    RectUtil.clearSplitOverride();
                    continue;
                }
            }
            try {
                draggable.renderNormal(drawContext);
            }
            catch (Throwable throwable) {
                this.handleRenderFailure(draggable, throwable);
            }
        }
        if (bl) {
            for (Draggable draggable : this.elements) {
                if (draggable.getDrag().isDragging()) {
                    float snapX = draggable.getDrag().getSnapLineX();
                    if (Float.isFinite(snapX)) {
                        Render2D.beginFrame(drawContext);
                        Render2D.rect(snapX - 0.5f, 0.0f, 1.0f, Position.screenHeight(), 0.5f, 0x8055AAFF);
                        Render2D.flush();
                    }
                    float snapY = draggable.getDrag().getSnapLineY();
                    if (Float.isFinite(snapY)) {
                        Render2D.beginFrame(drawContext);
                        Render2D.rect(0.0f, snapY - 0.5f, Position.screenWidth(), 1.0f, 0.5f, 0x8055AAFF);
                        Render2D.flush();
                    }
                }
            }
            for (Draggable draggable : this.elements) {
                if (!draggable.isInteractive() || mitosisController.isSplitting(draggable)) continue;
                try {
                    draggable.getDrag().renderOverlay(drawContext, draggable.width(), draggable.height(), draggable.overlayWidth(), draggable.overlayHeight());
                }
                catch (Throwable throwable) {
                    this.handleRenderFailure(draggable, throwable);
                }
            }
        }
        } finally {
            RectUtil.setHudRendering(false);
        }
    }

    public JsonObject writeDrags() {
        JsonObject jsonObject = new JsonObject();
        for (Draggable draggable : this.elements) {
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("x", (Number)Float.valueOf(draggable.getDrag().getTargetX()));
            jsonObject2.addProperty("y", (Number)Float.valueOf(draggable.getDrag().getTargetY()));
            if (draggable instanceof rtx.nv.api.drags.components.WatermarkComp wc) {
                jsonObject2.addProperty("centered", wc.isCentered());
            }
            if (draggable instanceof rtx.nv.api.drags.components.ScoreboardComp sc) {
                jsonObject2.addProperty("userMoved", sc.isUserMoved());
            }
            jsonObject.add(draggable.getId(), (JsonElement)jsonObject2);
        }
        return jsonObject;
    }

    public void applyDrags(JsonObject jsonObject) {
        if (jsonObject == null) {
            return;
        }
        for (Draggable draggable : this.elements) {
            if (draggable instanceof WatermarkComp wc) {
                float sw = Position.screenWidth();
                float currentW = draggable.width();
                if (rtx.nv.ClientEdition.isTrial()) {
                    wc.setCentered(true);
                    draggable.getDrag().setTargetX((sw - currentW) * 0.5f);
                    draggable.getDrag().setTargetY(6.0f);
                    draggable.getDrag().syncToTarget();
                    continue;
                }
                JsonElement jsonElement = jsonObject.has(draggable.getId()) ? jsonObject.get(draggable.getId()) : null;
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    boolean centered = !jsonObject2.has("centered") || jsonObject2.get("centered").getAsBoolean();
                    float x = jsonObject2.has("x") ? jsonObject2.get("x").getAsFloat() : (sw - currentW) * 0.5f;
                    float y = jsonObject2.has("y") ? jsonObject2.get("y").getAsFloat() : 6.0f;
                    if (centered || Math.abs((x + currentW * 0.5f) - sw * 0.5f) < currentW * 0.5f || Math.abs(x - sw * 0.5f) < 20.0f) {
                        wc.setCentered(true);
                        x = (sw - currentW) * 0.5f;
                    } else {
                        wc.setCentered(false);
                    }
                    draggable.getDrag().setTargetX(x);
                    draggable.getDrag().setTargetY(y);
                    draggable.getDrag().syncToTarget();
                } else {
                    wc.setCentered(true);
                    draggable.getDrag().setTargetX((sw - currentW) * 0.5f);
                    draggable.getDrag().setTargetY(6.0f);
                    draggable.getDrag().syncToTarget();
                }
                continue;
            }
            JsonElement jsonElement = jsonObject.has(draggable.getId()) ? jsonObject.get(draggable.getId()) : null;
            if (jsonElement == null || !jsonElement.isJsonObject()) continue;
            JsonObject jsonObject2 = jsonElement.getAsJsonObject();
            try {
                if (draggable instanceof rtx.nv.api.drags.components.ScoreboardComp sc) {
                    boolean moved = jsonObject2.has("userMoved") && jsonObject2.get("userMoved").getAsBoolean();
                    sc.setUserMoved(moved);
                    if (!moved) {
                        continue;
                    }
                }
                if (jsonObject2.has("x")) {
                    draggable.getDrag().setTargetX(jsonObject2.get("x").getAsFloat());
                }
                if (jsonObject2.has("y")) {
                    draggable.getDrag().setTargetY(jsonObject2.get("y").getAsFloat());
                }
                draggable.getDrag().syncToTarget();
            }
            catch (Exception exception) {}
        }
    }

    @EventHandler
    public void onMouseButton(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.button == 0 && (mouseButtonEvent.modifiers & org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT) != 0
            && MinecraftClient.getInstance().currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen
            && rtx.nv.api.modules.impl.Visuals.ViewModel.getInstance() != null
            && rtx.nv.api.modules.impl.Visuals.ViewModel.getInstance().isEnabled()) return;
        if (!this.isDragModeActive()) {
            return;
        }
        float f = Position.mouseX();
        float f2 = Position.mouseY();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.currentScreen instanceof rtx.nv.api.ui.UI) {
            float px = rtx.nv.api.ui.UI.panelX();
            float py = rtx.nv.api.ui.UI.panelY();
            if (f >= px && f <= px + rtx.nv.api.ui.UI.PANEL_W && f2 >= py && f2 <= py + rtx.nv.api.ui.UI.PANEL_H) {
                return;
            }
        }
        if (mouseButtonEvent.action == MouseButtonEvent.Action.PRESS && mouseButtonEvent.button == 1 && this.activeDrag != null) {
            this.activeDrag.getDrag().cancel();
            this.activeDrag = null;
            ConfigManager.markDirty();
            mouseButtonEvent.cancel();
            return;
        }
        if (mouseButtonEvent.action == MouseButtonEvent.Action.PRESS && mouseButtonEvent.button == 1) {
            for (int i = this.elements.size() - 1; i >= 0; --i) {
                Draggable draggable = this.elements.get(i);
                if (!draggable.isInteractive() || !draggable.hitTest(f, f2) || !MitosisController.get().split(draggable)) continue;
                mouseButtonEvent.cancel();
                return;
            }
            return;
        }
        if (mouseButtonEvent.button != 0) {
            return;
        }
        if (mouseButtonEvent.action == MouseButtonEvent.Action.PRESS) {
            MitosisController mitosisController = MitosisController.get();
            for (int i = this.elements.size() - 1; i >= 0; --i) {
                List<Setting> list;
                SplitRectComp splitRectComp;
                Draggable draggable = this.elements.get(i);
                if (!draggable.isInteractive() || mitosisController.isAnimating(draggable)) continue;
                if (draggable instanceof SplitRectComp && (splitRectComp = (SplitRectComp)draggable).origin() != null && !(list = splitRectComp.origin().hudSettings()).isEmpty() && HudSettingsPanel.click((float)f, (float)f2, (float)draggable.getDrag().getRenderX(), (float)draggable.getDrag().getRenderY(), (float)draggable.width(), list)) {
                    ConfigManager.markDirty();
                    mouseButtonEvent.cancel();
                    return;
                }
                if (!draggable.getDrag().tryGrab(f, f2, draggable.width(), draggable.height())) continue;
                this.activeDrag = draggable;
                this.bringToFront(draggable);
                mouseButtonEvent.cancel();
                return;
            }
        } else if (mouseButtonEvent.action == MouseButtonEvent.Action.RELEASE && this.activeDrag != null) {
            this.activeDrag.getDrag().release();
            this.activeDrag = null;
            ConfigManager.markDirty();
            mouseButtonEvent.cancel();
        }
    }
}

