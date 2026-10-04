package rtx.nv.mixin.shulkerview;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.mods.shulkerview.hook.ShulkerPreviewGuiGraphics;

@Mixin(net.minecraft.client.gui.screen.Screen.class)

public abstract class ScreenMixin {
    @Inject(method="renderWithTooltip", at={@At(value="HEAD")}, require=0)
    private void nv_shulkerPreviewCaptureMouse(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ((ShulkerPreviewGuiGraphics)graphics).nv_setMouse(mouseX, mouseY);
    }
}

