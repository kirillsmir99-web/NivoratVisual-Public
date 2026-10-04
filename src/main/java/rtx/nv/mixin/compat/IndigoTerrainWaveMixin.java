package rtx.nv.mixin.compat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.utils.render.wave.WindWaveTagger;

@Mixin(targets = {"net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext"}, remap = false)

public abstract class IndigoTerrainWaveMixin {
    @Shadow(remap=false)
    @Final
    protected BlockRenderInfo blockInfo;

    @WrapOperation(method={"bufferQuad"}, at={@At(value="INVOKE", target="Lnet/fabricmc/fabric/impl/client/indigo/renderer/render/AbstractTerrainRenderContext;getVertexConsumer(Lnet/minecraft/class_11515;)Lnet/minecraft/class_4588;")}, require = 0)
    private VertexConsumer nv_tagIndigoVertices(AbstractTerrainRenderContext instance, BlockRenderLayer layer, Operation<VertexConsumer> original) {
        VertexConsumer consumer = (VertexConsumer)original.call(new Object[]{instance, layer});
        try {
            if (this.blockInfo != null && this.blockInfo.blockState != null && this.blockInfo.blockPos != null && this.blockInfo.blockView != null) {
                return WindWaveTagger.wrap(consumer, this.blockInfo.blockState, this.blockInfo.blockPos, this.blockInfo.blockView, layer);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return consumer;
    }
}

