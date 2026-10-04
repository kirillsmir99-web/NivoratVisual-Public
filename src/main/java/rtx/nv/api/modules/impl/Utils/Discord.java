package rtx.nv.api.modules.impl.Utils;

import rtx.nv.api.config.ConfigManager;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.MultiSelectSetting;
import rtx.nv.api.modules.settings.impl.SelectSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.TextSetting;
import rtx.nv.utils.discord.rpc.DiscordRPCManager;

public final class Discord extends Module {
    private static Discord instance;

    public final SeparatorSetting mainSeparator = this.register(new SeparatorSetting("Основное"));

    public final ModeSetting mode = this.register(new ModeSetting(
        "Режим",
        "Вариант отображения статуса в Discord.",
        "Авто",
        "Авто", "Свой текст"
    ));

    public final SeparatorSetting displaySeparator = this.register(new SeparatorSetting("Отображение"));

    public final BooleanSetting showServer = this.register(new BooleanSetting(
        "Сервер",
        "Отображать название сервера в статусе.",
        true
    ));

    public final BooleanSetting showNick = this.register(new BooleanSetting(
        "Никнейм",
        "Отображать ник игрока в статусе.",
        true
    ));

    public final BooleanSetting animation = this.register(new BooleanSetting(
        "Анимация",
        "Периодически чередовать строки статуса.",
        true
    ));

    public final SeparatorSetting pagesSeparator = this.register(new SeparatorSetting(
        "Страницы"
    ).visible(() -> this.mode.is("Свой текст")));

    public final MultiSelectSetting activePages = this.register(new MultiSelectSetting(
        "Страницы",
        "Какие страницы отображать и сменять в статусе."
    ).value("Страница 1", "Страница 2", "Страница 3").selected("Страница 1", "Страница 2").minSelectedCount(1).visible(() -> this.mode.is("Свой текст")));

    public final SelectSetting editPage = this.register(new SelectSetting(
        "Редактор",
        "Какую страницу настраивать сейчас."
    ).value("Страница 1", "Страница 2", "Страница 3").selected("Страница 1").visible(() -> this.mode.is("Свой текст")));

    public final SeparatorSetting customTextSeparator = this.register(new SeparatorSetting(
        "Кастомный текст"
    ).visible(() -> this.mode.is("Свой текст")));

    public final TextSetting page1Line1 = this.register(new TextSetting(
        "Стр 1: Верх",
        "Первая строка страницы 1."
    ).setText("NV").setPlaceholder("NV").visible(() -> this.mode.is("Свой текст") && this.editPage.is("Страница 1")));

    public final TextSetting page1Line2 = this.register(new TextSetting(
        "Стр 1: Низ",
        "Вторая строка страницы 1."
    ).setText("Лучший визуал").setPlaceholder("Лучший визуал").visible(() -> this.mode.is("Свой текст") && this.editPage.is("Страница 1")));

    public final TextSetting page2Line1 = this.register(new TextSetting(
        "Стр 2: Верх",
        "Первая строка страницы 2."
    ).setText("NV").setPlaceholder("NV").visible(() -> this.mode.is("Свой текст") && this.editPage.is("Страница 2")));

    public final TextSetting page2Line2 = this.register(new TextSetting(
        "Стр 2: Низ",
        "Вторая строка страницы 2."
    ).setText("В игре").setPlaceholder("В игре").visible(() -> this.mode.is("Свой текст") && this.editPage.is("Страница 2")));

    public final TextSetting page3Line1 = this.register(new TextSetting(
        "Стр 3: Верх",
        "Первая строка страницы 3."
    ).setText("NV").setPlaceholder("NV").visible(() -> this.mode.is("Свой текст") && this.editPage.is("Страница 3")));

    public final TextSetting page3Line2 = this.register(new TextSetting(
        "Стр 3: Низ",
        "Вторая строка страницы 3."
    ).setText("by Nivorat").setPlaceholder("by Nivorat").visible(() -> this.mode.is("Свой текст") && this.editPage.is("Страница 3")));

    private String lastServer = "";
    private boolean lastInWorld = false;

    public Discord() {
        super("Discord", "Настройка Discord Rich Presence статуса в реальном времени.", Category.UTILS);
        instance = this;

        this.mode.setChangeListener(this::onSettingChanged);
        this.showServer.setChangeListener(this::onSettingChanged);
        this.showNick.setChangeListener(this::onSettingChanged);
        this.animation.setChangeListener(this::onSettingChanged);
        this.activePages.setChangeListener(this::onSettingChanged);
        this.editPage.setChangeListener(this::onSettingChanged);
        this.page1Line1.setChangeListener(this::onSettingChanged);
        this.page1Line2.setChangeListener(this::onSettingChanged);
        this.page2Line1.setChangeListener(this::onSettingChanged);
        this.page2Line2.setChangeListener(this::onSettingChanged);
        this.page3Line1.setChangeListener(this::onSettingChanged);
        this.page3Line2.setChangeListener(this::onSettingChanged);
    }

    public static Discord getInstance() {
        if (instance != null) {
            return instance;
        }
        try {
            instance = ModuleManager.get().get(Discord.class);
        } catch (Throwable ignored) {
        }
        return instance;
    }

    private void onSettingChanged() {
        ConfigManager.markDirty();
        if (this.isEnabled()) {
            DiscordRPCManager.updatePresenceImmediate();
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!event.isPre() || !this.isEnabled()) {
            return;
        }
        boolean inWorld = this.mc.world != null;
        String currentServer = DiscordRPCManager.detectServerName();
        if (inWorld != this.lastInWorld || !currentServer.equals(this.lastServer)) {
            this.lastInWorld = inWorld;
            this.lastServer = currentServer;
            DiscordRPCManager.updatePresenceImmediate();
        }
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    protected void onEnable() {
        DiscordRPCManager.start();
        DiscordRPCManager.updatePresenceImmediate();
    }

    @Override
    protected void onDisable() {
        DiscordRPCManager.clearPresence();
    }
}
