package rtx.nv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtx.nv.api.modules.impl.Utils.Globals;
import rtx.nv.api.modules.impl.Visuals.HitColor;
import rtx.nv.api.modules.impl.Visuals.NameTags;
import rtx.nv.utils.net.ClientPresence;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @Inject(method="updateRenderState*", at={@At(value="RETURN")}, cancellable=true, require = 0)
    private void nv_onUpdateRenderState(T entity, S state, float tickDelta, CallbackInfo ci) {
        boolean nameHidden = NameTags.hidesNameTagFor(entity);
        if (nameHidden) {
            state.displayName = null;
        }
        if (!nameHidden && NameTags.shouldShowSelfTagVanilla(entity)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (entity == mc.player && state.displayName == null && !mc.options.getPerspective().isFirstPerson()) {
                state.displayName = entity.getDisplayName();
                state.nameLabelPos = entity.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getYaw());
            }
        }
        if (state.displayName != null && entity instanceof PlayerEntity badgePlayer) {
            if (Globals.tagsBadge() && ClientPresence.INSTANCE.isNvUser(badgePlayer.getGameProfile().name())) {
                state.displayName = Text.empty().append(Text.literal("\ue000").setStyle(Style.EMPTY.withFont(StyleSpriteSource.DEFAULT).withColor(9081843))).append(Text.literal(" ").append(state.displayName)).append(Text.literal("  "));
            }
        }
        HitColor.captureTint(state, entity);
        if (HitColor.shouldTint(entity)) {
            state.hurt = false;
        }
    }

    @Inject(method="getMixColor", at={@At(value="RETURN")}, cancellable=true, require = 1)
    private void nv_customHurtTint(S state, CallbackInfoReturnable<Integer> cir) {
        Integer tint = HitColor.tintFor(state);
        if (tint != null) {
            cir.setReturnValue(tint);
        }
    }

    @Inject(method = "getBoundingBox(Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/util/math/Box;", at = @At("RETURN"), cancellable = true, require = 0)
    private void nv_expandWingsBoundingBox(T entity, CallbackInfoReturnable<net.minecraft.util.math.Box> cir) {
        if (entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity player) {
            rtx.nv.api.modules.impl.Visuals.Wings wings = rtx.nv.api.modules.impl.Visuals.Wings.getInstance();
            if (wings != null && wings.isEnabled() && wings.shouldRender(player)) {
                net.minecraft.util.math.Box box = cir.getReturnValue();
                if (box != null) {
                    cir.setReturnValue(box.expand(1.8, 1.5, 1.8));
                }
            }
        }
    }
}
