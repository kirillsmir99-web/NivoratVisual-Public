package rtx.nv.api.modules.impl.Utils;
import rtx.nv.api.events.EventHandler;
import net.minecraft.item.Items;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.mixin.accessor.LivingEntityAccessor;
import rtx.nv.mixin.accessor.MinecraftAccessor;

public final class FastExp
extends Module {
    public FastExp() {
        super("FastExp", "\u0423\u0431\u0438\u0440\u0430\u0435\u0442 \u0437\u0430\u0434\u0435\u0440\u0436\u043a\u0443 \u043f\u0440\u0438 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0438 \u0431\u0443\u0442\u044b\u043b\u043e\u0447\u0435\u043a \u043e\u043f\u044b\u0442\u0430.", Category.UTILS);
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        boolean bl;
        if (!tickEvent.isPre() || this.mc.player == null) {
            return;
        }
        boolean bl2 = bl = this.mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE) || this.mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE);
        if (!bl) {
            return;
        }
        ((MinecraftAccessor)(Object)this.mc).nv_setRightClickDelay(0);
        ((LivingEntityAccessor)(Object)this.mc.player).nv_setAttackAnim(0.0f);
    }
}

