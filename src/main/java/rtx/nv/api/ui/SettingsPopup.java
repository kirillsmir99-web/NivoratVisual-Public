package rtx.nv.api.ui;
import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.NV;
import rtx.nv.api.drags.DragController;
import rtx.nv.api.drags.Position;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.ui.ScrollBar;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.settings.SettingsFactory;
import rtx.nv.api.ui.settings.impl.SeparatorSetting;
import rtx.nv.api.ui.settings.impl.TextSetting;
import rtx.nv.utils.animations.Decelerate;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.others.RoundedScissor;
import rtx.nv.utils.render.render2d.Render2D;

public final class SettingsPopup {
    private static final float MIN_WIDTH = 110.0f;
    private static final float MAX_WIDTH = 180.0f;
    private static final float RADIUS = 8.0f;
    private static final float SIDE_PAD = 5.0f;
    private static final float TOP_PAD = 7.0f;
    private static final float BOTTOM_PAD = 7.0f;
    private static final float EDGE = 4.0f;
    private final Decelerate anim = new Decelerate();
    private final ScrollBar scrollBar = new ScrollBar();
    private Module module;
    private boolean open;
    private float width = 180.0f;
    private float px;
    private float py;
    private float height;
    private float bodyViewH;
    private float anchorY;
    private float maxBodyH;
    private float scroll;
    private float scrollTarget;
    private boolean draggingMove;
    private final DragController drag = new DragController(0.0f, 0.0f);
    private long lastNs = System.nanoTime();
    private List<Setting> widgets = Collections.emptyList();
    private final Set<String> widgetFailures = new HashSet<String>();

    public SettingsPopup() {
        this.anim.setMs(220);
        this.anim.setValue(1.0);
        this.anim.setDirection(Direction.BACKWARDS);
        this.anim.counter.setTime(System.currentTimeMillis() - 10000L);
    }

    private static float clamp(float f, float f2, float f3) {
        return f < f2 ? f2 : Math.min(f, f3);
    }

    public boolean contains(float f, float f2) {
        return this.isVisible() && f >= this.px && f <= this.px + this.width && f2 >= this.py && f2 <= this.py + this.height;
    }

    public boolean isOpen() {
        return this.open;
    }

    public void close() {
        if (!this.open) {
            return;
        }
        TextSetting.unfocusAll();
        this.open = false;
        this.draggingMove = false;
        this.drag.release();
        this.closeOverlays();
        this.anim.setDirection(Direction.BACKWARDS);
        this.anim.counter.resetCounter();
    }

    public void render(DrawContext drawContext, float f) {
        float f2;
        float f3;
        boolean bl;
        boolean bl2;
        if (this.module == null) {
            return;
        }
        float f4 = this.anim.getOutput().floatValue();
        if (f4 <= 0.01f) {
            if (!this.open) {
                this.module = null;
                this.widgets = Collections.emptyList();
            }
            return;
        }
        float f5 = f4 * f;
        long l = System.nanoTime();
        float f6 = Math.min(0.1f, (float)(l - this.lastNs) / 1.0E9f);
        this.lastNs = l;
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        boolean bl3 = interfaceModule != null && interfaceModule.dragStyle.is("\u041e\u0431\u044b\u0447\u043d\u044b\u0439");
        boolean bl4 = bl2 = bl3 && interfaceModule.dragTilt.getValue();
        if (this.draggingMove) {
            this.drag.tick(Position.mouseX(), Position.mouseY(), this.width, this.height, bl3);
        }
        this.drag.updateTilt(Position.mouseX(), bl2);
        this.px = this.drag.getRenderX();
        this.anchorY = this.drag.getRenderY();
        this.layout();
        this.scroll += (this.scrollTarget - this.scroll) * (1.0f - (float)Math.exp(-f6 * 16.0f));
        if (Math.abs(this.scrollTarget - this.scroll) < 0.05f) {
            this.scroll = this.scrollTarget;
        }
        float f7 = this.py + (1.0f - f4) * 4.0f;
        float f8 = this.drag.getTiltAngle();
        boolean bl5 = bl = Math.abs(f8) > 0.01f;
        if (bl) {
            f3 = this.px + this.width * 0.5f;
            f2 = f7 + this.height * 0.5f;
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(f3, f2);
            drawContext.getMatrices().rotate((float)Math.toRadians(f8));
            drawContext.getMatrices().translate(-f3, -f2);
        }
        RectUtil.drawClientRectFixedRadius(this.px, f7, this.width, this.height, 8.0f, f5, 0.0f);
        Render2D.outline(this.px, f7, this.width, this.height, 8.0f, 0.6f, SettingsPopup.rgba(255, 255, 255, 24.0f * f5));
        f3 = f7 + 7.0f;
        f2 = bl ? (float)Math.abs(Math.sin(Math.toRadians(f8))) * this.width : 0.0f;
        RoundedScissor.push(drawContext, this.px, f3, this.width, this.bodyViewH, 0.0f, 0.0f, 0.0f, 0.0f);
        Render2D.pushScissor(drawContext, this.px, f3 - f2, this.width, this.bodyViewH + f2 * 2.0f);
        float f9 = f3 - this.scroll;
        for (Setting setting : this.widgets) {
            if (!setting.isVisible()) continue;
            float f10 = setting.height();
            if (f9 + f10 >= f3 - 6.0f && f9 <= f3 + this.bodyViewH + 6.0f) {
                try {
                    setting.render(this.px + 5.0f, f9, this.width - 10.0f, f5);
                }
                catch (Throwable throwable) {
                    this.handleWidgetFailure(setting, throwable);
                }
            }
            f9 += f10 + 4.0f;
        }
        Render2D.popScissor(drawContext);
        RoundedScissor.pop();
        float f11 = this.contentHeight();
        if (f11 > this.bodyViewH + 0.5f) {
            float f12 = this.scrollBar.render(this.px + this.width - 4.5f, f3, this.bodyViewH, this.bodyViewH, f11, this.scroll, f5);
            if (this.scrollBar.isDragging()) {
                this.scroll = f12;
                this.scrollTarget = f12;
            }
        }
        float f13 = f3 - this.scroll;
        for (Setting setting : this.widgets) {
            if (!setting.isVisible()) continue;
            if (setting.hasOverlay()) {
                try {
                    setting.renderOverlay(drawContext, this.px + 5.0f, f13, this.width - 10.0f, f5);
                }
                catch (Throwable throwable) {
                    this.handleWidgetFailure(setting, throwable);
                }
            }
            f13 += setting.height() + 4.0f;
        }
        if (bl) {
            drawContext.getMatrices().popMatrix();
        }
        this.drag.renderOverlay(drawContext, this.width, this.height, this.width, this.height);
    }

    private static int rgba(int n, int n2, int n3, float f) {
        int n4 = Math.max(0, Math.min(255, Math.round(f)));
        if (n4 <= 0) {
            return 0;
        }
        return new Color(n, n2, n3, n4).getRGB();
    }

    public boolean toggle(Module module, float f, float f2, float f3, float f4, float f5, float f6) {
        if (this.isOpenFor(module.getName())) {
            this.close();
            return false;
        }
        this.module = module;
        this.widgets = SettingsFactory.build(module);
        this.open = true;
        this.scroll = 0.0f;
        this.scrollTarget = 0.0f;
        this.draggingMove = false;
        this.maxBodyH = f6 - 7.0f - 7.0f - 8.0f;
        float f7 = 0.0f;
        for (Setting setting : this.widgets) {
            f7 = Math.max(f7, setting.preferredWidth());
        }
        this.width = SettingsPopup.clamp(f7 + 10.0f + 4.0f, 110.0f, 180.0f);
        this.anchorY = f2;
        this.px = SettingsPopup.clamp(f + 2.5f, 4.0f, Position.screenWidth() - this.width - 4.0f);
        this.layout();
        this.anchorY = SettingsPopup.clamp(this.anchorY, 4.0f, Position.screenHeight() - this.height - 4.0f);
        this.layout();
        this.drag.release();
        this.drag.setTargetX(this.px);
        this.drag.setTargetY(this.anchorY);
        this.drag.syncToTarget();
        this.anim.setDirection(Direction.FORWARDS);
        this.anim.counter.resetCounter();
        return true;
    }

    public boolean isVisible() {
        return this.module != null && this.anim.getOutput().floatValue() > 0.01f;
    }

    private void layout() {
        float f = this.contentHeight();
        this.bodyViewH = Math.min(f, this.maxBodyH);
        this.height = 7.0f + this.bodyViewH + 7.0f;
        this.py = this.anchorY;
        float f2 = Math.max(0.0f, f - this.bodyViewH);
        this.scrollTarget = SettingsPopup.clamp(this.scrollTarget, 0.0f, f2);
        this.scroll = SettingsPopup.clamp(this.scroll, 0.0f, f2);
    }

    public boolean click(float f, float f2) {
        if (!this.open || this.module == null) {
            return false;
        }
        boolean bl = false;
        float f3 = this.bodyY() - this.scroll;
        for (Setting object : this.widgets) {
            if (!object.isVisible()) continue;
            if (object.isOverlayOpen()) {
                bl = true;
                if (object.clickOverlay(this.px + 5.0f, f3, this.width - 10.0f, f, f2)) {
                    return true;
                }
            }
            f3 += object.height() + 4.0f;
        }
        if (bl) {
            this.closeOverlays();
            return true;
        }
        if (!this.contains(f, f2)) {
            this.close();
            return true;
        }
        if (this.scrollBar.tryGrab(f, f2)) {
            return true;
        }
        if (f2 >= this.py + 7.0f) {
            float f4 = this.bodyY() - this.scroll;
            for (Setting setting : this.widgets) {
                if (!setting.isVisible()) continue;
                float f5 = setting.height();
                if (f2 >= f4 && f2 < f4 + f5) {
                    if (setting instanceof SeparatorSetting) break;
                    boolean bl2 = setting.click(this.px + 5.0f, f4, this.width - 10.0f, f, f2);
                    if (bl2 && setting.isOverlayOpen()) {
                        this.closeOtherOverlays(setting);
                    }
                    return true;
                }
                f4 += f5 + 4.0f;
            }
        }
        this.beginDrag(f, f2);
        return true;
    }

    public List<Setting> widgets() {
        return this.open ? this.widgets : Collections.emptyList();
    }

    public boolean isOpenFor(String string) {
        return this.open && this.module != null && string != null && this.module.getName().equals(string);
    }

    public float blurPhase() {
        if (this.module == null) {
            return 0.0f;
        }
        float f = this.anim.getOutput().floatValue();
        if (f <= 0.01f) {
            return 0.0f;
        }
        return SettingsPopup.clamp(1.0f - f, 0.0f, 1.0f);
    }

    public float shareX() {
        return this.px;
    }

    public float shareY() {
        return this.py;
    }

    public boolean scroll(float f, float f2, double d) {
        if (!this.open || this.module == null) {
            return false;
        }
        float f3 = this.bodyY() - this.scroll;
        for (Setting setting : this.widgets) {
            if (!setting.isVisible()) continue;
            if (setting.isOverlayOpen() && setting.scrollOverlay(this.px + 5.0f, f3, this.width - 10.0f, f, f2, d)) {
                return true;
            }
            f3 += setting.height() + 4.0f;
        }
        if (this.contains(f, f2)) {
            float f4 = Math.max(0.0f, this.contentHeight() - this.bodyViewH);
            this.scrollTarget = SettingsPopup.clamp(this.scrollTarget - (float)d * 12.0f, 0.0f, f4);
            return true;
        }
        return false;
    }

    public String shareModuleName() {
        return this.open && this.module != null ? this.module.getName() : "";
    }

    public void releaseDrags() {
        this.draggingMove = false;
        this.drag.release();
        this.scrollBar.release();
        for (Setting setting : this.widgets) {
            setting.releaseDrag();
        }
    }

    public boolean writeBlurRect(float[] fArray, int n) {
        if (this.module == null || fArray == null || n + 6 > fArray.length) {
            return false;
        }
        float f = this.anim.getOutput().floatValue();
        if (f <= 0.01f) {
            return false;
        }
        fArray[n] = this.px;
        fArray[n + 1] = this.py + (1.0f - f) * 4.0f;
        fArray[n + 2] = this.width;
        fArray[n + 3] = this.height;
        fArray[n + 4] = 1.0f;
        fArray[n + 5] = SettingsPopup.clamp(1.0f - f, 0.0f, 1.0f);
        return true;
    }

    public Module shareModule() {
        return this.open ? this.module : null;
    }

    public float shareScroll() {
        return this.scroll;
    }

    public boolean hasOpenOverlay() {
        for (Setting setting : this.widgets) {
            if (!setting.isOverlayOpen()) continue;
            return true;
        }
        return false;
    }

    public boolean middleClick(float f, float f2) {
        if (!this.open || this.module == null || !this.contains(f, f2)) {
            return false;
        }
        float f3 = this.bodyY() - this.scroll;
        for (Setting setting : this.widgets) {
            if (!setting.isVisible()) continue;
            float f4 = setting.height();
            if (f2 >= f3 && f2 < f3 + f4) {
                setting.middleClick(this.px + 5.0f, f3, this.width - 10.0f, f, f2);
                return true;
            }
            f3 += f4 + 4.0f;
        }
        return true;
    }

    public void closeOverlays() {
        for (Setting setting : this.widgets) {
            setting.closeOverlay();
        }
    }

    private void closeOtherOverlays(Setting setting) {
        for (Setting setting2 : this.widgets) {
            if (setting2 == setting) continue;
            setting2.closeOverlay();
        }
    }

    private void handleWidgetFailure(Setting setting, Throwable throwable) {
        try {
            Render2D.flush();
        }
        catch (Throwable throwable2) {
            // empty catch block
        }
        String string = setting.getClass().getName();
        if (this.widgetFailures.add(string)) {
            NV.LOGGER.error("[SettingsPopup] Setting {} failed to render and was isolated", (Object)string, (Object)throwable);
        }
    }

    private float contentHeight() {
        float f = 0.0f;
        int n = 0;
        for (Setting setting : this.widgets) {
            if (!setting.isVisible()) continue;
            f += setting.height() + 4.0f;
            ++n;
        }
        return n == 0 ? 0.0f : f - 4.0f;
    }

    private void beginDrag(float f, float f2) {
        this.drag.setTargetX(this.px);
        this.drag.setTargetY(this.anchorY);
        this.draggingMove = this.drag.tryGrab(f, f2, this.width, this.height);
    }

    private float bodyY() {
        return this.py + 7.0f;
    }
}

