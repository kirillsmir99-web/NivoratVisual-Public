package rtx.nv.api.mods.geckolib.cache.model.cuboid;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import rtx.nv.api.mods.geckolib.cache.model.GeoBone;
import rtx.nv.api.mods.geckolib.cache.model.cuboid.GeoCube;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo;

public final class CuboidGeoBone
extends GeoBone {
    public final GeoCube[] cubes;

    public CuboidGeoBone(GeoBone geoBone, String string, GeoBone[] geoBoneArray, GeoCube[] geoCubeArray, float f, float f2, float f3, float f4, float f5, float f6) {
        super(geoBone, string, geoBoneArray, f, f2, f3, f4, f5, f6);
        this.cubes = geoCubeArray;
    }

    @Override
    public <R extends GeoRenderState> void render(RenderPassInfo<R> renderPassInfo, MatrixStack matrixStack, VertexConsumer vertexConsumer, int n, int n2, int n3) {
        if (this.frameSnapshot == null || !this.frameSnapshot.isHidden()) {
            for (GeoCube geoCube : this.cubes) {
                matrixStack.push();
                geoCube.render(matrixStack, vertexConsumer, n, n2, n3);
                matrixStack.pop();
            }
        }
    }
}

