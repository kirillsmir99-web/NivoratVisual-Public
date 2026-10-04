package rtx.nv.api.mods.shulkerview.tooltip;

import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;

public class ShulkerPreviewClientTooltipComponent implements TooltipComponent {
    private final ShulkerPreviewTooltipComponent component;

    public ShulkerPreviewClientTooltipComponent(ShulkerPreviewTooltipComponent component) {
        this.component = component;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return 58;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 162;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        if (this.component == null || this.component.items() == null) {
            return;
        }
        List<ItemStack> items = this.component.items();
        for (int i = 0; i < items.size() && i < 27; i++) {
            ItemStack stack = items.get(i);
            int slotX = x + (i % 9) * 18;
            int slotY = y + (i / 9) * 18;
            context.fill(slotX, slotY, slotX + 18, slotY + 18, 0x44000000);
            context.fill(slotX, slotY, slotX + 17, slotY + 1, 0x22FFFFFF);
            context.fill(slotX, slotY, slotX + 1, slotY + 17, 0x22FFFFFF);
            if (!stack.isEmpty()) {
                context.drawItem(stack, slotX + 1, slotY + 1);
                context.drawStackOverlay(textRenderer, stack, slotX + 1, slotY + 1);
            }
        }
    }
}
