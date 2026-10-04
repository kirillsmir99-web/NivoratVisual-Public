package rtx.nv.api.mods.geckolib.animatable;

import net.minecraft.item.ItemStack;

public interface GeoItem extends GeoAnimatable {
    static long getId(ItemStack stack) {
        return stack == null ? 0L : stack.hashCode();
    }
}