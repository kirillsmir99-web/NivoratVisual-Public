package rtx.nv.api.modules.impl.Utils.guishare;
import java.lang.invoke.MethodHandle;
import java.lang.runtime.ObjectMethods;
import java.util.List;
import rtx.nv.api.modules.impl.Utils.guishare.GuiSharePopupRow;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareThemeState;

public record GuiShareLocalSnapshot(String category, int eventsSub, float eventsScroll, float listScroll, float mouseX, float mouseY, String popupModule, float popupScroll, float popupX, float popupY, List<GuiSharePopupRow> popupRows, String search, List<String> enabledModules, String role, String avatarUrl, GuiShareThemeState theme, String selectedTheme, float themesScroll) {
}

