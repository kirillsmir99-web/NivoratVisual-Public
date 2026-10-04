package rtx.nv.api.modules.impl.Utils;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareController;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.utils.net.ClientPresence;

public final class Globals
extends Module {
    private final BooleanSetting inTags = this.register(new BooleanSetting("\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0432 \u0442\u0435\u0433\u0430\u0445", "\u041b\u043e\u0433\u043e\u0442\u0438\u043f \u043a\u043b\u0438\u0435\u043d\u0442\u0430 \u0440\u044f\u0434\u043e\u043c \u0441 \u0438\u043c\u0435\u043d\u0435\u043c \u0438\u0433\u0440\u043e\u043a\u043e\u0432 NV \u0432 \u0442\u0430\u0431\u043b\u0438\u0447\u043a\u0430\u0445 \u043d\u0430\u0434 \u0433\u043e\u043b\u043e\u0432\u043e\u0439.", true));
    private final BooleanSetting inTab = this.register(new BooleanSetting("\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0432 \u0442\u0430\u0431\u0435", "\u041b\u043e\u0433\u043e\u0442\u0438\u043f \u043a\u043b\u0438\u0435\u043d\u0442\u0430 \u0440\u044f\u0434\u043e\u043c \u0441 \u0438\u043c\u0435\u043d\u0435\u043c \u0438\u0433\u0440\u043e\u043a\u043e\u0432 NV \u0432 \u0441\u043f\u0438\u0441\u043a\u0435 \u0438\u0433\u0440\u043e\u043a\u043e\u0432 (Tab).", true));
    private final BooleanSetting shareGui = this.register(new BooleanSetting("\u0422\u0440\u0430\u043d\u0441\u043b\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u0433\u0443\u0439", "\u041f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u0438 NV \u0440\u044f\u0434\u043e\u043c \u0432\u0438\u0434\u044f\u0442 \u0432\u0430\u0448 \u043e\u0442\u043a\u0440\u044b\u0442\u044b\u0439 \u0433\u0443\u0439 \u0432 \u043c\u0438\u0440\u0435 \u043f\u0435\u0440\u0435\u0434 \u0432\u0430\u043c\u0438.", false));
    private final BooleanSetting remoteGuis = this.register(new BooleanSetting("\u0413\u0443\u0439 \u0438\u0433\u0440\u043e\u043a\u043e\u0432 \u0432 \u043c\u0438\u0440\u0435", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u043e\u0442\u043a\u0440\u044b\u0442\u044b\u0435 \u0433\u0443\u0438 \u0434\u0440\u0443\u0433\u0438\u0445 \u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u0435\u0439 NV \u0443 \u0438\u0445 \u043b\u0438\u0446\u0430.", true));

    public Globals() {
        super("Globals", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u043c\u0435\u0442\u043a\u0443 \u043a\u043b\u0438\u0435\u043d\u0442\u0430 \u0443 \u0434\u0440\u0443\u0433\u0438\u0445 \u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u0435\u0439 NV.", Category.UTILS);
    }

    public static Globals getInstance() {
        return ModuleManager.get().get(Globals.class);
    }

    @Override
    protected void onDisable() {
        ClientPresence.INSTANCE.stop();
        GuiShareController.reset();
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.isPre()) {
            GuiShareController.tick(this);
        }
    }

    @Override
    protected void onEnable() {
        ClientPresence.INSTANCE.start();
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    public static boolean tagsBadge() {
        Globals globals = Globals.getInstance();
        return globals != null && globals.isEnabled() && globals.inTags.getValue();
    }

    public static boolean shareGuiEnabled() {
        Globals globals = Globals.getInstance();
        return globals != null && globals.isEnabled() && globals.shareGui.getValue();
    }

    public static boolean remoteGuisEnabled() {
        Globals globals = Globals.getInstance();
        return globals != null && globals.isEnabled() && globals.remoteGuis.getValue();
    }

    public static boolean tabBadge() {
        Globals globals = Globals.getInstance();
        return globals != null && globals.isEnabled() && globals.inTab.getValue();
    }
}

