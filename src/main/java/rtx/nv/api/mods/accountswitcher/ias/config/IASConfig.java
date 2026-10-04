package rtx.nv.api.mods.accountswitcher.ias.config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtx.nv.api.mods.accountswitcher.ias.account.Account;
import rtx.nv.api.mods.accountswitcher.ias.config.ServerMode;
import rtx.nv.api.mods.accountswitcher.ias.config.TextAlign;
import rtx.nv.api.mods.accountswitcher.ias.config.migrator.Migrator;
import rtx.nv.api.mods.accountswitcher.ias.utils.GSONUtils;
import rtx.nv.api.mods.accountswitcher.ias.utils.IUtils;

public final class IASConfig {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithModifiers(new int[]{128, 16}).create();
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"IAS/IASConfig");
    public static boolean titleText = true;
    public static String titleTextX = null;
    public static String titleTextY = null;
    public static TextAlign titleTextAlign = TextAlign.LEFT;
    public static boolean titleButton = true;
    public static String titleButtonX = null;
    public static String titleButtonY = null;
    public static boolean serversText = true;
    public static String serversTextX = null;
    public static String serversTextY = null;
    public static TextAlign serversTextAlign = TextAlign.LEFT;
    public static boolean serversButton = true;
    public static String serversButtonX = null;
    public static String serversButtonY = null;
    public static boolean allowNoCrypt = false;
    public static boolean nickWarns = true;
    public static boolean unexpectedPigs = true;
    public static boolean barNick = false;
    public static ServerMode server = ServerMode.AVAILABLE;
    public static boolean passwordEchoing = true;
    public static boolean restoreLastAccount = true;
    public static String lastAccountName = null;
    public static String lastAccountUuid = null;
    public static boolean lastAccountOnline = true;

    private IASConfig() {
    }

    public static void load(Path path) {
        try {
            LOGGER.debug("IAS: Loading config for {}...", (Object)path);
            Path path2 = path.resolve("ias.json");
            if (!Files.isRegularFile(path2, new LinkOption[0])) {
                LOGGER.debug("IAS: Config not found. Saving...");
                IASConfig.save(path);
                return;
            }
            String string = Files.readString(path2);
            JsonObject jsonObject = (JsonObject)GSON.fromJson(string, JsonObject.class);
            int n = jsonObject.has("version") ? GSONUtils.getIntOrThrow((JsonObject)jsonObject, (String)"version") : 1;
            LOGGER.trace("IAS: Loaded config version is {}.", (Object)n);
            Migrator migrator = Migrator.fromVersion((int)n);
            if (migrator != null) {
                LOGGER.info("IAS: Migrating old config version {} via {}.", (Object)n, (Object)migrator);
                migrator.load(jsonObject);
                LOGGER.info("IAS: Migrated old config.");
                IASConfig.save(path);
                return;
            }
            GSON.fromJson((JsonElement)jsonObject, IASConfig.class);
            LOGGER.debug("IAS: Config loaded.");
        }
        catch (Throwable throwable) {
            throw new RuntimeException("Unable to load IAS config.", throwable);
        }
        finally {
            titleTextAlign = Objects.requireNonNullElse(titleTextAlign, TextAlign.LEFT);
            serversTextAlign = Objects.requireNonNullElse(serversTextAlign, TextAlign.LEFT);
            server = Objects.requireNonNullElse(server, ServerMode.AVAILABLE);
        }
    }

    public static void save(Path path) {
        try {
            LOGGER.debug("IAS: Saving config into {}...", (Object)path);
            titleTextAlign = Objects.requireNonNullElse(titleTextAlign, TextAlign.LEFT);
            serversTextAlign = Objects.requireNonNullElse(serversTextAlign, TextAlign.LEFT);
            server = Objects.requireNonNullElse(server, ServerMode.AVAILABLE);
            Path path2 = path.resolve("ias.json");
            JsonObject jsonObject = (JsonObject)GSON.toJsonTree((Object)new IASConfig());
            jsonObject.addProperty("version", (Number)4);
            String string = GSON.toJson((JsonElement)jsonObject);
            Files.createDirectories(path2.getParent(), new FileAttribute[0]);
            Files.writeString(path2, (CharSequence)string, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.SYNC, StandardOpenOption.DSYNC);
            LOGGER.debug("IAS: Config saved to {}.", (Object)path2);
        }
        catch (Throwable throwable) {
            throw new RuntimeException("Unable to save IAS config.", throwable);
        }
    }

    public static void rememberLastAccount(Account account, boolean bl) {
        lastAccountName = account.name();
        lastAccountUuid = account.uuid().toString();
        lastAccountOnline = bl;
    }

    public static void clearLastAccount() {
        lastAccountName = null;
        lastAccountUuid = null;
        lastAccountOnline = true;
    }

    public static boolean matchesLastAccount(Account account) {
        if (account == null || lastAccountName == null || lastAccountUuid == null) {
            return false;
        }
        return lastAccountName.equalsIgnoreCase(account.name()) && lastAccountUuid.equalsIgnoreCase(account.uuid().toString());
    }

    public static boolean useServerAuth() {
        if (server == null) {
            return IUtils.canUseSunServer();
        }
        return switch (server) {
            case ALWAYS -> true;
            case NEVER -> false;
            case AVAILABLE -> IUtils.canUseSunServer();
        };
    }
}

