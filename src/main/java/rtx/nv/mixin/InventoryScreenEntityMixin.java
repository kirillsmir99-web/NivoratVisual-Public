package rtx.nv.mixin;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rtx.nv.api.modules.impl.Visuals.BetterMinecraft;

@Mixin(net.minecraft.client.gui.screen.ingame.InventoryScreen.class)

public abstract class InventoryScreenEntityMixin {
    @Redirect(method="drawBackground", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;method_2486(Lnet/minecraft/class_332;IIIIIFFFLnet/minecraft/class_1309;)V"), require = 0)
    private void nv_slideInventoryEntity(DrawContext graphics, int x1, int y1, int x2, int y2, int size, float f, float mouseX, float mouseY, LivingEntity entity) {
        int off = Math.round(BetterMinecraft.inventorySlideOffset());
        InventoryScreen.drawEntity((DrawContext)graphics, (int)x1, (int)(y1 + off), (int)x2, (int)(y2 + off), (int)size, (float)f, (float)mouseX, (float)mouseY, (LivingEntity)entity);
    }
}

