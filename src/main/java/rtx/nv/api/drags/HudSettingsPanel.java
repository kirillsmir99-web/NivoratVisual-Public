package rtx.nv.api.drags;

import java.util.List;
import rtx.nv.api.ui.settings.Setting;

public final class HudSettingsPanel {
    public static float height(int count) {
        return 24.0f + count * 18.0f;
    }

    public static boolean click(float mouseX, float mouseY, float x, float y, float width, List<Setting> settings) {
        return false;
    }
}