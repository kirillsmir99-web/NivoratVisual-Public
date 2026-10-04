package rtx.nv.mixin.emotions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionPreviewRenderer;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionPreviewState;

@Mixin(net.minecraft.client.gui.render.GuiRenderer.class)

public abstract class GuiRendererEmotionPipMixin {
    @Shadow
    @Final
    @Mutable
    private Map<Class<? extends SpecialGuiElementRenderState>, SpecialGuiElementRenderer<?>> specialElementRenderers;

    @Inject(method="<init>", at={@At(value="RETURN")}, require = 0)
    private void nv_registerEmotionPreview(GuiRenderState renderState, VertexConsumerProvider.Immediate bufferSource, OrderedRenderCommandQueue collector, RenderDispatcher features, List<SpecialGuiElementRenderer<?>> renderers, CallbackInfo ci) {
        HashMap extended = new HashMap(this.specialElementRenderers);
        extended.put(EmotionPreviewState.class, new EmotionPreviewRenderer(bufferSource));
        this.specialElementRenderers = extended;
    }
}

