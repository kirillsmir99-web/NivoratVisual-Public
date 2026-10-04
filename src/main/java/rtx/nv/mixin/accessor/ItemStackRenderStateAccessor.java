package rtx.nv.mixin.accessor;

import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.class)
public interface ItemStackRenderStateAccessor {
    @Accessor("layers")
    public ItemRenderState.LayerRenderState[] nv_getLayers();

    @Accessor("layerCount")
    public int nv_getActiveLayerCount();
}
