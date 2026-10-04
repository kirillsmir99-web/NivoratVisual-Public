package rtx.nv.utils.render.targetesp;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class TargetSegmentsRenderer {
    private static final int SEGMENT_COUNT = 4;
    private static final int SUB_STEPS = 16;

    public static void render(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrixStack,
        float radius,
        float height,
        float speed,
        float thickness,
        float alpha,
        float hurtProgress,
        boolean throughWalls,
        TargetEspColorProvider colorProvider
    ) {
        if (immediate == null || matrixStack == null || alpha <= 0.001f) {
            return;
        }

        long now = System.currentTimeMillis();
        float time = (float) ((now % 1000000L) / 1000.0) * speed;

        float pulse = MathHelper.sin(time * 3.0f) * 0.04f * radius;
        float hurtScale = 1.0f - hurtProgress * 0.15f;
        float actualRadius = (radius + pulse) * hurtScale;
        float actualThickness = Math.max(0.015f, thickness * 0.05f * (1.0f + hurtProgress * 0.4f));

        float angle = time * 1.5f;

        matrixStack.push();
        matrixStack.translate(0.0f, height, 0.0f);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(angle));

        VertexConsumer consumer = immediate.getBuffer(throughWalls ? RenderLayers.lightning() : RenderLayers.debugQuads());
        MatrixStack.Entry entry = matrixStack.peek();

        float segmentArc = (float) (Math.PI * 2.0 / SEGMENT_COUNT);
        float activeArc = segmentArc * 0.72f;

        for (int s = 0; s < SEGMENT_COUNT; s++) {
            float startAngle = s * segmentArc;
            int baseColorDeg = s * (360 / SEGMENT_COUNT);

            for (int i = 0; i < SUB_STEPS; i++) {
                float t1 = (float) i / SUB_STEPS;
                float t2 = (float) (i + 1) / SUB_STEPS;

                float a1 = startAngle + t1 * activeArc;
                float a2 = startAngle + t2 * activeArc;

                float sin1 = MathHelper.sin(a1);
                float cos1 = MathHelper.cos(a1);
                float sin2 = MathHelper.sin(a2);
                float cos2 = MathHelper.cos(a2);

                float cap1 = (float) Math.sin(t1 * Math.PI);
                float cap2 = (float) Math.sin(t2 * Math.PI);
                float capShape1 = (float) Math.pow(cap1, 0.45);
                float capShape2 = (float) Math.pow(cap2, 0.45);

                float thick1 = actualThickness * capShape1;
                float thick2 = actualThickness * capShape2;

                float inR1 = actualRadius - thick1 * 0.5f;
                float outR1 = actualRadius + thick1 * 0.5f;
                float inR2 = actualRadius - thick2 * 0.5f;
                float outR2 = actualRadius + thick2 * 0.5f;

                float segAlpha1 = alpha * (0.3f + 0.7f * capShape1);
                float segAlpha2 = alpha * (0.3f + 0.7f * capShape2);

                if (hurtProgress > 0.01f) {
                    segAlpha1 = Math.min(1.0f, segAlpha1 * (1.0f + hurtProgress * 0.5f));
                    segAlpha2 = Math.min(1.0f, segAlpha2 * (1.0f + hurtProgress * 0.5f));
                }

                int c1 = colorProvider != null ? colorProvider.getColor(baseColorDeg + (int)(t1 * 60), segAlpha1) : 0xFFFFFFFF;
                int c2 = colorProvider != null ? colorProvider.getColor(baseColorDeg + (int)(t2 * 60), segAlpha2) : 0xFFFFFFFF;

                consumer.vertex(entry, cos1 * inR1, 0.0f, sin1 * inR1).color(c1);
                consumer.vertex(entry, cos1 * outR1, 0.0f, sin1 * outR1).color(c1);
                consumer.vertex(entry, cos2 * outR2, 0.0f, sin2 * outR2).color(c2);
                consumer.vertex(entry, cos2 * inR2, 0.0f, sin2 * inR2).color(c2);

                consumer.vertex(entry, cos2 * inR2, 0.0f, sin2 * inR2).color(c2);
                consumer.vertex(entry, cos2 * outR2, 0.0f, sin2 * outR2).color(c2);
                consumer.vertex(entry, cos1 * outR1, 0.0f, sin1 * outR1).color(c1);
                consumer.vertex(entry, cos1 * inR1, 0.0f, sin1 * inR1).color(c1);
            }
        }

        renderSatelliteDots(entry, consumer, actualRadius * 1.18f, time, alpha, colorProvider);

        matrixStack.pop();
    }

    private static void renderSatelliteDots(
        MatrixStack.Entry entry,
        VertexConsumer consumer,
        float radius,
        float time,
        float alpha,
        TargetEspColorProvider colorProvider
    ) {
        int dotCount = 8;
        float dotSize = 0.035f;
        for (int d = 0; d < dotCount; d++) {
            float angle = (float) (d * (Math.PI * 2.0 / dotCount)) - time * 0.8f;
            float cx = MathHelper.cos(angle) * radius;
            float cz = MathHelper.sin(angle) * radius;

            int col = colorProvider != null ? colorProvider.getColor(d * 45, alpha * 0.75f) : 0xFFFFFFFF;

            consumer.vertex(entry, cx - dotSize, 0.0f, cz - dotSize).color(col);
            consumer.vertex(entry, cx + dotSize, 0.0f, cz - dotSize).color(col);
            consumer.vertex(entry, cx + dotSize, 0.0f, cz + dotSize).color(col);
            consumer.vertex(entry, cx - dotSize, 0.0f, cz + dotSize).color(col);

            consumer.vertex(entry, cx - dotSize, 0.0f, cz + dotSize).color(col);
            consumer.vertex(entry, cx + dotSize, 0.0f, cz + dotSize).color(col);
            consumer.vertex(entry, cx + dotSize, 0.0f, cz - dotSize).color(col);
            consumer.vertex(entry, cx - dotSize, 0.0f, cz - dotSize).color(col);
        }
    }
}
