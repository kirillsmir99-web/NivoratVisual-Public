package rtx.nv.api.events.impl.player;
import rtx.nv.api.events.CancellableEvent;

public final class UsingItemEvent
extends CancellableEvent {
    public static final int PRE = 0;
    public static final int POST = 1;
    private final int type;

    public UsingItemEvent(int n) {
        this.type = n;
    }
}

