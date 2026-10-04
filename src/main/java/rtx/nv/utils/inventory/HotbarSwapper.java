package rtx.nv.utils.inventory;

import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public final class HotbarSwapper {
    private HotbarSwapper() {}

    public static boolean has(Predicate<ItemStack> predicate) {
        return !find(predicate).isEmpty();
    }

    public static ItemStack find(Predicate<ItemStack> predicate) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (predicate.test(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean swapToOffhand(Predicate<ItemStack> predicate) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (predicate.test(stack)) {
                mc.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }
}
