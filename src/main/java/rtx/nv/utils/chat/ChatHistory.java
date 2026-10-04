package rtx.nv.utils.chat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public final class ChatHistory {
    private static final Path FILE = FabricLoader.getInstance().getGameDir().resolve("nv").resolve("command-history.txt");
    private static final int MAX = 200;
    private static final List<String> entries = new ArrayList<>();
    private static boolean loaded;
    public static volatile boolean seeding;

    private ChatHistory() {
    }

    private static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            if (Files.exists(FILE)) {
                for (String string : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                    if (string == null || string.isBlank()) continue;
                    entries.add(string);
                }
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public static synchronized void add(String string) {
        if (string == null) {
            return;
        }
        String text = string.strip();
        if (text.isEmpty()) {
            return;
        }
        load();
        if (!entries.isEmpty() && entries.get(entries.size() - 1).equals(text)) {
            return;
        }
        entries.add(text);
        while (entries.size() > MAX) {
            entries.remove(0);
        }
        save();
    }

    private static void save() {
        try {
            if (FILE.getParent() != null) {
                Files.createDirectories(FILE.getParent());
            }
            Files.write(FILE, entries, StandardCharsets.UTF_8);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public static synchronized List<String> entries() {
        load();
        return new ArrayList<>(entries);
    }
}
