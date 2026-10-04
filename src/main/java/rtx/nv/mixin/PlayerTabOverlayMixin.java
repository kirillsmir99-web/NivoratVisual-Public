package rtx.nv.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Utils.Globals;
import rtx.nv.api.modules.impl.Visuals.BetterMinecraft;
import rtx.nv.utils.animations.TabListAnimationAccess;
import rtx.nv.utils.net.ClientPresence;
import rtx.nv.utils.render.TabBadgeRenderer;

@Mixin(PlayerListHud.class)
public abstract class PlayerTabOverlayMixin
implements TabListAnimationAccess {
    @Unique
    private long nv_animationStart;
    @Unique
    private long nv_animationDuration;
    @Unique
    private float nv_animationFrom;
    @Unique
    private float nv_animationTarget;
    @Unique
    private boolean nv_visible;
    @Unique
    private boolean nv_scaledForAnimation;

    @Inject(method="setVisible", at={@At(value="HEAD")}, require = 0)
    private void nv_trackVisibility(boolean visible, CallbackInfo ci) {
        if (visible != this.nv_visible) {
            float current = this.nv_currentScale();
            this.nv_visible = visible;
            this.nv_animationFrom = current;
            this.nv_animationTarget = visible ? 1.0f : 0.0f;
            this.nv_animationStart = System.currentTimeMillis();
            long baseDur = BetterMinecraft.tabAnimDurationMs();
            this.nv_animationDuration = Math.max(1L, (long)Math.round((float)baseDur * Math.abs(this.nv_animationTarget - this.nv_animationFrom)));
        }
    }

    @Inject(method="render", at={@At(value="HEAD")}, require = 0)
    private void nv_beginTabAnimation(DrawContext graphics, int windowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        this.nv_scaledForAnimation = BetterMinecraft.tabAnimationEnabled();
        if (!this.nv_scaledForAnimation) {
            return;
        }
        float scale = Math.max(0.01f, Math.min(1.15f, this.nv_currentScale()));
        graphics.getMatrices().pushMatrix();
        graphics.getMatrices().translate((float)windowWidth / 2.0f, 10.0f);
        graphics.getMatrices().scale(scale, scale);
        graphics.getMatrices().translate((float)(-windowWidth) / 2.0f, -10.0f);
    }

    @Inject(method="render", at={@At(value="RETURN")}, require = 0)
    private void nv_endTabAnimation(DrawContext graphics, int windowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        if (this.nv_scaledForAnimation) {
            graphics.getMatrices().popMatrix();
            this.nv_scaledForAnimation = false;
        }
    }

    @WrapOperation(method="render", at={@At(value="INVOKE", target="Lnet/minecraft/client/font/TextRenderer;method_27525(Lnet/minecraft/class_5348;)I", ordinal=0)}, require = 0)
    private int nv_includeBadgeInFullNameWidth(TextRenderer font, StringVisitable fullServerName, Operation<Integer> original, @Local PlayerListEntry playerInfo) {
        int fullServerNameWidth = (Integer)original.call(new Object[]{font, fullServerName});
        if (!Globals.tabBadge()) {
            return fullServerNameWidth;
        }
        return fullServerNameWidth + TabBadgeRenderer.extraWidth();
    }

    @WrapOperation(method="render", at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/DrawContext;method_27535(Lnet/minecraft/class_327;Lnet/minecraft/class_2561;III)V")}, require = 0)
    private void nv_drawFullTabName(DrawContext graphics, TextRenderer font, Text fullServerName, int x, int y, int color, Operation<Void> original, @Local PlayerListEntry playerInfo) {
        if (!Globals.tabBadge()) {
            original.call(new Object[]{graphics, font, fullServerName, x, y, color});
            return;
        }
        original.call(new Object[]{graphics, font, fullServerName, x + TabBadgeRenderer.extraWidth(), y, color});
        if (PlayerTabOverlayMixin.nv_hasTabBadge(playerInfo)) {
            TabBadgeRenderer.drawBadge((DrawContext)graphics, (TextRenderer)font, (int)x, (int)y);
        }
    }

    @Unique
    private static boolean nv_hasTabBadge(PlayerListEntry playerInfo) {
        return playerInfo != null && Globals.tabBadge() && ClientPresence.INSTANCE.isNvUser(playerInfo.getProfile().name());
    }

    @Override
    public boolean nv_shouldRenderClosingTab() {
        return BetterMinecraft.tabAnimationEnabled() && !this.nv_visible && this.nv_currentScale() > 0.01f;
    }

    @Unique
    private float nv_currentScale() {
        long duration = Math.max(1L, this.nv_animationDuration);
        float progress = Math.min(1.0f, (float)(System.currentTimeMillis() - this.nv_animationStart) / (float)duration);
        float eased = this.nv_animationTarget > this.nv_animationFrom ? this.nv_easeOutBack(progress) : this.nv_easeInBack(progress);
        return this.nv_animationFrom + (this.nv_animationTarget - this.nv_animationFrom) * eased;
    }

    @Unique
    private float nv_easeOutBack(float value) {
        float c1 = BetterMinecraft.isReducedIntensity() ? 0.6f : 1.70158f;
        float c3 = c1 + 1.0f;
        float t = value - 1.0f;
        return 1.0f + c3 * t * t * t + c1 * t * t;
    }

    @Unique
    private float nv_easeInBack(float value) {
        float c1 = BetterMinecraft.isReducedIntensity() ? 0.6f : 1.70158f;
        float c3 = c1 + 1.0f;
        return c3 * value * value * value - c1 * value * value;
    }
}
