package rtx.nv.mixin.geckolib.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Optional;
import net.minecraft.component.Component;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import net.minecraft.screen.sync.ComponentChangesHash;
import net.minecraft.screen.sync.ItemStackHash;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;

@Mixin(targets = {"net.minecraft.screen.sync.ItemStackHash$Impl"})
public class HashedStackMixin {
    @WrapOperation(method={"hashEquals"}, at={@At(value="INVOKE", target="Lnet/minecraft/screen/sync/ComponentChangesHash;hashEquals(Lnet/minecraft/component/ComponentChanges;Lnet/minecraft/screen/sync/ComponentChangesHash$ComponentHasher;)Z")}, require = 1)
    public boolean geckolib_allowLazyStackIdParity(ComponentChangesHash patchMap, ComponentChanges componentPatch, ComponentChangesHash.ComponentHasher hasher, Operation<Boolean> original) {
        int localStackHashedId;
        if (!original.call(patchMap, componentPatch, hasher)) {
            return false;
        }
        ComponentType<Long> componentType = GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get();
        int remoteStackHashedId = patchMap.addedComponents().getOrDefault(componentType, Integer.MIN_VALUE);
        return remoteStackHashedId == (localStackHashedId = Optional.ofNullable(componentPatch.get(componentType)).map(optional -> optional.map(value -> (Integer)hasher.apply(new Component<>(componentType, value))).orElse(Integer.MIN_VALUE)).orElse(Integer.MIN_VALUE).intValue());
    }
}
