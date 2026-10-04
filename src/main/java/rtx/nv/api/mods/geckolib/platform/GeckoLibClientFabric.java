package rtx.nv.api.mods.geckolib.platform;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import rtx.nv.api.mods.geckolib.service.GeckoLibClient;

public class GeckoLibClientFabric
implements GeckoLibClient {
    @Override
    public <S extends BipedEntityRenderState> Model<?> getArmorModelForItem(S s, ItemStack itemStack, EquipmentSlot equipmentSlot, EquipmentModel.LayerType layerType, BipedEntityModel<S> bipedEntityModel) {
        return bipedEntityModel;
    }

    @Override
    public int getDyedItemColor(ItemStack itemStack, int n) {
        return itemStack.isIn(ItemTags.DYEABLE) ? DyedColorComponent.getColor((ItemStack)itemStack, (int)n) : n;
    }
}

