package rtx.nv.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.inventory.HandledScreenEvent;
import rtx.nv.api.modules.impl.Visuals.BetterMinecraft;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    protected int x;
    @Shadow
    protected int y;
    @Shadow
    @Nullable
    protected Slot focusedSlot;
    @Unique
    private boolean nv_panelAnimated;

    @Inject(method="init", at={@At(value="TAIL")}, require = 0)
    private void nv_trackInventoryOpen(CallbackInfo ci) {
        BetterMinecraft.markInventoryOpen();
    }

    @Inject(method="render", at={@At(value="HEAD")}, require = 0)
    private void nv_animatePanel(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        this.nv_panelAnimated = BetterMinecraft.inventoryAnimationEnabled();
        if (!this.nv_panelAnimated) {
            return;
        }
        float scale = BetterMinecraft.inventoryScale();
        float slide = BetterMinecraft.inventorySlideOffset();
        if (scale < 0.999f || Math.abs(slide) > 0.01f) {
            float cx = (float) graphics.getScaledWindowWidth() / 2.0f;
            float cy = (float) graphics.getScaledWindowHeight() / 2.0f;
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(cx, cy + slide);
            graphics.getMatrices().scale(scale, scale);
            graphics.getMatrices().translate(-cx, -(cy + slide));
            this.nv_panelAnimated = true;
        } else {
            this.nv_panelAnimated = false;
        }
    }

    @Inject(method="render", at={@At(value="TAIL")}, require = 0)
    private void nv_endPanelAnimation(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.nv_panelAnimated) {
            this.nv_panelAnimated = false;
            graphics.getMatrices().popMatrix();
        }
        EventBus.get().post(new HandledScreenEvent(graphics, this.x, this.y, this.focusedSlot));
    }
}
