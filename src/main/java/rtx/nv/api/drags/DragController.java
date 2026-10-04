package rtx.nv.api.drags;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.drags.DragLerpAnim;
import rtx.nv.api.drags.DragOverlayRenderer;
import rtx.nv.api.drags.Position;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.utils.math.MathUtils;

public final class DragController {
    private float xPos;
    private float yPos;
    private float desiredX;
    private float desiredY;
    private float startX;
    private float startY;
    private float preDragX;
    private float preDragY;
    private boolean dragging;
    private boolean cancelled;
    private float snapLineX = Float.NaN;
    private float snapLineY = Float.NaN;
    private float tiltAngle;
    private float tiltVelocity;
    private float filteredMouseVelocity;
    private float lastTiltMouseX;
    private long lastTiltUpdateNs;
    private boolean renderPositionLocked;
    private float lockedRenderX;
    private float lockedRenderY;
    private boolean directDrag;
    private boolean forceDirectDrag;
    private final DragLerpAnim dragAnimPc = new DragLerpAnim(0.0f, 0.0f, 0.125f);
    private DragLerpAnim animX;
    private DragLerpAnim animY;
    private DragLerpAnim animX2;
    private DragLerpAnim animY2;
    public static final long PULSE_PERIOD_MS = 500L;

    public DragController(float f, float f2) {
        this.xPos = f;
        this.yPos = f2;
        this.desiredX = f;
        this.desiredY = f2;
    }

    public void cancel() {
        if (!this.dragging) {
            return;
        }
        this.cancelled = true;
        this.dragging = false;
        this.xPos = this.preDragX;
        this.yPos = this.preDragY;
        this.desiredX = this.preDragX;
        this.desiredY = this.preDragY;
        this.ensureAnims();
        this.animX.setTo(this.xPos);
        this.animY.setTo(this.yPos);
        this.animX2.setTo(this.xPos);
        this.animY2.setTo(this.yPos);
    }

    public void release() {
        if (!this.dragging) {
            return;
        }
        this.cancelled = false;
        this.dragging = false;
        if (this.animX != null) {
            this.animX.setAnim(this.xPos);
            this.animX.setTo(this.xPos);
        }
        if (this.animY != null) {
            this.animY.setAnim(this.yPos);
            this.animY.setTo(this.yPos);
        }
    }

    public float getTiltAngle() {
        return this.tiltAngle;
    }

    void beginRender() {
        if (this.renderPositionLocked) {
            return;
        }
        this.lockedRenderX = this.computeRenderX();
        this.lockedRenderY = this.computeRenderY();
        this.renderPositionLocked = true;
    }

    public void syncToTarget() {
        this.ensureAnims();
        this.animX.setAnim(this.xPos);
        this.animX.setTo(this.xPos);
        this.animY.setAnim(this.yPos);
        this.animY.setTo(this.yPos);
        this.animX2.setAnim(this.xPos);
        this.animX2.setTo(this.xPos);
        this.animY2.setAnim(this.yPos);
        this.animY2.setTo(this.yPos);
    }

    public void applyScreenClamp(float f, float f2) {
        this.xPos = Position.clampX(this.desiredX, f);
        this.yPos = Position.clampY(this.desiredY, f2);
    }

    public void renderOverlay(DrawContext drawContext, float f, float f2, float f3, float f4) {
        // Ghost renderer removed per Task 43
    }

    public void swapGrabOffset() {
        if (!this.dragging) {
            return;
        }
        float f = this.startX;
        this.startX = this.startY;
        this.startY = f;
    }

    private void ensureAnims() {
        if (this.animX == null) {
            this.animX = new DragLerpAnim(this.xPos, this.xPos, 0.08f);
        }
        if (this.animY == null) {
            this.animY = new DragLerpAnim(this.yPos, this.yPos, 0.08f);
        }
        if (this.animX2 == null) {
            this.animX2 = new DragLerpAnim(this.xPos, this.xPos, 0.1f);
        }
        if (this.animY2 == null) {
            this.animY2 = new DragLerpAnim(this.yPos, this.yPos, 0.1f);
        }
    }

    private static float jitterStrength() {
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        if (interfaceModule != null && (interfaceModule.dragStyle.is("\u041e\u0431\u044b\u0447\u043d\u044b\u0439") || !interfaceModule.dragJitter.getValue())) {
            return 0.0f;
        }
        float f = DragController.pulsePhase();
        float f2 = f > 0.5f ? 1.0f - f : f;
        float f3 = f2 < 0.5f ? 4.0f * f2 * f2 * f2 : (float)(1.0 - Math.pow(-2.0 * (double)(f2 *= 2.0f) + 2.0, 3.0) / 2.0);
        return f3 * f3;
    }

    public float getSnapLineX() {
        return this.snapLineX;
    }

    public float overlayAlpha() {
        this.dragAnimPc.setToAsBoolean(this.dragging);
        return this.dragAnimPc.getAnim();
    }

    public void snapOverlayToTarget() {
        this.ensureAnims();
        this.animX2.setAnim(this.xPos);
        this.animX2.setTo(this.xPos);
        this.animY2.setAnim(this.yPos);
        this.animY2.setTo(this.yPos);
    }

    public void setSnapLineX(float f) {
        this.snapLineX = f;
    }

    public void setSnapLineY(float f) {
        this.snapLineY = f;
    }

    public void setDirectDrag(boolean direct) {
        this.directDrag = direct;
    }

    public boolean isDirectDrag() {
        return this.directDrag;
    }

    public void setForceDirectDrag(boolean force) {
        this.forceDirectDrag = force;
        if (force) {
            this.directDrag = true;
        }
    }

    public boolean isForceDirectDrag() {
        return this.forceDirectDrag;
    }

    private float computeRenderY() {
        this.ensureAnims();
        if (this.forceDirectDrag || this.directDrag || this.dragging) {
            if (this.animY != null) {
                this.animY.setTo(this.yPos);
            }
            return this.yPos;
        }
        float f = this.animY.getAnim();
        return f;
    }

    public float getSnapLineY() {
        return this.snapLineY;
    }

    private float computeRenderX() {
        this.ensureAnims();
        if (this.forceDirectDrag || this.directDrag || this.dragging) {
            if (this.animX != null) {
                this.animX.setTo(this.xPos);
            }
            return this.xPos;
        }
        float f = this.animX.getAnim();
        return f;
    }

    public static float pulsePhase() {
        return (float)(System.currentTimeMillis() % 500L) / 500.0f;
    }

    public void updateTilt(float f, boolean bl) {
        long l = System.nanoTime();
        if (!bl) {
            this.tiltAngle = 0.0f;
            this.tiltVelocity = 0.0f;
            this.filteredMouseVelocity = 0.0f;
            this.lastTiltMouseX = f;
            this.lastTiltUpdateNs = l;
            return;
        }
        if (this.lastTiltUpdateNs == 0L) {
            this.lastTiltMouseX = f;
            this.lastTiltUpdateNs = l;
            return;
        }
        float f2 = Math.clamp((float)(l - this.lastTiltUpdateNs) / 1.0E9f, 0.001f, 0.05f);
        float f3 = this.dragging ? (f - this.lastTiltMouseX) / f2 : 0.0f;
        this.lastTiltMouseX = f;
        this.lastTiltUpdateNs = l;
        float f4 = 1.0f - (float)Math.exp(-14.0f * f2);
        this.filteredMouseVelocity += (f3 - this.filteredMouseVelocity) * f4;
        float f5 = this.dragging ? Math.clamp(this.filteredMouseVelocity * 0.012f, -6.5f, 6.5f) : 0.0f;
        float f6 = (f5 - this.tiltAngle) * 105.0f - this.tiltVelocity * 16.0f;
        this.tiltVelocity += f6 * f2;
        this.tiltAngle = Math.clamp(this.tiltAngle + this.tiltVelocity * f2, -7.0f, 7.0f);
        if (!this.dragging && Math.abs(this.tiltAngle) < 0.01f && Math.abs(this.tiltVelocity) < 0.05f) {
            this.tiltAngle = 0.0f;
            this.tiltVelocity = 0.0f;
            this.filteredMouseVelocity = 0.0f;
        }
    }

    public void setTargetX(float f) {
        this.xPos = f;
        this.desiredX = f;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public float getTargetY() {
        return this.yPos;
    }

    void endRender() {
        this.renderPositionLocked = false;
    }

    public boolean tryGrab(float f, float f2, float f3, float f4) {
        if (!this.isHovered(f, f2, f3, f4)) {
            return false;
        }
        this.cancelled = false;
        this.dragging = true;
        this.preDragX = this.xPos;
        this.preDragY = this.yPos;
        this.startX = f - this.xPos;
        this.startY = f2 - this.yPos;
        this.lastTiltMouseX = f;
        this.lastTiltUpdateNs = System.nanoTime();
        this.filteredMouseVelocity = 0.0f;
        return true;
    }

    public float getRenderX() {
        if (this.renderPositionLocked) {
            return this.lockedRenderX;
        }
        return this.computeRenderX();
    }

    public float getRenderY() {
        if (this.renderPositionLocked) {
            return this.lockedRenderY;
        }
        return this.computeRenderY();
    }

    public float getTargetX() {
        return this.xPos;
    }

    public void setTargetY(float f) {
        this.yPos = f;
        this.desiredY = f;
    }

    public boolean isHovered(float f, float f2, float f3, float f4) {
        return f > this.xPos && f < this.xPos + f3 && f2 > this.yPos && f2 < this.yPos + f4;
    }

    public void adjustX(float f) {
        this.xPos += f;
        this.desiredX += f;
    }

    public void adjustY(float f) {
        this.yPos += f;
        this.desiredY += f;
    }

    DragLerpAnim getAnimY() {
        return this.animY;
    }

    DragLerpAnim getAnimX() {
        return this.animX;
    }

    DragLerpAnim getAnimX2() {
        return this.animX2;
    }

    DragLerpAnim getAnimY2() {
        return this.animY2;
    }

    public boolean wasCancelled() {
        return this.cancelled;
    }

    public void tick(float f, float f2, float f3, float f4, boolean bl) {
        this.directDrag = true;
        this.dragAnimPc.setToAsBoolean(this.dragging);
        this.dragAnimPc.getAnim();
        this.dragAnimPc.setSpeed(0.04f);
        this.ensureAnims();
        this.snapLineX = Float.NaN;
        this.snapLineY = Float.NaN;
        if (this.dragging) {
            this.xPos = f - this.startX;
            this.yPos = f2 - this.startY;
            DragOverlayRenderer.clampToScreen((DragController)(Object)this, (float)f3, (float)f4);
            DragOverlayRenderer.applyGridSnap((DragController)(Object)this, (float)f3, (float)f4);
            this.desiredX = this.xPos;
            this.desiredY = this.yPos;
            this.animX2.setTo(this.xPos);
            this.animY2.setTo(this.yPos);
            if (bl || this.forceDirectDrag) {
                this.animX.setTo(this.xPos);
                this.animY.setTo(this.yPos);
            }
        } else {
            this.animX.setTo(this.xPos);
            this.animY.setTo(this.yPos);
        }
    }
}

