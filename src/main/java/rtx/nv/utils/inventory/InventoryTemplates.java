package rtx.nv.utils.inventory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public final class InventoryTemplates {
    private InventoryTemplates() {}

    public static boolean hasTemplate(int slot) {
        return false;
    }

    public static ItemStack getTemplate(int slot) {
        return ItemStack.EMPTY;
    }

    public static Template active() {
        return null;
    }

    public static Map<String, MissingItem> missingFor(Template template, PlayerInventory inventory) {
        return Collections.emptyMap();
    }

    public static ItemStack stackFor(Entry entry) {
        return ItemStack.EMPTY;
    }

    public static String entryLayoutKey(Entry entry) {
        return entry != null ? entry.key : "";
    }

    public static class Template {
        public Map<Integer, Entry> slots = new HashMap<>();
    }

    public static class Entry {
        public String key = "";
        public int count = 1;
    }

    public static class MissingItem {
        public int count = 0;
    }
}