package rtx.nv.mixin.geckolib.common;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.sync.TrackedSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;

@Mixin(targets = {"net.minecraft.screen.sync.TrackedSlot$Impl"})
public class SynchronizedRemoteSlotMixin {
    @WrapOperation(method={"isInSync", "isInSync"}, at={@At(value="INVOKE", target="Lnet/minecraft/item/ItemStack;method_7973(Lnet/minecraft/class_1799;Lnet/minecraft/class_1799;)Z")}, require = 0)
    public boolean geckolib_allowLazyStackIdParity(ItemStack remoteStack, ItemStack localStack, Operation<Boolean> original) {
        return (Boolean)original.call(new Object[]{remoteStack, localStack}) != false && ((Number)remoteStack.getOrDefault(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get(), (Object)Integer.MIN_VALUE)).equals(localStack.getOrDefault(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get(), (Object)Integer.MIN_VALUE));
    }
}

