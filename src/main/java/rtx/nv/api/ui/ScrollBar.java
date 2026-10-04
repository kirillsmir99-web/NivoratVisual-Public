package rtx.nv.api.ui;
import java.awt.Color;
import rtx.nv.api.drags.Position;
import rtx.nv.api.ui.theme.AccentGradient;
import rtx.nv.utils.render.render2d.Render2D;

public final class ScrollBar {
    private static final float BAR_W = 1.5f;
    private static final float GRAB_PAD_X = 4.0f;
    private static final float GRAB_PAD_Y = 2.0f;
    private static final float ANIM_RATE = 16.0f;
    private static final float VERTICAL_SHRINK = 1.0f;
    private boolean dragging;
    private float grabOffset;
    private float hoverT;
    private float dragT;
    private long lastNs = System.nanoTime();
    private boolean visible;
    private float gTrackX;
    private float gTrackY;
    private float gTrackH;
    private float gThumbY;
    private float gThumbH;
    private float gHitW;

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    public void release() {
        this.dragging = false;
    }

    public float render(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        float f8;
        long l = System.nanoTime();
        float f9 = Math.min(0.1f, (float)(l - this.lastNs) / 1.0E9f);
        this.lastNs = l;
        float f10 = Math.max(0.0f, f5 - f4);
        boolean bl = this.visible = f10 > 0.5f;
        if (!this.visible) {
            this.dragging = false;
            this.hoverT += (0.0f - this.hoverT) * this.follow(f9);
            this.dragT += (0.0f - this.dragT) * this.follow(f9);
            return f6;
        }
        float f11 = f3 / 1.0f;
        float f12 = f2 + (f3 - f11) * 0.5f;
        float f13 = Math.max(12.0f, f11 * (f4 / f5));
        float f14 = Math.max(1.0f, f11 - f13);
        if (this.dragging) {
            f8 = ScrollBar.clamp(Position.mouseY() - this.grabOffset, f12, f12 + f14);
            f6 = (f8 - f12) / f14 * f10;
        }
        f6 = ScrollBar.clamp(f6, 0.0f, f10);
        f8 = f12 + f14 * (f6 / f10);
        float f15 = Position.mouseX();
        float f16 = Position.mouseY();
        boolean bl2 = f15 >= f - 4.0f && f15 <= f + 1.5f + 4.0f && f16 >= f8 - 2.0f && f16 <= f8 + f13 + 2.0f;
        this.hoverT += ((bl2 ? 1.0f : 0.0f) - this.hoverT) * this.follow(f9);
        this.dragT += ((this.dragging ? 1.0f : 0.0f) - this.dragT) * this.follow(f9);
        float f17 = Math.max(this.hoverT, this.dragT);
        int n = ScrollBar.clamp255((20.0f + 18.0f * f17) * f7);
        Render2D.rect(f, f12, 1.5f, f11, 0.75f, new Color(255, 255, 255, n).getRGB());
        float f18 = (160.0f + 95.0f * f17) * f7;
        AccentGradient.fillVertical(f, f8, 1.5f, f13, 0.75f, f18);
        this.gTrackX = f;
        this.gTrackY = f12;
        this.gTrackH = f11;
        this.gThumbY = f8;
        this.gThumbH = f13;
        this.gHitW = 1.5f;
        return f6;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public boolean tryGrab(float f, float f2) {
        if (!this.visible) {
            return false;
        }
        if (f < this.gTrackX - 4.0f || f > this.gTrackX + this.gHitW + 4.0f) {
            return false;
        }
        if (f2 >= this.gThumbY - 2.0f && f2 <= this.gThumbY + this.gThumbH + 2.0f) {
            this.dragging = true;
            this.grabOffset = f2 - this.gThumbY;
            return true;
        }
        if (f2 >= this.gTrackY && f2 <= this.gTrackY + this.gTrackH) {
            this.dragging = true;
            this.grabOffset = this.gThumbH * 0.5f;
            return true;
        }
        return false;
    }

    private static int clamp255(float f) {
        return Math.max(0, Math.min(255, Math.round(f)));
    }

    private float follow(float f) {
        return 1.0f - (float)Math.exp(-f * 16.0f);
    }
}

