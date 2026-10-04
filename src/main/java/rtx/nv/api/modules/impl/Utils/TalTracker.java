package rtx.nv.api.modules.impl.Utils;
import rtx.nv.api.events.EventHandler;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import rtx.nv.api.events.impl.player.AttackEntityEvent;
import rtx.nv.api.events.impl.player.TotemPopEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.utils.chat.ChatMessage;

public final class TalTracker
extends Module {
    private static final long DUPLICATE_POP_WINDOW_MS = 150L;
    private static final Map<Integer, Long> LAST_POP_MESSAGES = new HashMap<Integer, Long>();
    private final BooleanSetting onlyTarget = new BooleanSetting("\u0422\u043e\u043b\u044c\u043a\u043e \u043f\u043e \u0446\u0435\u043b\u0438", "\u041f\u0438\u0441\u0430\u0442\u044c \u0442\u043e\u043b\u044c\u043a\u043e \u043a\u043e\u0433\u0434\u0430 \u0442\u043e\u0442\u0435\u043c \u043b\u043e\u043f\u043d\u0443\u043b \u0443 \u043f\u0440\u043e\u0442\u0438\u0432\u043d\u0438\u043a\u0430, \u043a\u043e\u0442\u043e\u0440\u043e\u0433\u043e \u0442\u044b \u0431\u044c\u0451\u0448\u044c.", true);
    private final SliderSetting memory = new SliderSetting("\u041f\u0430\u043c\u044f\u0442\u044c \u0446\u0435\u043b\u0438 (\u0441\u0435\u043a)", "\u0421\u043a\u043e\u043b\u044c\u043a\u043e \u0441\u0435\u043a\u0443\u043d\u0434 \u0446\u0435\u043b\u044c \u0441\u0447\u0438\u0442\u0430\u0435\u0442\u0441\u044f \u0442\u0432\u043e\u0435\u0439 \u043f\u043e\u0441\u043b\u0435 \u0443\u0434\u0430\u0440\u0430.").range(1, 10).increment(1).setValue(4.0f).visible(this.onlyTarget::getValue);
    private final BooleanSetting showSelf = new BooleanSetting("\u0421\u0432\u043e\u0438 \u0442\u043e\u0442\u0435\u043c\u044b", "\u041f\u0438\u0441\u0430\u0442\u044c \u0442\u0430\u043a\u0436\u0435 \u043a\u043e\u0433\u0434\u0430 \u0442\u0432\u043e\u0439 \u0441\u043e\u0431\u0441\u0442\u0432\u0435\u043d\u043d\u044b\u0439 \u0442\u043e\u0442\u0435\u043c \u043b\u043e\u043f\u0430\u0435\u0442\u0441\u044f.", false);
    private int lastTargetId = -1;
    private long lastTargetTime;

    public TalTracker() {
        super("TalTracker", "\u0421\u043e\u043e\u0431\u0449\u0430\u0435\u0442 \u0432 \u0447\u0430\u0442 \u043a\u043e\u0433\u0434\u0430 \u0443 \u043f\u0440\u043e\u0442\u0438\u0432\u043d\u0438\u043a\u0430 \u043b\u043e\u043f\u0430\u0435\u0442\u0441\u044f \u0442\u043e\u0442\u0435\u043c \u0438 \u0431\u044b\u043b \u043b\u0438 \u043e\u043d \u0437\u0430\u0447\u0430\u0440\u043e\u0432\u0430\u043d.", Category.UTILS);
        this.register(this.onlyTarget, this.memory, this.showSelf);
    }

    @Override
    protected void onEnable() {
        LAST_POP_MESSAGES.clear();
    }

    @EventHandler
    public void onAttack(AttackEntityEvent attackEntityEvent) {
        if (attackEntityEvent.isSynthetic()) {
            return;
        }
        Entity entity = attackEntityEvent.getTarget();
        if (entity instanceof LivingEntity) {
            this.lastTargetId = entity.getId();
            this.lastTargetTime = System.currentTimeMillis();
        }
    }

    private boolean isCurrentTarget(LivingEntity livingEntity) {
        if (livingEntity.getId() != this.lastTargetId) {
            return false;
        }
        long l = (long)this.memory.getInt() * 1000L;
        return System.currentTimeMillis() - this.lastTargetTime <= l;
    }

    @EventHandler
    public void onTotemPop(TotemPopEvent totemPopEvent) {
        boolean bl;
        if (this.mc.player == null) {
            return;
        }
        LivingEntity livingEntity = totemPopEvent.getEntity();
        boolean bl2 = bl = livingEntity == this.mc.player;
        if (bl && !this.showSelf.getValue()) {
            return;
        }
        if (!bl && this.onlyTarget.getValue() && !this.isCurrentTarget(livingEntity)) {
            return;
        }
        long l = Util.getMeasuringTimeMs();
        Long l2 = LAST_POP_MESSAGES.put(livingEntity.getId(), l);
        if (l2 != null && l - l2 < 150L) {
            return;
        }
        String string = livingEntity.getName().getString();
        boolean bl3 = totemPopEvent.isEnchanted();
        MutableText mutableText = Text.literal((String)"\u00bb ").formatted(Formatting.DARK_GRAY).append((Text)Text.literal((String)string).formatted(Formatting.WHITE)).append((Text)Text.literal((String)" \u043f\u043e\u0442\u0435\u0440\u044f\u043b \u0442\u043e\u0442\u0435\u043c. ").formatted(Formatting.GRAY)).append((Text)Text.literal((String)"\u0422\u0430\u043b\u0438\u043a: ").formatted(Formatting.LIGHT_PURPLE)).append((Text)Text.literal((String)(bl3 ? "\u2713" : "\u2717")).formatted(bl3 ? Formatting.GREEN : Formatting.RED));
        ChatMessage.brandmessage((Text)mutableText);
    }
}

