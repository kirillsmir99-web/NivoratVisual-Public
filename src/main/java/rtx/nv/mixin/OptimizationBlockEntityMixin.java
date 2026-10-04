package rtx.nv.mixin;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtx.nv.api.modules.impl.Utils.Optimization;

@Mixin(BlockEntityRenderManager.class)

public abstract class OptimizationBlockEntityMixin {
    @Shadow
    private Vec3d cameraPos;

    @Inject(method="getRenderState", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private <E extends BlockEntity, S extends BlockEntityRenderState> void nv_cullDistantBlockEntities(E blockEntity, float partialTick, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay, CallbackInfoReturnable<S> cir) {
        if (this.cameraPos == null) {
            return;
        }
        double distSq = blockEntity.getPos().getSquaredDistance((Position)(Object)this.cameraPos);
        if (!Optimization.allowBlockEntity(distSq)) {
            cir.setReturnValue(null);
        }
    }
}

