package rtx.nv.mixin;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.NoRender;

@Mixin(net.minecraft.client.gui.hud.InGameOverlayRenderer.class)

public abstract class ScreenEffectRendererNoRenderMixin {
    @Inject(method="renderFireOverlay", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private static void nv_noRenderFire(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
        if (NoRender.isActive("\u041e\u0433\u043e\u043d\u044c")) {
            ci.cancel();
        }
    }
}

