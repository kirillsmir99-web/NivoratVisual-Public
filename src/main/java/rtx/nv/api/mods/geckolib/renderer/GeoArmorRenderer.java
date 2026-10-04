package rtx.nv.api.mods.geckolib.renderer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import rtx.nv.api.mods.geckolib.GeckoLibClientServices;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.GeoItem;
import rtx.nv.api.mods.geckolib.animatable.client.GeoRenderProvider;
import rtx.nv.api.mods.geckolib.constant.DataTickets;
import rtx.nv.api.mods.geckolib.model.GeoModel;
import rtx.nv.api.mods.geckolib.renderer.base.BoneSnapshots;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderer;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo;
import rtx.nv.api.mods.geckolib.renderer.layer.GeoRenderLayer;
import rtx.nv.api.mods.geckolib.renderer.layer.GeoRenderLayersContainer;

public class GeoArmorRenderer<T extends Item & GeoItem, R extends BipedEntityRenderState & GeoRenderState>
implements GeoRenderer<T, GeoArmorRenderer.RenderData, R> {
    protected static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    protected final GeoRenderLayersContainer<T, RenderData, R> renderLayers = new GeoRenderLayersContainer(this);
    protected final GeoModel<T> model;
    protected float scaleWidth = 1.0f;
    protected float scaleHeight = 1.0f;

    public GeoArmorRenderer(GeoModel<T> geoModel) {
        this.model = geoModel;
    }

    @Override
    public RenderLayer getRenderType(R r, Identifier identifier) {
        return RenderLayers.armorCutoutNoCull((Identifier)identifier);
    }

    public static <R extends BipedEntityRenderState, A extends BipedEntityModel<R>> boolean tryRenderGeoArmorPiece(BiFunction<R, EquipmentSlot, A> biFunction, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, ItemStack itemStack, EquipmentSlot equipmentSlot, int n, R r) {
        GeoArmorRenderer.StackForRender stackForRender = GeoArmorRenderer.StackForRender.find(itemStack, equipmentSlot, r, biFunction);
        if (stackForRender == null) {
            return false;
        }
        EnumMap enumMap = ((GeoRenderState)r).getGeckolibData(DataTickets.PER_SLOT_RENDER_DATA);
        if (enumMap == null || !enumMap.containsKey(equipmentSlot)) {
            return false;
        }
        BipedEntityRenderState bipedEntityRenderState = (BipedEntityRenderState)enumMap.get(equipmentSlot);
        stackForRender.renderer.performRenderPass((GeoRenderState)bipedEntityRenderState, matrixStack, orderedRenderCommandQueue, MinecraftClient.getInstance().worldRenderer.worldRenderState.cameraRenderState, null);
        return true;
    }

    @Override
    public void fireCompileRenderLayersEvent() {
        GeckoLibClientServices.EVENTS.fireCompileArmorRenderLayers(this);
    }

    public void fireCompileRenderStateEvent(T t, RenderData renderData, R r, float f) {
        GeckoLibClientServices.EVENTS.fireCompileArmorRenderState(this, r, t, renderData);
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<R> renderPassInfo, float f, float f2) {
        GeoRenderer.super.scaleModelForRender(renderPassInfo, this.scaleWidth * f, this.scaleHeight * f2);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<R> renderPassInfo) {
        renderPassInfo.poseStack().translate(0.0f, 1.5f, 0.0f);
        renderPassInfo.poseStack().scale(-1.0f, -1.0f, 1.0f);
    }

    public GeoArmorRenderer<T, R> withRenderLayer(Function<? super GeoArmorRenderer<T, R>, GeoRenderLayer<T, RenderData, R>> function) {
        return this.withRenderLayer(function.apply(this));
    }

    public GeoArmorRenderer<T, R> withRenderLayer(GeoRenderLayer<T, RenderData, R> geoRenderLayer) {
        this.renderLayers.addLayer(geoRenderLayer);
        return this;
    }

    @Override
    public GeoModel<T> getGeoModel() {
        return this.model;
    }

    public List<GeoRenderLayer<T, RenderData, R>> getRenderLayers() {
        return this.renderLayers.getRenderLayers();
    }

    public void captureDefaultRenderState(T t, RenderData renderData, R r, float f) {
        GeoRenderer.super.captureDefaultRenderState(t, renderData, r, f);
        ((GeoRenderState)r).addGeckolibData(DataTickets.POSITION, renderData.entity().getEntityPos());
        ((GeoRenderState)r).addGeckolibData(DataTickets.IS_GECKOLIB_WEARER, renderData.entity() instanceof GeoAnimatable);
        ((GeoRenderState)r).addGeckolibData(DataTickets.EQUIPMENT_SLOT, renderData.slot());
        ((GeoRenderState)r).addGeckolibData(DataTickets.HAS_GLINT, renderData.itemStack().hasGlint());
        ((GeoRenderState)r).addGeckolibData(DataTickets.HUMANOID_MODEL, renderData.baseModel);
    }

    public int getRenderColor(T t, RenderData renderData, float f) {
        return GeckoLibClientServices.ITEM_RENDERING.getDyedItemColor(renderData.itemStack(), -1);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots boneSnapshots) {
        R bipedEntityRenderState = renderPassInfo.renderState();
        EquipmentSlot equipmentSlot = Objects.requireNonNull(((GeoRenderState)bipedEntityRenderState).getGeckolibData(DataTickets.EQUIPMENT_SLOT));
        BipedEntityModel bipedEntityModel = Objects.requireNonNull(((GeoRenderState)bipedEntityRenderState).getGeckolibData(DataTickets.HUMANOID_MODEL));
        List<ArmorSegment> list = this.getSegmentsForSlot(bipedEntityRenderState, equipmentSlot);
        if (!list.isEmpty()) {
            bipedEntityModel.setAngles(bipedEntityRenderState);
            for (ArmorSegment armorSegment : this.getSegmentsForSlot(bipedEntityRenderState, equipmentSlot)) {
                boneSnapshots.get(this.getBoneNameForSegment(bipedEntityRenderState, armorSegment)).ifPresent(boneSnapshot -> {
                    ModelPart modelPart = (ModelPart)armorSegment.modelPartGetter.apply(bipedEntityModel);
                    Vector3f vector3f = (Vector3f)armorSegment.modelPartMatcher.apply(new Vector3f(modelPart.originX, modelPart.originY, modelPart.originZ));
                    boneSnapshot.setRotX(-modelPart.pitch).setRotY(-modelPart.yaw).setRotZ(modelPart.roll).setTranslateX(vector3f.x).setTranslateY(vector3f.y).setTranslateZ(vector3f.z);
                });
            }
        }
    }

    public long getInstanceId(T t, RenderData renderData) {
        long l = GeoItem.getId((ItemStack)renderData.itemStack());
        if (l == Long.MAX_VALUE) {
            int n = renderData.entity().getId() * 13;
            return (long)n * (long)n * (long)n * (long)(-(renderData.slot().ordinal() + 1));
        }
        return -l;
    }

    @Override
    public boolean firePreRenderEvent(RenderPassInfo<R> renderPassInfo, OrderedRenderCommandQueue orderedRenderCommandQueue) {
        return GeckoLibClientServices.EVENTS.fireArmorPreRender(this, renderPassInfo, orderedRenderCommandQueue);
    }

    @Override
    public void submitRenderTasks(RenderPassInfo<R> renderPassInfo, RenderCommandQueue renderCommandQueue, RenderLayer renderLayer) {
        if (renderLayer == null) {
            return;
        }
        int n = renderPassInfo.packedLight();
        int n2 = renderPassInfo.packedOverlay();
        int n3 = renderPassInfo.renderColor();
        R bipedEntityRenderState = renderPassInfo.renderState();
        EquipmentSlot equipmentSlot = Objects.requireNonNull(((GeoRenderState)bipedEntityRenderState).getGeckolibData(DataTickets.EQUIPMENT_SLOT));
        renderCommandQueue.submitCustom(renderPassInfo.poseStack(), renderLayer, (entry, vertexConsumer) -> {
            MatrixStack matrixStack = renderPassInfo.poseStack();
            matrixStack.push();
            matrixStack.peek().copy(entry);
            renderPassInfo.renderPosed(() -> {
                for (ArmorSegment armorSegment : this.getSegmentsForSlot(bipedEntityRenderState, equipmentSlot)) {
                    renderPassInfo.model().getBone(this.getBoneNameForSegment(bipedEntityRenderState, armorSegment)).ifPresent(geoBone -> geoBone.positionAndRender(renderPassInfo, vertexConsumer, n, n2, n3));
                }
            });
            matrixStack.pop();
        });
    }

    @SuppressWarnings("unchecked")
    public R createRenderState(T t, RenderData renderData) {
        return (R) new BipedEntityRenderState();
    }

    public static <R extends BipedEntityRenderState, A extends BipedEntityModel<R>> void captureRenderStates(R r, LivingEntity livingEntity, float f, BiFunction<R, EquipmentSlot, A> biFunction, Function<EquipmentSlot, R> function) {
        List<GeoArmorRenderer.StackForRender> list = GeoArmorRenderer.getRelevantSlotsForRendering(livingEntity, r, biFunction);
        if (list == null) {
            return;
        }
        EnumMap<EquipmentSlot, BipedEntityRenderState> enumMap = new EnumMap<EquipmentSlot, BipedEntityRenderState>(EquipmentSlot.class);
        for (GeoArmorRenderer.StackForRender stackForRender : list) {
            RenderData renderData = new RenderData(livingEntity, stackForRender.stack, stackForRender.slot, stackForRender.baseModel);
            BipedEntityRenderState bipedEntityRenderState = (BipedEntityRenderState)function.apply(stackForRender.slot);
            stackForRender.renderer.fillRenderState((GeoAnimatable)stackForRender.stack.getItem(), renderData, (GeoRenderState)bipedEntityRenderState, f);
            enumMap.put(stackForRender.slot, bipedEntityRenderState);
        }
        ((GeoRenderState)r).addGeckolibData(DataTickets.PER_SLOT_RENDER_DATA, enumMap);
    }

    public String getBoneNameForSegment(R r, ArmorSegment armorSegment) {
        return switch (armorSegment) {
            case HEAD -> "armorHead";
            case BODY, CHEST -> "armorBody";
            case RIGHT_ARM -> "armorRightArm";
            case LEFT_ARM -> "armorLeftArm";
            case RIGHT_LEG -> "armorRightLeg";
            case LEFT_LEG -> "armorLeftLeg";
            case RIGHT_FOOT -> "armorRightBoot";
            case LEFT_FOOT -> "armorLeftBoot";
        };
    }

    public List<ArmorSegment> getSegmentsForSlot(R r, EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case HEAD -> List.of(ArmorSegment.HEAD);
            case CHEST -> List.of(ArmorSegment.CHEST, ArmorSegment.LEFT_ARM, ArmorSegment.RIGHT_ARM);
            case LEGS -> List.of(ArmorSegment.LEFT_LEG, ArmorSegment.RIGHT_LEG);
            case FEET -> List.of(ArmorSegment.LEFT_FOOT, ArmorSegment.RIGHT_FOOT);
            default -> List.of();
        };
    }

    public GeoArmorRenderer<T, R> withScale(float f, float f2) {
        this.scaleWidth = f;
        this.scaleHeight = f2;
        return this;
    }

    public GeoArmorRenderer<T, R> withScale(float f) {
        return this.withScale(f, f);
    }

    private static <R extends BipedEntityRenderState, A extends BipedEntityModel<R>> List<GeoArmorRenderer.StackForRender> getRelevantSlotsForRendering(LivingEntity livingEntity, R r, BiFunction<R, EquipmentSlot, A> biFunction) {
        List list = null;
        for (int i = 0; i < ARMOR_SLOTS.length; ++i) {
            EquipmentSlot equipmentSlot = ARMOR_SLOTS[i];
            GeoArmorRenderer.StackForRender stackForRender = GeoArmorRenderer.StackForRender.find(livingEntity.getEquippedStack(equipmentSlot), equipmentSlot, r, biFunction);
            if (stackForRender == null) continue;
            if (list == null) {
                list = new ObjectArrayList(ARMOR_SLOTS.length - i);
            }
            list.add(stackForRender);
        }
        return list;
    }

    public static record RenderData(LivingEntity entity, ItemStack itemStack, EquipmentSlot slot, BipedEntityModel<?> baseModel) {}

    public enum ArmorSegment {
        HEAD(m -> m.head, v -> v),
        CHEST(m -> m.body, v -> v),
        BODY(m -> m.body, v -> v),
        RIGHT_ARM(m -> m.rightArm, v -> v),
        LEFT_ARM(m -> m.leftArm, v -> v),
        RIGHT_LEG(m -> m.rightLeg, v -> v),
        LEFT_LEG(m -> m.leftLeg, v -> v),
        RIGHT_FOOT(m -> m.rightLeg, v -> v),
        LEFT_FOOT(m -> m.leftLeg, v -> v);

        public final Function<BipedEntityModel<?>, ModelPart> modelPartGetter;
        public final Function<Vector3f, Vector3f> modelPartMatcher;

        ArmorSegment(Function<BipedEntityModel<?>, ModelPart> modelPartGetter, Function<Vector3f, Vector3f> modelPartMatcher) {
            this.modelPartGetter = modelPartGetter;
            this.modelPartMatcher = modelPartMatcher;
        }
    }

    public static record StackForRender(ItemStack stack, EquipmentSlot slot, GeoArmorRenderer renderer, BipedEntityModel<?> baseModel) {
        private static <S extends BipedEntityRenderState, A extends BipedEntityModel<S>> StackForRender find(ItemStack itemStack, EquipmentSlot equipmentSlot, S s, BiFunction<S, EquipmentSlot, A> biFunction) {
            GeoRenderProvider geoRenderProvider;
            EquippableComponent equippableComponent = (EquippableComponent)itemStack.get(DataComponentTypes.EQUIPPABLE);
            if (equippableComponent == null || !ArmorFeatureRenderer.hasModel((EquippableComponent)equippableComponent, (EquipmentSlot)equipmentSlot) || (geoRenderProvider = GeoRenderProvider.of((ItemStack)itemStack)) == GeoRenderProvider.DEFAULT) {
                return null;
            }
            BipedEntityModel bipedEntityModel = (BipedEntityModel)biFunction.apply(s, equipmentSlot);
            GeoArmorRenderer<?, ?> geoArmorRenderer = geoRenderProvider.getGeoArmorRenderer(itemStack, equipmentSlot);
            if (geoArmorRenderer == null) {
                return null;
            }
            return new StackForRender(itemStack, equipmentSlot, geoArmorRenderer, bipedEntityModel);
        }
    }
}

