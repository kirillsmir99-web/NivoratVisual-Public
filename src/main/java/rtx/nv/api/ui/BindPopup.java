package rtx.nv.api.ui;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.drags.DragController;
import rtx.nv.api.drags.Position;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.Module.BindMode;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.animations.Decelerate;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.key.KeyBind;
import rtx.nv.utils.key.KeyHelper;
import rtx.nv.utils.render.render2d.Render2D;

public final class BindPopup {
    private static final String FONT = "montserrat-medium";
    private static final float WIDTH = 96.0f;
    private static final float PAD = 6.0f;
    private static final float ROW_H = 13.0f;
    private static final float GAP = 5.0f;
    private static final float HEIGHT = 43.0f;
    private final Decelerate anim = new Decelerate();
    private Module module;
    private boolean open;
    private boolean listening;
    private float px;
    private float py;
    private float selT;
    private long lastFrameMs = System.currentTimeMillis();
    private boolean dragging;
    private final DragController drag = new DragController(0.0f, 0.0f);

    public BindPopup() {
        this.anim.setMs(220);
        this.anim.setValue(1.0);
        this.anim.setDirection(Direction.BACKWARDS);
        this.anim.counter.setTime(System.currentTimeMillis() - 10000L);
    }

    public Module module() {
        return this.module;
    }

    public boolean isOpen() {
        return this.open;
    }

    public void close() {
        if (!this.open) {
            return;
        }
        this.open = false;
        this.listening = false;
        this.anim.setDirection(Direction.BACKWARDS);
        this.anim.counter.resetCounter();
    }

    public void open(Module module, float f, float f2) {
        this.module = module;
        this.open = true;
        this.listening = false;
        this.px = Math.max(8.0f, Math.min(Position.screenWidth() - 110.0f - 8.0f, f));
        this.py = Math.max(8.0f, Math.min(Position.screenHeight() - 70.0f - 8.0f, f2 + 2.0f));
        this.dragging = false;
        this.drag.release();
        this.drag.setTargetX(this.px);
        this.drag.setTargetY(this.py);
        this.drag.syncToTarget();
        this.selT = module.getBindMode() == Module.BindMode.HOLD ? 1.0f : 0.0f;
        this.anim.setDirection(Direction.FORWARDS);
        this.anim.counter.resetCounter();
    }

    public void render(DrawContext drawContext, float f) {
        float f2;
        float f3;
        boolean bl;
        if (this.module == null) {
            return;
        }
        float f4 = this.anim.getOutput().floatValue();
        if (f4 <= 0.01f) {
            if (!this.open) {
                this.module = null;
            }
            return;
        }
        float f5 = f4 * f;
        long l = System.currentTimeMillis();
        float f6 = Math.min((float)(l - this.lastFrameMs) / 1000.0f, 0.1f);
        this.lastFrameMs = l;
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        boolean bl2 = interfaceModule != null && interfaceModule.dragStyle.is("\u041e\u0431\u044b\u0447\u043d\u044b\u0439");
        boolean bl3 = bl2 && interfaceModule.dragTilt.getValue();
        float f7 = Position.mouseX();
        if (this.dragging) {
            this.drag.tick(f7, Position.mouseY(), 96.0f, 43.0f, bl2);
        }
        this.drag.updateTilt(f7, bl3);
        this.px = this.drag.getRenderX();
        this.py = this.drag.getRenderY();
        float f8 = this.py + (1.0f - f4) * 4.0f;
        float f9 = this.drag.getTiltAngle();
        boolean bl4 = bl = Math.abs(f9) > 0.01f;
        if (bl) {
            f3 = this.px + 48.0f;
            f2 = f8 + 21.5f;
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(f3, f2);
            drawContext.getMatrices().rotate((float)Math.toRadians(f9));
            drawContext.getMatrices().translate(-f3, -f2);
        }
        f3 = this.module.getBindMode() == Module.BindMode.HOLD ? 1.0f : 0.0f;
        this.selT += (f3 - this.selT) * Math.min(1.0f, f6 * 14.0f);
        RenderHelper.drawDropBackground(this.px, f8, 96.0f, 43.0f, f5);
        f2 = f8 + 6.0f;
        Render2D.text(FONT, "\u0411\u0438\u043d\u0434", this.px + 6.0f, f2 + 3.0f, 6.0f, ColorUtil.multAlpha(-1, 0.85f * f5));
        String string = this.listening ? "..." : this.keyDisplay();
        float f10 = Math.max(34.0f, Render2D.textWidth(FONT, string, 6.0f) + 10.0f);
        float f11 = this.px + 96.0f - 6.0f - f10;
        RenderHelper.drawPanelBg(f11, f2, f10, 13.0f, 3.0f, f5);
        Render2D.text(FONT, string, f11 + (f10 - Render2D.textWidth(FONT, string, 6.0f)) * 0.5f, f2 + 3.0f, 6.0f, ColorUtil.multAlpha(-1, 0.9f * f5));
        float f12 = f2 + 13.0f + 5.0f;
        float f13 = 39.5f;
        float f14 = this.px + 6.0f;
        float f15 = this.px + 6.0f + f13 + 5.0f;
        RenderHelper.drawPanelBg(f14, f12, f13, 13.0f, 3.0f, f5);
        RenderHelper.drawPanelBg(f15, f12, f13, 13.0f, 3.0f, f5);
        float f16 = f14 + (f15 - f14) * this.selT;
        Render2D.rect(f16, f12, f13, 13.0f, 3.0f, ColorUtil.multAlpha(ClientAccent.accentSoft(210.0f), f5));
        this.drawModeLabel(f14, f12, f13, "Toggle", 1.0f - this.selT, f5);
        this.drawModeLabel(f15, f12, f13, "Hold", this.selT, f5);
        if (bl) {
            drawContext.getMatrices().popMatrix();
        }
        this.drag.renderOverlay(drawContext, 96.0f, 43.0f, 96.0f, 43.0f);
    }

    public boolean isVisible() {
        return this.module != null && this.anim.getOutput().floatValue() > 0.01f;
    }

    public boolean click(float f, float f2) {
        if (!this.open || this.module == null) {
            return false;
        }
        if (f < this.px || f > this.px + 96.0f || f2 < this.py || f2 > this.py + 43.0f) {
            this.close();
            return false;
        }
        float f3 = this.py + 6.0f;
        String string = this.listening ? "..." : this.keyDisplay();
        float f4 = Math.max(34.0f, Render2D.textWidth(FONT, string, 6.0f) + 10.0f);
        float f5 = this.px + 96.0f - 6.0f - f4;
        if (f >= f5 && f <= f5 + f4 && f2 >= f3 && f2 <= f3 + 13.0f) {
            this.listening = !this.listening;
            return true;
        }
        float f6 = f3 + 13.0f + 5.0f;
        float f7 = 39.5f;
        if (f2 >= f6 && f2 <= f6 + 13.0f) {
            if (f >= this.px + 6.0f && f <= this.px + 6.0f + f7) {
                this.module.setBindMode(Module.BindMode.TOGGLE);
                return true;
            }
            if (f >= this.px + 6.0f + f7 + 5.0f && f <= this.px + 6.0f + f7 + 5.0f + f7) {
                this.module.setBindMode(Module.BindMode.HOLD);
                return true;
            }
        }
        this.drag.setTargetX(this.px);
        this.drag.setTargetY(this.py);
        this.dragging = this.drag.tryGrab(f, f2, 96.0f, 43.0f);
        return true;
    }

    public float blurPhase() {
        if (this.module == null) {
            return 0.0f;
        }
        float f = this.anim.getOutput().floatValue();
        if (f <= 0.01f) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, 1.0f - f));
    }

    public boolean mouseBind(int n) {
        if (!this.open || this.module == null || !this.listening) {
            return false;
        }
        if (this.module instanceof rtx.nv.api.modules.impl.Interface.ClickGui) {
            this.listening = false;
            return true;
        }
        this.module.setBind(KeyBind.mouse(n));
        this.listening = false;
        return true;
    }

    public boolean keyPressed(int n) {
        if (!this.open || this.module == null || !this.listening) {
            return false;
        }
        if (this.module instanceof rtx.nv.api.modules.impl.Interface.ClickGui) {
            if (n == 256 || n <= 0) {
                this.module.setBind(KeyBind.keyboard(344));
            } else {
                this.module.setBind(KeyBind.keyboard(n));
            }
            this.listening = false;
            return true;
        }
        if (n == 256) {
            this.module.setBind(KeyBind.NONE);
        } else {
            this.module.setBind(KeyBind.keyboard(n));
        }
        this.listening = false;
        return true;
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
        fArray[n + 2] = 96.0f;
        fArray[n + 3] = 43.0f;
        fArray[n + 4] = 1.0f;
        fArray[n + 5] = Math.max(0.0f, Math.min(1.0f, 1.0f - f));
        return true;
    }

    public void releaseDrag() {
        this.dragging = false;
        this.drag.release();
    }

    private void drawModeLabel(float f, float f2, float f3, String string, float f4, float f5) {
        int n = ColorUtil.multAlpha(-1, 0.65f * f5);
        int n2 = ColorUtil.multAlpha(-15856114, f5);
        int n3 = ColorUtil.lerpColor(n, n2, f4);
        float f6 = Render2D.textWidth(FONT, string, 6.0f);
        Render2D.text(FONT, string, f + (f3 - f6) * 0.5f, f2 + 3.0f, 6.0f, n3);
    }

    private String keyDisplay() {
        if (this.module == null) {
            return "None";
        }
        KeyBind keyBind = this.module.getBind();
        return keyBind != null && keyBind.isBound() ? KeyHelper.getShortName(keyBind.getCode()) : "None";
    }
}

