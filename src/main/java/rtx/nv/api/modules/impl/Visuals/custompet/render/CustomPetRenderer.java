package rtx.nv.api.modules.impl.Visuals.custompet.render;

import net.minecraft.block.Blocks;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import rtx.nv.api.mods.geckolib.renderer.GeoEntityRenderer;
import rtx.nv.api.mods.geckolib.renderer.base.BoneSnapshots;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo;
import rtx.nv.api.modules.impl.Visuals.custompet.CustomPetVariant;
import rtx.nv.api.modules.impl.Visuals.custompet.entity.CustomPetEntity;
import rtx.nv.api.modules.impl.Visuals.custompet.model.CustomPetModel;
import rtx.nv.api.modules.impl.Visuals.custompet.render.CustomPetRenderState;

public class CustomPetRenderer extends GeoEntityRenderer<CustomPetEntity, CustomPetRenderState> {
    private static final float LILY_PAD_SCALE = 1.35f;
    private static final float LILY_PAD_Y_OFFSET = -0.008f;
    private static final float OWL_SCALE = 0.55f;
    private static final String[] OWL_EFFECT_BONES = new String[]{"trick_star", "stumble_star_left", "stumble_star_right", "celebrate_confetti_1", "celebrate_confetti_2", "celebrate_confetti_3", "signature"};
    private static final String[] OWL_GROUND_ROPE_BONES = new String[]{"rope_spin", "left_handle", "right_handle"};
    private static final String[] OWL_FLIGHT_ROPE_BONES = new String[]{"fly_rope_left", "fly_rope_right"};
    private static final String[] CONDITIONAL_BONES = new String[]{"body_default", "body_merchant", "leaf", "gardener_hat", "watering_can", "sourcerer_hat", "accessories", "fishing_rod", "fishing_rod_2", "fishing_rod_3", "guitar", "flute", "bongo", "bass", "umbrella", "umbrella2", "umbrella3", "fisherman_umbrella", "fisherman_umbrella2", "fisherman_umbrella3", "pride"};

    public CustomPetRenderer(EntityRendererFactory.Context context) {
        super(context, new CustomPetModel());
        this.shadowRadius = 0.35f;
    }

    @Override
    public void captureDefaultRenderState(CustomPetEntity entity, Void unused, CustomPetRenderState state, float tickDelta) {
        super.captureDefaultRenderState(entity, unused, state, tickDelta);
        // This imported Gecko port has plain tickets; LivingEntityRenderState fields are not extracted automatically.
        state.addGeckolibData(rtx.nv.api.mods.geckolib.constant.DataTickets.ENTITY_BODY_YAW, state.bodyYaw);
        state.addGeckolibData(rtx.nv.api.mods.geckolib.constant.DataTickets.ENTITY_PITCH, state.pitch);
        state.addGeckolibData(rtx.nv.api.mods.geckolib.constant.DataTickets.ENTITY_POSE, state.pose);
        state.addGeckolibData(rtx.nv.api.mods.geckolib.constant.DataTickets.IS_SHAKING, state.shaking);
        state.addGeckolibData(rtx.nv.api.mods.geckolib.constant.DataTickets.INVISIBLE_TO_PLAYER, state.invisibleToPlayer);
        if (entity != null) {
            state.addGeckolibData(CustomPetModel.AIRBORNE, entity.isAirborneMode());
            state.addGeckolibData(CustomPetModel.OWL, entity.isOwl());
            state.addGeckolibData(CustomPetModel.VARIANT, entity.getPetVariant().name());
            state.addGeckolibData(CustomPetModel.ROBOT_TYPE, entity.getRobotType());
        }
    }

    public void submit(CustomPetRenderState r, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState) {
        CustomPetVariant variant = CustomPetVariant.fromSerializedName(r.getOrDefaultGeckolibData(CustomPetModel.VARIANT, "SPIRIT"));
        if (variant == CustomPetVariant.SPIRIT || variant == CustomPetVariant.MOTH || variant == CustomPetVariant.MASCOT) r.light = 0x00F000F0;
        super.render(r, matrixStack, orderedRenderCommandQueue, cameraRenderState);
        if (!Boolean.TRUE.equals(((GeoRenderState)r).getOrDefaultGeckolibData(CustomPetModel.AIRBORNE, false))) {
            return;
        }
        if (Boolean.TRUE.equals(((GeoRenderState)r).getOrDefaultGeckolibData(CustomPetModel.OWL, false))) {
            return;
        }
        String string = ((GeoRenderState)r).getOrDefaultGeckolibData(CustomPetModel.VARIANT, CustomPetVariant.NITWIT.name());
        CustomPetVariant customPetVariant = CustomPetVariant.fromSerializedName(string);
        if (customPetVariant.isRobot() || customPetVariant.isFantasy()) {
            return;
        }
        matrixStack.push();
        matrixStack.translate(-0.675f, -0.008f, -0.675f);
        matrixStack.scale(1.35f, 1.0f, 1.35f);
        orderedRenderCommandQueue.submitBlock(matrixStack, Blocks.LILY_PAD.getDefaultState(), ((LivingEntityRenderState)r).light, OverlayTexture.DEFAULT_UV, ((LivingEntityRenderState)r).outlineColor);
        matrixStack.pop();
    }

    @Override
    public RenderLayer getRenderType(CustomPetRenderState r, Identifier identifier) {
        return RenderLayers.entityCutoutNoCull((Identifier)identifier);
    }

    public float getMotionAnimThreshold(CustomPetEntity customPetEntity) {
        return 5.0E-4f;
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<CustomPetRenderState> renderPassInfo, float f, float f2) {
        if (Boolean.TRUE.equals(renderPassInfo.getOrDefaultGeckolibData(CustomPetModel.OWL, false))) {
            f *= 0.55f;
            f2 *= 0.55f;
        }
        CustomPetVariant variant = CustomPetVariant.fromSerializedName(renderPassInfo.getOrDefaultGeckolibData(CustomPetModel.VARIANT, "SPIRIT"));
        if (variant.isFantasy()) {
            f *= 0.8f;
            f2 *= 0.8f;
        }
        super.scaleModelForRender(renderPassInfo, f, f2);
    }

    private static void adjustOwlBones(BoneSnapshots boneSnapshots, boolean bl) {
        for (String string : OWL_EFFECT_BONES) {
            boneSnapshots.ifPresent(string, boneSnapshot -> boneSnapshot.skipRender(true).skipChildrenRender(true));
        }
        for (String string : OWL_GROUND_ROPE_BONES) {
            boneSnapshots.ifPresent(string, boneSnapshot -> boneSnapshot.skipRender(bl).skipChildrenRender(bl));
        }
        for (String string : OWL_FLIGHT_ROPE_BONES) {
            boneSnapshots.ifPresent(string, boneSnapshot -> boneSnapshot.skipRender(!bl).skipChildrenRender(!bl));
        }
    }

    private static boolean isBoneVisible(String string, CustomPetVariant customPetVariant, boolean bl, boolean bl2) {
        return switch (string) {
            case "body_merchant" -> customPetVariant.usesMerchantBody();
            case "body_default" -> {
                if (!customPetVariant.usesMerchantBody()) {
                    yield true;
                }
                yield false;
            }
            case "leaf" -> {
                if (customPetVariant.usesMerchantLeaf() && !bl) {
                    yield true;
                }
                yield false;
            }
            case "gardener_hat", "watering_can" -> customPetVariant.usesGardenerGear();
            case "sourcerer_hat" -> customPetVariant.usesSorcererHat();
            case "accessories", "fishing_rod", "fishing_rod_2", "fishing_rod_3" -> customPetVariant.usesFishermanGear();
            case "umbrella2" -> {
                if (bl2 && !customPetVariant.usesFishermanGear()) {
                    yield true;
                }
                yield false;
            }
            case "fisherman_umbrella2" -> {
                if (bl2 && customPetVariant.usesFishermanGear()) {
                    yield true;
                }
                yield false;
            }
            case "umbrella3" -> {
                if (!bl2 && bl && !customPetVariant.usesFishermanGear()) {
                    yield true;
                }
                yield false;
            }
            case "fisherman_umbrella3" -> {
                if (!bl2 && bl && customPetVariant.usesFishermanGear()) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<CustomPetRenderState> renderPassInfo, BoneSnapshots boneSnapshots) {
        boolean bl = Boolean.TRUE.equals(renderPassInfo.getOrDefaultGeckolibData(CustomPetModel.AIRBORNE, false));
        if (Boolean.TRUE.equals(renderPassInfo.getOrDefaultGeckolibData(CustomPetModel.OWL, false))) {
            CustomPetRenderer.adjustOwlBones(boneSnapshots, bl);
            return;
        }
        String string = (String)renderPassInfo.getOrDefaultGeckolibData(CustomPetModel.VARIANT, CustomPetVariant.NITWIT.name());
        CustomPetVariant customPetVariant = CustomPetVariant.fromSerializedName(string);
        if (customPetVariant.isFantasy()) return;
        boolean bl2 = Boolean.TRUE.equals(renderPassInfo.getOrDefaultGeckolibData(CustomPetModel.UMBRELLA, false));
        for (String string2 : CONDITIONAL_BONES) {
            boolean bl3 = !CustomPetRenderer.isBoneVisible(string2, customPetVariant, bl2, bl);
            boneSnapshots.ifPresent(string2, boneSnapshot -> boneSnapshot.skipRender(bl3).skipChildrenRender(bl3));
        }
    }

    @Override
    public CustomPetRenderState createRenderState() {
        return new CustomPetRenderState();
    }

    public CustomPetRenderState createRenderState(CustomPetEntity customPetEntity, Void void_) {
        return new CustomPetRenderState();
    }
}
