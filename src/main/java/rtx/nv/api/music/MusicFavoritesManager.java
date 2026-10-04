package rtx.nv.api.music;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

public final class MusicFavoritesManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final MusicFavoritesManager INSTANCE = new MusicFavoritesManager();

    private final Set<String> favorites = new HashSet<>();
    private File favoritesFile;

    private MusicFavoritesManager() {
    }

    public static MusicFavoritesManager get() {
        return INSTANCE;
    }

    public void init() {
        File configDir = new File(MinecraftClient.getInstance().runDirectory, "config/nv");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        this.favoritesFile = new File(configDir, "favorites.json");
        this.load();
    }

    private String key(String artist, String title) {
        String a = artist != null ? artist.trim().toLowerCase() : "";
        String t = title != null ? title.trim().toLowerCase() : "";
        return a + ":::" + t;
    }

    public synchronized boolean isFavorite(String artist, String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        return this.favorites.contains(key(artist, title));
    }

    public synchronized boolean toggleFavorite(String artist, String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        String k = key(artist, title);
        boolean added;
        if (this.favorites.contains(k)) {
            this.favorites.remove(k);
            added = false;
        } else {
            this.favorites.add(k);
            added = true;
        }
        this.save();
        return added;
    }

    private synchronized void load() {
        if (this.favoritesFile == null || !this.favoritesFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(this.favoritesFile, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("favorites") && obj.get("favorites").isJsonArray()) {
                    JsonArray arr = obj.getAsJsonArray("favorites");
                    for (JsonElement item : arr) {
                        if (item.isJsonPrimitive()) {
                            this.favorites.add(item.getAsString());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private synchronized void save() {
        if (this.favoritesFile == null) {
            return;
        }
        try (FileWriter writer = new FileWriter(this.favoritesFile, StandardCharsets.UTF_8)) {
            JsonObject obj = new JsonObject();
            JsonArray arr = new JsonArray();
            for (String s : this.favorites) {
                arr.add(s);
            }
            obj.add("favorites", arr);
            GSON.toJson(obj, writer);
        } catch (Exception ignored) {
        }
    }
}
