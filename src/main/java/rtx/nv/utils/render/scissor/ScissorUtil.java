package rtx.nv.utils.render.scissor;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.gui.ScreenRect;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;

public final class ScissorUtil {
    private static final ScreenRect EMPTY = new ScreenRect(0, 0, 0, 0);
    private static final Deque<ScreenRect> STACK = new ArrayDeque<ScreenRect>();

    private ScissorUtil() {
    }

    public static ScreenRect current() {
        return STACK.peek();
    }

    public static void push(Matrix3x2f matrix3x2f, float f, float f2, float f3, float f4) {
        if (matrix3x2f == null) {
            ScissorUtil.push(f, f2, f3, f4);
            return;
        }
        ScreenRect screenRect = new ScreenRect(Math.round(f), Math.round(f2), Math.max(0, Math.round(f3)), Math.max(0, Math.round(f4))).transformEachVertex((Matrix3x2fc)matrix3x2f);
        ScissorUtil.push(screenRect);
    }

    private static void push(ScreenRect screenRect) {
        ScreenRect screenRect2 = STACK.peek();
        ScreenRect screenRect3 = screenRect2 == null ? screenRect : screenRect2.intersection(screenRect);
        STACK.push(screenRect3 == null ? EMPTY : screenRect3);
    }

    public static void push(float f, float f2, float f3, float f4) {
        int n = Render2DCoordinateSpace.toGuiInt(f);
        int n2 = Render2DCoordinateSpace.toGuiInt(f2);
        int n3 = Render2DCoordinateSpace.toGuiInt(f + f3);
        int n4 = Render2DCoordinateSpace.toGuiInt(f2 + f4);
        ScissorUtil.push(new ScreenRect(n, n2, Math.max(0, n3 - n), Math.max(0, n4 - n2)));
    }

    public static void pop() {
        if (!STACK.isEmpty()) {
            STACK.pop();
        }
    }
}

