package rtx.nv.api.modules.impl.Visuals.particles;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.others.WorldVertex;
import rtx.nv.utils.render.pipeline.ClientPipelines;

public final class ParticleRenderer {
    private final Set<RenderLayer> usedRenderTypes = new HashSet<RenderLayer>();

    public void clear() {
        this.usedRenderTypes.clear();
    }

    public void flush(VertexConsumerProvider.Immediate immediate) {
        for (RenderLayer renderLayer : this.usedRenderTypes) {
            immediate.draw(renderLayer);
        }
        this.usedRenderTypes.clear();
    }

    void drawBox(MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, Vec3d vec3d, Vec3d vec3d2, float f, int n) {
        if (f <= 0.001f || ColorUtil.alpha(n) <= 0) {
            return;
        }
        float f2 = f / 4.5f;
        RenderLayer renderLayer = ClientPipelines.WORLD_PARTICLES_COLOR;
        this.usedRenderTypes.add(renderLayer);
        VertexConsumer vertexConsumer = immediate.getBuffer(renderLayer);
        matrixStack.push();
        matrixStack.translate(vec3d.x - vec3d2.x, vec3d.y - vec3d2.y, vec3d.z - vec3d2.z);
        MatrixStack.Entry entry = matrixStack.peek();
        this.addCube(vertexConsumer, entry, f2, n);
        matrixStack.pop();
    }

    private void addCube(VertexConsumer vertexConsumer, MatrixStack.Entry entry, float f, int n) {
        this.vertex(vertexConsumer, entry, -f, -f, -f, n);
        this.vertex(vertexConsumer, entry, f, -f, -f, n);
        this.vertex(vertexConsumer, entry, f, f, -f, n);
        this.vertex(vertexConsumer, entry, -f, f, -f, n);
        this.vertex(vertexConsumer, entry, -f, -f, f, n);
        this.vertex(vertexConsumer, entry, -f, f, f, n);
        this.vertex(vertexConsumer, entry, f, f, f, n);
        this.vertex(vertexConsumer, entry, f, -f, f, n);
        this.vertex(vertexConsumer, entry, -f, -f, -f, n);
        this.vertex(vertexConsumer, entry, -f, -f, f, n);
        this.vertex(vertexConsumer, entry, f, -f, f, n);
        this.vertex(vertexConsumer, entry, f, -f, -f, n);
        this.vertex(vertexConsumer, entry, -f, f, -f, n);
        this.vertex(vertexConsumer, entry, f, f, -f, n);
        this.vertex(vertexConsumer, entry, f, f, f, n);
        this.vertex(vertexConsumer, entry, -f, f, f, n);
        this.vertex(vertexConsumer, entry, -f, -f, -f, n);
        this.vertex(vertexConsumer, entry, -f, f, -f, n);
        this.vertex(vertexConsumer, entry, -f, f, f, n);
        this.vertex(vertexConsumer, entry, -f, -f, f, n);
        this.vertex(vertexConsumer, entry, f, -f, -f, n);
        this.vertex(vertexConsumer, entry, f, -f, f, n);
        this.vertex(vertexConsumer, entry, f, f, f, n);
        this.vertex(vertexConsumer, entry, f, f, -f, n);
    }

    private void vertex(VertexConsumer vertexConsumer, MatrixStack.Entry entry, float f, float f2, float f3, int n) {
        vertexConsumer.vertex(entry, f, f2, f3).color(n);
    }

    public void drawTexture(MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, Identifier identifier, Vec3d vec3d, Vec3d vec3d2, Quaternionf quaternionf, float f, float f2, int n, boolean bl) {
        RenderLayer renderLayer = bl ? RenderLayers.entityTranslucentEmissive(identifier) : RenderLayers.entityTranslucent(identifier);
        this.renderBillboard(matrixStack, immediate, renderLayer, vec3d, vec3d2, quaternionf, f, f2, n);
    }

    private void renderBillboard(MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, RenderLayer renderLayer, Vec3d vec3d, Vec3d vec3d2, Quaternionf quaternionf, float f, float f2, int n) {
        if (f <= 0.001f || ColorUtil.alpha(n) <= 0) {
            return;
        }
        this.usedRenderTypes.add(renderLayer);
        VertexConsumer vertexConsumer = immediate.getBuffer(renderLayer);
        matrixStack.push();
        matrixStack.translate(vec3d.x - vec3d2.x, vec3d.y - vec3d2.y, vec3d.z - vec3d2.z);
        matrixStack.multiply((Quaternionfc)quaternionf);
        matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Z.rotationDegrees(f2));
        MatrixStack.Entry entry = matrixStack.peek();
        float f3 = f / 2.0f;
        WorldVertex.textured((VertexConsumer)vertexConsumer, (MatrixStack.Entry)entry, (float)(-f3), (float)(-f3), (float)0.0f, (float)0.0f, (float)0.0f, (int)n);
        WorldVertex.textured((VertexConsumer)vertexConsumer, (MatrixStack.Entry)entry, (float)f3, (float)(-f3), (float)0.0f, (float)1.0f, (float)0.0f, (int)n);
        WorldVertex.textured((VertexConsumer)vertexConsumer, (MatrixStack.Entry)entry, (float)f3, (float)f3, (float)0.0f, (float)1.0f, (float)1.0f, (int)n);
        WorldVertex.textured((VertexConsumer)vertexConsumer, (MatrixStack.Entry)entry, (float)(-f3), (float)f3, (float)0.0f, (float)0.0f, (float)1.0f, (int)n);
        matrixStack.pop();
    }
}

