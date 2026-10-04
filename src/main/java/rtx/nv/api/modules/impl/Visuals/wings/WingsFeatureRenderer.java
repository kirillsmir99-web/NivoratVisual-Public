package rtx.nv.api.modules.impl.Visuals.wings;

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
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import rtx.nv.api.modules.impl.Visuals.Wings;
import rtx.nv.utils.render.wings.WingModel;
import rtx.nv.utils.render.wings.WingModelRegistry;

public class WingsFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private final java.util.Map<AbstractClientPlayerEntity, Motion> motions = new java.util.WeakHashMap<>();
    private static final class Motion {
        float age, glide, movement;
        float prevBodyYaw;
        float yawLag;
        float pitchLag;
        boolean initialized;
    }

    public WingsFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float yaw, float pitch) {
        Wings wings = Wings.getInstance();
        boolean preview = rtx.nv.api.ui.WingPreviewRenderer.isPreview(state);
        if (wings == null || (!preview && !wings.isEnabled()) || state.invisible || state.baby) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        AbstractClientPlayerEntity player = null;
        if (mc.world != null) {
            Entity entity = mc.world.getEntityById(state.id);
            if (entity instanceof AbstractClientPlayerEntity p) {
                player = p;
            }
        }
        if (player == null && mc.player != null && (state.id == mc.player.getId() || state.id == 0 || preview)) {
            player = mc.player;
        }
        if (player == null || (!preview && !wings.shouldRender(player))) {
            return;
        }

        if (!preview && player == mc.player && mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            return;
        }

        boolean hasChestItem = state.equippedChestStack != null && !state.equippedChestStack.isEmpty();
        boolean hasElytra = (hasChestItem && state.equippedChestStack.isOf(Items.ELYTRA)) || state.isGliding;

        if (!preview && wings.isOnlyWithElytra() && !hasElytra) {
            return;
        }

        String skinId = wings.getSelectedSkinId();
        WingModel model = WingModelRegistry.getModel(skinId);
        if (model == null) {
            return;
        }

        Identifier textureId = wings.getTexture(skinId);
        RenderLayer renderLayer = RenderLayers.armorCutoutNoCull(textureId);
        int renderLight = wings.isEmissive() ? 0x00F000F0 : light;

        matrices.push();
        try {
            if ("angel_leg".equals(skinId) || skinId.contains("leg")) {
                getContextModel().getRootPart().applyTransform(matrices);
                getContextModel().leftLeg.applyTransform(matrices);
                matrices.translate(0.0f, 0.45f, 0.0f);
            } else {
                getContextModel().getRootPart().applyTransform(matrices);
                getContextModel().body.applyTransform(matrices);

                // Upper back of torso (Minecraft torso: Y=0 neck, Y=3/16 shoulder blades, Z=2/16 back)
                matrices.translate(0.0f, 0.1875f, 0.145f);

                // If wearing armor chestplate, offset back slightly to avoid clipping through thick armor
                if (hasChestItem && !hasElytra) {
                    matrices.translate(0.0f, 0.0f, 0.055f);
                }
            }

            float scale = wings.getScale();
            if (scale != 1.0f) {
                matrices.scale(scale, scale, scale);
            }

            float age = state.age + (state.id & 31) * 0.37f;
            float motion = Math.clamp(state.limbSwingAmplitude, 0.0f, 1.0f);
            float speed = wings.getAnimSpeed();
            Motion previous = motions.computeIfAbsent(player, ignored -> new Motion());
            float delta = Math.clamp(age - previous.age, 0, 4);
            float follow = 1 - (float)Math.exp(-delta * .3f);
            previous.age = age;
            previous.glide += ((state.isGliding && !preview ? 1 : 0) - previous.glide) * follow;
            previous.movement += ((preview ? 0 : motion) - previous.movement) * follow;
            float glide = preview ? 0 : previous.glide;
            float movement = preview ? 0 : previous.movement;
            float limbSwing = state.limbSwingAnimationProgress;

            if (!preview) {
                float currentBodyYaw = state.bodyYaw;
                if (!previous.initialized) {
                    previous.prevBodyYaw = currentBodyYaw;
                    previous.initialized = true;
                }
                float deltaBodyYaw = net.minecraft.util.math.MathHelper.wrapDegrees(currentBodyYaw - previous.prevBodyYaw);
                previous.prevBodyYaw = currentBodyYaw;

                // Yaw inertia lag: when body turns quickly, wings lag in opposite direction and spring back softly
                float targetYawLag = Math.clamp(-deltaBodyYaw * 1.5f, -20.0f, 20.0f);
                previous.yawLag += (targetYawLag - previous.yawLag) * (1.0f - (float) Math.exp(-delta * 0.45f));

                // Vertical pitch lag: air resistance when falling or jumping
                float targetPitchLag = 0.0f;
                if (player.getVelocity() != null) {
                    targetPitchLag = (float) Math.clamp(-player.getVelocity().y * 10.0, -12.0f, 15.0f);
                }
                previous.pitchLag += (targetPitchLag - previous.pitchLag) * (1.0f - (float) Math.exp(-delta * 0.35f));

                // Aerodynamic drag: wings sweep backward behind the player when running forward
                float forwardSweep = Math.clamp(motion * 8.0f, 0.0f, 14.0f);

                if (previous.yawLag != 0.0f) {
                    matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(previous.yawLag));
                }
                if (previous.pitchLag != 0.0f) {
                    matrices.multiply(net.minecraft.util.math.RotationAxis.NEGATIVE_X.rotationDegrees(previous.pitchLag));
                }
                if (forwardSweep != 0.0f) {
                    matrices.multiply(net.minecraft.util.math.RotationAxis.NEGATIVE_X.rotationDegrees(forwardSweep));
                }
            }

            queue.submitCustom(matrices, renderLayer, (pose, vertices) -> {
                MatrixStack local = new MatrixStack();
                local.peek().copy(pose);
                model.render(local, vertices, renderLight, glide, limbSwing, movement, age, speed);
            });
        } finally {
            matrices.pop();
        }
    }
}
