package rtx.nv.api.localization;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import rtx.nv.api.config.ConfigManager;

public final class LocalizationManager {
    public enum Language {
        RU("ru", "RU", "assets/nv/lang/ru_ru.json"),
        EN("en", "EN", "assets/nv/lang/en_us.json");

        private final String code;
        private final String label;
        private final String assetPath;

        Language(String code, String label, String assetPath) {
            this.code = code;
            this.label = label;
            this.assetPath = assetPath;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public String getAssetPath() {
            return assetPath;
        }

        public static Language fromCode(String code) {
            if (code == null) return RU;
            for (Language lang : values()) {
                if (lang.code.equalsIgnoreCase(code) || lang.name().equalsIgnoreCase(code)) {
                    return lang;
                }
            }
            return RU;
        }
    }

    private static Language currentLanguage = Language.RU;
    private static final Map<String, String> translations = new HashMap<>();
    private static final Map<String, String> fallbackTranslations = new HashMap<>();
    private static boolean initialized = false;

    private LocalizationManager() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        loadLanguage(Language.EN, fallbackTranslations);
        loadLanguage(currentLanguage, translations);
    }

    public static Language getCurrentLanguage() {
        if (!initialized) init();
        return currentLanguage;
    }

    public static void setLanguage(Language language) {
        if (language == null || language == currentLanguage) return;
        currentLanguage = language;
        translations.clear();
        loadLanguage(language, translations);
        ConfigManager.markDirty();
    }

    public static void setLanguage(String code) {
        setLanguage(Language.fromCode(code));
    }

    public static void toggleLanguage() {
        if (currentLanguage == Language.RU) {
            setLanguage(Language.EN);
        } else {
            setLanguage(Language.RU);
        }
    }

    public static String get(String key) {
        if (!initialized) init();
        String val = translations.get(key);
        if (val != null) return val;
        val = fallbackTranslations.get(key);
        if (val != null) return val;
        return key;
    }

    public static String get(String key, String defaultValue) {
        if (!initialized) init();
        String val = translations.get(key);
        if (val != null) return val;
        val = fallbackTranslations.get(key);
        if (val != null) return val;
        return defaultValue;
    }

    public static String format(String key, Object... args) {
        String template = get(key);
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }

    private static void loadLanguage(Language lang, Map<String, String> map) {
        try {
            ClassLoader cl = LocalizationManager.class.getClassLoader();
            InputStream is = cl.getResourceAsStream(lang.getAssetPath());
            if (is == null) {
                // Try leading slash
                is = cl.getResourceAsStream("/" + lang.getAssetPath());
            }
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) {
                            map.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            rtx.nv.NV.LOGGER.error("Failed to load localization", e);
        }
    }
}
