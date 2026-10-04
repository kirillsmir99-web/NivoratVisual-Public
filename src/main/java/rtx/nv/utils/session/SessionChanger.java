package rtx.nv.utils.session;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import rtx.nv.api.mods.accountswitcher.ias.auth.LoginData;

public class SessionChanger {
    private static Consumer<Session> sessionSetter;

    public static void setSessionSetter(Consumer<Session> consumer) {
        sessionSetter = consumer;
    }

    public static void changeUsername(String string) {
        if (sessionSetter == null || string == null || string.isEmpty()) {
            return;
        }
        UUID uUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + string).getBytes());
        SessionChanger.changeSession(string, uUID, "", false);
    }

    public static void applyLoginData(LoginData loginData) {
        if (loginData == null) {
            return;
        }
        SessionChanger.changeSession(loginData.name(), loginData.uuid(), loginData.token(), loginData.online());
    }

    public static void changeSession(String string, UUID uUID, String string2, boolean bl) {
        if (sessionSetter == null || string == null || string.isEmpty() || uUID == null) {
            return;
        }
        Session session = new Session(string, uUID, string2 == null ? "" : string2, Optional.empty(), Optional.empty());
        sessionSetter.accept(session);
    }

    public static String getCurrentUsername() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient != null && minecraftClient.getSession() != null) {
            return minecraftClient.getSession().getUsername();
        }
        return "";
    }
}

