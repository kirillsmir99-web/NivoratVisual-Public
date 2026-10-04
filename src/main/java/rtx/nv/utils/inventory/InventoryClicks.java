package rtx.nv.utils.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.SlotActionType;

public final class InventoryClicks {
    private InventoryClicks() {}

    public static void pickup(int slot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler != null) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
        }
    }
}
