package rtx.nv.utils.render.world;

import java.util.List;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class WorldShapeRenderer {
    private WorldShapeRenderer() {}

    public static void line(VertexConsumerProvider.Immediate consumers, MatrixStack matrices, Vec3d camera, Vec3d from, Vec3d to, int color, float width) {
        drawEdge(consumers.getBuffer(RenderLayers.lines()), matrices.peek(),
            (float)(from.x-camera.x), (float)(from.y-camera.y), (float)(from.z-camera.z),
            (float)(to.x-camera.x), (float)(to.y-camera.y), (float)(to.z-camera.z), color, width);
    }

    public static void boxes(VertexConsumerProvider.Immediate consumers, MatrixStack matrixStack, Vec3d cameraPos, List<Box> boxes, int fillColor, int outlineColor, float lineWidth) {
        if (boxes == null || boxes.isEmpty() || consumers == null || matrixStack == null) {
            return;
        }
        MatrixStack.Entry entry = matrixStack.peek();
        VertexConsumer fillConsumer = consumers.getBuffer(RenderLayers.debugQuads());

        VertexConsumer lineConsumer = consumers.getBuffer(RenderLayers.lines());

        for (Box box : boxes) {
            float x1 = (float) (box.minX - cameraPos.x);
            float y1 = (float) (box.minY - cameraPos.y);
            float z1 = (float) (box.minZ - cameraPos.z);
            float x2 = (float) (box.maxX - cameraPos.x);
            float y2 = (float) (box.maxY - cameraPos.y);
            float z2 = (float) (box.maxZ - cameraPos.z);

            if ((fillColor >>> 24) > 0) {
                // Bottom
                fillConsumer.vertex(entry, x1, y1, z1).color(fillColor);
                fillConsumer.vertex(entry, x2, y1, z1).color(fillColor);
                fillConsumer.vertex(entry, x2, y1, z2).color(fillColor);
                fillConsumer.vertex(entry, x1, y1, z2).color(fillColor);

                // Top
                fillConsumer.vertex(entry, x1, y2, z1).color(fillColor);
                fillConsumer.vertex(entry, x1, y2, z2).color(fillColor);
                fillConsumer.vertex(entry, x2, y2, z2).color(fillColor);
                fillConsumer.vertex(entry, x2, y2, z1).color(fillColor);

                // North
                fillConsumer.vertex(entry, x1, y1, z1).color(fillColor);
                fillConsumer.vertex(entry, x1, y2, z1).color(fillColor);
                fillConsumer.vertex(entry, x2, y2, z1).color(fillColor);
                fillConsumer.vertex(entry, x2, y1, z1).color(fillColor);

                // South
                fillConsumer.vertex(entry, x1, y1, z2).color(fillColor);
                fillConsumer.vertex(entry, x2, y1, z2).color(fillColor);
                fillConsumer.vertex(entry, x2, y2, z2).color(fillColor);
                fillConsumer.vertex(entry, x1, y2, z2).color(fillColor);

                // West
                fillConsumer.vertex(entry, x1, y1, z1).color(fillColor);
                fillConsumer.vertex(entry, x1, y1, z2).color(fillColor);
                fillConsumer.vertex(entry, x1, y2, z2).color(fillColor);
                fillConsumer.vertex(entry, x1, y2, z1).color(fillColor);

                // East
                fillConsumer.vertex(entry, x2, y1, z1).color(fillColor);
                fillConsumer.vertex(entry, x2, y2, z1).color(fillColor);
                fillConsumer.vertex(entry, x2, y2, z2).color(fillColor);
                fillConsumer.vertex(entry, x2, y1, z2).color(fillColor);
            }

            if ((outlineColor >>> 24) > 0) {
                drawEdge(lineConsumer, entry, x1, y1, z1, x2, y1, z1, outlineColor, lineWidth);
                drawEdge(lineConsumer, entry, x2, y1, z1, x2, y1, z2, outlineColor, lineWidth);
                drawEdge(lineConsumer, entry, x2, y1, z2, x1, y1, z2, outlineColor, lineWidth);
                drawEdge(lineConsumer, entry, x1, y1, z2, x1, y1, z1, outlineColor, lineWidth);

                drawEdge(lineConsumer, entry, x1, y2, z1, x2, y2, z1, outlineColor, lineWidth);
                drawEdge(lineConsumer, entry, x2, y2, z1, x2, y2, z2, outlineColor, lineWidth);
                drawEdge(lineConsumer, entry, x2, y2, z2, x1, y2, z2, outlineColor, lineWidth);
                drawEdge(lineConsumer, entry, x1, y2, z2, x1, y2, z1, outlineColor, lineWidth);

                drawEdge(lineConsumer, entry, x1, y1, z1, x1, y2, z1, outlineColor, lineWidth);
                drawEdge(lineConsumer, entry, x2, y1, z1, x2, y2, z1, outlineColor, lineWidth);
                drawEdge(lineConsumer, entry, x2, y1, z2, x2, y2, z2, outlineColor, lineWidth);
                drawEdge(lineConsumer, entry, x1, y1, z2, x1, y2, z2, outlineColor, lineWidth);
            }
        }
    }

    public static void drawEdge(VertexConsumer consumer, MatrixStack.Entry entry, float x1, float y1, float z1, float x2, float y2, float z2, int color, float lineWidth) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float length = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-6f) return;
        dx /= length; dy /= length; dz /= length;
        consumer.vertex(entry, x1, y1, z1).color(color).normal(entry, dx, dy, dz).lineWidth(lineWidth);
        consumer.vertex(entry, x2, y2, z2).color(color).normal(entry, dx, dy, dz).lineWidth(lineWidth);
    }
}
