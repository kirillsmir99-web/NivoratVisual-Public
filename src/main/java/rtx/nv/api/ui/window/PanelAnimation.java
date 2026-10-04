package rtx.nv.api.ui.window;

public class PanelAnimation {
    private boolean open;
    private float factor;

    public PanelAnimation(boolean open) {
        this.open = open;
        this.factor = open ? 1.0f : 0.0f;
    }

    public void finish() {
        this.factor = this.open ? 1.0f : 0.0f;
    }

    public void update() {
    }

    public float getFactor() {
        return this.factor;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
