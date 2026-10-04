package rtx.nv.api.modules.settings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import rtx.nv.api.modules.settings.Setting;

public final class SettingRepository {
    private final List<Setting> settings = new ArrayList<Setting>();

    public int size() {
        return this.settings.size();
    }

    public boolean isEmpty() {
        return this.settings.isEmpty();
    }

    public void add(Setting ... settingArray) {
        this.settings.addAll(Arrays.asList(settingArray));
    }

    public Setting get(String string) {
        for (Setting setting : this.settings) {
            if (!setting.getName().equalsIgnoreCase(string)) continue;
            return setting;
        }
        return null;
    }

    public <T extends Setting> T get(String string, Class<T> clazz) {
        Setting setting = this.get(string);
        return (T)(clazz.isInstance(setting) ? setting : null);
    }

    public List<Setting> all() {
        return Collections.unmodifiableList(this.settings);
    }
}

