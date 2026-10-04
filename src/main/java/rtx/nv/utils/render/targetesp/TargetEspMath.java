package rtx.nv.utils.render.targetesp;

public class TargetEspMath {
    public static float easeInOutQuad(float t) {
        return t < 0.5f ? 2.0f * t * t : -1.0f + (4.0f - 2.0f * t) * t;
    }

    public static float approach(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) <= speed) {
            return target;
        }
        return current + Math.signum(diff) * speed;
    }
}
