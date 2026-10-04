package rtx.nv.api.mods.geckolib.service;
import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EquipmentModelData;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public interface GeckoLibClient {
    public static final Supplier<EquipmentModelData<BipedEntityModel<?>>> HUMANOID_ARMOR_MODEL = Suppliers.memoize(() -> EquipmentModelData.mapToEntityModel((EquipmentModelData)EntityModelLayers.PLAYER_EQUIPMENT, (LoadedEntityModels)MinecraftClient.getInstance().getLoadedEntityModels(), BipedEntityModel::new));
    public static final Supplier<ElytraEntityModel> GENERIC_ELYTRA_MODEL = Suppliers.memoize(() -> new ElytraEntityModel(MinecraftClient.getInstance().getLoadedEntityModels().getModelPart(EntityModelLayers.ELYTRA)));

    public <S extends BipedEntityRenderState> Model<?> getArmorModelForItem(S var1, ItemStack var2, EquipmentSlot var3, EquipmentModel.LayerType var4, BipedEntityModel<S> var5);

    public int getDyedItemColor(ItemStack var1, int var2);
}

