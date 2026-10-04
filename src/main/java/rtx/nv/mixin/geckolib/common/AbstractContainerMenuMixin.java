package rtx.nv.mixin.geckolib.common;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;

@Mixin(net.minecraft.screen.ScreenHandler.class)

public class AbstractContainerMenuMixin {
    @WrapOperation(method="internalOnSlotClick", at={@At(value="INVOKE", target="Lnet/minecraft/item/ItemStack;method_46651(I)Lnet/minecraft/class_1799;", ordinal=1)}, require = 0)
    public ItemStack geckolib_removeGeckolibIdOnCopy(ItemStack instance, int count, Operation<ItemStack> original) {
        ItemStack copy = (ItemStack)original.call(new Object[]{instance, count});
        if (copy.contains(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get())) {
            copy.remove(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get());
        }
        return copy;
    }

    @WrapOperation(method="updateTrackedSlot", at={@At(value="INVOKE", target="Lnet/minecraft/item/ItemStack;method_7973(Lnet/minecraft/class_1799;Lnet/minecraft/class_1799;)Z")}, require = 0)
    public boolean geckolib_allowLazyStackIdParity(ItemStack stack, ItemStack other, Operation<Boolean> original) {
        return (Boolean)original.call(new Object[]{stack, other}) != false && ((Number)stack.getOrDefault(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get(), (Object)Integer.MIN_VALUE)).equals(other.getOrDefault(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get(), (Object)Integer.MIN_VALUE));
    }
}

