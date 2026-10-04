package rtx.nv.api.events.impl.render;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import rtx.nv.api.events.Event;

public final class WorldRenderEvent
extends Event {
    private final MatrixStack stack;
    private final float partialTicks;
    private final Camera camera;
    private final Matrix4f positionMatrix;
    private final Matrix4f projectionMatrix;

    public WorldRenderEvent(MatrixStack matrixStack, float f, Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2) {
        this.stack = matrixStack;
        this.partialTicks = f;
        this.camera = camera;
        this.positionMatrix = matrix4f;
        this.projectionMatrix = matrix4f2;
    }

    public MatrixStack getStack() {
        return this.stack;
    }

    public Camera getCamera() {
        return this.camera;
    }

    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }

    public Matrix4f getPositionMatrix() {
        return this.positionMatrix;
    }
}

