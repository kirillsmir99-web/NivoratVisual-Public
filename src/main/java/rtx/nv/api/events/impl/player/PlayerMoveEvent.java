package rtx.nv.api.events.impl.player;
import rtx.nv.api.events.CancellableEvent;

public final class PlayerMoveEvent
extends CancellableEvent {
    private boolean forward;
    private boolean backward;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean sprint;
    private boolean shift;

    public PlayerMoveEvent(boolean bl, boolean bl2, boolean bl3, boolean bl4, boolean bl5, boolean bl6, boolean bl7) {
        this.forward = bl;
        this.backward = bl2;
        this.left = bl3;
        this.right = bl4;
        this.jump = bl5;
        this.sprint = bl6;
        this.shift = bl7;
    }

    public boolean isForward() {
        return this.forward;
    }

    public boolean isBackward() {
        return this.backward;
    }

    public boolean isLeft() {
        return this.left;
    }

    public boolean isRight() {
        return this.right;
    }

    public boolean isJump() {
        return this.jump;
    }

    public boolean isShift() {
        return this.shift;
    }

    public boolean isSprint() {
        return this.sprint;
    }

    public void setBackward(boolean backward) {
        this.backward = backward;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public void setSprint(boolean sprint) {
        this.sprint = sprint;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public void setRight(boolean right) {
        this.right = right;
    }

    public void setShift(boolean shift) {
        this.shift = shift;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }
}
