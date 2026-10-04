package rtx.nv.utils.render.render2d.effecticon;

import org.joml.Matrix3x2f;

public record PoseKey(float m00, float m01, float m10, float m11, float m20, float m21) {
    public static PoseKey of(Matrix3x2f m) {
        return new PoseKey(m.m00, m.m01, m.m10, m.m11, m.m20, m.m21);
    }
}
