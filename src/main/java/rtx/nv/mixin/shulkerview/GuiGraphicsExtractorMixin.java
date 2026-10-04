package rtx.nv.mixin.shulkerview;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rtx.nv.api.mods.shulkerview.hook.ShulkerPreviewGuiGraphics;

@Mixin(net.minecraft.client.gui.DrawContext.class)

public abstract class GuiGraphicsExtractorMixin
implements ShulkerPreviewGuiGraphics {
    @Unique
    private int nv_shulkerPreviewMouseX = Integer.MIN_VALUE;
    @Unique
    private int nv_shulkerPreviewMouseY = Integer.MIN_VALUE;

    @Override
    public int nv_getMouseX() {
        return this.nv_shulkerPreviewMouseX;
    }

    @Override
    public int nv_getMouseY() {
        return this.nv_shulkerPreviewMouseY;
    }

    @Override
    public void nv_setMouse(int mouseX, int mouseY) {
        this.nv_shulkerPreviewMouseX = mouseX;
        this.nv_shulkerPreviewMouseY = mouseY;
    }
}

