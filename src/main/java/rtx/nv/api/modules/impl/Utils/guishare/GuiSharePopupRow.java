package rtx.nv.api.modules.impl.Utils.guishare;

import java.util.List;
import rtx.nv.api.modules.Module;

public record GuiSharePopupRow(String type, String name, String value, float fraction, int rgb, boolean open, List<String> options, int selectedMask, int alpha) {
    public static List<GuiSharePopupRow> capture(Module module, List<?> widgets) {
        return List.of();
    }
}
