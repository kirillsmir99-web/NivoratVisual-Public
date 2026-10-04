package rtx.nv.api.modules.impl.Utils;

import net.minecraft.sound.SoundEvent;
import rtx.nv.api.config.ConfigManager;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.module.ModuleToggleEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.utils.sounds.SoundManager;

public final class ClientSounds extends Module {
    private static ClientSounds instance;
    private static long lastSliderMs = 0L;
    private static long lastTypingMs = 0L;

    // 1. Громкость
    private final SeparatorSetting volumeSeparator = this.register(new SeparatorSetting("Громкость"));
    private final NumberSetting volume = this.register(new NumberSetting(
        "Общая громкость",
        "Общая громкость всех звуков клиента.",
        1.0, 0.0, 1.0, 0.05
    ));
    public final NumberSetting interfaceVolume = this.register(new NumberSetting(
        "Громкость интерфейса",
        "Громкость звуков меню, ползунков и UI.",
        1.0, 0.0, 1.0, 0.05
    ));
    private final NumberSetting chatVolume = this.register(new NumberSetting(
        "Громкость чата",
        "Громкость звуков чата и команд.",
        1.0, 0.0, 1.0, 0.05
    ));
    private final NumberSetting pitch = this.register(new NumberSetting(
        "Высота тона",
        "Высота тона звука переключения модулей.",
        1.0, 0.5, 2.0, 0.05
    ));

    // 2. События
    private final SeparatorSetting eventsSeparator = this.register(new SeparatorSetting("События"));
    private final BooleanSetting moduleToggleSound = this.register(new BooleanSetting(
        "Вкл/выкл модулей",
        "Звук включения и выключения модулей.",
        true
    ));
    public final BooleanSetting guiSound = this.register(new BooleanSetting(
        "Открытие и закрытие меню",
        "Звук открытия и закрытия меню.",
        true
    ));
    private final BooleanSetting sliderSound = this.register(new BooleanSetting(
        "Ползунок",
        "Звук перемещения ползунка.",
        true
    ));
    private final BooleanSetting categorySound = this.register(new BooleanSetting(
        "Смена категории",
        "Звук переключения категории.",
        true
    ));
    public final BooleanSetting themeSwitchSound = this.register(new BooleanSetting(
        "Смена темы",
        "Звук успешного переключения цветовой темы.",
        true
    ));
    private final BooleanSetting moduleSettingsSound = this.register(new BooleanSetting(
        "Настройки модуля",
        "Звук открытия и закрытия настроек модуля.",
        true
    ));
    private final BooleanSetting dropdownSound = this.register(new BooleanSetting(
        "Выпадающие списки",
        "Звук открытия и закрытия выпадающих списков.",
        true
    ));
    private final BooleanSetting searchTypingSound = this.register(new BooleanSetting(
        "Ввод в поиске",
        "Звук ввода текста в поле поиска.",
        true
    ));
    private final BooleanSetting commandErrorSound = this.register(new BooleanSetting(
        "Ошибки команд",
        "Звук ошибки или неизвестной команды.",
        true
    ));

    // 3. Набор звуков
    private final SeparatorSetting themeSeparator = this.register(new SeparatorSetting("Звуковой набор"));
    private final ModeSetting soundType = this.register(new ModeSetting(
        "Тема звуков",
        "Общий звуковой профиль клиента.",
        "Стекло",
        "Стекло", "Serene", "Лаунчер", "Пузыри", "Киберпанк"
    ));
    private final ModeSetting toggleSoundChoice = this.register(new ModeSetting(
        "Звук вкл/выкл",
        "Выбор звука включения и выключения модулей.",
        "По теме",
        "По теме", "Стекло", "Serene", "Лаунчер", "Пузыри", "Киберпанк"
    ));
    private final ModeSetting guiOpenSoundChoice = this.register(new ModeSetting(
        "Звук меню",
        "Выбор звука открытия и закрытия меню.",
        "По теме",
        "По теме", "Стекло", "Serene", "Лаунчер", "Пузыри", "Киберпанк"
    ));
    private final ModeSetting sliderSoundChoice = this.register(new ModeSetting(
        "Звук ползунка",
        "Выбор звука перемещения ползунков.",
        "По теме",
        "По теме", "Стекло", "Serene", "Лаунчер", "Пузыри", "Киберпанк"
    ));
    private final ModeSetting categorySoundChoice = this.register(new ModeSetting(
        "Звук категорий",
        "Выбор звука смены категории.",
        "По теме",
        "По теме", "Стекло", "Serene", "Лаунчер", "Пузыри", "Киберпанк"
    ));

    public ClientSounds() {
        super("Client Sounds", "Звуки клиента для действий модулей и меню.", Category.DISPLAY);
        instance = this;

        this.soundType.setChangeListener(() -> {
            if (!ConfigManager.isLoading()) {
                SoundEvent event = this.getToggleSoundEvent(true);
                SoundManager.playSoundDirect(event, this.volume.getFloat(), this.pitch.getFloat());
            }
        });
        this.sliderSoundChoice.setChangeListener(() -> {
            if (!ConfigManager.isLoading()) {
                SoundEvent event = this.resolveSliderSound();
                SoundManager.playSoundDirect(event, this.getInterfaceVolume(), 1.0f);
            }
        });
        this.toggleSoundChoice.setChangeListener(() -> {
            if (!ConfigManager.isLoading()) {
                SoundEvent event = this.getToggleSoundEvent(true);
                SoundManager.playSoundDirect(event, this.volume.getFloat(), this.pitch.getFloat());
            }
        });
        this.guiOpenSoundChoice.setChangeListener(() -> {
            if (!ConfigManager.isLoading()) {
                SoundEvent event = this.resolveGuiOpenSound();
                SoundManager.playSoundDirect(event, this.getInterfaceVolume(), 1.0f);
            }
        });
        this.categorySoundChoice.setChangeListener(() -> {
            if (!ConfigManager.isLoading()) {
                SoundEvent event = this.resolveCategorySound();
                SoundManager.playSoundDirect(event, this.getInterfaceVolume(), 1.0f);
            }
        });
    }

    public static ClientSounds getInstance() {
        return instance;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @EventHandler
    private void onModuleToggle(ModuleToggleEvent moduleToggleEvent) {
        if (ConfigManager.isLoading()) {
            return;
        }
        if (moduleToggleEvent.getModule() == this) {
            return;
        }
        this.playToggleSound(moduleToggleEvent.isEnabled());
    }

    public float getMasterVolume() {
        return this.volume.getFloat();
    }

    public float getInterfaceVolume() {
        return this.interfaceVolume.getFloat() * this.volume.getFloat();
    }

    public float getChatVolume() {
        return this.chatVolume.getFloat() * this.volume.getFloat();
    }

    public float getVolumeFor(String string) {
        if (string == null) {
            return this.getInterfaceVolume();
        }
        return switch (string) {
            case "gui_open", "gui_close", "select_category", "module_settings_open", "module_settings_close",
                 "settings_open", "settings_close", "slider", "search_typing", "buttonclick", "button_click",
                 "pin", "unpin", "theme_switch" -> this.getInterfaceVolume();
            case "command_error" -> this.getChatVolume();
            default -> this.getMasterVolume();
        };
    }

    public SoundEvent getToggleSoundEvent(boolean enabled) {
        String choice = this.toggleSoundChoice.getSelected();
        String theme = "По теме".equals(choice) ? this.soundType.getSelected() : choice;
        return switch (theme) {
            case "Стекло", "NV · Стекло", "NV стекло" -> enabled ? SoundManager.NV_GLASS_TOGGLE_ON : SoundManager.NV_GLASS_TOGGLE_OFF;
            case "Serene" -> enabled ? SoundManager.SERENE_TOGGLE_ON : SoundManager.SERENE_TOGGLE_OFF;
            case "Лаунчер" -> enabled ? SoundManager.LAUNCHER_TOGGLE_ON : SoundManager.LAUNCHER_TOGGLE_OFF;
            case "Пузыри" -> enabled ? SoundManager.BUBBLE_TOGGLE_ON : SoundManager.BUBBLE_TOGGLE_OFF;
            case "Киберпанк" -> enabled ? SoundManager.CYBER_TOGGLE_ON : SoundManager.CYBER_TOGGLE_OFF;
            default -> enabled ? SoundManager.NV_GLASS_TOGGLE_ON : SoundManager.NV_GLASS_TOGGLE_OFF;
        };
    }

    private void playToggleSound(boolean bl) {
        if (!this.isEnabled() || !this.moduleToggleSound.getValue()) {
            return;
        }
        float f = this.volume.getFloat();
        float f2 = this.pitch.getFloat();
        SoundEvent event = this.getToggleSoundEvent(bl);
        SoundManager.playSoundDirect(event, f, f2);
    }

    public SoundEvent resolveSliderSound() {
        String choice = this.sliderSoundChoice.getSelected();
        String theme = "По теме".equals(choice) ? this.soundType.getSelected() : choice;
        return switch (theme) {
            case "Стекло", "NV · Стекло", "NV стекло" -> SoundManager.NV_GLASS_SLIDER;
            case "Serene" -> SoundManager.SERENE_SLIDER;
            case "Лаунчер" -> SoundManager.LAUNCHER_SLIDER;
            case "Пузыри" -> SoundManager.BUBBLE_SLIDER;
            case "Киберпанк" -> SoundManager.CYBER_SLIDER;
            default -> SoundManager.NV_GLASS_SLIDER;
        };
    }

    public SoundEvent resolveGuiOpenSound() {
        String choice = this.guiOpenSoundChoice.getSelected();
        String theme = "По теме".equals(choice) ? this.soundType.getSelected() : choice;
        return switch (theme) {
            case "Стекло", "NV · Стекло", "NV стекло" -> SoundManager.NV_GLASS_OPEN;
            case "Serene" -> SoundManager.SERENE_OPEN;
            case "Лаунчер" -> SoundManager.LAUNCHER_OPEN;
            case "Пузыри" -> SoundManager.BUBBLE_OPEN;
            case "Киберпанк" -> SoundManager.CYBER_OPEN;
            default -> SoundManager.NV_GLASS_OPEN;
        };
    }

    public SoundEvent resolveGuiCloseSound() {
        String choice = this.guiOpenSoundChoice.getSelected();
        String theme = "По теме".equals(choice) ? this.soundType.getSelected() : choice;
        return switch (theme) {
            case "Стекло", "NV · Стекло", "NV стекло" -> SoundManager.NV_GLASS_CLOSE;
            case "Serene" -> SoundManager.SERENE_CLOSE;
            case "Лаунчер" -> SoundManager.LAUNCHER_CLOSE;
            case "Пузыри" -> SoundManager.BUBBLE_CLOSE;
            case "Киберпанк" -> SoundManager.CYBER_CLOSE;
            default -> SoundManager.NV_GLASS_CLOSE;
        };
    }

    public SoundEvent resolveCategorySound() {
        String choice = this.categorySoundChoice.getSelected();
        String theme = "По теме".equals(choice) ? this.soundType.getSelected() : choice;
        return switch (theme) {
            case "Стекло", "NV · Стекло", "NV стекло" -> SoundManager.NV_GLASS_CATEGORY;
            case "Serene" -> SoundManager.SERENE_CATEGORY;
            case "Лаунчер" -> SoundManager.LAUNCHER_CATEGORY;
            case "Пузыри" -> SoundManager.BUBBLE_CATEGORY;
            case "Киберпанк" -> SoundManager.CYBER_CATEGORY;
            default -> SoundManager.NV_GLASS_CATEGORY;
        };
    }

    public void playThemeSwitchSound() {
        if (!this.isEnabled() || !this.themeSwitchSound.getValue()) {
            return;
        }
        SoundManager.playSoundDirect(SoundManager.NV_GLASS_SLIDER, this.getInterfaceVolume(), 1.25f);
    }

    public SoundEvent resolveSoundEvent(String string, SoundEvent defaultEvent) {
        if (string == null) {
            return defaultEvent;
        }
        return switch (string) {
            case "command_error" -> this.soundType.is("Стекло") ? SoundManager.NV_GLASS_ERROR : defaultEvent;
            case "slider" -> this.resolveSliderSound();
            case "gui_open" -> this.resolveGuiOpenSound();
            case "gui_close" -> this.resolveGuiCloseSound();
            case "select_category" -> this.resolveCategorySound();
            case "settings_open" -> switch (this.soundType.getSelected()) {
                case "Стекло", "NV · Стекло", "NV стекло" -> SoundManager.NV_GLASS_DROPDOWN_OPEN;
                case "Serene" -> SoundManager.SERENE_DROPDOWN_OPEN;
                case "Лаунчер" -> SoundManager.LAUNCHER_DROPDOWN_OPEN;
                case "Пузыри" -> SoundManager.BUBBLE_OPEN;
                case "Киберпанк" -> SoundManager.CYBER_BUTTON;
                default -> SoundManager.NV_GLASS_DROPDOWN_OPEN;
            };
            case "settings_close" -> switch (this.soundType.getSelected()) {
                case "Стекло", "NV · Стекло", "NV стекло" -> SoundManager.NV_GLASS_DROPDOWN_CLOSE;
                case "Serene" -> SoundManager.SERENE_DROPDOWN_CLOSE;
                case "Лаунчер" -> SoundManager.LAUNCHER_DROPDOWN_CLOSE;
                case "Пузыри" -> SoundManager.BUBBLE_CLOSE;
                case "Киберпанк" -> SoundManager.CYBER_BUTTON;
                default -> SoundManager.NV_GLASS_DROPDOWN_CLOSE;
            };
            case "module_settings_open" -> switch (this.soundType.getSelected()) {
                case "Стекло", "NV · Стекло", "NV стекло" -> SoundManager.NV_GLASS_OPEN;
                case "Serene" -> SoundManager.SERENE_OPEN;
                case "Лаунчер" -> SoundManager.LAUNCHER_DROPDOWN_OPEN;
                case "Пузыри" -> SoundManager.BUBBLE_OPEN;
                case "Киберпанк" -> SoundManager.CYBER_OPEN;
                default -> SoundManager.NV_GLASS_OPEN;
            };
            case "module_settings_close" -> switch (this.soundType.getSelected()) {
                case "Стекло", "NV · Стекло", "NV стекло" -> SoundManager.NV_GLASS_CLOSE;
                case "Serene" -> SoundManager.SERENE_CLOSE;
                case "Лаунчер" -> SoundManager.LAUNCHER_DROPDOWN_CLOSE;
                case "Пузыри" -> SoundManager.BUBBLE_CLOSE;
                case "Киберпанк" -> SoundManager.CYBER_CLOSE;
                default -> SoundManager.NV_GLASS_CLOSE;
            };
            case "buttonclick", "button_click" -> switch (this.soundType.getSelected()) {
                case "Стекло", "NV · Стекло", "NV стекло" -> SoundManager.NV_GLASS_BUTTON;
                case "Serene" -> SoundManager.SERENE_BUTTON;
                case "Лаунчер" -> SoundManager.LAUNCHER_BUTTON;
                case "Пузыри" -> SoundManager.BUBBLE_BUTTON;
                case "Киберпанк" -> SoundManager.CYBER_BUTTON;
                default -> SoundManager.NV_GLASS_BUTTON;
            };
            case "pin" -> (this.soundType.is("Стекло") || this.soundType.is("NV · Стекло") || this.soundType.is("NV стекло")) ? SoundManager.NV_GLASS_PIN : SoundManager.SERENE_PIN;
            case "unpin" -> (this.soundType.is("Стекло") || this.soundType.is("NV · Стекло") || this.soundType.is("NV стекло")) ? SoundManager.NV_GLASS_UNPIN : SoundManager.SERENE_UNPIN;
            case "notification" -> (this.soundType.is("Стекло") || this.soundType.is("NV · Стекло") || this.soundType.is("NV стекло")) ? SoundManager.NV_GLASS_NOTIFY : defaultEvent;
            case "search_typing" -> this.resolveSliderSound();
            default -> defaultEvent;
        };
    }

    public static boolean isAllowed(String string) {
        ClientSounds clientSounds = instance;
        if (clientSounds == null || string == null) {
            return true;
        }
        if (!clientSounds.isEnabled()) {
            return false;
        }
        long now = System.currentTimeMillis();
        return switch (string) {
            case "gui_open", "gui_close" -> clientSounds.guiSound.getValue();
            case "select_category" -> clientSounds.categorySound.getValue();
            case "module_settings_open", "module_settings_close" -> clientSounds.moduleSettingsSound.getValue();
            case "settings_open", "settings_close" -> clientSounds.dropdownSound.getValue();
            case "slider" -> {
                if (!clientSounds.sliderSound.getValue()) yield false;
                if (now - lastSliderMs < 60L) yield false;
                lastSliderMs = now;
                yield true;
            }
            case "search_typing" -> {
                if (!clientSounds.searchTypingSound.getValue()) yield false;
                if (now - lastTypingMs < 50L) yield false;
                lastTypingMs = now;
                yield true;
            }
            case "command_error" -> clientSounds.commandErrorSound.getValue();
            case "theme_switch" -> clientSounds.themeSwitchSound.getValue();
            default -> true;
        };
    }

    public float getPitch() {
        return this.pitch.getFloat();
    }

    public float getVolume() {
        return this.volume.getFloat();
    }
}
