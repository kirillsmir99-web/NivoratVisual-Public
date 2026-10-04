package rtx.nv.api.modules.impl.Utils;
import rtx.nv.api.events.EventHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;
import rtx.nv.api.events.impl.inventory.ClickSlotEvent;
import rtx.nv.api.events.impl.inventory.HandledScreenEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.utils.time.StopWatch;

public final class ItemScroller
extends Module {
    private final StopWatch stopWatch = new StopWatch();
    private final SliderSetting scrollDelay = new SliderSetting("\u0417\u0430\u0434\u0435\u0440\u0436\u043a\u0430 \u043f\u0440\u043e\u043a\u0440\u0443\u0442\u043a\u0438", "\u0417\u0430\u0434\u0435\u0440\u0436\u043a\u0430 \u043c\u0435\u0436\u0434\u0443 \u043a\u043b\u0438\u043a\u0430\u043c\u0438 \u043f\u0440\u043e\u043a\u0440\u0443\u0442\u043a\u0438 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432.").range(0, 200).increment(1).setValue(50.0f);

    public ItemScroller() {
        super("ItemScroller", "\u041f\u0440\u043e\u043a\u0440\u0443\u0447\u0438\u0432\u0430\u0435\u0442 \u043f\u043e\u0434\u0445\u043e\u0434\u044f\u0449\u0438\u0435 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u0432 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u0435 \u0441 \u043a\u043b\u0430\u0432\u0438\u0448\u0430\u043c\u0438-\u043c\u043e\u0434\u0438\u0444\u0438\u043a\u0430\u0442\u043e\u0440\u0430\u043c\u0438.", Category.UTILS);
        this.register(this.scrollDelay);
    }

    @EventHandler
    public void onHandledScreen(HandledScreenEvent handledScreenEvent) {
        int n;
        if (this.mc.player == null || this.mc.interactionManager == null) {
            return;
        }
        Slot slot = handledScreenEvent.getSlotHover();
        SlotActionType slotActionType = this.isKeyDown(this.mc.options.dropKey) ? SlotActionType.THROW : (this.isKeyDown(this.mc.options.attackKey) ? SlotActionType.QUICK_MOVE : null);
        if (this.isKeyDown(this.mc.options.sneakKey) && !this.isKeyDown(this.mc.options.sprintKey) && slot != null && slot.hasStack() && slotActionType != null && this.stopWatch.every(this.scrollDelay.getValue()) && (n = this.menuSlotId(slot)) != -1) {
            this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, n, slotActionType == SlotActionType.THROW ? 1 : 0, slotActionType, (PlayerEntity)(Object)this.mc.player);
        }
    }

    @EventHandler
    public void onClickSlot(ClickSlotEvent clickSlotEvent) {
        if (this.mc.player == null || this.mc.interactionManager == null) {
            return;
        }
        int n = clickSlotEvent.getSlotId();
        if (n < 0 || n >= this.mc.player.currentScreenHandler.slots.size()) {
            return;
        }
        Slot slot3 = this.mc.player.currentScreenHandler.getSlot(n);
        if (!slot3.hasStack()) {
            return;
        }
        Item item = slot3.getStack().getItem();
        if (this.isKeyDown(this.mc.options.sneakKey) && this.isKeyDown(this.mc.options.sprintKey) && this.stopWatch.every(50.0)) {
            this.mc.player.currentScreenHandler.slots.stream().filter(slot2 -> slot2.hasStack() && slot2.getStack().getItem().equals(item) && slot2.inventory.equals((Object)slot3.inventory)).forEach(slot -> {
                int n2 = this.menuSlotId((Slot)slot);
                if (n2 != -1) {
                    this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, n2, 1, clickSlotEvent.getActionType(), (PlayerEntity)(Object)this.mc.player);
                }
            });
        }
    }

    private boolean isKeyDown(KeyBinding keyBinding) {
        if (this.mc.getWindow() == null) {
            return false;
        }
        InputUtil.Key key = keyBinding.getDefaultKey();
        int n = key.getCode();
        if (n == -1) {
            return false;
        }
        return switch (key.getCategory()) {
            case KEYSYM, SCANCODE -> GLFW.glfwGetKey(this.mc.getWindow().getHandle(), n) == 1;
            case MOUSE -> GLFW.glfwGetMouseButton(this.mc.getWindow().getHandle(), n) == 1;
            default -> false;
        };
    }

    private int menuSlotId(Slot slot) {
        if (slot == null || this.mc.player == null || this.mc.player.currentScreenHandler == null) {
            return -1;
        }
        DefaultedList defaultedList = this.mc.player.currentScreenHandler.slots;
        for (int i = 0; i < defaultedList.size(); ++i) {
            if (defaultedList.get(i) != slot) continue;
            return i;
        }
        return slot.id;
    }
}

