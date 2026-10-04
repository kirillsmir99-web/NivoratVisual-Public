package rtx.nv.utils.render.world;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class BlockOverlayRenderer {
    private BlockOverlayRenderer() {}
    private static final net.minecraft.client.render.RenderLayer COSMOS = portalLayer("cosmos", 15);
    private static final net.minecraft.client.render.RenderLayer ABYSS = portalLayer("abyss", 8);

    private static net.minecraft.client.render.RenderLayer portalLayer(String name, int layers) {
        var pipeline = net.minecraft.client.gl.RenderPipelines.register(
            com.mojang.blaze3d.pipeline.RenderPipeline.builder(net.minecraft.client.gl.RenderPipelines.RENDERTYPE_END_PORTAL_SNIPPET)
                .withLocation(net.minecraft.util.Identifier.of("nv", "pipeline/block_" + name))
                .withShaderDefine("PORTAL_LAYERS", layers)
                .withDepthBias(-1f, -10f).withDepthWrite(false).withCull(false).build());
        return net.minecraft.client.render.RenderLayer.of("nv_block_" + name,
            net.minecraft.client.render.RenderSetup.builder(pipeline)
                .texture("Sampler0", net.minecraft.client.render.block.entity.AbstractEndPortalBlockEntityRenderer.SKY_TEXTURE)
                .texture("Sampler1", net.minecraft.client.render.block.entity.AbstractEndPortalBlockEntityRenderer.PORTAL_TEXTURE).build());
    }

    public static void renderPortal(VertexConsumerProvider.Immediate consumers, MatrixStack matrices, Vec3d camera, Box box, boolean abyss) {
        VertexConsumer vertices = consumers.getBuffer(abyss ? ABYSS : COSMOS);
        var entry = matrices.peek();
        float x0 = (float)(box.minX-camera.x), x1 = (float)(box.maxX-camera.x);
        float y0 = (float)(box.minY-camera.y), y1 = (float)(box.maxY-camera.y);
        float z0 = (float)(box.minZ-camera.z), z1 = (float)(box.maxZ-camera.z);
        float[][] faces = {
            {x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1},
            {x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0},
            {x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0},
            {x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1},
            {x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0},
            {x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1}
        };
        for (float[] face : faces) for (int i=0; i<12; i+=3) vertices.vertex(entry, face[i], face[i+1], face[i+2]);
    }

    public static void renderFill(VertexConsumerProvider.Immediate consumers, MatrixStack matrixStack, Vec3d cameraPos, Box box, int fillColor) {
        if (box == null || consumers == null || matrixStack == null || (fillColor >>> 24) == 0) return;
        WorldShapeRenderer.boxes(consumers, matrixStack, cameraPos, java.util.List.of(box), fillColor, 0, 1.0f);
    }

    public static void renderOutline(VertexConsumerProvider.Immediate consumers, MatrixStack matrixStack, Vec3d cameraPos, net.minecraft.util.math.BlockPos pos, net.minecraft.util.shape.VoxelShape shape, int outlineColor, float lineWidth) {
        if (shape == null || shape.isEmpty() || consumers == null || matrixStack == null || (outlineColor >>> 24) == 0) return;
        MatrixStack.Entry entry = matrixStack.peek();
        VertexConsumer lineConsumer = consumers.getBuffer(net.minecraft.client.render.RenderLayers.lines());
        double px = pos.getX() - cameraPos.x;
        double py = pos.getY() - cameraPos.y;
        double pz = pos.getZ() - cameraPos.z;
        shape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float x1 = (float)(px + minX);
            float y1 = (float)(py + minY);
            float z1 = (float)(pz + minZ);
            float x2 = (float)(px + maxX);
            float y2 = (float)(py + maxY);
            float z2 = (float)(pz + maxZ);
            WorldShapeRenderer.drawEdge(lineConsumer, entry, x1, y1, z1, x2, y2, z2, outlineColor, lineWidth);
        });
    }

    public static void render(VertexConsumerProvider.Immediate consumers, MatrixStack matrixStack, Vec3d cameraPos, Box box, int[] colors1, int[] colors2, int[] colors3) {
        if (box == null || consumers == null || matrixStack == null) {
            return;
        }
        int outline = colors1 != null && colors1.length > 0 ? colors1[0] : 0xFFFFFFFF;
        int fill = colors2 != null && colors2.length > 0 ? colors2[0] : 0x33FFFFFF;
        WorldShapeRenderer.boxes(consumers, matrixStack, cameraPos, java.util.List.of(box), fill, outline, 1.5f);
    }
}
