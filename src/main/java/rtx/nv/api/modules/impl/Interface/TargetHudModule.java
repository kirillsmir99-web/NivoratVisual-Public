package rtx.nv.api.modules.impl.Interface;

import rtx.nv.api.modules.impl.Interface.InterfaceComponentModule;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;

public final class TargetHudModule extends InterfaceComponentModule {
    public static final String MODE_CARD = "Карточка";
    public static final String MODE_PANEL = "Панель";
    private static final String BAR_FROM_HP = "От хп";
    private static final String BAR_WHITE = "Белая";
    private static final String BAR_CLIENT = "Клиентский";

    public final ModeSetting hudMode = this.register(new ModeSetting("Режим", "Внешний вид TargetHud: карточка или панель.", MODE_CARD, MODE_CARD, MODE_PANEL));
    public final ModeSetting barColorMode = this.register(new ModeSetting("Режим цвета полосы хп", "Цвет заливки полосы здоровья.", BAR_FROM_HP, BAR_FROM_HP, BAR_WHITE, BAR_CLIENT));
    public final BooleanSetting showArmor = this.register(new BooleanSetting("Броня", "Показывать броню и предметы в руках цели.", true));
    public final BooleanSetting followTarget = this.register(new BooleanSetting("Следовать", "HUD плавно следует за целью на экране и возвращается на своё место.", false));

    public TargetHudModule() {
        super("TargetHud", "Перемещаемый HUD с информацией о цели.");
    }

    public boolean showArmor() {
        return this.showArmor.getValue();
    }

    public boolean isPanelMode() {
        return this.hudMode.is(MODE_PANEL);
    }

    public boolean isNewMode() {
        return this.isPanelMode();
    }

    public boolean isCardMode() {
        return this.hudMode.is(MODE_CARD);
    }

    public boolean barWhite() {
        return this.barColorMode.is(BAR_WHITE);
    }

    public boolean barClient() {
        return this.barColorMode.is(BAR_CLIENT);
    }

    public boolean followTarget() {
        return this.followTarget.getValue();
    }
}
