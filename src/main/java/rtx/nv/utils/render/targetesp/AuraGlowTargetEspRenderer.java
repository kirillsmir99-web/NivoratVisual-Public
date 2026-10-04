package rtx.nv.utils.render.targetesp;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class AuraGlowTargetEspRenderer {
    private static final int SEGMENTS = 48;

    public static void render(
        MatrixStack matrixStack,
        VertexConsumerProvider.Immediate immediate,
        TargetEspRenderContext context,
        TargetEspColorProvider colorProvider
    ) {
        if (immediate == null || matrixStack == null || context == null || context.alpha() <= 0.001f) {
            return;
        }
        MatrixStack.Entry entry = matrixStack.peek();
        VertexConsumer consumer = immediate.getBuffer(context.throughWalls() ? RenderLayers.lightning() : RenderLayers.debugQuads());

        float radius = Math.max(0.4f, (context.target().getWidth() * 0.5f + 0.2f));
        float height = context.circleHeight();
        float alpha = context.alpha();
        float time = (float) ((System.currentTimeMillis() % 100000L) / 1000.0);

        for (int i = 0; i < SEGMENTS; i++) {
            float f1 = (float) i / SEGMENTS;
            float f2 = (float) (i + 1) / SEGMENTS;
            float a1 = f1 * (float) (Math.PI * 2);
            float a2 = f2 * (float) (Math.PI * 2);

            float sin1 = MathHelper.sin(a1);
            float cos1 = MathHelper.cos(a1);
            float sin2 = MathHelper.sin(a2);
            float cos2 = MathHelper.cos(a2);

            float wave1 = (MathHelper.sin(a1 * 3.0f + time * 3.0f) * 0.5f + 0.5f) * height;
            float wave2 = (MathHelper.sin(a2 * 3.0f + time * 3.0f) * 0.5f + 0.5f) * height;

            int c1 = colorProvider != null ? colorProvider.getColor((int)(f1 * 360), alpha * 0.6f) : 0xFFFFFFFF;
            int c2 = colorProvider != null ? colorProvider.getColor((int)(f2 * 360), alpha * 0.6f) : 0xFFFFFFFF;
            int cTop1 = colorProvider != null ? colorProvider.getColor((int)(f1 * 360), 0.0f) : 0x00FFFFFF;
            int cTop2 = colorProvider != null ? colorProvider.getColor((int)(f2 * 360), 0.0f) : 0x00FFFFFF;

            consumer.vertex(entry, cos1 * radius, 0.0f, sin1 * radius).color(c1);
            consumer.vertex(entry, cos1 * radius, wave1, sin1 * radius).color(cTop1);
            consumer.vertex(entry, cos2 * radius, wave2, sin2 * radius).color(cTop2);
            consumer.vertex(entry, cos2 * radius, 0.0f, sin2 * radius).color(c2);
        }
    }

    public static void endBatch(VertexConsumerProvider.Immediate immediate, boolean throughWalls) {
        if (immediate != null) {
            immediate.draw(throughWalls ? RenderLayers.lightning() : RenderLayers.debugQuads());
        }
    }
}
