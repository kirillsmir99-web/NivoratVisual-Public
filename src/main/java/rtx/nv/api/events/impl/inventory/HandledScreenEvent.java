package rtx.nv.api.events.impl.inventory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;
import rtx.nv.api.events.CancellableEvent;

public final class HandledScreenEvent extends CancellableEvent {
    private final Slot slotHover;
    private final DrawContext drawContext;
    private final int x;
    private final int y;

    public HandledScreenEvent(Slot slotHover) {
        this.slotHover = slotHover;
        this.drawContext = null;
        this.x = 0;
        this.y = 0;
    }

    public HandledScreenEvent(DrawContext drawContext, int x, int y, Slot slotHover) {
        this.drawContext = drawContext;
        this.x = x;
        this.y = y;
        this.slotHover = slotHover;
    }

    public Slot getSlotHover() {
        return this.slotHover;
    }

    public DrawContext getDrawContext() {
        return this.drawContext;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }
}
