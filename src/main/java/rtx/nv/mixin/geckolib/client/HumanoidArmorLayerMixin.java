package rtx.nv.mixin.geckolib.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.mods.geckolib.renderer.GeoArmorRenderer;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;

@Mixin(ArmorFeatureRenderer.class)
public abstract class HumanoidArmorLayerMixin<S extends BipedEntityRenderState, M extends BipedEntityModel<S>, A extends BipedEntityModel<S>>
extends FeatureRenderer<S, M> {
    public HumanoidArmorLayerMixin(FeatureRendererContext<S, M> renderer) {
        super(renderer);
    }

    @WrapWithCondition(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V", at={@At(value="INVOKE", target="Lnet/minecraft/client/render/entity/feature/ArmorFeatureRenderer;renderArmor(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;ILnet/minecraft/client/render/entity/model/BipedEntityModel;)V")}, require = 0)
    public boolean geckolib_wrapArmorPieceRender(ArmorFeatureRenderer<S, M, A> layer, MatrixStack poseStack, OrderedRenderCommandQueue renderTasks, ItemStack stack, EquipmentSlot slot, int packedLight, S entityRenderState) {
        return entityRenderState instanceof BipedEntityRenderState && !GeoArmorRenderer.tryRenderGeoArmorPiece((renderState, equipmentSlot) -> (BipedEntityModel)layer.getModel((S)(Object)renderState, equipmentSlot), poseStack, renderTasks, stack, slot, packedLight, (BipedEntityRenderState)(Object)entityRenderState);
    }
}
