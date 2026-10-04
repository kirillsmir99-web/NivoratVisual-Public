package rtx.nv.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.BetterMinecraft;
import rtx.nv.utils.render.anim.ItemMoveAnimator;

@Mixin(HandledScreen.class)
public abstract class SlotAnimationMixin {
    @Shadow
    protected int x;
    @Shadow
    protected int y;
    @Shadow
    @Final
    protected ScreenHandler handler;

    @Inject(method="render", at={@At(value="HEAD")}, require = 0)
    private void nv_beginSlotAnim(DrawContext graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (BetterMinecraft.itemMoveAnimationEnabled()) {
            ItemMoveAnimator.beginFrame(this.handler, mouseX, mouseY, this.x, this.y);
        }
    }

    @WrapOperation(method="drawSlot", at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;II)V")}, require = 0)
    private void nv_animateSlot(HandledScreen<?> instance, DrawContext graphics, Slot slot, int mouseX, int mouseY, Operation<Void> original) {
        float[] off = BetterMinecraft.itemMoveAnimationEnabled() ? ItemMoveAnimator.offset(slot) : null;
        if (off == null) {
            original.call(instance, graphics, slot, mouseX, mouseY);
            return;
        }
        graphics.getMatrices().pushMatrix();
        if (off[2] != 1.0f) {
            float cx = (float)slot.x + 8.0f;
            float cy = (float)slot.y + 8.0f;
            graphics.getMatrices().translate(cx, cy);
            graphics.getMatrices().scale(off[2], off[2]);
            graphics.getMatrices().translate(-cx, -cy);
        } else {
            graphics.getMatrices().translate(off[0], off[1]);
        }
        original.call(instance, graphics, slot, mouseX, mouseY);
        graphics.getMatrices().popMatrix();
    }
}
