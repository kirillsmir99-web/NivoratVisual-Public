package rtx.nv.api.modules;
import net.minecraft.client.MinecraftClient;
import rtx.nv.api.config.ConfigManager;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.module.ModuleToggleEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.restrict.ServerRestrictions;
import rtx.nv.api.modules.settings.Setting;
import rtx.nv.api.modules.settings.SettingRepository;
import rtx.nv.utils.key.KeyBind;

public abstract class Module {
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    private final String name;
    private final String description;
    private final Category category;
    private final SettingRepository settings = new SettingRepository();
    private boolean enabled = false;
    private KeyBind bind = KeyBind.NONE;
    private Module.BindMode bindMode = Module.BindMode.TOGGLE;

    protected Module(String string, String string2, Category category) {
        this.name = string;
        this.description = string2;
        this.category = category;
    }

    public String toString() {
        return this.name + " [" + String.valueOf((Object)this.category) + "] " + (this.enabled ? "ON" : "OFF");
    }

    public String getName() {
        return this.name;
    }

    protected final <T extends Setting> T register(T t) {
        t.setChangeListener(ConfigManager::markDirty);
        this.settings.add(t);
        return t;
    }

    protected final void register(Setting ... settingArray) {
        for (Setting setting : settingArray) {
            setting.setChangeListener(ConfigManager::markDirty);
        }
        this.settings.add(settingArray);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void enable() {
        if (this.enabled) {
            return;
        }
        String string = ServerRestrictions.blockReason(this, ServerRestrictions.current());
        if (string != null) {
            ServerRestrictions.notify(string);
            return;
        }
        this.enabled = true;
        try {
            EventBus.get().subscribe(this);
            this.onEnable();
        } catch (RuntimeException | LinkageError failure) {
            this.enabled = false;
            try {
                this.onDisable();
            } catch (RuntimeException | LinkageError cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            } finally {
                EventBus.get().unsubscribe(this);
            }
            rtx.nv.NV.LOGGER.error("[Module] Failed to enable {}", this.name, failure);
            return;
        }
        EventBus.get().post(new ModuleToggleEvent(this, true));
    }

    public String getDisplayName() {
        return rtx.nv.api.localization.Lang.translateModule(this.name, this.getDefaultDisplayName());
    }

    public String getDefaultDisplayName() {
        StringBuilder stringBuilder = new StringBuilder(this.name.length() + 4);
        char[] cArray = this.name.toCharArray();
        for (int i = 0; i < cArray.length; ++i) {
            char c = cArray[i];
            if (i > 0 && Character.isUpperCase(c)) {
                boolean bl;
                char c2 = cArray[i - 1];
                char c3 = i + 1 < cArray.length ? cArray[i + 1] : (char)'\u0000';
                boolean bl2 = Character.isLowerCase(c2) || Character.isDigit(c2);
                boolean bl3 = bl = Character.isUpperCase(c2) && Character.isLowerCase(c3);
                if (c2 != ' ' && (bl2 || bl)) {
                    stringBuilder.append(' ');
                }
            }
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

    public String getDescription() {
        return rtx.nv.api.localization.Lang.translateModuleDesc(this.name, this.description);
    }

    public void toggle() {
        if (this.enabled) {
            this.disable();
        } else {
            this.enable();
        }
    }

    public KeyBind getBind() {
        return this.bind;
    }

    protected void onDisable() {
    }

    public void setBind(KeyBind keyBind) {
        KeyBind keyBind2;
        KeyBind keyBind3 = keyBind2 = keyBind == null ? KeyBind.NONE : keyBind;
        if (this.bind.getCode() != keyBind2.getCode()) {
            this.bind = keyBind2;
            ConfigManager.markDirty();
            return;
        }
        this.bind = keyBind2;
    }

    protected void onEnable() {
    }

    public SettingRepository getSettings() {
        return this.settings;
    }

    public Module.BindMode getBindMode() {
        return this.bindMode;
    }

    public Module.BindMode bindMode() {
        return this.bindMode;
    }

    public boolean defaultEnabled() {
        return false;
    }

    public void setBindMode(Module.BindMode bindMode) {
        Module.BindMode targetMode = bindMode == null ? Module.BindMode.TOGGLE : bindMode;
        if (this.bindMode != targetMode) {
            this.bindMode = targetMode;
            ConfigManager.markDirty();
        }
    }

    public void disable() {
        if (!this.enabled) {
            return;
        }
        this.enabled = false;
        try {
            this.onDisable();
        } catch (RuntimeException | LinkageError failure) {
            rtx.nv.NV.LOGGER.error("[Module] Failed to disable {}", this.name, failure);
        } finally {
            EventBus.get().unsubscribe(this);
            EventBus.get().post(new ModuleToggleEvent(this, false));
        }
    }

    public Category getCategory() {
        return this.category;
    }

    public void setEnabled(boolean bl) {
        if (bl) {
            this.enable();
        } else {
            this.disable();
        }
    }


    public static enum BindMode {
        TOGGLE,
        HOLD;
    
    }
}

