package rtx.nv.test.mixin;

import rtx.nv.test.FrameMetrics;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class RenderMetricsMixin {
    @Inject(method="render", at=@At("HEAD"), require=1)
    private void nv_cpuStart(RenderTickCounter counter, boolean tick, CallbackInfo ci) { FrameMetrics.head(); }
    @Inject(method="render", at=@At("RETURN"), require=1)
    private void nv_cpuEnd(RenderTickCounter counter, boolean tick, CallbackInfo ci) { FrameMetrics.tail(); }
}
