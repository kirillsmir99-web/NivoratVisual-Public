package rtx.nv.api.mods.geckolib.cache.animation.keyframeevent;

public class CustomInstructionKeyframeData extends KeyFrameData {
    private final String instructions;

    public CustomInstructionKeyframeData() {
        this(0.0, "");
    }

    public CustomInstructionKeyframeData(double time, String instructions) {
        super(time);
        this.instructions = instructions;
    }

    public String getInstructions() {
        return this.instructions;
    }
}
