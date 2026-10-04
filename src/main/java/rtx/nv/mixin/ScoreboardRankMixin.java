package rtx.nv.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.drags.components.ScoreboardComp;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.ScoreboardModule;

@Mixin(net.minecraft.client.gui.hud.InGameHud.class)
public abstract class ScoreboardRankMixin {
    @Unique
    private boolean nv_scoreboardTranslated;

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nv_preScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        this.nv_scoreboardTranslated = false;
        ScoreboardModule module = ModuleManager.get().get(ScoreboardModule.class);
        if (module != null && module.isEnabled()) {
            if (module.hide.getValue()) {
                ci.cancel();
                return;
            }
        }
        ScoreboardComp comp = ScoreboardComp.get();
        if (comp != null && (module == null || !module.isEnabled() || module.customPosition.getValue())) {
            comp.updateFromObjective(context, objective);
            if (comp.isUserMoved()) {
                float scale = rtx.nv.utils.render.render2d.Render2DCoordinateSpace.guiIndependentScale();
                float targetVanillaX = comp.getDrag().getRenderX() * scale;
                float targetVanillaY = comp.getDrag().getRenderY() * scale;
                float dx = targetVanillaX - comp.getVanillaX();
                float dy = targetVanillaY - comp.getVanillaY();
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(dx, dy);
                this.nv_scoreboardTranslated = true;
            }
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("RETURN"), require = 0)
    private void nv_postScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (this.nv_scoreboardTranslated) {
            context.getMatrices().popMatrix();
            this.nv_scoreboardTranslated = false;
        }
    }

}
