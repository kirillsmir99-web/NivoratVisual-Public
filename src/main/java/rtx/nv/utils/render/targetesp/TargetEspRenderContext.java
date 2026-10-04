package rtx.nv.utils.render.targetesp;

import net.minecraft.entity.LivingEntity;

public record TargetEspRenderContext(
    LivingEntity target, float alpha, float partialTicks, long timeMs,
    int color1, int color2, float hurtProgress, float chainImpactProgress,
    float circleHeight, float brightness, boolean throughWalls
) {}
