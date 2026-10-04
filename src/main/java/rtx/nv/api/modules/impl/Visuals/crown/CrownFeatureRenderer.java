package rtx.nv.api.modules.impl.Visuals.crown;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import rtx.nv.api.modules.impl.Visuals.Crown;

public class CrownFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    public CrownFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float yaw, float pitch) {
        Crown crown = Crown.getInstance();
        if (crown == null || !crown.isEnabled() || state.invisible || state.baby) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return;
        }

        Entity entity = mc.world.getEntityById(state.id);
        if (!(entity instanceof AbstractClientPlayerEntity player) || !crown.shouldRender(player)) {
            return;
        }

        if (player == mc.player && mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            return;
        }

        boolean hasHelmet = !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty();
        RenderLayer renderLayer = RenderLayers.lightning();

        matrices.push();
        try {
            getContextModel().getRootPart().applyTransform(matrices);
            getContextModel().head.applyTransform(matrices);

            queue.submitCustom(matrices, renderLayer, (pose, vertices) -> {
                crown.renderModel(pose, vertices, hasHelmet);
            });
        } finally {
            matrices.pop();
        }
    }
}
