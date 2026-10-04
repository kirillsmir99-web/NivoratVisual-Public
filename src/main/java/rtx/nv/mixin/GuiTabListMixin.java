package rtx.nv.mixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.utils.animations.TabListAnimationAccess;

@Mixin(net.minecraft.client.gui.hud.InGameHud.class)

public abstract class GuiTabListMixin {
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    private PlayerListHud playerListHud;

    @Inject(method="renderPlayerList", at={@At(value="TAIL")}, require = 0)
    private void nv_renderClosingTabList(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        TabListAnimationAccess animation;
        PlayerListHud playerListHud = this.playerListHud;
        if (!(playerListHud instanceof TabListAnimationAccess) || !(animation = (TabListAnimationAccess)playerListHud).nv_shouldRenderClosingTab()) {
            return;
        }
        if (this.client.world == null || this.client.options.playerListKey.isPressed()) {
            return;
        }
        Scoreboard scoreboard = this.client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
        this.playerListHud.render(graphics, graphics.getScaledWindowWidth(), scoreboard, objective);
    }
}

