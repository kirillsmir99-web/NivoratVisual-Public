package rtx.nv.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.SectionBuilder;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.utils.render.wave.WindWaveTagger;

@Mixin(net.minecraft.client.render.chunk.SectionBuilder.class)

public class SectionCompilerMixin {
    @WrapOperation(method="build", at={@At(value="INVOKE", target="Lnet/minecraft/client/render/block/BlockRenderManager;method_3355(Lnet/minecraft/class_2680;Lnet/minecraft/class_2338;Lnet/minecraft/class_1920;Lnet/minecraft/class_4587;Lnet/minecraft/class_4588;ZLjava/util/List;)V")}, require = 0)
    private void nv_tagWavingVertices(BlockRenderManager dispatcher, BlockState state, BlockPos pos, BlockRenderView level, MatrixStack poseStack, VertexConsumer consumer, boolean checkSides, List<BlockModelPart> parts, Operation<Void> original) {
        VertexConsumer target = consumer;
        try {
            target = WindWaveTagger.wrap(consumer, state, pos, level);
        }
        catch (Throwable ignored) {
            target = consumer;
        }
        original.call(new Object[]{dispatcher, state, pos, level, poseStack, target, checkSides, parts});
    }
}

