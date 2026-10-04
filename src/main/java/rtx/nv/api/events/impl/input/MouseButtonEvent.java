package rtx.nv.api.events.impl.input;
import rtx.nv.api.events.CancellableEvent;

public final class MouseButtonEvent
extends CancellableEvent {
    public final int button;
    public final int modifiers;
    public final MouseButtonEvent.Action action;

    public MouseButtonEvent(int n, int n2, MouseButtonEvent.Action action) {
        this.button = n;
        this.modifiers = n2;
        this.action = action;
    }


    public static enum Action {
        PRESS,
        RELEASE;
    
    
        public static Action of(int n) {
            return n == 1 ? PRESS : RELEASE;
        }
    }
}

