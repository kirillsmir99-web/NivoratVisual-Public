package rtx.nv.mixin;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.utils.inventory.InventoryTemplates;

@Mixin(HandledScreen.class)
public abstract class ContainerGhostOverlayMixin {
    @Shadow
    @Final
    protected ScreenHandler handler;

    @Inject(method="render", at={@At(value="TAIL")}, require = 0)
    private void nv_renderGhostSlots(DrawContext graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
                return;
            }
            InventoryTemplates.Template template = InventoryTemplates.active();
            if (template == null) {
                return;
            }
            PlayerInventory inventory = mc.player.getInventory();
            Map<String, Integer> missing = ContainerGhostOverlayMixin.missingCounts(template, inventory);
            for (Slot slot : this.handler.slots) {
                ItemStack ghost;
                int missingCount;
                InventoryTemplates.Entry entry;
                if (slot.inventory != inventory || !slot.isEnabled() || (entry = template.slots.get(slot.getIndex())) == null || !slot.getStack().isEmpty() || (missingCount = ContainerGhostOverlayMixin.takeMissing(missing, entry)) <= 0 || (ghost = InventoryTemplates.stackFor(entry)).isEmpty()) continue;
                graphics.drawItemWithoutEntity(ghost, slot.x, slot.y);
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, -1198222184);
                ContainerGhostOverlayMixin.drawMissingCount(graphics, missingCount, slot.x, slot.y);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static Map<String, Integer> missingCounts(InventoryTemplates.Template template, PlayerInventory inventory) {
        HashMap<String, Integer> result = new HashMap<>();
        InventoryTemplates.missingFor(template, inventory).forEach((key, item) -> result.put(key, item.count));
        return result;
    }

    private static int takeMissing(Map<String, Integer> missing, InventoryTemplates.Entry entry) {
        String key = InventoryTemplates.entryLayoutKey(entry);
        int count = missing.getOrDefault(key, 0);
        int used = Math.min(count, entry.count);
        if (used > 0) {
            missing.put(key, count - used);
        }
        return used;
    }

    private static void drawMissingCount(DrawContext graphics, int count, int x, int y) {
        if (count <= 1) {
            return;
        }
        String text = String.valueOf(count);
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        graphics.drawText(font, text, x + 17 - font.getWidth(text), y + 9, -4671304, true);
    }
}
