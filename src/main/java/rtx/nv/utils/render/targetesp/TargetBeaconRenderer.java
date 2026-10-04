package rtx.nv.utils.render.targetesp;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class TargetBeaconRenderer {
    private static final int SIDES = 24;
    private static final int RING_COUNT = 3;

    public static void render(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrixStack,
        float radius,
        float targetHeight,
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

        VertexConsumer consumer = immediate.getBuffer(throughWalls ? RenderLayers.lightning() : RenderLayers.debugQuads());
        MatrixStack.Entry entry = matrixStack.peek();

        float beamHeight = Math.max(1.0f, targetHeight);
        float beamRadius = Math.max(0.2f, radius * 0.75f);

        renderGroundContact(entry, consumer, beamRadius * 1.3f, time, alpha, colorProvider);

        renderPillar(entry, consumer, beamRadius, beamHeight, time, alpha, hurtProgress, colorProvider);

        renderRisingRings(entry, consumer, beamRadius, beamHeight, time, thickness, alpha, colorProvider);
    }

    private static void renderGroundContact(
        MatrixStack.Entry entry,
        VertexConsumer consumer,
        float radius,
        float time,
        float alpha,
        TargetEspColorProvider colorProvider
    ) {
        float y = 0.02f;
        float pulse = MathHelper.sin(time * 2.5f) * 0.04f * radius;
        float r = radius + pulse;
        float innerR = Math.max(0.05f, r - 0.08f);

        float rot = time * 1.0f;

        for (int i = 0; i < SIDES; i++) {
            float f1 = (float) i / SIDES;
            float f2 = (float) (i + 1) / SIDES;
            float a1 = f1 * (float)(Math.PI * 2) + rot;
            float a2 = f2 * (float)(Math.PI * 2) + rot;

            float sin1 = MathHelper.sin(a1);
            float cos1 = MathHelper.cos(a1);
            float sin2 = MathHelper.sin(a2);
            float cos2 = MathHelper.cos(a2);

            int c1 = colorProvider != null ? colorProvider.getColor((int)(f1 * 360), alpha * 0.9f) : 0xFFFFFFFF;
            int c2 = colorProvider != null ? colorProvider.getColor((int)(f2 * 360), alpha * 0.9f) : 0xFFFFFFFF;

            consumer.vertex(entry, cos1 * innerR, y, sin1 * innerR).color(c1);
            consumer.vertex(entry, cos1 * r, y, sin1 * r).color(c1);
            consumer.vertex(entry, cos2 * r, y, sin2 * r).color(c2);
            consumer.vertex(entry, cos2 * innerR, y, sin2 * innerR).color(c2);

            consumer.vertex(entry, cos2 * innerR, y, sin2 * innerR).color(c2);
            consumer.vertex(entry, cos2 * r, y, sin2 * r).color(c2);
            consumer.vertex(entry, cos1 * r, y, sin1 * r).color(c1);
            consumer.vertex(entry, cos1 * innerR, y, sin1 * innerR).color(c1);
        }
    }

    private static void renderPillar(
        MatrixStack.Entry entry,
        VertexConsumer consumer,
        float radius,
        float height,
        float time,
        float alpha,
        float hurtProgress,
        TargetEspColorProvider colorProvider
    ) {
        float pulse = (hurtProgress > 0.01f ? MathHelper.sin(time * 12.0f) * 0.06f : 0.0f);
        float r = radius + pulse;

        float bottomAlpha = alpha * (0.35f + hurtProgress * 0.3f);
        float topAlpha = alpha * 0.04f;

        for (int i = 0; i < SIDES; i++) {
            float f1 = (float) i / SIDES;
            float f2 = (float) (i + 1) / SIDES;
            float a1 = f1 * (float)(Math.PI * 2);
            float a2 = f2 * (float)(Math.PI * 2);

            float sin1 = MathHelper.sin(a1);
            float cos1 = MathHelper.cos(a1);
            float sin2 = MathHelper.sin(a2);
            float cos2 = MathHelper.cos(a2);

            int c1Bot = colorProvider != null ? colorProvider.getColor((int)(f1 * 360), bottomAlpha) : 0xFFFFFFFF;
            int c2Bot = colorProvider != null ? colorProvider.getColor((int)(f2 * 360), bottomAlpha) : 0xFFFFFFFF;
            int c1Top = colorProvider != null ? colorProvider.getColor((int)(f1 * 360), topAlpha) : 0xFFFFFFFF;
            int c2Top = colorProvider != null ? colorProvider.getColor((int)(f2 * 360), topAlpha) : 0xFFFFFFFF;

            consumer.vertex(entry, cos1 * r, 0.0f, sin1 * r).color(c1Bot);
            consumer.vertex(entry, cos2 * r, 0.0f, sin2 * r).color(c2Bot);
            consumer.vertex(entry, cos2 * r * 0.85f, height, sin2 * r * 0.85f).color(c2Top);
            consumer.vertex(entry, cos1 * r * 0.85f, height, sin1 * r * 0.85f).color(c1Top);

            consumer.vertex(entry, cos1 * r * 0.85f, height, sin1 * r * 0.85f).color(c1Top);
            consumer.vertex(entry, cos2 * r * 0.85f, height, sin2 * r * 0.85f).color(c2Top);
            consumer.vertex(entry, cos2 * r, 0.0f, sin2 * r).color(c2Bot);
            consumer.vertex(entry, cos1 * r, 0.0f, sin1 * r).color(c1Bot);
        }
    }

    private static void renderRisingRings(
        MatrixStack.Entry entry,
        VertexConsumer consumer,
        float baseRadius,
        float height,
        float time,
        float thickness,
        float alpha,
        TargetEspColorProvider colorProvider
    ) {
        float ringThick = Math.max(0.015f, thickness * 0.035f);

        for (int r = 0; r < RING_COUNT; r++) {
            float offset = (float) r / RING_COUNT;
            float progress = ((time * 0.5f + offset) % 1.0f);
            float ringY = progress * height;

            float curRadius = baseRadius * (0.85f + 0.35f * progress);
            float ringAlpha = (float) Math.sin(progress * Math.PI) * alpha * 0.85f;

            if (ringAlpha <= 0.01f) {
                continue;
            }

            float inR = Math.max(0.05f, curRadius - ringThick);
            float outR = curRadius + ringThick;

            for (int i = 0; i < SIDES; i++) {
                float f1 = (float) i / SIDES;
                float f2 = (float) (i + 1) / SIDES;
                float a1 = f1 * (float)(Math.PI * 2);
                float a2 = f2 * (float)(Math.PI * 2);

                float sin1 = MathHelper.sin(a1);
                float cos1 = MathHelper.cos(a1);
                float sin2 = MathHelper.sin(a2);
                float cos2 = MathHelper.cos(a2);

                int c1 = colorProvider != null ? colorProvider.getColor((int)(f1 * 360) + (int)(progress * 180), ringAlpha) : 0xFFFFFFFF;
                int c2 = colorProvider != null ? colorProvider.getColor((int)(f2 * 360) + (int)(progress * 180), ringAlpha) : 0xFFFFFFFF;

                consumer.vertex(entry, cos1 * inR, ringY, sin1 * inR).color(c1);
                consumer.vertex(entry, cos1 * outR, ringY, sin1 * outR).color(c1);
                consumer.vertex(entry, cos2 * outR, ringY, sin2 * outR).color(c2);
                consumer.vertex(entry, cos2 * inR, ringY, sin2 * inR).color(c2);

                consumer.vertex(entry, cos2 * inR, ringY, sin2 * inR).color(c2);
                consumer.vertex(entry, cos2 * outR, ringY, sin2 * outR).color(c2);
                consumer.vertex(entry, cos1 * outR, ringY, sin1 * outR).color(c1);
                consumer.vertex(entry, cos1 * inR, ringY, sin1 * inR).color(c1);
            }
        }
    }
}
