package rtx.nv.utils.render.wings;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public record WingFace(
        float[] x,
        float[] y,
        float[] z,
        float[] u,
        float[] v,
        float nx,
        float ny,
        float nz
) {
    public void render(MatrixStack.Entry pose, VertexConsumer vertices, int light, int overlay, float vOffset, float vScale, float uScale) {
        for (int i = 0; i < 4; i++) {
            float texU = u[i] * uScale;
            float texV = (v[i] + vOffset) * vScale;
            vertices.vertex(pose, x[i], y[i], z[i])
                    .color(255, 255, 255, 255)
                    .texture(texU, texV)
                    .overlay(overlay)
                    .light(light)
                    .normal(pose, nx, ny, nz);
        }
    }
}
