package rtx.nv.api.mods.geckolib.renderer.internal;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.joml.Vector3fc;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;

public class GeckolibItemSpecialRenderer<T extends Item & GeoAnimatable>
implements SpecialModelRenderer<Item> {
    @SuppressWarnings("unchecked")
    private T makeCovariantItem(Item item) {
        return item instanceof GeoAnimatable ? (T) item : null;
    }

    @Override
    public Item getData(ItemStack stack) {
        return stack.getItem();
    }

    @Override
    public void render(Item data, ItemDisplayContext displayContext, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, boolean glint, int seed) {
    }

    @Override
    public void collectVertices(Consumer<Vector3fc> consumer) {
    }

    public Object extractArgument(ItemStack itemStack, Object renderState, ItemDisplayContext displayContext, Object level, Object itemOwner) {
        return this.getData(itemStack);
    }

    public static class Unbaked
    implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
            return new GeckolibItemSpecialRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> getCodec() {
            return MAP_CODEC;
        }
    }
}
