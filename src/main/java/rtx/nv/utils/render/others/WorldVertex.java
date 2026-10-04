package rtx.nv.utils.render.others;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public final class WorldVertex {
    public static void textured(VertexConsumer vc, MatrixStack.Entry entry, float x, float y, float z, float u, float v, int color) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        vc.vertex(entry, x, y, z).color(r, g, b, a).texture(u, v).overlay(0, 10).light(0xF000F0).normal(entry, 0.0f, 1.0f, 0.0f);
    }
}