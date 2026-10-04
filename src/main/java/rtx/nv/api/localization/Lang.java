package rtx.nv.api.localization;

import java.util.Locale;

public final class Lang {
    private Lang() {}

    public static String get(String key) {
        return LocalizationManager.get(key);
    }

    public static String get(String key, String defaultValue) {
        return LocalizationManager.get(key, defaultValue);
    }

    public static String format(String key, Object... args) {
        return LocalizationManager.format(key, args);
    }

    public static String toKey(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT)
                   .replace(" ", "_")
                   .replace("-", "_")
                   .replaceAll("[^a-z0-9_\\u0430-\\u044f\\u0451]", "");
    }

    public static String translateSetting(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String key = toKey(name);
        String val = get("setting." + key + ".name", null);
        if (val == null) {
            val = get("setting." + key.replace("_", "") + ".name", name);
        }
        return val;
    }

    public static String translateSettingDesc(String name, String defaultDesc) {
        if (name == null || name.isEmpty()) {
            return defaultDesc == null ? "" : defaultDesc;
        }
        String key = toKey(name);
        String val = get("setting." + key + ".desc", null);
        if (val == null) {
            val = get("setting." + key.replace("_", "") + ".desc", defaultDesc != null ? defaultDesc : "");
        }
        return val;
    }

    public static String translateOption(String option) {
        if (option == null || option.isEmpty()) {
            return "";
        }
        String key = toKey(option);
        String val = get("option." + key, null);
        if (val == null) {
            val = get("option." + key.replace("_", ""), option);
        }
        return val;
    }

    public static String translateModule(String name, String defaultDisplayName) {
        if (name == null || name.isEmpty()) {
            return defaultDisplayName != null ? defaultDisplayName : "";
        }
        String key = toKey(name);
        String val = get("module." + key + ".name", null);
        if (val == null) {
            val = get("module." + key.replace("_", "") + ".name", defaultDisplayName != null ? defaultDisplayName : name);
        }
        return val;
    }

    public static String translateModuleDesc(String name, String defaultDesc) {
        if (name == null || name.isEmpty()) {
            return defaultDesc != null ? defaultDesc : "";
        }
        String key = toKey(name);
        String val = get("module." + key + ".desc", null);
        if (val == null) {
            val = get("module." + key.replace("_", "") + ".desc", defaultDesc != null ? defaultDesc : "");
        }
        return val;
    }
}
