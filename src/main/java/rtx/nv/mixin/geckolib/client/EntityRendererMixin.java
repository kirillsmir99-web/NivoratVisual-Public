package rtx.nv.mixin.geckolib.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import rtx.nv.api.mods.geckolib.renderer.GeoArmorRenderer;
import rtx.nv.api.mods.geckolib.renderer.GeoEntityRenderer;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @ModifyConstant(method = "hasLabel(Lnet/minecraft/entity/Entity;D)Z", constant = @Constant(doubleValue = 4096.0), require = 0)
    public double modifyMaxNameplateDistance(double constant) {
        return (Object)this instanceof GeoEntityRenderer ? 65536.0 : constant;
    }

    @SuppressWarnings("unchecked")
    @WrapMethod(method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V")
    public void geckolib_captureDataForArmorLayer(T entity, S state, float partialTick, Operation<Void> original) {
        original.call(entity, state, partialTick);
        if (state instanceof BipedEntityRenderState && (Object)this instanceof LivingEntityRenderer livingRenderer) {
            for (Object layer : livingRenderer.features) {
                if (!(layer instanceof ArmorFeatureRenderer armorLayer)) continue;
                GeoArmorRenderer.captureRenderStates(EntityRendererMixin.geckolib_castRenderState(state), (LivingEntity)entity, partialTick, (renderState2, slot) -> armorLayer.getModel(renderState2, slot), slot -> EntityRendererMixin.geckolib_castRenderState(state));
                break;
            }
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static <R extends BipedEntityRenderState> R geckolib_castRenderState(EntityRenderState renderState) {
        return (R)((BipedEntityRenderState)renderState);
    }
}
