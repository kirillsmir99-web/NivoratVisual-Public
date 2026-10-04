package rtx.nv.utils.render.wings;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import java.util.List;
import java.util.Map;

public class WingBone {
    private final String name;
    private final float originX;
    private final float originY;
    private final float originZ;
    private final float rotX;
    private final float rotY;
    private final float rotZ;
    private final boolean isLeft;
    private final boolean isRight;
    private final List<WingCube> cubes;
    private final List<WingBone> children;

    public WingBone(String name, float[] origin, float[] rotation, boolean isLeft, boolean isRight, List<WingCube> cubes, List<WingBone> children) {
        this.name = name;
        if (origin != null && origin.length >= 3) {
            this.originX = origin[0];
            this.originY = origin[1];
            this.originZ = origin[2];
        } else {
            this.originX = 0;
            this.originY = 0;
            this.originZ = 0;
        }

        if (rotation != null && rotation.length >= 3) {
            this.rotX = rotation[0];
            this.rotY = rotation[1];
            this.rotZ = rotation[2];
        } else {
            this.rotX = 0;
            this.rotY = 0;
            this.rotZ = 0;
        }

        this.isLeft = isLeft;
        this.isRight = isRight;
        this.cubes = cubes != null ? cubes : List.of();
        this.children = children != null ? children : List.of();
    }

    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, Map<String, float[]> animRotations, float proceduralFlap, float glideFlap, float vOffset, float vScale, float uScale) {
        matrices.push();
        matrices.translate(this.originX, this.originY, this.originZ);

        if (this.rotZ != 0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotZ));
        if (this.rotY != 0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotY));
        if (this.rotX != 0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotX));

        float[] anim = animRotations != null ? animRotations.get(this.name) : null;
        if (anim != null) {
            if (anim[2] != 0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(anim[2]));
            if (anim[1] != 0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(anim[1]));
            if (anim[0] != 0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(anim[0]));
            if (glideFlap > 0.0f) {
                if (this.isLeft) {
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(glideFlap));
                    matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(glideFlap * 0.20f));
                } else if (this.isRight) {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(glideFlap));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(glideFlap * 0.20f));
                }
            }
        } else if (proceduralFlap != 0) {
            if (this.isLeft) {
                matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(proceduralFlap));
                matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(proceduralFlap * 0.20f));
            } else if (this.isRight) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(proceduralFlap));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(proceduralFlap * 0.20f));
            }
        }

        matrices.translate(-this.originX, -this.originY, -this.originZ);

        for (WingCube cube : this.cubes) {
            cube.render(matrices, vertices, light, overlay, vOffset, vScale, uScale);
        }

        for (WingBone child : this.children) {
            child.render(matrices, vertices, light, overlay, animRotations, proceduralFlap, glideFlap, vOffset, vScale, uScale);
        }

        matrices.pop();
    }

    public String getName() {
        return this.name;
    }

    public boolean isLeft() {
        return this.isLeft;
    }

    public boolean isRight() {
        return this.isRight;
    }
}
