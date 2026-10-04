package rtx.nv.api.modules.impl.Utils;
import rtx.nv.api.events.EventHandler;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Arm;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.BindSetting;
import rtx.nv.api.modules.settings.impl.BindSetting.Type;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.notifications.Notifications;
import rtx.nv.utils.sounds.SoundManager;

public class HandSwap
extends Module {
    private final SeparatorSetting separator = new SeparatorSetting("\u0421\u043c\u0435\u043d\u0430 \u0440\u0443\u043a\u0438");
    private final BindSetting swapKey = new BindSetting("\u041a\u043b\u0430\u0432\u0438\u0448\u0430 \u0441\u043c\u0435\u043d\u044b", "\u041e\u0434\u043d\u043e \u043d\u0430\u0436\u0430\u0442\u0438\u0435 \u2014 \u0441\u043c\u0435\u043d\u0438\u0442\u044c \u0432\u0435\u0434\u0443\u0449\u0443\u044e \u0440\u0443\u043a\u0443 (\u043f\u0440\u0430\u0432\u0430\u044f/\u043b\u0435\u0432\u0430\u044f).").setType(BindSetting.Type.TOGGLE);
    private final BooleanSetting notify = new BooleanSetting("\u0423\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u0435", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0443\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u0435 \u043f\u0440\u0438 \u0441\u043c\u0435\u043d\u0435 \u0440\u0443\u043a\u0438.", true);
    private boolean wasDown;

    public HandSwap() {
        super("HandSwap", "\u0421\u043c\u0435\u043d\u0430 \u0432\u0435\u0434\u0443\u0449\u0435\u0439 \u0440\u0443\u043a\u0438 \u043e\u0434\u043d\u0438\u043c \u043d\u0430\u0436\u0430\u0442\u0438\u0435\u043c \u043a\u043b\u0430\u0432\u0438\u0448\u0438, \u0431\u0435\u0437 \u043c\u0435\u043d\u044e.", Category.UTILS);
        this.register(this.separator, this.swapKey, this.notify);
    }

    private void swap() {
        Arm arm = ((Arm)(Object)this.mc.options.getMainArm().getValue()).getOpposite();
        this.mc.options.getMainArm().setValue(arm);
        this.mc.options.sendClientSettings();
        this.mc.options.write();
        if (this.notify.getValue()) {
            String string = arm == Arm.LEFT ? "\u043b\u0435\u0432\u0430\u044f" : "\u043f\u0440\u0430\u0432\u0430\u044f";
            Notifications.push((String)"HandSwap", (String)("\u0412\u0435\u0434\u0443\u0449\u0430\u044f \u0440\u0443\u043a\u0430: " + string + "."), (long)2000L, (SoundEvent)SoundManager.NOTIFICATION_LOW);
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        boolean bl;
        if (!tickEvent.isPre()) {
            return;
        }
        if (this.mc.player == null || this.mc.world == null || this.mc.currentScreen != null) {
            this.wasDown = false;
            return;
        }
        boolean bl2 = bl = this.swapKey.isBound() && this.swapKey.getValue().isDown(this.mc.getWindow().getHandle());
        if (bl && !this.wasDown) {
            this.swap();
        }
        this.wasDown = bl;
    }

    @Override
    protected void onEnable() {
        this.wasDown = true;
    }
}

