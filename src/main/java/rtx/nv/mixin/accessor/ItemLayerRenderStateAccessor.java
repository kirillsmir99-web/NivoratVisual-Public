package rtx.nv.mixin.accessor;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.json.Transformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.LayerRenderState.class)
public interface ItemLayerRenderStateAccessor {
    @Accessor("glint")
    public ItemRenderState.Glint nv_getFoilType();

    @Accessor("useLight")
    public boolean nv_getUsesBlockLight();

    @Accessor("specialModelType")
    public SpecialModelRenderer<Object> nv_getSpecialRenderer();

    @Accessor("transform")
    public Transformation nv_getItemTransform();

    @Accessor("tints")
    public int[] nv_getTintLayers();
}
