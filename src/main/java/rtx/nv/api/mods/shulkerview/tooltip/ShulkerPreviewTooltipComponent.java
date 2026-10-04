package rtx.nv.api.mods.shulkerview.tooltip;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;

public record ShulkerPreviewTooltipComponent(ItemStack stack, List<ItemStack> items, boolean show) implements TooltipData {
    public ShulkerPreviewTooltipComponent(ItemStack stack) {
        this(stack, List.of(), true);
    }
}
