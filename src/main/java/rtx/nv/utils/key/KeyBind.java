package rtx.nv.utils.key;
import org.lwjgl.glfw.GLFW;
import rtx.nv.utils.key.InputType;
import rtx.nv.utils.key.KeyHelper;

public final class KeyBind {
    public static final int SCROLL_UP = 1000;
    public static final int SCROLL_DOWN = 1001;
    public static final int MIDDLE_MOUSE = 1002;
    public static final KeyBind NONE = new KeyBind(-1);
    private final int code;

    public KeyBind(int n) {
        this.code = n;
    }

    public InputType getType() {
        if (!this.isBound()) {
            return InputType.NONE;
        }
        if (this.code == 1000 || this.code == 1001) {
            return InputType.SCROLL;
        }
        if (this.code == 1002 || KeyBind.isRawMouseButton(this.code)) {
            return InputType.MOUSE;
        }
        return InputType.KEYBOARD;
    }

    public boolean isBound() {
        return this.code != -1;
    }

    public String getDisplayName() {
        return KeyHelper.getShortName(this.code);
    }

    public String getName() {
        return getDisplayName();
    }

    public static KeyBind scrollDown() {
        return new KeyBind(1001);
    }

    public boolean isDown(long l) {
        if (!this.isBound()) {
            return false;
        }
        if (this.getType() == InputType.MOUSE) {
            return GLFW.glfwGetMouseButton((long)l, (int)this.toMouseButton()) == 1;
        }
        if (this.getType() == InputType.SCROLL) {
            return false;
        }
        return GLFW.glfwGetKey((long)l, (int)this.code) == 1;
    }

    public static KeyBind scrollUp() {
        return new KeyBind(1000);
    }

    public boolean matchesMouseButton(int n) {
        return this.getType() == InputType.MOUSE && this.toMouseButton() == n;
    }

    public boolean matchesScroll(double d) {
        return this.code == 1000 && d > 0.0 || this.code == 1001 && d < 0.0;
    }

    private static int normalizeMouseCode(int n) {
        return n == 2 ? 1002 : n;
    }

    private int toMouseButton() {
        return this.code == 1002 ? 2 : this.code;
    }

    private static boolean isRawMouseButton(int n) {
        return n >= 0 && n <= 7;
    }

    public static KeyBind keyboard(int n) {
        return new KeyBind(n);
    }

    public static KeyBind mouse(int n) {
        return new KeyBind(KeyBind.normalizeMouseCode(n));
    }

    public int getCode() {
        return this.code;
    }
}

