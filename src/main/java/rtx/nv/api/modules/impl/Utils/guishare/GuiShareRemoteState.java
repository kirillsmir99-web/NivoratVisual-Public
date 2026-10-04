package rtx.nv.api.modules.impl.Utils.guishare;
import java.lang.invoke.MethodHandle;
import java.lang.runtime.ObjectMethods;
import java.util.List;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareCloseState;
import rtx.nv.api.modules.impl.Utils.guishare.GuiSharePopupRow;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareThemeState;

public record GuiShareRemoteState(String identityKey, String profileUsername, String minecraftUsername, String world, double x, double y, double z, boolean open, Vec3d anchor, float yaw, float pitch, String category, int eventsSub, float eventsScroll, float listScroll, float mouseX, float mouseY, String popupModule, float popupScroll, float popupX, float popupY, List<GuiSharePopupRow> popupRows, String search, List<String> enabledModules, String role, String avatarUrl, GuiShareCloseState closeState, GuiShareThemeState theme, String selectedTheme, float themesScroll) {
}

