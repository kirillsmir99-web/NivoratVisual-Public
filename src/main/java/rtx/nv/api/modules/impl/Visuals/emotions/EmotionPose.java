package rtx.nv.api.modules.impl.Visuals.emotions;

public class EmotionPose {
    public float headX = Float.NaN, headY = Float.NaN, headZ = Float.NaN;
    public float bodyX = Float.NaN, bodyY = Float.NaN, bodyZ = Float.NaN;
    public float leftArmX = Float.NaN, leftArmY = Float.NaN, leftArmZ = Float.NaN;
    public float rightArmX = Float.NaN, rightArmY = Float.NaN, rightArmZ = Float.NaN;
    public float leftLegX = Float.NaN, leftLegY = Float.NaN, leftLegZ = Float.NaN;
    public float rightLegX = Float.NaN, rightLegY = Float.NaN, rightLegZ = Float.NaN;

    public void reset() {
        headX = headY = headZ = Float.NaN;
        bodyX = bodyY = bodyZ = Float.NaN;
        leftArmX = leftArmY = leftArmZ = Float.NaN;
        rightArmX = rightArmY = rightArmZ = Float.NaN;
        leftLegX = leftLegY = leftLegZ = Float.NaN;
        rightLegX = rightLegY = rightLegZ = Float.NaN;
    }
}
