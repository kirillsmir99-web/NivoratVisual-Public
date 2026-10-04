package rtx.nv.api.modules.impl.Interface;

import rtx.nv.api.config.ConfigManager;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.utils.key.KeyBind;

public final class ClickGui extends Module {
    public static final String SCALE_AUTO = "Авто";
    public static final String SCALE_MINECRAFT = "По Minecraft";
    public static final String SCALE_CUSTOM = "Свой";

    public final SeparatorSetting scaleSeparator = this.register(new SeparatorSetting("Масштаб"));

    public final ModeSetting scaleMode = this.register(new ModeSetting(
        "Масштаб",
        "Вариант масштабирования интерфейса меню.",
        SCALE_AUTO,
        SCALE_AUTO, SCALE_MINECRAFT, SCALE_CUSTOM
    ));

    public final SliderSetting customScale = this.register(new SliderSetting(
        "Размер",
        "Пользовательский размер интерфейса меню в процентах."
    ).range(0.70f, 1.30f).increment(0.05f).setValue(1.0f).visible(() -> this.scaleMode.is(SCALE_CUSTOM)));

    public ClickGui() {
        super("ClickGui", "Открывает клик-меню клиента.", Category.DISPLAY);
        this.setBind(KeyBind.keyboard(344));

        this.scaleMode.setChangeListener(ConfigManager::markDirty);
        this.customScale.setChangeListener(ConfigManager::markDirty);
    }

    @Override
    public KeyBind getBind() {
        KeyBind bind = super.getBind();
        if (bind == null || bind.getCode() <= 0) {
            return KeyBind.keyboard(344);
        }
        return bind;
    }

    @Override
    public void setBind(KeyBind keyBind) {
        if (keyBind == null || keyBind.getCode() <= 0 || keyBind.getType() == rtx.nv.utils.key.InputType.MOUSE) {
            super.setBind(KeyBind.keyboard(344));
            return;
        }
        super.setBind(keyBind);
    }

    @Override
    public void onEnable() {
        if (this.mc != null && this.mc.world != null && this.mc.player != null && this.mc.currentScreen == null) {
            this.mc.setScreen((net.minecraft.client.gui.screen.Screen) rtx.nv.api.ui.UI.INSTANCE);
            rtx.nv.utils.sounds.Sounds.play("gui_open");
        }
        this.setEnabled(false);
    }
}

