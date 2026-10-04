package rtx.nv.api.events.impl.input;
import rtx.nv.api.events.CancellableEvent;

public final class KeyPressEvent
extends CancellableEvent {
    public final int keyCode;
    public final int scanCode;
    public final int modifiers;
    public final KeyPressEvent.Action action;

    public KeyPressEvent(int n, int n2, int n3, KeyPressEvent.Action action) {
        this.keyCode = n;
        this.scanCode = n2;
        this.modifiers = n3;
        this.action = action;
    }


    public static enum Action {
        PRESS,
        RELEASE,
        REPEAT;
    
    
        public static Action of(int n) {
            return switch (n) {
                case 1 -> PRESS;
                case 0 -> RELEASE;
                default -> REPEAT;
            };
        }
    }
}

