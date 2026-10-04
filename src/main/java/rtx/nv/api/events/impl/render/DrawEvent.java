package rtx.nv.api.events.impl.render;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import rtx.nv.api.events.Event;

public final class DrawEvent
extends Event {
    private final DrawContext graphics;
    private final RenderTickCounter delta;

    public DrawEvent(DrawContext drawContext, RenderTickCounter renderTickCounter) {
        this.graphics = drawContext;
        this.delta = renderTickCounter;
    }

    public RenderTickCounter getDelta() {
        return this.delta;
    }

    public DrawContext getGraphics() {
        return this.graphics;
    }

    public float getPartialTicks() {
        return this.delta.getTickProgress(true);
    }
}

