package rtx.nv.mixin.geckolib.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.mods.geckolib.renderer.internal.GeckolibItemSpecialRenderer;

@Mixin(targets = {"net.minecraft.client.render.item.model.SpecialItemModel"})

public class SpecialModelWrapperMixin {
    @WrapOperation(method={"update", "update"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/item/model/special/SpecialModelRenderer;method_65695(Lnet/minecraft/class_1799;)Ljava/lang/Object;")}, require = 0)
    @Nullable
    public <T> T geckolib_extractAllArguments(SpecialModelRenderer<T> instance, ItemStack itemStack, Operation<T> original, ItemRenderState renderState, ItemStack itemStack2, ItemModelManager modelResolver, ItemDisplayContext displayContext, @Nullable ClientWorld level, @Nullable HeldItemContext itemOwner, int layerIndex) {
        Object object;
        if (instance instanceof GeckolibItemSpecialRenderer) {
            GeckolibItemSpecialRenderer geckolibRenderer = (GeckolibItemSpecialRenderer)instance;
            object = geckolibRenderer.extractArgument(itemStack, renderState, displayContext, level, itemOwner);
        } else {
            object = original.call(new Object[]{instance, itemStack});
        }
        return (T)object;
    }
}
