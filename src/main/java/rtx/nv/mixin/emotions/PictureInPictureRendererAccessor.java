package rtx.nv.mixin.emotions;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpecialGuiElementRenderer.class)
public interface PictureInPictureRendererAccessor {
    @Accessor("textureView")
    public GpuTextureView nv_textureView();
}
