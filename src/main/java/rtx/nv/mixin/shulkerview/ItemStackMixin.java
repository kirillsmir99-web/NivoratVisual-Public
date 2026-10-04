package rtx.nv.mixin.shulkerview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtx.nv.api.mods.shulkerview.ShulkerPreviewHelper;
import rtx.nv.api.mods.shulkerview.tooltip.ShulkerPreviewTooltipComponent;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method="getTooltipData", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_shulkerPreviewTooltipImage(CallbackInfoReturnable<Optional<TooltipData>> cir) {
        ItemStack stack = (ItemStack)(Object)this;
        if (ShulkerPreviewHelper.shouldShowPreview(stack)) {
            List<ItemStack> items = ShulkerPreviewHelper.getItems(stack);
            cir.setReturnValue(Optional.of(new ShulkerPreviewTooltipComponent(stack.copy(), items, true)));
        }
    }

    @Inject(method="getTooltip", at={@At(value="RETURN")}, cancellable=true, require = 0)
    private void nv_shulkerPreviewTooltipLines(Item.TooltipContext context, PlayerEntity player, TooltipType flag, CallbackInfoReturnable<List<Text>> cir) {
        ItemStack stack = (ItemStack)(Object)this;
        ArrayList<Text> lines = new ArrayList<Text>((Collection<? extends Text>)cir.getReturnValue());
        lines.addAll(ShulkerPreviewHelper.tooltipLines(stack));
        cir.setReturnValue(lines);
    }
}
