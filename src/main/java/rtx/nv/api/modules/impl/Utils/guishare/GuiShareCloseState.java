package rtx.nv.api.modules.impl.Utils.guishare;

public record GuiShareCloseState(
    boolean active,
    float openness,
    float shatterProgress,
    float screenScale,
    long seed,
    float rectX,
    float rectY,
    float rectW,
    float rectH,
    float rectPad,
    float screenW,
    float screenH
) {
    public static final GuiShareCloseState IDLE = new GuiShareCloseState(
        false, 0.0f, 0.0f, 1.0f, 0L, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
    );
}
