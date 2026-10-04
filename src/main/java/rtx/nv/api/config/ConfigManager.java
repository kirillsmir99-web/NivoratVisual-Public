package rtx.nv.api.config;
import rtx.nv.api.events.EventHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import rtx.nv.NV;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.module.ModuleToggleEvent;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.Module.BindMode;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.ClickGui;
import rtx.nv.api.modules.settings.Setting;
import rtx.nv.api.modules.settings.impl.BindSetting;
import rtx.nv.api.modules.settings.impl.BindSetting.Type;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.MultiSelectSetting;
import rtx.nv.api.modules.settings.impl.PositionSettings;
import rtx.nv.api.modules.settings.impl.SelectSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.api.modules.settings.impl.TextSetting;
import rtx.nv.api.ui.theme.Theme;
import rtx.nv.api.ui.theme.ThemeManager;
import rtx.nv.utils.key.KeyBind;
import rtx.nv.utils.storage.RepositoryStorage;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ConfigManager INSTANCE = new ConfigManager();
    private static final String MODULE_CONFIG = "autocfg";
    private static final String CONFIG_EXTENSION = ".nv";
    private static final long AUTOSAVE_INTERVAL_MS = 5000L;
    private boolean initialized;
    private boolean loading;
    private boolean dirty;
    private long lastAutosaveMs;
    private String lastSavedModuleConfig;
    private JsonObject activeRoot = new JsonObject();

    private ConfigManager() {
    }

    public static void init() {
        if (ConfigManager.INSTANCE.initialized) {
            return;
        }
        ConfigManager.INSTANCE.initialized = true;
        ConfigManager.ensureDefaultSystemFiles();
        ConfigManager.INSTANCE.loading = true;
        try {
            ConfigManager.loadAll();
        }
        finally {
            ConfigManager.INSTANCE.loading = false;
        }
        ConfigManager.saveModules(true);
        EventBus.get().subscribe(INSTANCE);
    }

    private static int readInt(JsonObject jsonObject, String string, int n) {
        try {
            return jsonObject.has(string) ? jsonObject.get(string).getAsInt() : n;
        }
        catch (Exception exception) {
            return n;
        }
    }

    private static void loadModules() {
        ConfigManager.applyModuleRoot(ConfigManager.readModuleConfig());
    }

    private static boolean readBoolean(JsonObject jsonObject, String string, boolean bl) {
        try {
            return jsonObject.has(string) ? jsonObject.get(string).getAsBoolean() : bl;
        }
        catch (Exception exception) {
            return bl;
        }
    }

    public static void loadAll() {
        ConfigManager.loadModules();
    }

    public static boolean isLoading() {
        return ConfigManager.INSTANCE.loading;
    }

    public static void markDirty() {
        if (ConfigManager.INSTANCE.initialized && !ConfigManager.INSTANCE.loading) {
            ConfigManager.INSTANCE.dirty = true;
        }
    }

    @EventHandler
    private void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPost()) {
            return;
        }
        if (!this.dirty) {
            return;
        }
        long l = System.currentTimeMillis();
        if (l - this.lastAutosaveMs < 5000L) {
            return;
        }
        this.lastAutosaveMs = l;
        ConfigManager.saveModules(false);
    }

    private static void applyDrags(JsonObject jsonObject) {
        if (jsonObject.has("drags") && jsonObject.get("drags").isJsonObject()) {
            DragSystem.get().applyDrags(jsonObject.getAsJsonObject("drags"));
        }
    }

    public static Path systemConfigDirectory() {
        return RepositoryStorage.root();
    }

    private static void ensureDefaultSystemFiles() {
        RepositoryStorage.ensureObject("friends", ConfigManager.objectWithArray("list"));
        RepositoryStorage.ensureObject("waypoints", ConfigManager.objectWithArray("ways"));
        RepositoryStorage.ensureObject("macros", ConfigManager.objectWithArray("macros"));
        RepositoryStorage.ensureObject("staff", ConfigManager.objectWithArray("list"));
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("prefix", ".");
        RepositoryStorage.ensureObject("prefix", jsonObject);
    }

    @EventHandler
    private void onModuleToggle(ModuleToggleEvent moduleToggleEvent) {
        this.dirty = true;
        ConfigManager.saveModules(false);
    }

    private static JsonObject objectWithArray(String string) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(string, (JsonElement)new JsonArray());
        return jsonObject;
    }

    private static JsonObject readModuleConfig() {
        JsonObject jsonObject;
        block13: {
            Object object;
            Object object2;
            Path configPath = ConfigManager.moduleConfigPath();
            if (!Files.exists(configPath)) {
                return new JsonObject();
            }
            try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                jsonObject = jsonElement != null && jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : new JsonObject();
            } catch (Exception exception) {
                NV.LOGGER.error("[ConfigManager] Failed to read module config", (Throwable)exception);
                return new JsonObject();
            }
        }
        return jsonObject;
    }

    private static void applyModuleRoot(JsonObject jsonObject) {
        JsonObject jsonObject2;
        ConfigManager.INSTANCE.activeRoot = jsonObject;
        if (jsonObject.has("theme") && jsonObject.get("theme").isJsonPrimitive()) {
            try {
                ThemeManager.setById(jsonObject.get("theme").getAsString());
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (jsonObject.has("language") && jsonObject.get("language").isJsonPrimitive()) {
            try {
                rtx.nv.api.localization.LocalizationManager.setLanguage(jsonObject.get("language").getAsString());
            } catch (Exception ignored) {}
        }
        if (jsonObject.has("lastCategory") && jsonObject.get("lastCategory").isJsonPrimitive()) {
            try {
                rtx.nv.api.ui.UI.setStartCategoryName(jsonObject.get("lastCategory").getAsString());
            } catch (Exception ignored) {}
        }
        JsonObject jsonObject3 = jsonObject.has("modules") && jsonObject.get("modules").isJsonObject() ? jsonObject.getAsJsonObject("modules") : new JsonObject();
        jsonObject3.remove("Tape Mouse");
        jsonObject3.remove("TapeMouse");
        jsonObject3.remove("Автокликер");
        jsonObject3.remove("IRC Chat");
        jsonObject3.remove("Чат IRC");
        jsonObject3.remove("Irc");
        JsonObject streamerObj = ConfigManager.findModuleObject(jsonObject3, "Streamer Mode");
        if (streamerObj != null && streamerObj.has("settings") && streamerObj.get("settings").isJsonObject()) {
            JsonObject settings = streamerObj.getAsJsonObject("settings");
            settings.remove("Кастомный ранг");
            settings.remove("Ранг ReallyWorld");
        }
        for (Module module : ModuleManager.get().getAll()) {
            jsonObject2 = ConfigManager.findModuleObject(jsonObject3, module.getName());
            if (jsonObject2 == null) continue;
            if (jsonObject2.has("bind")) {
                int code = ConfigManager.readInt(jsonObject2, "bind", KeyBind.NONE.getCode());
                if (module instanceof ClickGui && code <= 0) {
                    code = 344;
                }
                module.setBind(new KeyBind(code));
            }
            if (jsonObject2.has("bindMode")) {
                try {
                    module.setBindMode(Module.BindMode.valueOf(jsonObject2.get("bindMode").getAsString()));
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            if (!jsonObject2.has("settings") || !jsonObject2.get("settings").isJsonObject()) continue;
            ConfigManager.applySettings(module, jsonObject2.getAsJsonObject("settings"));
        }
        Iterator<Module> iterator = ModuleManager.get().getAll().iterator();
        while (iterator.hasNext()) {
            Module module;
            jsonObject2 = ConfigManager.findModuleObject(jsonObject3, (module = iterator.next()).getName());
            module.setEnabled(jsonObject2 != null ? ConfigManager.readBoolean(jsonObject2, "enabled", module.defaultEnabled()) : module.defaultEnabled());
        }
        JsonObject legacySelfTag = ConfigManager.findModuleObject(jsonObject3, "SelfTag");
        if (legacySelfTag != null) {
            boolean wasSelfTagEnabled = ConfigManager.readBoolean(legacySelfTag, "enabled", false);
            rtx.nv.api.modules.impl.Visuals.NameTags nt = rtx.nv.api.modules.impl.Visuals.NameTags.getInstance();
            if (nt != null) {
                nt.migrateLegacySelfTag(wasSelfTagEnabled);
            }
        }
        ConfigManager.applyDrags(jsonObject);
        if (jsonObject.has("pinned") && jsonObject.get("pinned").isJsonArray()) {
            List<String> list = new ArrayList<>();
            for (JsonElement el : jsonObject.getAsJsonArray("pinned")) {
                if (el.isJsonPrimitive()) {
                    list.add(el.getAsString());
                }
            }
            rtx.nv.api.ui.pin.PinManager.setPinnedNames(list);
        }
    }

    private static JsonObject buildModuleRoot() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("version", (Number)1);
        jsonObject.addProperty("theme", ThemeManager.currentThemeId());
        jsonObject.addProperty("language", rtx.nv.api.localization.LocalizationManager.getCurrentLanguage().getCode());
        jsonObject.addProperty("lastCategory", rtx.nv.api.ui.UI.getLastCategoryName());
        JsonArray pinnedArr = new JsonArray();
        for (String name : rtx.nv.api.ui.pin.PinManager.getPinnedNames()) {
            pinnedArr.add(name);
        }
        jsonObject.add("pinned", pinnedArr);
        JsonObject jsonObject2 = new JsonObject();
        for (Module module : ModuleManager.get().getAll()) {
            JsonObject jsonObject3 = new JsonObject();
            jsonObject3.addProperty("enabled", Boolean.valueOf(module.isEnabled()));
            int bindCode = module.getBind().getCode();
            if (module instanceof ClickGui && bindCode <= 0) {
                bindCode = 344;
            }
            jsonObject3.addProperty("bind", (Number)bindCode);
            jsonObject3.addProperty("bindMode", module.getBindMode().name());
            jsonObject3.add("settings", (JsonElement)ConfigManager.writeSettings(module));
            jsonObject2.add(module.getName(), (JsonElement)jsonObject3);
        }
        jsonObject.add("modules", (JsonElement)jsonObject2);
        jsonObject.add("drags", (JsonElement)ConfigManager.currentDrags());
        return jsonObject;
    }

    private static Path moduleConfigPath() {
        return RepositoryStorage.root().resolve("autocfg.nv");
    }

    private static JsonObject writeSettings(Module module) {
        JsonObject jsonObject = new JsonObject();
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        for (Setting setting : module.getSettings().all()) {
            String string = ConfigManager.occurrenceKey(hashMap, setting.getName());
            JsonElement jsonElement = ConfigManager.serializeSetting(setting);
            if (jsonElement == null) continue;
            jsonObject.add(string, jsonElement);
        }
        return jsonObject;
    }

    private static JsonObject currentDrags() {
        JsonObject jsonObject = DragSystem.get().writeDrags();
        if (!jsonObject.entrySet().isEmpty()) {
            return jsonObject;
        }
        if (ConfigManager.INSTANCE.activeRoot.has("drags") && ConfigManager.INSTANCE.activeRoot.get("drags").isJsonObject()) {
            return ConfigManager.INSTANCE.activeRoot.getAsJsonObject("drags").deepCopy();
        }
        return jsonObject;
    }

    private static JsonObject findModuleObject(JsonObject jsonObject, String string) {
        if ("Crown".equalsIgnoreCase(string) && !jsonObject.has("Crown")) {
            JsonObject legacy = findModuleObject(jsonObject, "China Hat");
            if (legacy != null) return legacy;
        }
        if ("ProjectileHelper".equalsIgnoreCase(string) && !jsonObject.has("ProjectileHelper")) {
            JsonObject legacy = findModuleObject(jsonObject, "Predict");
            if (legacy == null) legacy = findModuleObject(jsonObject, "Predictions");
            if (legacy != null) return legacy;
        }
        if (jsonObject.has(string) && jsonObject.get(string).isJsonObject()) {
            return jsonObject.getAsJsonObject(string);
        }
        for (String string2 : jsonObject.keySet()) {
            if (!string2.equalsIgnoreCase(string) || !jsonObject.get(string2).isJsonObject()) continue;
            return jsonObject.getAsJsonObject(string2);
        }
        return null;
    }

    private static void applySettings(Module module, JsonObject jsonObject) {
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        for (Setting setting : module.getSettings().all()) {
            String string = ConfigManager.occurrenceKey(hashMap, setting.getName());
            JsonElement jsonElement = jsonObject.has(string) ? jsonObject.get(string) : ConfigManager.findSettingValue(jsonObject, setting.getName());
            if (jsonElement == null || jsonElement.isJsonNull()) continue;
            ConfigManager.applySetting(setting, jsonElement);
        }
    }

    public static void applyActiveDrags() {
        ConfigManager.INSTANCE.loading = true;
        try {
            ConfigManager.applyDrags(ConfigManager.INSTANCE.activeRoot);
        }
        finally {
            ConfigManager.INSTANCE.loading = false;
        }
    }

    private static void saveModules(boolean bl) {
        if (!bl && !ConfigManager.INSTANCE.dirty) {
            return;
        }
        try {
            Files.createDirectories(RepositoryStorage.root(), new FileAttribute[0]);
            JsonObject jsonObject = ConfigManager.buildModuleRoot();
            String string = GSON.toJson((JsonElement)jsonObject);
            if (!bl && string.equals(ConfigManager.INSTANCE.lastSavedModuleConfig)) {
                ConfigManager.INSTANCE.dirty = false;
                return;
            }
            rtx.nv.utils.storage.AtomicFiles.writeUtf8(ConfigManager.moduleConfigPath(), string);
            ConfigManager.INSTANCE.lastSavedModuleConfig = string;
            ConfigManager.INSTANCE.dirty = false;
        }
        catch (Exception exception) {
            NV.LOGGER.error("[ConfigManager] Failed to save module config", (Throwable)exception);
        }
    }

    public static List<String> listProfiles() {
        Path path2 = ConfigManager.profilesDirectory();
        if (!Files.exists(path2, new LinkOption[0])) {
            return new ArrayList<String>();
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        try (Stream<Path> stream = Files.list(path2);){
            stream.forEach(path -> {
                String string = path.getFileName().toString();
                if (string.endsWith(CONFIG_EXTENSION)) {
                    arrayList.add(string.substring(0, string.length() - CONFIG_EXTENSION.length()));
                }
            });
        }
        catch (Exception exception) {
            NV.LOGGER.error("[ConfigManager] Failed to list profiles", (Throwable)exception);
        }
        arrayList.sort(String.CASE_INSENSITIVE_ORDER);
        return arrayList;
    }

    public static Path profilesDirectory() {
        return RepositoryStorage.root().resolve("profiles");
    }

    public static void saveProfile(String string) {
        String string2 = ConfigManager.sanitizeProfileName(string);
        if (string2.isEmpty()) {
            return;
        }
        try {
            Path path = ConfigManager.profilesDirectory();
            Files.createDirectories(path, new FileAttribute[0]);
            rtx.nv.utils.storage.AtomicFiles.writeUtf8(path.resolve(string2 + CONFIG_EXTENSION), GSON.toJson((JsonElement)ConfigManager.buildModuleRoot()));
        }
        catch (Exception exception) {
            NV.LOGGER.error("[ConfigManager] Failed to save profile {}", (Object)string, (Object)exception);
        }
    }

    private static String sanitizeProfileName(String string) {
        if (string == null) {
            return "";
        }
        return string.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static Path profilePath(String string) {
        return ConfigManager.profilesDirectory().resolve(ConfigManager.sanitizeProfileName(string) + CONFIG_EXTENSION);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void loadProfile(String string) {
        Path path = ConfigManager.profilePath(string);
        if (!Files.exists(path, new LinkOption[0])) {
            return;
        }
        ConfigManager.INSTANCE.loading = true;
        try (BufferedReader bufferedReader = Files.newBufferedReader(path, StandardCharsets.UTF_8);){
            JsonElement jsonElement = JsonParser.parseReader((Reader)bufferedReader);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                ConfigManager.applyModuleRoot(jsonElement.getAsJsonObject());
            }
        }
        catch (Exception exception) {
            NV.LOGGER.error("[ConfigManager] Failed to load profile {}", (Object)string, (Object)exception);
        }
        finally {
            ConfigManager.INSTANCE.loading = false;
        }
        ConfigManager.saveModules(true);
    }

    public static Path userConfigDirectory() {
        return RepositoryStorage.configRoot();
    }

    private static void resetSetting(Setting setting) {
        try {
            if (setting instanceof BooleanSetting) {
                BooleanSetting booleanSetting = (BooleanSetting)setting;
                booleanSetting.setValue(booleanSetting.getDefaultValue());
            } else if (setting instanceof SliderSetting) {
                SliderSetting sliderSetting = (SliderSetting)setting;
                sliderSetting.setValue(sliderSetting.getDefaultValue());
            } else if (setting instanceof SelectSetting) {
                SelectSetting selectSetting = (SelectSetting)setting;
                selectSetting.setSelected(selectSetting.getDefaultSelected());
            } else if (setting instanceof MultiSelectSetting) {
                MultiSelectSetting multiSelectSetting = (MultiSelectSetting)setting;
                multiSelectSetting.selected((String[])multiSelectSetting.getDefaultSelected().toArray(String[]::new));
            } else if (setting instanceof ColorSetting) {
                ColorSetting colorSetting = (ColorSetting)setting;
                colorSetting.setColor(colorSetting.getDefaultColor());
            } else if (setting instanceof TextSetting) {
                TextSetting textSetting = (TextSetting)setting;
                textSetting.setText(textSetting.getDefaultText());
            } else if (setting instanceof BindSetting) {
                BindSetting bindSetting = (BindSetting)setting;
                bindSetting.setKey(bindSetting.getDefaultKey());
            } else if (setting instanceof PositionSettings) {
                PositionSettings positionSettings = (PositionSettings)setting;
                if (positionSettings.isDefaultXCaptured()) {
                    positionSettings.setX(positionSettings.getDefaultX());
                }
                if (positionSettings.isDefaultYCaptured()) {
                    positionSettings.setY(positionSettings.getDefaultY());
                }
                if (positionSettings.isDefaultZCaptured()) {
                    positionSettings.setZ(positionSettings.getDefaultZ());
                }
            }
        }
        catch (Exception exception) {
            NV.LOGGER.warn("[ConfigManager] Failed to reset setting {}", (Object)setting.getName(), (Object)exception);
        }
    }

    private static String occurrenceKey(Map<String, Integer> map, String string) {
        int n = map.merge(string, 1, Integer::sum);
        return n == 1 ? string : string + "#" + n;
    }

    public static void deleteProfile(String string) {
        try {
            Files.deleteIfExists(ConfigManager.profilePath(string));
        }
        catch (Exception exception) {
            NV.LOGGER.error("[ConfigManager] Failed to delete profile {}", (Object)string, (Object)exception);
        }
    }

    private static void applySetting(Setting setting, JsonElement jsonElement) {
        try {
            Setting setting2;
            if (setting instanceof BooleanSetting) {
                setting2 = (BooleanSetting)setting;
                if (jsonElement.isJsonPrimitive()) {
                    ((BooleanSetting)setting2).setValue(jsonElement.getAsBoolean());
                    return;
                }
            }
            if (setting instanceof SliderSetting) {
                setting2 = (SliderSetting)setting;
                if (jsonElement.isJsonPrimitive()) {
                    ((SliderSetting)setting2).setValue(jsonElement.getAsFloat());
                    return;
                }
            }
            if (setting instanceof PositionSettings posSetting) {
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    if (jsonObject.has("x")) {
                        posSetting.setX(jsonObject.get("x").getAsFloat());
                    }
                    if (jsonObject.has("y")) {
                        posSetting.setY(jsonObject.get("y").getAsFloat());
                    }
                    if (jsonObject.has("z")) {
                        posSetting.setZ(jsonObject.get("z").getAsFloat());
                    }
                    return;
                }
            }
            if (setting instanceof SelectSetting) {
                setting2 = (SelectSetting)setting;
                if (jsonElement.isJsonPrimitive()) {
                    String val = jsonElement.getAsString();
                    if ("Осколки".equalsIgnoreCase(val) || "Обычный".equalsIgnoreCase(val)) {
                        if (((SelectSetting) setting2).getOptions().contains("Лента")) {
                            val = "Лента";
                        }
                    } else if ("Радуга".equalsIgnoreCase(val) || "Rainbow".equalsIgnoreCase(val) || "Спектр".equalsIgnoreCase(val)) {
                        if (((SelectSetting) setting2).getOptions().contains("Палитра")) {
                            val = "Палитра";
                        } else if (((SelectSetting) setting2).getOptions().contains("Клиент")) {
                            val = "Клиент";
                        }
                    } else if ("Классический".equalsIgnoreCase(val)) {
                        if (((SelectSetting) setting2).getOptions().contains("Карточка")) {
                            val = "Карточка";
                        }
                    } else if ("Новый".equalsIgnoreCase(val)) {
                        if (((SelectSetting) setting2).getOptions().contains("Панель")) {
                            val = "Панель";
                        }
                    }
                    ((SelectSetting)setting2).setSelected(val);
                    return;
                }
            }
            if (setting instanceof MultiSelectSetting) {
                setting2 = (MultiSelectSetting)setting;
                if (jsonElement.isJsonArray()) {
                    ArrayList<String> arrayList = new ArrayList<String>();
                    for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
                        if (!jsonElement2.isJsonPrimitive()) continue;
                        String string = jsonElement2.getAsString();
                        if (setting2 instanceof rtx.nv.api.modules.settings.impl.CategoryFilterSetting filter) {
                            string = filter.resolveOption(string);
                        }
                        if (!((MultiSelectSetting)setting2).getOptions().contains(string)) continue;
                        arrayList.add(string);
                    }
                    if (setting2 instanceof rtx.nv.api.modules.settings.impl.CategoryFilterSetting
                        && !jsonElement.getAsJsonArray().isEmpty() && arrayList.isEmpty()) return;
                    if (arrayList.size() >= ((MultiSelectSetting)setting2).getMinSelectedCount()) {
                        ((MultiSelectSetting)setting2).selected((String[])arrayList.toArray(String[]::new));
                    }
                    return;
                }
            }
            if (setting instanceof BindSetting) {
                setting2 = (BindSetting)setting;
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    ((BindSetting)setting2).setKey(ConfigManager.readInt(jsonObject, "key", ((BindSetting)setting2).getKey()));
                    if (jsonObject.has("type")) {
                        ((BindSetting)setting2).setType(BindSetting.Type.valueOf(jsonObject.get("type").getAsString()));
                    }
                    return;
                }
            }
            if (setting instanceof ColorSetting) {
                setting2 = (ColorSetting)setting;
                if (jsonElement.isJsonPrimitive()) {
                    ((ColorSetting)setting2).setColor(jsonElement.getAsInt());
                    return;
                }
            }
            if (setting instanceof TextSetting) {
                setting2 = (TextSetting)setting;
                if (jsonElement.isJsonPrimitive()) {
                    ((TextSetting)setting2).setText(jsonElement.getAsString());
                }
            }
        }
        catch (Exception exception) {
            NV.LOGGER.warn("[ConfigManager] Failed to apply setting {}", (Object)setting.getName(), (Object)exception);
        }
    }

    private static JsonElement serializeSetting(Setting setting) {
        if (setting instanceof BooleanSetting) {
            BooleanSetting booleanSetting = (BooleanSetting)setting;
            return GSON.toJsonTree((Object)booleanSetting.getValue());
        }
        if (setting instanceof SliderSetting) {
            SliderSetting sliderSetting = (SliderSetting)setting;
            return GSON.toJsonTree((Object)Float.valueOf(sliderSetting.getFloat()));
        }
        if (setting instanceof PositionSettings) {
            PositionSettings positionSettings = (PositionSettings)setting;
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("x", (Number)Float.valueOf(positionSettings.getX()));
            jsonObject.addProperty("y", (Number)Float.valueOf(positionSettings.getY()));
            jsonObject.addProperty("z", (Number)Float.valueOf(positionSettings.getZ()));
            return jsonObject;
        }
        if (setting instanceof SelectSetting) {
            SelectSetting selectSetting = (SelectSetting)setting;
            return GSON.toJsonTree((Object)selectSetting.getValue());
        }
        if (setting instanceof MultiSelectSetting) {
            MultiSelectSetting multiSelectSetting = (MultiSelectSetting)setting;
            JsonArray jsonArray = new JsonArray();
            for (String string : multiSelectSetting.getSelected()) {
                jsonArray.add(string);
            }
            return jsonArray;
        }
        if (setting instanceof BindSetting) {
            BindSetting bindSetting = (BindSetting)setting;
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("key", (Number)bindSetting.getKey());
            jsonObject.addProperty("type", bindSetting.getType().name());
            return jsonObject;
        }
        if (setting instanceof ColorSetting) {
            ColorSetting colorSetting = (ColorSetting)setting;
            return GSON.toJsonTree((Object)colorSetting.getColor());
        }
        if (setting instanceof TextSetting) {
            TextSetting textSetting = (TextSetting)setting;
            return GSON.toJsonTree((Object)textSetting.getText());
        }
        return null;
    }

    private static JsonElement findSettingValue(JsonObject jsonObject, String string) {
        if (jsonObject.has(string)) {
            return jsonObject.get(string);
        }
        for (String string2 : jsonObject.keySet()) {
            if (!string2.equalsIgnoreCase(string)) continue;
            return jsonObject.get(string2);
        }
        return null;
    }

    public static String nextProfileName() {
        List<String> list = ConfigManager.listProfiles();
        int n = 1;
        while (true) {
            String string = "Config " + n;
            if (list.stream().noneMatch(string::equalsIgnoreCase)) {
                return string;
            }
            ++n;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void resetToFactory() {
        ConfigManager.INSTANCE.loading = true;
        try {
            ThemeManager.set(Theme.NIVORA);
            for (Module object : ModuleManager.get().getAll()) {
                for (Setting setting : object.getSettings().all()) {
                    ConfigManager.resetSetting(setting);
                }
                if (object instanceof ClickGui) {
                    object.setBind(KeyBind.keyboard(344));
                } else {
                    object.setBind(KeyBind.NONE);
                }
                object.setEnabled(object.defaultEnabled());
            }
            for (Draggable draggable : DragSystem.get().getAll()) {
                draggable.resetToDefault();
            }
        }
        finally {
            ConfigManager.INSTANCE.loading = false;
        }
        try {
            Files.deleteIfExists(ConfigManager.moduleConfigPath());
        }
        catch (Exception exception) {
            NV.LOGGER.error("[ConfigManager] Failed to delete active config during reset", (Throwable)exception);
        }
        ConfigManager.saveModules(true);
    }

    public static boolean profileExists(String string) {
        return Files.exists(ConfigManager.profilePath(string), new LinkOption[0]);
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static int profileEnabledCount(String string) {
        Path path = ConfigManager.profilePath(string);
        if (!Files.exists(path, new LinkOption[0])) {
            return 0;
        }
        try (BufferedReader bufferedReader = Files.newBufferedReader(path, StandardCharsets.UTF_8);){
            JsonElement jsonElement = JsonParser.parseReader((Reader)bufferedReader);
            if (jsonElement == null || !jsonElement.isJsonObject()) {
                int n = 0;
                return n;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (!jsonObject.has("modules") || !jsonObject.get("modules").isJsonObject()) {
                int n = 0;
                return n;
            }
            JsonObject jsonObject2 = jsonObject.getAsJsonObject("modules");
            int n = 0;
            for (String string2 : jsonObject2.keySet()) {
                if (!jsonObject2.get(string2).isJsonObject() || !ConfigManager.readBoolean(jsonObject2.getAsJsonObject(string2), "enabled", false)) continue;
                ++n;
            }
            int n2 = n;
            return n2;
        }
        catch (Exception exception) {
            return 0;
        }
    }

    public static void saveAll() {
        ConfigManager.saveModules(true);
    }
}

