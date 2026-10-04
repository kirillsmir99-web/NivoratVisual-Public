package rtx.nv.api.events.impl.game;
import rtx.nv.api.events.Event;

public final class TickEvent
extends Event {
    private final TickEvent.Phase phase;

    public TickEvent(TickEvent.Phase phase) {
        this.phase = phase;
    }

    public boolean isPost() {
        return this.phase == TickEvent.Phase.POST;
    }

    public boolean isPre() {
        return this.phase == TickEvent.Phase.PRE;
    }

    public TickEvent.Phase phase() {
        return this.phase;
    }


    public static enum Phase {
        PRE,
        POST;
    
    }
}

