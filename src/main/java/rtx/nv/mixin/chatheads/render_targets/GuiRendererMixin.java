package rtx.nv.mixin.chatheads.render_targets;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.mods.chatheads.ChatHeads;

@Mixin(net.minecraft.client.gui.render.GuiRenderer.class)

public abstract class GuiRendererMixin {
    @Inject(method="prepare", at={@At(value="HEAD")}, require = 0)
    public void chatheads_isInsideGui(CallbackInfo ci) {
        ChatHeads.customHeadRendering = true;
    }

    @Inject(method="prepare", at={@At(value="RETURN")}, require = 0)
    public void chatheads_isOutsideGui(CallbackInfo ci) {
        ChatHeads.customHeadRendering = false;
    }
}

