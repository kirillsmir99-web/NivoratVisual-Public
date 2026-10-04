package rtx.nv.utils.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import rtx.nv.NV;

public final class RepositoryStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_ROOT = FabricLoader.getInstance().getGameDir().resolve("nv").resolve("configs");
    private static final Path ROOT = CONFIG_ROOT.resolve("system");
    private static final Map<String, String> FILE_NAMES = Map.of("waypoints", "way");

    private RepositoryStorage() {
    }

    private static String fileName(String string) {
        return FILE_NAMES.getOrDefault(string, string);
    }

    private static Path file(String string) {
        return ROOT.resolve(RepositoryStorage.fileName(string) + ".nv");
    }

    public static JsonObject readObject(String string) {
        Path path = RepositoryStorage.file(string);
        if (!Files.exists(path)) {
            return new JsonObject();
        }
        try (BufferedReader reader = Files.newBufferedReader(path, java.nio.charset.StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            NV.LOGGER.error("[RepositoryStorage] Failed to read {}", string, exception);
            return new JsonObject();
        }
    }

    public static Path root() {
        return ROOT;
    }

    public static void write(String string, JsonObject jsonObject) {
        try {
            Files.createDirectories(ROOT, new FileAttribute[0]);
            Path path = RepositoryStorage.file(string);
            AtomicFiles.writeUtf8(path, GSON.toJson(jsonObject));
        }
        catch (Exception exception) {
            NV.LOGGER.error("[RepositoryStorage] Failed to write {}", (Object)string, (Object)exception);
        }
    }

    public static Path configRoot() {
        return CONFIG_ROOT;
    }

    public static void ensureObject(String string, JsonObject jsonObject) {
        Path path = RepositoryStorage.file(string);
        if (Files.exists(path, new LinkOption[0])) {
            return;
        }
        JsonObject jsonObject2 = RepositoryStorage.readObject(string);
        RepositoryStorage.write(string, jsonObject2.isEmpty() ? jsonObject : jsonObject2);
    }
}
