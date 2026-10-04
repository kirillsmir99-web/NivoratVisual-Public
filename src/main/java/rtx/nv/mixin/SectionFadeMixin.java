package rtx.nv.mixin;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk.class)

public abstract class SectionFadeMixin {
    @Inject(method={"method_76298"}, at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_noChunkFade(long time, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Float.valueOf(1.0f));
    }

    @ModifyVariable(method={"method_76548"}, at=@At(value="HEAD"), argsOnly=true, require = 0)
    private long nv_zeroFadeDuration(long duration) {
        return 0L;
    }
}

