package rtx.nv.api.drags;
import net.minecraft.client.MinecraftClient;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;

public final class Position {
    public static final float SCREEN_MARGIN = 5.0f;
    private float x;
    private float y;

    public Position(float f, float f2) {
        this.x = f;
        this.y = f2;
    }

    public float x() {
        return this.x;
    }

    public void set(float f, float f2, float f3, float f4) {
        this.x = Position.clampX(f, f3);
        this.y = Position.clampY(f2, f4);
    }

    public float y() {
        return this.y;
    }

    public static float clampX(float f, float f2) {
        float f3 = Math.max(0.0f, Position.screenWidth() - f2);
        return Math.max(0.0f, Math.min(f3, f));
    }

    public static float clampY(float f, float f2) {
        float f3 = Math.max(0.0f, Position.screenHeight() - f2);
        return Math.max(0.0f, Math.min(f3, f));
    }

    public void setRaw(float f, float f2) {
        this.x = f;
        this.y = f2;
    }

    public static float mouseX() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null) {
            return 0.0f;
        }
        double d = minecraftClient.mouse.getScaledX(minecraftClient.getWindow());
        return (float)(d / (double)Render2DCoordinateSpace.guiIndependentScale());
    }

    public static float mouseY() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null) {
            return 0.0f;
        }
        double d = minecraftClient.mouse.getScaledY(minecraftClient.getWindow());
        return (float)(d / (double)Render2DCoordinateSpace.guiIndependentScale());
    }

    public static float screenWidth() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.getWindow() == null) {
            return 960.0f;
        }
        return (float)minecraftClient.getWindow().getFramebufferWidth() / Render2DCoordinateSpace.designGuiScale();
    }

    public static float screenHeight() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.getWindow() == null) {
            return 540.0f;
        }
        return (float)minecraftClient.getWindow().getFramebufferHeight() / Render2DCoordinateSpace.designGuiScale();
    }
}

