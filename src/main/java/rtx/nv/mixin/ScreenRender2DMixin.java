package rtx.nv.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.utils.render.render2d.Render2D;

@Mixin(net.minecraft.client.gui.screen.Screen.class)
public abstract class ScreenRender2DMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), require = 0)
    private void nv_r2dBegin(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Render2D.beginFrame(graphics);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("RETURN"), require = 0)
    private void nv_r2dFlush(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Render2D.flush();
    }
}
