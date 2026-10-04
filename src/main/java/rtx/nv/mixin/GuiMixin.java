package rtx.nv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.render.DrawEvent;
import rtx.nv.api.events.impl.render.HudRenderEvent;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.PotionsModule;
import rtx.nv.api.modules.impl.Utils.guishare.RemoteGuiWorld;
import rtx.nv.api.modules.impl.Visuals.BetterMinecraft;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionWheelOverlay;
import rtx.nv.api.ui.BaseScreen;
import rtx.nv.api.ui.UI;
import rtx.nv.utils.combat.CombatTagTracker;
import rtx.nv.utils.render.others.LoadingVisualGuard;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.warmup.Render2DWarmup;

@Mixin(net.minecraft.client.gui.hud.InGameHud.class)
public abstract class GuiMixin {
    @Shadow
    @Final
    private MinecraftClient client;
    @Unique
    private boolean nv_debugEarly;

    @Shadow
    private Text overlayMessage;
    @Shadow
    private int overlayRemaining;

    @Shadow
    public abstract void renderDebugHud(DrawContext context);

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", shift = At.Shift.BEFORE), require = 0)
    private void nv_renderHudUnderVanilla(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        if (LoadingVisualGuard.shouldSuppressHud(this.client)) {
            return;
        }
        RectUtil.setHudRendering(true);
        try {
            EventBus.get().post(new HudRenderEvent(graphics, deltaTracker));
        } finally {
            RectUtil.setHudRendering(false);
        }
        graphics.createNewRootLayer();
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), require = 0)
    private void nv_renderRemoteGuiPanels(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        if (LoadingVisualGuard.shouldSuppressHud(this.client)) {
            return;
        }
        RemoteGuiWorld.renderPanels(graphics);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("RETURN"), require = 0)
    private void nv_renderTopLayer(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        if (LoadingVisualGuard.shouldSuppressHud(this.client)) {
            Render2DWarmup.runWarmupFrame(graphics);
            return;
        }
        EventBus.get().post(new DrawEvent(graphics, deltaTracker));
        UI.renderClosingPanelOverHud(graphics);
        BaseScreen.renderClosingOverlay(graphics);
        EmotionWheelOverlay.render(graphics);
        if (UI.isOpen() && this.client.getDebugHud() != null && this.client.getDebugHud().shouldShowDebugHud()) {
            this.nv_debugEarly = true;
            this.renderDebugHud(graphics);
            this.nv_debugEarly = false;
        }
    }

    @Inject(method = "renderDebugHud(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nv_moveDebugUnderClickGui(DrawContext graphics, CallbackInfo ci) {
        if (!this.nv_debugEarly && UI.isOpen()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nv_hideVanillaEffects(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        PotionsModule potions = ModuleManager.get().get(PotionsModule.class);
        if (potions != null && potions.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFood(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;II)V", at = @At("RETURN"), require = 0)
    private void nv_renderSaturation(DrawContext graphics, PlayerEntity player, int top, int right, CallbackInfo ci) {
        BetterMinecraft.renderSaturation(graphics, player, top, right);
    }

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true, require = 0)
    private void nv_stabilizeOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        if (message != null) {
            String text = message.getString();
            CombatTagTracker.onActionBarMessage(text);
            if (text.trim().isEmpty() && CombatTagTracker.isInCombat()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderOverlayMessage", at = @At("HEAD"), require = 0)
    private void nv_preventOverlayBlink(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (CombatTagTracker.isInCombat()) {
            if (this.overlayRemaining < 40) {
                this.overlayRemaining = 40;
            }
        }
    }
}
