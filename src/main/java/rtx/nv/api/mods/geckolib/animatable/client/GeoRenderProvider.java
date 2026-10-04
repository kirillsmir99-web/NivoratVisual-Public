package rtx.nv.api.mods.geckolib.animatable.client;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import rtx.nv.api.mods.geckolib.renderer.GeoArmorRenderer;
import rtx.nv.api.mods.geckolib.renderer.GeoItemRenderer;

public interface GeoRenderProvider {
    public static final GeoRenderProvider DEFAULT = new GeoRenderProvider() {};

    default public GeoItemRenderer<?> getGeoItemRenderer() {
        return null;
    }

    default public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack itemStack, EquipmentSlot equipmentSlot) {
        return null;
    }

    public static GeoRenderProvider of(ItemStack itemStack) {
        return DEFAULT;
    }
}
