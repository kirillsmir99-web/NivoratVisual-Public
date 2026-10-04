package rtx.nv.utils.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class InventoryItems {
    private InventoryItems() {}

    public static boolean isPlayerHead(ItemStack stack) {
        return stack != null && (stack.isOf(Items.PLAYER_HEAD) || stack.isOf(Items.ZOMBIE_HEAD) || stack.isOf(Items.CREEPER_HEAD) || stack.isOf(Items.DRAGON_HEAD) || stack.isOf(Items.PIGLIN_HEAD) || stack.isOf(Items.SKELETON_SKULL) || stack.isOf(Items.WITHER_SKELETON_SKULL));
    }

    public static boolean isEnchantedTotem(ItemStack stack) {
        return stack != null && stack.isOf(Items.TOTEM_OF_UNDYING);
    }
}
