package rtx.nv.utils.render.renderitem;

import net.minecraft.item.ItemStack;

public record BuiltRenderItem(ItemStack stack, float x, float y, float size, RenderItemOptions options, int seed) {
    public BuiltRenderItem(ItemStack stack, float x, float y, float size) {
        this(stack, x, y, size, RenderItemOptions.defaults(), 0);
    }

    public boolean visible() {
        return this.stack != null && !this.stack.isEmpty() && this.size > 0.0f && this.options.alpha() > 0.0f;
    }
}