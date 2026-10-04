package rtx.nv.mixin.shulkerview;
import java.util.function.Consumer;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Utils.ShulkerPreview;

@Mixin(net.minecraft.component.type.ContainerComponent.class)

public abstract class ItemContainerContentsMixin {
    @Inject(method="appendTooltip", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_shulkerPreviewHideVanillaContainerTooltip(Item.TooltipContext context, Consumer<Text> consumer, TooltipType flag, ComponentsAccess components, CallbackInfo ci) {
        if (ShulkerPreview.enabled()) {
            ci.cancel();
        }
    }
}

