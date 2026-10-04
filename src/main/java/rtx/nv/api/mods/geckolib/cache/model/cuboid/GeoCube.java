package rtx.nv.api.mods.geckolib.cache.model.cuboid;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import rtx.nv.api.mods.geckolib.cache.model.GeoQuad;
import rtx.nv.api.mods.geckolib.util.RenderUtil;

public record GeoCube(GeoQuad[] quads, Vec3d pivot, Vec3d rotation, Vec3d size) {
    public void rotate(MatrixStack matrixStack) {
        Vec3d vec3d = this.rotation();
        matrixStack.multiply((Quaternionfc)new Quaternionf().rotationXYZ(0.0f, 0.0f, (float)vec3d.getZ()));
        matrixStack.multiply((Quaternionfc)new Quaternionf().rotationXYZ(0.0f, (float)vec3d.getY(), 0.0f));
        matrixStack.multiply((Quaternionfc)new Quaternionf().rotationXYZ((float)vec3d.getX(), 0.0f, 0.0f));
    }

    public void render(MatrixStack matrixStack, VertexConsumer vertexConsumer, int n, int n2, int n3) {
        this.translateToPivotPoint(matrixStack);
        this.rotate(matrixStack);
        this.translateAwayFromPivotPoint(matrixStack);
        Matrix3f matrix3f = matrixStack.peek().getNormalMatrix();
        Matrix4f matrix4f = new Matrix4f((Matrix4fc)matrixStack.peek().getPositionMatrix());
        for (GeoQuad geoQuad : this.quads) {
            if (geoQuad == null) continue;
            Vector3f vector3f = matrix3f.transform(geoQuad.normalVec());
            RenderUtil.fixInvertedFlatCube(this, vector3f);
            geoQuad.render(matrix4f, vector3f, vertexConsumer, n, n2, n3);
        }
    }

    public void translateAwayFromPivotPoint(MatrixStack matrixStack) {
        matrixStack.translate(-this.pivot().getX() / 16.0, -this.pivot().getY() / 16.0, -this.pivot().getZ() / 16.0);
    }

    public void translateToPivotPoint(MatrixStack matrixStack) {
        matrixStack.translate(this.pivot().getX() / 16.0, this.pivot().getY() / 16.0, this.pivot().getZ() / 16.0);
    }
}

