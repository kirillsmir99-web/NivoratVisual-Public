package rtx.nv.api.mods.geckolib.util;
import com.mojang.blaze3d.textures.GpuTexture;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.client.GeoRenderProvider;
import rtx.nv.api.mods.geckolib.cache.model.GeoBone;
import rtx.nv.api.mods.geckolib.cache.model.cuboid.GeoCube;
import rtx.nv.api.mods.geckolib.renderer.GeoArmorRenderer;
import rtx.nv.api.mods.geckolib.renderer.GeoBlockRenderer;
import rtx.nv.api.mods.geckolib.renderer.GeoEntityRenderer;
import rtx.nv.api.mods.geckolib.renderer.GeoItemRenderer;
import rtx.nv.api.mods.geckolib.renderer.GeoReplacedEntityRenderer;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo;

public final class RenderUtil {
    private RenderUtil() {
    }

    public static void fixInvertedFlatCube(GeoCube geoCube, Vector3f vector3f) {
        if (vector3f.x() < 0.0f && (geoCube.size().getY() == 0.0 || geoCube.size().getZ() == 0.0)) {
            vector3f.mul(-1.0f, 1.0f, 1.0f);
        }
        if (vector3f.y() < 0.0f && (geoCube.size().getX() == 0.0 || geoCube.size().getZ() == 0.0)) {
            vector3f.mul(1.0f, -1.0f, 1.0f);
        }
        if (vector3f.z() < 0.0f && (geoCube.size().getX() == 0.0 || geoCube.size().getY() == 0.0)) {
            vector3f.mul(1.0f, 1.0f, -1.0f);
        }
    }

    public static void transformToBone(MatrixStack matrixStack, GeoBone geoBone) {
        java.util.List<GeoBone> objectArrayList = new ObjectArrayList<>();
        GeoBone geoBone2 = geoBone;
        objectArrayList.add(geoBone);
        while ((geoBone2 = geoBone2.parent()) != null) {
            objectArrayList.add(geoBone2);
        }
        for (GeoBone geoBone3 : objectArrayList.reversed()) {
            RenderUtil.prepMatrixForBone(matrixStack, geoBone3);
        }
        geoBone.translateToPivotPoint(matrixStack);
    }

    public static void prepMatrixForBoneAndUpdateListeners(MatrixStack matrixStack, GeoBone geoBone, RenderPassInfo<?> renderPassInfo) {
        if (geoBone.frameSnapshot != null) {
            geoBone.frameSnapshot.translate(matrixStack);
        }
        RenderUtil.translateAndRotateMatrixForBone(matrixStack, geoBone);
        if (geoBone.frameSnapshot != null) {
            geoBone.frameSnapshot.scale(matrixStack);
        }
        if (renderPassInfo != null) {
            geoBone.updateBonePositionListeners(matrixStack, renderPassInfo);
        }
        geoBone.translateAwayFromPivotPoint(matrixStack);
    }

    public static Vec3d renderPoseToPosition(Matrix4fc matrix4fc, float f, float f2, float f3) {
        Vector4f vector4f = matrix4fc.transform(new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
        return new Vec3d((double)(vector4f.x() * f), (double)(vector4f.y() * f2), (double)(vector4f.z() * f3));
    }

    public static void prepMatrixForBone(MatrixStack matrixStack, GeoBone geoBone) {
        RenderUtil.prepMatrixForBoneAndUpdateListeners(matrixStack, geoBone, null);
    }

    public static Matrix4f addPosToMatrix(Matrix4f matrix4f, Vec3d vec3d) {
        matrix4f.m30(matrix4f.m30() + (float)vec3d.x).m31(matrix4f.m31() + (float)vec3d.y).m32(matrix4f.m32() + (float)vec3d.z);
        return matrix4f;
    }

    public static Matrix4f extractPoseFromRoot(Matrix4fc matrix4fc, Matrix4f matrix4f) {
        matrix4f = new Matrix4f((Matrix4fc)matrix4f);
        matrix4f.invert();
        matrix4f.mul(matrix4fc);
        return matrix4f;
    }

    public static GeoAnimatable getReplacedAnimatable(EntityType<?> entityType) {
        GeoReplacedEntityRenderer<?, ?, ?> geoReplacedEntityRenderer = RenderUtil.getReplacedEntityRenderer(entityType);
        return geoReplacedEntityRenderer == null ? null : geoReplacedEntityRenderer.getAnimatable();
    }

    public static void translateAndRotateMatrixForBone(MatrixStack matrixStack, GeoBone geoBone) {
        geoBone.translateToPivotPoint(matrixStack);
        float f = geoBone.baseRotX();
        float f2 = geoBone.baseRotY();
        float f3 = geoBone.baseRotZ();
        if (geoBone.frameSnapshot != null) {
            f += geoBone.frameSnapshot.getRotX();
            f2 += geoBone.frameSnapshot.getRotY();
            f3 += geoBone.frameSnapshot.getRotZ();
        }
        if (f3 != 0.0f) {
            matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Z.rotation(f3));
        }
        if (f2 != 0.0f) {
            matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotation(f2));
        }
        if (f != 0.0f) {
            matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_X.rotation(f));
        }
    }

    public static Identifier getEmissiveResource(Identifier identifier) {
        return identifier.withPath(string -> string.replace(".png", "_glowmask.png"));
    }

    public static GeoEntityRenderer<?, ?> getGeckoLibEntityRenderer(EntityType<?> entityType) {
        GeoEntityRenderer geoEntityRenderer;
        Object v = MinecraftClient.getInstance().getEntityRenderDispatcher().renderers.get(entityType);
        return v instanceof GeoEntityRenderer ? (geoEntityRenderer = (GeoEntityRenderer)v) : null;
    }

    public static GeoBlockRenderer<?, ?> getGeckoLibBlockRenderer(BlockEntityType<?> blockEntityType) {
        GeoBlockRenderer geoBlockRenderer;
        Object v = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().renderers.get(blockEntityType);
        return v instanceof GeoBlockRenderer ? (geoBlockRenderer = (GeoBlockRenderer)v) : null;
    }

    public static GeoReplacedEntityRenderer<?, ?, ?> getReplacedEntityRenderer(EntityType<?> entityType) {
        GeoReplacedEntityRenderer geoReplacedEntityRenderer;
        Object v = MinecraftClient.getInstance().getEntityRenderDispatcher().renderers.get(entityType);
        return v instanceof GeoReplacedEntityRenderer ? (geoReplacedEntityRenderer = (GeoReplacedEntityRenderer)v) : null;
    }

    public static void faceRotation(MatrixStack matrixStack, Entity entity, float f) {
        matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp((float)f, (float)entity.lastYaw, (float)entity.getYaw()) - 90.0f));
        matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp((float)f, (float)entity.lastPitch, (float)entity.getPitch())));
    }

    public static GeoItemRenderer<?> getGeckoLibItemRenderer(Item item) {
        return GeoRenderProvider.of(item.getDefaultStack()).getGeoItemRenderer();
    }

    public static Matrix4f translateMatrix(Matrix4f matrix4f, Vector3f vector3f) {
        return matrix4f.add((Matrix4fc)new Matrix4f().m30(vector3f.x).m31(vector3f.y).m32(vector3f.z));
    }

    public static IntIntPair getTextureDimensions(Identifier identifier) {
        GpuTexture gpuTexture = MinecraftClient.getInstance().getTextureManager().getTexture(identifier).getGlTexture();
        return IntIntPair.of((int)gpuTexture.getWidth(0), (int)gpuTexture.getHeight(0));
    }

    public static GeoArmorRenderer<?, ?> getGeckoLibArmorRenderer(ItemStack itemStack, EquipmentSlot equipmentSlot) {
        return GeoRenderProvider.of((ItemStack)itemStack).getGeoArmorRenderer(itemStack, equipmentSlot);
    }

    public static GeoArmorRenderer<?, ?> getGeckoLibArmorRenderer(Item item) {
        ItemStack itemStack = item.getDefaultStack();
        EquippableComponent equippableComponent = (EquippableComponent)itemStack.getOrDefault(DataComponentTypes.EQUIPPABLE, null);
        if (equippableComponent == null) {
            return null;
        }
        return RenderUtil.getGeckoLibArmorRenderer(itemStack, equippableComponent.slot());
    }
}

