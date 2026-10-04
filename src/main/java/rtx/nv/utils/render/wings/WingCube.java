package rtx.nv.utils.render.wings;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public class WingCube {
    private final WingFace[] faces;
    private final float originX;
    private final float originY;
    private final float originZ;
    private final float rotX;
    private final float rotY;
    private final float rotZ;
    private final boolean hasRotation;

    public WingCube(WingFace[] faces, float[] origin, float[] rotation) {
        this.faces = faces != null ? faces : new WingFace[0];
        if (origin != null && origin.length >= 3) {
            this.originX = origin[0];
            this.originY = origin[1];
            this.originZ = origin[2];
        } else {
            this.originX = 0;
            this.originY = 0;
            this.originZ = 0;
        }

        if (rotation != null && rotation.length >= 3 && (rotation[0] != 0 || rotation[1] != 0 || rotation[2] != 0)) {
            this.rotX = rotation[0];
            this.rotY = rotation[1];
            this.rotZ = rotation[2];
            this.hasRotation = true;
        } else {
            this.rotX = 0;
            this.rotY = 0;
            this.rotZ = 0;
            this.hasRotation = false;
        }
    }

    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float vOffset, float vScale, float uScale) {
        if (this.hasRotation) {
            matrices.push();
            matrices.translate(this.originX, this.originY, this.originZ);
            if (this.rotZ != 0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotZ));
            if (this.rotY != 0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotY));
            if (this.rotX != 0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotX));
            matrices.translate(-this.originX, -this.originY, -this.originZ);

            MatrixStack.Entry entry = matrices.peek();
            for (WingFace face : this.faces) {
                face.render(entry, vertices, light, overlay, vOffset, vScale, uScale);
            }
            matrices.pop();
        } else {
            MatrixStack.Entry entry = matrices.peek();
            for (WingFace face : this.faces) {
                face.render(entry, vertices, light, overlay, vOffset, vScale, uScale);
            }
        }
    }
}
