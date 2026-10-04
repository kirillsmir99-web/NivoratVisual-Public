package rtx.nv.api.mods.geckolib.cache.animation.keyframeevent;

public class SoundKeyframeData extends KeyFrameData {
    private final String sound;

    public SoundKeyframeData() {
        this(0.0, "");
    }

    public SoundKeyframeData(double time, String sound) {
        super(time);
        this.sound = sound;
    }

    public String getSound() {
        return this.sound;
    }
}
