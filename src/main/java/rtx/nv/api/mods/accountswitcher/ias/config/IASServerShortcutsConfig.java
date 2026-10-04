package rtx.nv.api.mods.accountswitcher.ias.config;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class IASServerShortcutsConfig {
    private IASServerShortcutsConfig() {}

    public static List<ShortcutEntry> load(Path path) {
        return Collections.emptyList();
    }

    public record ShortcutEntry(String name, String address, String icon) {}
}
