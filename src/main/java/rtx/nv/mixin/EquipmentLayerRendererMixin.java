package rtx.nv.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.modules.impl.Visuals.HitColor;

@Mixin(net.minecraft.client.render.entity.equipment.EquipmentRenderer.class)

public abstract class EquipmentLayerRendererMixin {
    @WrapOperation(method="render", at={@At(value="INVOKE", target="Lnet/minecraft/client/render/command/RenderCommandQueue;method_73490(Lnet/minecraft/class_3879;Ljava/lang/Object;Lnet/minecraft/class_4587;Lnet/minecraft/class_1921;IIILnet/minecraft/class_1058;ILnet/minecraft/class_11683$class_11792;)V")}, require = 0)
    private void nv_tintHitArmor(RenderCommandQueue collector, Model model, Object state, MatrixStack poseStack, RenderLayer renderType, int light, int overlay, int color, Sprite sprite, int outlineColor, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay, Operation<Void> original) {
        Integer n;
        if (state instanceof LivingEntityRenderState) {
            LivingEntityRenderState livingState = (LivingEntityRenderState)state;
            n = HitColor.tintFor(livingState);
        } else {
            n = null;
        }
        Integer hitColor = n;
        original.call(new Object[]{collector, model, state, poseStack, renderType, light, overlay, hitColor != null ? hitColor : color, sprite, outlineColor, crumblingOverlay});
    }
}

