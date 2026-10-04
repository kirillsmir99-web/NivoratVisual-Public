package rtx.nv.api.mods.geckolib.renderer;
import java.util.List;
import java.util.function.Function;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionfc;
import rtx.nv.api.mods.geckolib.GeckoLibClientServices;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.constant.DataTickets;
import rtx.nv.api.mods.geckolib.model.DefaultedEntityGeoModel;
import rtx.nv.api.mods.geckolib.model.GeoModel;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderer;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo;
import rtx.nv.api.mods.geckolib.renderer.layer.GeoRenderLayer;
import rtx.nv.api.mods.geckolib.renderer.layer.GeoRenderLayersContainer;
import rtx.nv.api.mods.geckolib.util.ClientUtil;
import rtx.nv.api.mods.geckolib.util.MiscUtil;

public class GeoEntityRenderer<T extends Entity & GeoAnimatable, R extends EntityRenderState & GeoRenderState>
extends EntityRenderer<T, R>
implements GeoRenderer<T, Void, R> {
    protected final GeoRenderLayersContainer<T, Void, R> renderLayers = new GeoRenderLayersContainer(this);
    protected final GeoModel<T> model;
    protected final ItemModelManager itemModelResolver;
    protected float scaleWidth = 1.0f;
    protected float scaleHeight = 1.0f;

    public GeoEntityRenderer(EntityRendererFactory.Context context, EntityType<? extends T> entityType) {
        this(context, new DefaultedEntityGeoModel(Registries.ENTITY_TYPE.getId(entityType)));
    }

    public GeoEntityRenderer(EntityRendererFactory.Context context, GeoModel<T> geoModel) {
        super(context);
        this.model = geoModel;
        this.itemModelResolver = context.getItemModelManager();
    }

    @Override
    public RenderLayer getRenderType(R r, Identifier identifier) {
        if (((EntityRenderState)r).invisible && !((GeoRenderState)r).getOrDefaultGeckolibData(DataTickets.INVISIBLE_TO_PLAYER, false).booleanValue()) {
            return RenderLayers.itemEntityTranslucentCull((Identifier)identifier);
        }
        if (!((EntityRenderState)r).invisible) {
            return GeoRenderer.super.getRenderType(r, identifier);
        }
        return r.hasOutline() ? RenderLayers.outlineNoCull((Identifier)identifier) : null;
    }

    public final R getAndUpdateRenderState(T entity, float tickProgress) {
        R r = this.createRenderState(entity, null);
        this.updateRenderState(entity, r, tickProgress);
        this.updateShadow(entity, r);
        return r;
    }

    public double getNameRenderCutoffDistance(T t) {
        return 32.0;
    }

    protected void extractLivingEntityRenderState(LivingEntity livingEntity, LivingEntityRenderState livingEntityRenderState, float f, ItemModelManager itemModelManager) {
        LivingEntity livingEntity2;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        float f2 = MathHelper.lerpAngleDegrees((float)f, (float)livingEntity.lastHeadYaw, (float)livingEntity.headYaw);
        Text text = livingEntity.getCustomName();
        ItemStack itemStack = livingEntity.getEquippedStack(EquipmentSlot.HEAD);
        livingEntityRenderState.bodyYaw = LivingEntityRenderer.clampBodyYaw((LivingEntity)livingEntity, (float)f2, (float)f);
        livingEntityRenderState.relativeHeadYaw = MathHelper.wrapDegrees((float)(f2 - livingEntityRenderState.bodyYaw));
        livingEntityRenderState.pitch = livingEntity.getLerpedPitch(f);
        boolean bl = livingEntityRenderState.flipUpsideDown = text != null && LivingEntityRenderer.shouldFlipUpsideDown((String)text.getString());
        if (livingEntityRenderState.flipUpsideDown) {
            livingEntityRenderState.pitch *= -1.0f;
            livingEntityRenderState.relativeHeadYaw *= -1.0f;
        }
        if (!livingEntity.hasVehicle() && livingEntity.isAlive()) {
            livingEntityRenderState.limbSwingAnimationProgress = livingEntity.limbAnimator.getAnimationProgress(f);
            livingEntityRenderState.limbSwingAmplitude = livingEntity.limbAnimator.getAmplitude(f);
        } else {
            livingEntityRenderState.limbSwingAnimationProgress = 0.0f;
            livingEntityRenderState.limbSwingAmplitude = 0.0f;
        }
        Entity entity = livingEntity.getVehicle();
        if (entity instanceof LivingEntity) {
            livingEntity2 = (LivingEntity)entity;
            livingEntityRenderState.headItemAnimationProgress = livingEntity2.limbAnimator.getAnimationProgress(f);
        } else {
            livingEntityRenderState.headItemAnimationProgress = livingEntityRenderState.limbSwingAnimationProgress;
        }
        livingEntityRenderState.baseScale = livingEntity.getScale();
        livingEntityRenderState.ageScale = livingEntity.getScaleFactor();
        livingEntityRenderState.pose = livingEntity.getPose();
        livingEntityRenderState.sleepingDirection = livingEntity.getSleepingDirection();
        if (livingEntityRenderState.sleepingDirection != null) {
            livingEntityRenderState.standingEyeHeight = livingEntity.getEyeHeight(EntityPose.STANDING);
        }
        livingEntityRenderState.shaking = livingEntity.isFrozen();
        livingEntityRenderState.baby = livingEntity.isBaby();
        livingEntityRenderState.touchingWater = livingEntity.isTouchingWater();
        livingEntityRenderState.usingRiptide = livingEntity.isUsingRiptide();
        livingEntityRenderState.hurt = livingEntity.hurtTime > 0 || livingEntity.deathTime > 0;
        Item item = itemStack.getItem();
        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock skullBlock) {
            livingEntityRenderState.wearingSkullType = skullBlock.getSkullType();
            livingEntityRenderState.wearingSkullProfile = (ProfileComponent)itemStack.get(DataComponentTypes.PROFILE);
            livingEntityRenderState.headItemRenderState.clear();
        } else {
            livingEntityRenderState.wearingSkullType = null;
            livingEntityRenderState.wearingSkullProfile = null;
            if (!ArmorFeatureRenderer.hasModel((ItemStack)itemStack, (EquipmentSlot)EquipmentSlot.HEAD)) {
                this.itemModelResolver.updateForLivingEntity(livingEntityRenderState.headItemRenderState, itemStack, ItemDisplayContext.HEAD, livingEntity);
            } else {
                livingEntityRenderState.headItemRenderState.clear();
            }
        }
        livingEntityRenderState.deathTime = livingEntity.deathTime > 0 ? (float)livingEntity.deathTime + f : 0.0f;
        livingEntityRenderState.invisibleToPlayer = livingEntityRenderState.invisible && minecraftClient.player != null && livingEntity.isInvisibleTo((PlayerEntity)minecraftClient.player);
    }

    @Override
    public void fireCompileRenderLayersEvent() {
        GeckoLibClientServices.EVENTS.fireCompileEntityRenderLayers(this);
    }

    public void fireCompileRenderStateEvent(T t, Void void_, R r, float f) {
        GeckoLibClientServices.EVENTS.fireCompileEntityRenderState(this, r, t);
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<R> renderPassInfo, float f, float f2) {
        float f3;
        Object r = renderPassInfo.renderState();
        if (r instanceof LivingEntityRenderState) {
            LivingEntityRenderState livingEntityRenderState = (LivingEntityRenderState)r;
            f3 = livingEntityRenderState.baseScale;
        } else {
            f3 = 1.0f;
        }
        float f4 = f3;
        GeoRenderer.super.scaleModelForRender(renderPassInfo, f * this.scaleWidth * f4, f2 * this.scaleHeight * f4);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<R> renderPassInfo) {
        Direction direction;
        MatrixStack poseStack = renderPassInfo.poseStack();
        R entityRenderState = renderPassInfo.renderState();
        LivingEntityRenderState livingEntityRenderState = entityRenderState instanceof LivingEntityRenderState ? (LivingEntityRenderState) entityRenderState : null;
        if (livingEntityRenderState != null && ((GeoRenderState)entityRenderState).getGeckolibData(DataTickets.ENTITY_POSE) == EntityPose.SLEEPING && (direction = livingEntityRenderState.sleepingDirection) != null) {
            float f = livingEntityRenderState.standingEyeHeight - 0.1f;
            poseStack.translate((float)(-direction.getOffsetX()) * f, 0.0f, (float)(-direction.getOffsetZ()) * f);
        }
        this.applyRotations(renderPassInfo, poseStack, livingEntityRenderState != null ? livingEntityRenderState.baseScale : 1.0f);
        poseStack.translate(0.0f, 0.01f, 0.0f);
    }

    protected void applyRotations(RenderPassInfo<R> renderPassInfo, MatrixStack matrixStack, float f) {
        boolean bl;
        EntityRenderState entityRenderState = (EntityRenderState)renderPassInfo.renderState();
        float f2 = ((GeoRenderState)entityRenderState).getOrDefaultGeckolibData(DataTickets.ENTITY_BODY_YAW, Float.valueOf(0.0f)).floatValue();
        if (((GeoRenderState)entityRenderState).getOrDefaultGeckolibData(DataTickets.IS_SHAKING, false).booleanValue()) {
            f2 += (float)(Math.cos((double)entityRenderState.age * 3.25) * Math.PI * 0.4);
        }
        boolean bl2 = bl = ((GeoRenderState)entityRenderState).getGeckolibData(DataTickets.ENTITY_POSE) == EntityPose.SLEEPING;
        if (!bl) {
            matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - f2));
        }
        if (entityRenderState instanceof LivingEntityRenderState) {
            LivingEntityRenderState livingEntityRenderState = (LivingEntityRenderState)entityRenderState;
            if (livingEntityRenderState.deathTime > 0.0f) {
                matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Z.rotationDegrees(Math.min(MathHelper.sqrt((float)((livingEntityRenderState.deathTime - 1.0f) / 20.0f * 1.6f)), 1.0f) * this.getDeathMaxRotation((GeoRenderState)entityRenderState)));
            } else if (livingEntityRenderState.usingRiptide) {
                matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_X.rotationDegrees(-90.0f - livingEntityRenderState.pitch));
                matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(entityRenderState.age * -75.0f));
            } else if (bl) {
                Direction direction = livingEntityRenderState.sleepingDirection;
                matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(direction != null ? MiscUtil.getDirectionAngle(direction) : f2));
                matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Z.rotationDegrees(this.getDeathMaxRotation((GeoRenderState)entityRenderState)));
                matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(270.0f));
            } else if (livingEntityRenderState.flipUpsideDown) {
                matrixStack.translate(0.0f, (livingEntityRenderState.height + 0.1f) / f, 0.0f);
                matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
            }
        }
    }

    protected float getDeathMaxRotation(GeoRenderState geoRenderState) {
        return 90.0f;
    }

    protected float calculateYRot(T t, float f, float f2) {
        Entity entity = t.getVehicle();
        if (!(entity instanceof LivingEntity)) {
            float f3;
            if (t instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)t;
                f3 = MathHelper.lerpAngleDegrees((float)f2, (float)livingEntity.lastBodyYaw, (float)livingEntity.bodyYaw);
            } else {
                f3 = t.getBodyYaw();
            }
            return f3;
        }
        LivingEntity livingEntity = (LivingEntity)entity;
        float f4 = MathHelper.lerpAngleDegrees((float)f2, (float)livingEntity.lastBodyYaw, (float)livingEntity.bodyYaw);
        float f5 = MathHelper.clamp((float)MathHelper.wrapDegrees((float)(-f4)), (float)-85.0f, (float)85.0f);
        f4 = f - f4;
        if (Math.abs(f5) > 50.0f) {
            f4 += f5 * 0.2f;
        }
        return f4;
    }

    protected <S extends LivingEntityRenderState> S convertRenderStateToLiving(R r) {
        return (S)((LivingEntityRenderState)r);
    }

    public GeoEntityRenderer<T, R> withRenderLayer(Function<? super GeoEntityRenderer<T, R>, GeoRenderLayer<T, Void, R>> function) {
        return this.withRenderLayer(function.apply(this));
    }

    public GeoEntityRenderer<T, R> withRenderLayer(GeoRenderLayer<T, Void, R> geoRenderLayer) {
        this.renderLayers.addLayer(geoRenderLayer);
        return this;
    }

    @Override
    public GeoModel<T> getGeoModel() {
        return this.model;
    }

    public List<GeoRenderLayer<T, Void, R>> getRenderLayers() {
        return this.renderLayers.getRenderLayers();
    }

    public int getPackedOverlay(T t, Void void_, float f, float f2) {
        if (!(t instanceof LivingEntity)) {
            return OverlayTexture.DEFAULT_UV;
        }
        LivingEntity livingEntity = (LivingEntity)t;
        return OverlayTexture.packUv((int)OverlayTexture.getU((float)f), (int)OverlayTexture.getV((livingEntity.hurtTime > 0 || livingEntity.deathTime > 0 ? 1 : 0) != 0));
    }

    public void captureDefaultRenderState(T t, Void void_, R r, float f) {
        double d;
        LivingEntity livingEntity;
        GeoRenderer.super.captureDefaultRenderState(t, void_, r, f);
        ((GeoRenderState)r).addGeckolibData(DataTickets.VELOCITY, t.getVelocity());
        ((GeoRenderState)r).addGeckolibData(DataTickets.BLOCKPOS, t.getBlockPos());
        ((GeoRenderState)r).addGeckolibData(DataTickets.SPRINTING, t.isSprinting());
        ((GeoRenderState)r).addGeckolibData(DataTickets.IS_CROUCHING, t.isInSneakingPose());
        ((GeoRenderState)r).addGeckolibData(DataTickets.POSITION, t.getEntityPos());
        GeoRenderState geoRenderState = (GeoRenderState)r;
        if (t instanceof LivingEntity) {
            livingEntity = (LivingEntity)t;
            d = livingEntity.limbAnimator.getSpeed();
        } else {
            d = t.getVelocity().lengthSquared();
        }
        geoRenderState.addGeckolibData(DataTickets.IS_MOVING, d >= (double)this.getMotionAnimThreshold(t));
        if (t instanceof LivingEntity) {
            livingEntity = (LivingEntity)t;
            ((GeoRenderState)r).addGeckolibData(DataTickets.SWINGING_ARM, livingEntity.handSwinging);
            ((GeoRenderState)r).addGeckolibData(DataTickets.IS_DEAD_OR_DYING, livingEntity.isDead());
        }
        if (!(r instanceof LivingEntityRenderState)) {
            ((GeoRenderState)r).addGeckolibData(DataTickets.INVISIBLE_TO_PLAYER, t.isInvisible() && (ClientUtil.getClientPlayer() == null || t.isInvisibleTo(ClientUtil.getClientPlayer())));
            ((GeoRenderState)r).addGeckolibData(DataTickets.IS_SHAKING, t.isFrozen());
            ((GeoRenderState)r).addGeckolibData(DataTickets.ENTITY_POSE, t.getPose());
            ((GeoRenderState)r).addGeckolibData(DataTickets.ENTITY_PITCH, Float.valueOf(t.getLerpedPitch(f)));
            ((GeoRenderState)r).addGeckolibData(DataTickets.ENTITY_YAW, Float.valueOf(this.calculateYRot(t, 0.0f, f)));
            ((GeoRenderState)r).addGeckolibData(DataTickets.ENTITY_BODY_YAW, ((GeoRenderState)r).getOrDefaultGeckolibData(DataTickets.ENTITY_YAW, Float.valueOf(0.0f)));
        }
    }

    public int getRenderColor(T t, Void void_, float f) {
        int n = GeoRenderer.super.getRenderColor(t, void_, f);
        PlayerEntity playerEntity = ClientUtil.getClientPlayer();
        if (t.isInvisible() && playerEntity != null && !t.isInvisibleTo(playerEntity)) {
            n = ColorHelper.withAlpha((int)MathHelper.ceil((float)((float)(ColorHelper.getAlpha((int)n) * 38) / 255.0f)), (int)n);
        }
        return n;
    }

    public long getInstanceId(T t, Void void_) {
        return t.getId();
    }

    @Override
    public boolean firePreRenderEvent(RenderPassInfo<R> renderPassInfo, OrderedRenderCommandQueue orderedRenderCommandQueue) {
        return GeckoLibClientServices.EVENTS.fireEntityPreRender(this, renderPassInfo, orderedRenderCommandQueue);
    }

    @Override
    public void postRenderPass(RenderPassInfo<R> renderPassInfo, OrderedRenderCommandQueue orderedRenderCommandQueue) {
        super.render(renderPassInfo.renderState(), renderPassInfo.poseStack(), orderedRenderCommandQueue, renderPassInfo.cameraState());
    }

    @SuppressWarnings("unchecked")
    public R createRenderState(T t, Void void_) {
        return (R) (t instanceof LivingEntity ? new LivingEntityRenderState() : new EntityRenderState());
    }

    public boolean hasLabel(T entity, double squaredDistanceToCamera) {
        double d;
        if (!(entity instanceof LivingEntity)) {
            return super.hasLabel(entity, squaredDistanceToCamera);
        }
        if (entity.isSneaky() && squaredDistanceToCamera >= (d = this.getNameRenderCutoffDistance(entity)) * d) {
            return false;
        }
        if (!(!(entity instanceof MobEntity) || entity.shouldRenderName() || entity.hasCustomName() && entity == this.dispatcher.targetedEntity)) {
            return false;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerEntity playerEntity = ClientUtil.getClientPlayer();
        boolean bl = playerEntity != null && !entity.isInvisibleTo(playerEntity);
        Team team = entity.getScoreboardTeam();
        if (playerEntity == null || team == null) {
            return MinecraftClient.isHudEnabled() && entity != minecraftClient.getCameraEntity() && bl && !entity.hasPassengers();
        }
        Team team2 = ClientUtil.getClientPlayer().getScoreboardTeam();
        return switch (team.getNameTagVisibilityRule()) {
            case ALWAYS -> bl;
            case NEVER -> false;
            case HIDE_FOR_OTHER_TEAMS -> {
                if (team2 == null) {
                    yield bl;
                }
                if (team.isEqual(team2) && (team.shouldShowFriendlyInvisibles() || bl)) {
                    yield true;
                }
                yield false;
            }
            case HIDE_FOR_OWN_TEAM -> team2 == null ? bl : !team.isEqual(team2) && bl;
        };
    }

    public void render(R renderState, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        GeoRenderer.super.performRenderPass(renderState, matrices, queue, cameraState);
    }

    public R createRenderState() {
        return this.createRenderState(null, null);
    }

    public void updateRenderState(T entity, R state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        if (state instanceof LivingEntityRenderState) {
            LivingEntityRenderState livingEntityRenderState = (LivingEntityRenderState)state;
            this.extractLivingEntityRenderState((LivingEntity)entity, livingEntityRenderState, tickProgress, this.itemModelResolver);
        }
        this.fillRenderState(entity, null, state, tickProgress);
    }

    public GeoEntityRenderer<T, R> withScale(float f, float f2) {
        this.scaleWidth = f;
        this.scaleHeight = f2;
        return this;
    }

    public GeoEntityRenderer<T, R> withScale(float f) {
        return this.withScale(f, f);
    }
}

