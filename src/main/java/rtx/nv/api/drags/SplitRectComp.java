package rtx.nv.api.drags;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.utils.animations.Easing;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;

public final class SplitRectComp extends Draggable {
    private static final Easing OUT_QUART = Easings.QUART_OUT;
    private final float w;
    private final float h;
    private final float radius;
    private final Draggable origin;
    private final SmoothAnimation reveal = new SmoothAnimation();
    private boolean revealStarted;

    public SplitRectComp(String string, float f, float f2, float f3, float f4, float f5, Draggable draggable) {
        super(string, f, f2);
        this.w = f3;
        this.h = f4;
        this.radius = f5;
        this.origin = draggable;
    }

    public Draggable origin() {
        return this.origin;
    }

    @Override
    public float width() {
        return this.w;
    }

    @Override
    public float height() {
        return this.h;
    }

    @Override
    protected void render(DrawContext drawContext) {
    }

    @Override
    protected List<Setting> buildHudSettings() {
        return Collections.emptyList();
    }

    public float radius() {
        return this.radius;
    }
}
