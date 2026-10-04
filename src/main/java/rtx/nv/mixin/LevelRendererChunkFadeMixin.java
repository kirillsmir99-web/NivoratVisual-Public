package rtx.nv.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gl.DynamicUniforms;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.client.render.WorldRenderer.class)
public abstract class LevelRendererChunkFadeMixin {
    @WrapOperation(method="renderBlockLayers", at={@At(value="NEW", target="(Lorg/joml/Matrix4fc;IIIFII)Lnet/minecraft/class_11282$class_12294;")}, require = 0)
    private DynamicUniforms.ChunkSectionsValue nv_fullSectionVisibility(Matrix4fc matrix, int x, int y, int z, float visibility, int a, int b, Operation<DynamicUniforms.ChunkSectionsValue> original) {
        return (DynamicUniforms.ChunkSectionsValue)original.call(matrix, x, y, z, 1.0f, a, b);
    }
}
