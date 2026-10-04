package rtx.nv.mixin;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Interface.CustomHotbar;
import rtx.nv.api.modules.impl.Visuals.ItemHighlight;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;

@Mixin(net.minecraft.client.gui.hud.InGameHud.class)

public abstract class PotionHighlightHotbarMixin {
    @Inject(method="renderHotbarItem", at={@At(value="HEAD")}, require = 0)
    private void nv_hotbarHighlightBackground(DrawContext graphics, int x, int y, RenderTickCounter deltaTracker, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        if (graphics == null || stack == null || stack.isEmpty()) {
            return;
        }
        ItemHighlight module = ItemHighlight.getInstance();
        if (module == null) {
            return;
        }
        int argb = module.backgroundFor(stack, true);
        if (argb != 0) {
            if (CustomHotbar.isActive()) {
                Render2D.beginFrame(graphics);
                module.drawRoundedSlotBackground(x, y, 16.0f, argb, 1.0f);
                Render2D.flush();
            } else {
                module.drawSlotBackground(graphics, x, y, argb);
            }
        }
    }
}

