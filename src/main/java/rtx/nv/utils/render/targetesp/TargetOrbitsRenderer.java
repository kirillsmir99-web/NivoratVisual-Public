package rtx.nv.utils.render.targetesp;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class TargetOrbitsRenderer {
    private static final int SEGMENTS = 48;

    public static void render(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrixStack,
        float radius,
        float height,
        float speed,
        float thickness,
        float alpha,
        boolean throughWalls,
        TargetEspColorProvider colorProvider
    ) {
        if (immediate == null || matrixStack == null || alpha <= 0.001f) {
            return;
        }

        long now = System.currentTimeMillis();
        float time = (float) ((now % 1000000L) / 1000.0) * speed;

        float ringRadius = radius * 1.15f;
        float halfThick = Math.max(0.015f, thickness * 0.035f);

        VertexConsumer consumer = immediate.getBuffer(throughWalls ? RenderLayers.lightning() : RenderLayers.debugQuads());

        renderOrbitRing(matrixStack, consumer, height, ringRadius, halfThick, 40.0f, time * 1.4f, alpha * 0.85f, 0, colorProvider);
        renderOrbitRing(matrixStack, consumer, height, ringRadius * 1.05f, halfThick, -40.0f, -time * 1.1f + 1.5f, alpha * 0.85f, 120, colorProvider);
        float wobble = MathHelper.sin(time * 2.0f) * 12.0f;
        renderOrbitRing(matrixStack, consumer, height, ringRadius * 0.95f, halfThick * 0.9f, wobble, time * 0.8f + 3.0f, alpha * 0.7f, 240, colorProvider);
    }

    private static void renderOrbitRing(
        MatrixStack matrixStack,
        VertexConsumer consumer,
        float height,
        float radius,
        float halfThick,
        float tiltDeg,
        float angleRad,
        float alpha,
        int colorOffset,
        TargetEspColorProvider colorProvider
    ) {
        matrixStack.push();
        matrixStack.translate(0.0f, height, 0.0f);
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(tiltDeg));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(angleRad));

        MatrixStack.Entry entry = matrixStack.peek();
        float innerR = Math.max(0.05f, radius - halfThick);
        float outerR = radius + halfThick;

        for (int i = 0; i < SEGMENTS; i++) {
            float f1 = (float) i / SEGMENTS;
            float f2 = (float) (i + 1) / SEGMENTS;
            float a1 = f1 * (float)(Math.PI * 2);
            float a2 = f2 * (float)(Math.PI * 2);

            float sin1 = MathHelper.sin(a1);
            float cos1 = MathHelper.cos(a1);
            float sin2 = MathHelper.sin(a2);
            float cos2 = MathHelper.cos(a2);

            float sat1 = MathHelper.sin(a1 * 2.0f) * 0.35f + 0.65f;
            float sat2 = MathHelper.sin(a2 * 2.0f) * 0.35f + 0.65f;

            int c1 = colorProvider != null ? colorProvider.getColor((int)(f1 * 360) + colorOffset, alpha * sat1) : 0xFFFFFFFF;
            int c2 = colorProvider != null ? colorProvider.getColor((int)(f2 * 360) + colorOffset, alpha * sat2) : 0xFFFFFFFF;

            consumer.vertex(entry, cos1 * innerR, 0.0f, sin1 * innerR).color(c1);
            consumer.vertex(entry, cos1 * outerR, 0.0f, sin1 * outerR).color(c1);
            consumer.vertex(entry, cos2 * outerR, 0.0f, sin2 * outerR).color(c2);
            consumer.vertex(entry, cos2 * innerR, 0.0f, sin2 * innerR).color(c2);

            consumer.vertex(entry, cos2 * innerR, 0.0f, sin2 * innerR).color(c2);
            consumer.vertex(entry, cos2 * outerR, 0.0f, sin2 * outerR).color(c2);
            consumer.vertex(entry, cos1 * outerR, 0.0f, sin1 * outerR).color(c1);
            consumer.vertex(entry, cos1 * innerR, 0.0f, sin1 * innerR).color(c1);
        }

        matrixStack.pop();
    }
}
