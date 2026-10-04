package rtx.nv.mixin;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import rtx.nv.utils.render.post.handsflame.HandsFlameRenderer;
import rtx.nv.utils.render.post.handsflame.HandsItemHitboxTracker;
import rtx.nv.utils.render.post.handsflame.IrisShaderCompat;
import rtx.nv.utils.render.post.shaderhands.ShaderHandsRenderer;
import rtx.nv.utils.render.underhand.UnderHand2D;

@Mixin(net.minecraft.client.render.GameRenderer.class)

public abstract class GameRendererHandsMixin {
    @Inject(method="renderHand", at={@At(value="HEAD")}, require = 0)
    private void nv_captureHandsSceneBefore(float partialTicks, boolean renderItem, Matrix4f projectionMatrix, CallbackInfo ci) {
        boolean irisActive = IrisShaderCompat.isShaderPackInUse();
        GuiLayerBlurRenderer.compositeRemotePanels();
        UnderHand2D.renderNow();
        ShaderHandsRenderer.beginHandFrame();
        HandsItemHitboxTracker.captureProjection((Matrix4fc)projectionMatrix);
        if (irisActive) {
            return;
        }
        HandsFlameRenderer.captureBeforeHandRender();
    }

    @Inject(method="renderHand", at={@At(value="RETURN")}, require = 0)
    private void nv_captureHandsSceneAfter(float partialTicks, boolean renderItem, Matrix4f projectionMatrix, CallbackInfo ci) {
        GuiLayerBlurRenderer.snapshotHandDepth();
        if (IrisShaderCompat.isShaderPackInUse()) {
            return;
        }
        HandsFlameRenderer.captureAfterHandRender();
        HandsFlameRenderer.renderCapturedHandsFlame();
    }
}

