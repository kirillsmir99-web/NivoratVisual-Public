package rtx.nv.utils.render.targetesp;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionfc;

public class TargetCircleRenderer {
    private static final int SEGMENTS = 64;

    public static void render(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrixStack,
        float radius,
        float height,
        float tilt,
        float angle,
        float thickness,
        float alpha,
        boolean throughWalls,
        TargetEspColorProvider colorProvider
    ) {
        if (immediate == null || matrixStack == null || alpha <= 0.001f) {
            return;
        }
        matrixStack.push();
        matrixStack.translate(0.0f, height, 0.0f);
        matrixStack.multiply((Quaternionfc) RotationAxis.POSITIVE_X.rotation(tilt));
        matrixStack.multiply((Quaternionfc) RotationAxis.POSITIVE_Y.rotation(angle));

        MatrixStack.Entry entry = matrixStack.peek();
        VertexConsumer consumer = immediate.getBuffer(throughWalls ? RenderLayers.lightning() : RenderLayers.debugQuads());

        float innerR = Math.max(0.1f, radius - thickness * 0.5f);
        float outerR = radius + thickness * 0.5f;

        for (int arc = 0; arc < 2; arc++) {
            float startAngle = arc * (float) Math.PI;
            float arcLength = (float) Math.PI * 0.75f;

            for (int i = 0; i < SEGMENTS; i++) {
                float f1 = (float) i / SEGMENTS;
                float f2 = (float) (i + 1) / SEGMENTS;
                float a1 = startAngle + f1 * arcLength;
                float a2 = startAngle + f2 * arcLength;

                float sin1 = MathHelper.sin(a1);
                float cos1 = MathHelper.cos(a1);
                float sin2 = MathHelper.sin(a2);
                float cos2 = MathHelper.cos(a2);

                float fade1 = (float) Math.sin(f1 * Math.PI) * alpha;
                float fade2 = (float) Math.sin(f2 * Math.PI) * alpha;

                int c1 = colorProvider != null ? colorProvider.getColor((int)(f1 * 360), fade1) : 0xFFFFFFFF;
                int c2 = colorProvider != null ? colorProvider.getColor((int)(f2 * 360), fade2) : 0xFFFFFFFF;

                consumer.vertex(entry, cos1 * innerR, 0.0f, sin1 * innerR).color(c1);
                consumer.vertex(entry, cos1 * outerR, 0.0f, sin1 * outerR).color(c1);
                consumer.vertex(entry, cos2 * outerR, 0.0f, sin2 * outerR).color(c2);
                consumer.vertex(entry, cos2 * innerR, 0.0f, sin2 * innerR).color(c2);
            }
        }

        matrixStack.pop();
    }
}
