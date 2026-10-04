package rtx.nv.mixin;
import java.util.List;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.ui.UI;
import rtx.nv.utils.render.post.GuiRenderStateLayerAccessor;

@Mixin(net.minecraft.client.gui.render.state.GuiRenderState.class)

public abstract class GuiRenderStateMixin
implements GuiRenderStateLayerAccessor {
    @Unique
    private int nv_layerSerial;
    @Shadow
    private int blurLayer;
    @Shadow
    @Final
    private List<?> rootLayers;

    @Inject(method="applyBlur", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_mergeDuplicateBlurMark(CallbackInfo ci) {
        if (this.blurLayer != Integer.MAX_VALUE) {
            this.blurLayer = this.rootLayers.size() - 1;
            UI.requestVanillaBlurAtSplit();
            ci.cancel();
        }
    }

    @Override
    public int nv_getLayerSerial() {
        return this.nv_layerSerial;
    }

    @Inject(method="createNewRootLayer", at={@At(value="RETURN")}, require = 0)
    private void nv_trackNextStratum(CallbackInfo ci) {
        ++this.nv_layerSerial;
    }

    @Inject(method="goUpLayer", at={@At(value="RETURN")}, require = 0)
    private void nv_trackUpLayer(CallbackInfo ci) {
        ++this.nv_layerSerial;
    }

    @Inject(method="clear", at={@At(value="HEAD")}, require = 0)
    private void nv_resetLayerSerial(CallbackInfo ci) {
        this.nv_layerSerial = 0;
    }
}

