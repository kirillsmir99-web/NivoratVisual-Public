package rtx.nv.api.mods.geckolib.cache.animation.keyframeevent;
import java.util.Objects;

public abstract class KeyFrameData {
    private final double animationTime;

    public KeyFrameData() {
        this(0.0);
    }

    public KeyFrameData(double d) {
        this.animationTime = d;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || this.getClass() != object.getClass()) {
            return false;
        }
        return this.hashCode() == object.hashCode();
    }

    public int hashCode() {
        return Objects.hashCode(this.animationTime);
    }

    public double getTime() {
        return this.animationTime;
    }
}

