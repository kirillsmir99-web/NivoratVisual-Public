package rtx.nv.api.modules.impl.Utils;
import rtx.nv.api.events.EventHandler;
import net.minecraft.client.option.Perspective;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.mods.freelook.FreeLookState;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.BindSetting;
import rtx.nv.api.modules.settings.impl.BindSetting.Type;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;

public class Freelook
extends Module {
    private final SeparatorSetting separator = new SeparatorSetting("\u0421\u0432\u043e\u0431\u043e\u0434\u043d\u044b\u0439 \u043e\u0431\u0437\u043e\u0440");
    private final BindSetting lookKey = new BindSetting("\u041a\u043b\u0430\u0432\u0438\u0448\u0430 \u043e\u0431\u0437\u043e\u0440\u0430", "\u0417\u0430\u0436\u043c\u0438\u0442\u0435, \u0447\u0442\u043e\u0431\u044b \u0441\u0432\u043e\u0431\u043e\u0434\u043d\u043e \u043e\u0441\u043c\u0430\u0442\u0440\u0438\u0432\u0430\u0442\u044c\u0441\u044f \u043e\u0442 \u0442\u0440\u0435\u0442\u044c\u0435\u0433\u043e \u043b\u0438\u0446\u0430").setType(BindSetting.Type.HOLD);
    private boolean active;
    private Perspective previousCamera;

    public Freelook() {
        super("Freelook", "\u0421\u0432\u043e\u0431\u043e\u0434\u043d\u044b\u0439 \u043e\u0431\u0437\u043e\u0440 \u043a\u0430\u043c\u0435\u0440\u043e\u0439 \u043e\u0442 \u0442\u0440\u0435\u0442\u044c\u0435\u0433\u043e \u043b\u0438\u0446\u0430 \u043f\u0440\u0438 \u0437\u0430\u0436\u0430\u0442\u0438\u0438 \u043a\u043b\u0430\u0432\u0438\u0448\u0438", Category.UTILS);
        this.register(this.separator, this.lookKey);
    }

    private void start() {
        this.previousCamera = this.mc.options.getPerspective();
        this.mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        FreeLookState.active = true;
        this.active = true;
    }

    private void stop() {
        if (!this.active) {
            return;
        }
        this.active = false;
        FreeLookState.active = false;
        if (this.previousCamera != null) {
            this.mc.options.setPerspective(this.previousCamera);
            this.previousCamera = null;
        }
    }

    @Override
    protected void onDisable() {
        this.stop();
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        boolean bl;
        if (!tickEvent.isPre()) {
            return;
        }
        if (this.mc.player == null || this.mc.world == null || this.mc.currentScreen != null) {
            this.stop();
            return;
        }
        FreeLookState.maxHeadYaw = 360.0f;
        boolean bl2 = bl = this.lookKey.isBound() && this.lookKey.getValue().isDown(this.mc.getWindow().getHandle());
        if (bl && !this.active) {
            this.start();
        } else if (!bl && this.active) {
            this.stop();
        }
    }
}

