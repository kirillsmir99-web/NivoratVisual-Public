package rtx.nv.mixin.geckolib.common;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.util.GeckoLibUtil;

@Mixin(net.minecraft.item.ItemStack.class)

public class ItemStackMixin {
    @WrapOperation(method="split", at={@At(value="INVOKE", target="Lnet/minecraft/item/ItemStack;method_46651(I)Lnet/minecraft/class_1799;")}, require = 0)
    public ItemStack geckolib_removeGeckolibIdOnCopy(ItemStack instance, int count, Operation<ItemStack> original) {
        ItemStack copy = (ItemStack)original.call(new Object[]{instance, count});
        if (count < instance.getCount() && copy.contains(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get())) {
            copy.remove(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get());
        }
        return copy;
    }

    @WrapOperation(method="areItemsAndComponentsEqual", at={@At(value="INVOKE", target="Ljava/util/Objects;equals(Ljava/lang/Object;Ljava/lang/Object;)Z")}, require = 0)
    private static boolean geckolib_skipGeckolibIdOnCompare(Object a, Object b, Operation<Boolean> original) {
        MergedComponentMap components;
        block5: {
            block4: {
                if (((Boolean)original.call(new Object[]{a, b})).booleanValue()) {
                    return true;
                }
                if (!(a instanceof MergedComponentMap)) break block4;
                components = (MergedComponentMap)a;
                if (b instanceof MergedComponentMap) break block5;
            }
            return false;
        }
        MergedComponentMap components2 = (MergedComponentMap)b;
        return GeckoLibUtil.areComponentsMatchingIgnoringGeckoLibId(components, components2);
    }
}

