package rtx.nv.mixin;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.ItemHighlight;

@Mixin(net.minecraft.client.gui.screen.ingame.HandledScreen.class)

public abstract class PotionHighlightContainerMixin {
    @Inject(method="drawSlot", at={@At(value="HEAD")}, require = 0)
    private void nv_slotHighlightBackground(DrawContext graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (slot == null || graphics == null) {
            return;
        }
        ItemHighlight module = ItemHighlight.getInstance();
        if (module == null) {
            return;
        }
        int argb = module.backgroundFor(slot.getStack(), false);
        if (argb != 0) {
            module.drawSlotBackground(graphics, slot.x, slot.y, argb);
        }
    }
}

