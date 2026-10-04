package rtx.nv.api.drags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.drags.DragController;
import rtx.nv.api.drags.Position;
import rtx.nv.api.ui.settings.Setting;

public abstract class Draggable {
    private final String id;
    private final DragController drag;
    private final float defaultX;
    private final float defaultY;
    private boolean visible = true;
    private List<Setting> cachedHudSettings;

    protected Draggable(String string, float f, float f2) {
        this.id = string;
        this.defaultX = f;
        this.defaultY = f2;
        this.drag = new DragController(f, f2);
    }

    public String getId() {
        return this.id;
    }

    public String displayName() {
        return this.id;
    }

    public abstract float width();

    public Position getPosition() {
        return new Position(this.drag.getTargetX(), this.drag.getTargetY());
    }

    public abstract float height();

    public final List<Setting> hudSettings() {
        if (this.cachedHudSettings == null) {
            this.cachedHudSettings = this.buildHudSettings();
        }
        return this.cachedHudSettings;
    }

    public float overlayHeight() {
        return this.height();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    final void renderNormal(DrawContext drawContext) {
        if (!this.visible) {
            return;
        }
        this.drag.beginRender();
        HudLayoutCoordinator.place(this);
        float f = this.drag.getTiltAngle();
        if (Math.abs(f) < 0.01f) {
            try {
                this.render(drawContext);
            }
            finally {
                this.drag.endRender();
            }
            return;
        }
        float f2 = this.getX() + this.width() * 0.5f;
        float f3 = this.getY() + this.height() * 0.5f;
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(f2, f3);
        drawContext.getMatrices().rotate((float)Math.toRadians(f));
        drawContext.getMatrices().translate(-f2, -f3);
        try {
            this.render(drawContext);
        }
        finally {
            drawContext.getMatrices().popMatrix();
            this.drag.endRender();
        }
    }

    public boolean isInteractive() {
        return this.visible;
    }

    public float overlayWidth() {
        return this.width();
    }

    protected List<Setting> buildHudSettings() {
        return new ArrayList<Setting>();
    }

    protected abstract void render(DrawContext var1);

    public boolean isVisible() {
        return this.visible;
    }

    public void resetToDefault() {
        this.drag.setTargetX(this.defaultX);
        this.drag.setTargetY(this.defaultY);
        this.drag.syncToTarget();
    }

    final boolean hitTest(float f, float f2) {
        return this.drag.isHovered(f, f2, this.width(), this.height());
    }

    public DragController getDrag() {
        return this.drag;
    }

    protected float getX() {
        return this.drag.getRenderX() + HudLayoutCoordinator.x(this);
    }

    protected float getY() {
        return this.drag.getRenderY() + HudLayoutCoordinator.y(this);
    }

    public void setVisible(boolean bl) {
        this.visible = bl;
    }
}

