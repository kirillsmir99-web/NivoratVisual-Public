package rtx.nv.api.modules.restrict;
import java.util.EnumSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import rtx.nv.api.liteapi.FeatureBlocklist;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.impl.Interface.NotificationsModule;
import rtx.nv.api.modules.restrict.Server;
import rtx.nv.api.modules.restrict.ServerRule;
import rtx.nv.api.modules.restrict.ServerRule.Mode;

public final class ServerRestrictions {
    private ServerRestrictions() {
    }

    public static void notify(String string) {
        NotificationsModule.notify(string, 2500L);
    }

    public static EnumSet<Server> current() {
        Object object;
        EnumSet<Server> enumSet = EnumSet.noneOf(Server.class);
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null) {
            return enumSet;
        }
        String string = "";
        String string2 = "";
        ClientPlayNetworkHandler clientPlayNetworkHandler = minecraftClient.getNetworkHandler();
        if (clientPlayNetworkHandler != null) {
            object = clientPlayNetworkHandler.getServerInfo();
            if (object != null && ((ServerInfo)object).address != null) {
                string = ((ServerInfo)object).address;
            }
            string2 = ServerRestrictions.normalize(clientPlayNetworkHandler.getBrand());
        }
        if (string.isEmpty() && minecraftClient.getCurrentServerEntry() != null && minecraftClient.getCurrentServerEntry().address != null) {
            string = minecraftClient.getCurrentServerEntry().address;
        }
        object = string.toLowerCase().trim();
        String string3 = ServerRestrictions.normalize((String)object);
        if (((String)object).isEmpty() && string2.isEmpty()) {
            return enumSet;
        }
        for (Server server : Server.values()) {
            if (!server.matches((String)object, string3, string2)) continue;
            enumSet.add(server);
        }
        return enumSet;
    }

    private static String normalize(String string) {
        if (string == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); ++i) {
            char c = Character.toLowerCase(string.charAt(i));
            if (c < 'a' || c > 'z') continue;
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

    public static boolean isBlocked(Module module) {
        return ServerRestrictions.blockReason(module, ServerRestrictions.current()) != null;
    }

    public static boolean isHiddenBy(Module module, EnumSet<Server> enumSet) {
        if (module == null) {
            return false;
        }
        ServerRule serverRule = module.getClass().getAnnotation(ServerRule.class);
        if (serverRule == null || serverRule.mode() != ServerRule.Mode.HIDE || serverRule.servers().length == 0) {
            return false;
        }
        for (Server server : serverRule.servers()) {
            if (!enumSet.contains((Object)server)) continue;
            return true;
        }
        return false;
    }

    public static String blockReason(Module module, EnumSet<Server> enumSet) {
        if (module == null) {
            return null;
        }
        if (FeatureBlocklist.isModuleBlocked(module)) {
            return module.getName() + " \u0437\u0430\u0431\u043b\u043e\u043a\u0438\u0440\u043e\u0432\u0430\u043d \u0441\u0435\u0440\u0432\u0435\u0440\u043e\u043c";
        }
        ServerRule serverRule = module.getClass().getAnnotation(ServerRule.class);
        if (serverRule == null || serverRule.servers().length == 0) {
            return null;
        }
        if (serverRule.mode() == ServerRule.Mode.HIDE) {
            for (Server server : serverRule.servers()) {
                if (!enumSet.contains((Object)server)) continue;
                return module.getName() + " \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d \u043d\u0430 " + server.display();
            }
            return null;
        }
        if (serverRule.mode() == ServerRule.Mode.ONLY) {
            for (Server server : serverRule.servers()) {
                if (!enumSet.contains((Object)server)) continue;
                return null;
            }
            return "\u041c\u043e\u0434\u0443\u043b\u044c " + module.getName() + " \u043f\u0440\u0435\u0434\u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d \u0442\u043e\u043b\u044c\u043a\u043e \u0434\u043b\u044f \u0441\u0435\u0440\u0432\u0435\u0440\u0430: " + ServerRestrictions.displayList(serverRule.servers());
        }
        for (Server server : serverRule.servers()) {
            if (!enumSet.contains((Object)server)) continue;
            return module.getName() + " \u0437\u0430\u043f\u0440\u0435\u0449\u0451\u043d \u043d\u0430 " + server.display();
        }
        return null;
    }

    private static String displayList(Server[] serverArray) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < serverArray.length; ++i) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(serverArray[i].display());
        }
        return stringBuilder.toString();
    }

    public static boolean isBlockedBy(Module module, EnumSet<Server> enumSet) {
        return ServerRestrictions.blockReason(module, enumSet) != null;
    }
}

