package rtx.nv.utils.render.post.glowesp;

import java.util.List;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.world.WorldShapeRenderer;

public final class GlowEspRenderer {
    public static final String SHAPE_OUTLINE = "Контур";
    public static final String SHAPE_SILHOUETTE = "Силуэт";
    public static final String SHAPE_CORNERS = "Углы";

    private GlowEspRenderer() {}

    public static void clear() {
    }

    public static void render(
        VertexConsumerProvider.Immediate consumers,
        MatrixStack matrixStack,
        Vec3d cameraPos,
        List<LivingEntity> entities,
        float tickDelta,
        String shape,
        float thickness,
        float opacity,
        boolean throughWalls,
        int primaryColor,
        int secondaryColor,
        boolean useSecondColor
    ) {
        if (consumers == null || matrixStack == null || cameraPos == null || entities == null || entities.isEmpty()) {
            return;
        }

        MatrixStack.Entry entry = matrixStack.peek();
        VertexConsumer lineConsumer = consumers.getBuffer(RenderLayers.lines());
        VertexConsumer fillConsumer = null;

        boolean isSilhouette = SHAPE_SILHOUETTE.equals(shape);
        boolean isCorners = SHAPE_CORNERS.equals(shape);

        if (isSilhouette) {
            fillConsumer = consumers.getBuffer(throughWalls ? RenderLayers.lightning() : RenderLayers.debugQuads());
        }

        int entityCount = entities.size();
        for (int idx = 0; idx < entityCount; idx++) {
            LivingEntity entity = entities.get(idx);
            if (entity == null) continue;

            Vec3d offset = entity.getLerpedPos(tickDelta).subtract(entity.getEntityPos());
            Box box = entity.getBoundingBox().offset(offset);

            float factor = entityCount > 1 && useSecondColor ? (float) idx / (float) (entityCount - 1) : 0.0f;
            int baseColor = useSecondColor ? ColorUtil.lerpColor(primaryColor, secondaryColor, factor) : primaryColor;
            int strokeColor = ColorUtil.multAlpha(baseColor, opacity);

            float x1 = (float) (box.minX - cameraPos.x);
            float y1 = (float) (box.minY - cameraPos.y);
            float z1 = (float) (box.minZ - cameraPos.z);
            float x2 = (float) (box.maxX - cameraPos.x);
            float y2 = (float) (box.maxY - cameraPos.y);
            float z2 = (float) (box.maxZ - cameraPos.z);

            if (isSilhouette && fillConsumer != null) {
                int fillColor = ColorUtil.multAlpha(strokeColor, 0.32f);
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

            if (isCorners) {
                renderCorners(lineConsumer, entry, x1, y1, z1, x2, y2, z2, strokeColor, thickness);
            } else {
                renderOutlineBox(lineConsumer, entry, x1, y1, z1, x2, y2, z2, strokeColor, thickness);
            }
        }

        if (fillConsumer != null) {
            consumers.draw(throughWalls ? RenderLayers.lightning() : RenderLayers.debugQuads());
        }
        consumers.draw(RenderLayers.lines());
    }

    private static void renderOutlineBox(VertexConsumer consumer, MatrixStack.Entry entry, float x1, float y1, float z1, float x2, float y2, float z2, int color, float width) {
        WorldShapeRenderer.drawEdge(consumer, entry, x1, y1, z1, x2, y1, z1, color, width);
        WorldShapeRenderer.drawEdge(consumer, entry, x2, y1, z1, x2, y1, z2, color, width);
        WorldShapeRenderer.drawEdge(consumer, entry, x2, y1, z2, x1, y1, z2, color, width);
        WorldShapeRenderer.drawEdge(consumer, entry, x1, y1, z2, x1, y1, z1, color, width);

        WorldShapeRenderer.drawEdge(consumer, entry, x1, y2, z1, x2, y2, z1, color, width);
        WorldShapeRenderer.drawEdge(consumer, entry, x2, y2, z1, x2, y2, z2, color, width);
        WorldShapeRenderer.drawEdge(consumer, entry, x2, y2, z2, x1, y2, z2, color, width);
        WorldShapeRenderer.drawEdge(consumer, entry, x1, y2, z2, x1, y2, z1, color, width);

        WorldShapeRenderer.drawEdge(consumer, entry, x1, y1, z1, x1, y2, z1, color, width);
        WorldShapeRenderer.drawEdge(consumer, entry, x2, y1, z1, x2, y2, z1, color, width);
        WorldShapeRenderer.drawEdge(consumer, entry, x2, y1, z2, x2, y2, z2, color, width);
        WorldShapeRenderer.drawEdge(consumer, entry, x1, y1, z2, x1, y2, z2, color, width);
    }

    private static void renderCorners(VertexConsumer consumer, MatrixStack.Entry entry, float x1, float y1, float z1, float x2, float y2, float z2, int color, float width) {
        float cx = (x2 - x1) * 0.28f;
        float cy = (y2 - y1) * 0.22f;
        float cz = (z2 - z1) * 0.28f;

        float[] xs = {x1, x2};
        float[] ys = {y1, y2};
        float[] zs = {z1, z2};

        for (float x : xs) {
            float dx = (x == x1) ? cx : -cx;
            for (float y : ys) {
                float dy = (y == y1) ? cy : -cy;
                for (float z : zs) {
                    float dz = (z == z1) ? cz : -cz;
                    WorldShapeRenderer.drawEdge(consumer, entry, x, y, z, x + dx, y, z, color, width);
                    WorldShapeRenderer.drawEdge(consumer, entry, x, y, z, x, y + dy, z, color, width);
                    WorldShapeRenderer.drawEdge(consumer, entry, x, y, z, x, y, z + dz, color, width);
                }
            }
        }
    }
}
