package rtx.nv.utils.animations;

public class Decelerate extends Animation {
    @Override
    public double calculation(double x) {
        return 1.0 - (1.0 - x) * (1.0 - x);
    }
}
