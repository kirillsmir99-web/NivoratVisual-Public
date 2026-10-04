package rtx.nv.mixin;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.player.AttackEntityEvent;
import rtx.nv.api.modules.impl.Visuals.custompet.entity.CustomPetEntity;

@Mixin(MinecraftClient.class)
public abstract class ClientAttackFallbackMixin {
    @Inject(method="doAttack", at={@At(value="HEAD")}, require = 0)
    private void nv_clientAttackFallback(CallbackInfoReturnable<Boolean> cir) {
        try {
            MinecraftClient mc = (MinecraftClient)(Object)this;
            ClientPlayerEntity player = mc.player;
            if (player == null || mc.world == null) {
                return;
            }
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                return;
            }
            double reach = player.getEntityInteractionRange();
            if (reach <= 0.0) {
                return;
            }
            Vec3d eye = player.getCameraPosVec(1.0f);
            Vec3d look = player.getRotationVec(1.0f);
            Vec3d end = eye.add(look.x * reach, look.y * reach, look.z * reach);
            Entity best = null;
            double bestDist = Double.MAX_VALUE;
            for (Entity entity : mc.world.getOtherEntities((Entity)player, player.getBoundingBox().stretch(look.x * reach, look.y * reach, look.z * reach).expand(1.0), e -> e != null && e != player && !e.isSpectator() && !(e instanceof CustomPetEntity))) {
                double d;
                Optional clip = entity.getBoundingBox().expand(0.1).raycast(eye, end);
                if (!clip.isPresent() || !((d = eye.squaredDistanceTo((Vec3d)clip.get())) < bestDist)) continue;
                bestDist = d;
                best = entity;
            }
            if (best != null) {
                EventBus.get().post(new AttackEntityEvent(best, true));
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}

