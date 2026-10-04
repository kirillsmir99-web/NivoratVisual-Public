package rtx.nv.api.mods.geckolib.cache.animation.keyframeevent;

public class ParticleKeyframeData extends KeyFrameData {
    private final String effect;
    private final String locator;
    private final String script;

    public ParticleKeyframeData() {
        this(0.0, "", "", "");
    }

    public ParticleKeyframeData(double time, String effect, String locator, String script) {
        super(time);
        this.effect = effect;
        this.locator = locator;
        this.script = script;
    }

    public String getEffect() {
        return this.effect;
    }

    public String getLocator() {
        return this.locator;
    }

    public String getScript() {
        return this.script;
    }
}
