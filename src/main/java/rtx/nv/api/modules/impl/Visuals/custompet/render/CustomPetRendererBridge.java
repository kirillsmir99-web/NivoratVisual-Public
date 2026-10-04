package rtx.nv.api.modules.impl.Visuals.custompet.render;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.entity.Entity;
import rtx.nv.api.modules.impl.Visuals.custompet.entity.CustomPetEntity;
import rtx.nv.api.modules.impl.Visuals.custompet.render.CustomPetRenderState;
import rtx.nv.api.modules.impl.Visuals.custompet.render.CustomPetRenderer;

public final class CustomPetRendererBridge {
    private static MinecraftClient minecraft;
    private static EntityRenderManager dispatcher;
    private static BlockRenderManager blockRenderDispatcher;
    private static ItemModelManager itemModelResolver;
    private static MapRenderer mapRenderer;
    private static TextRenderer font;
    private static Supplier<LoadedEntityModels> modelSetSupplier;
    private static EquipmentModelLoader equipmentAssetManager;
    private static AtlasManager atlasManager;
    private static PlayerSkinCache playerSkinRenderCache;
    private static EntityRenderer<? super CustomPetEntity, ?> renderer;

    private CustomPetRendererBridge() {
    }

    public static void bootstrap(EntityRenderManager entityRenderManager, MinecraftClient minecraftClient, BlockRenderManager blockRenderManager, ItemModelManager itemModelManager, MapRenderer mapRenderer, AtlasManager atlasManager, TextRenderer textRenderer, Supplier<LoadedEntityModels> supplier, EquipmentModelLoader equipmentModelLoader, PlayerSkinCache playerSkinCache) {
        dispatcher = entityRenderManager;
        minecraft = minecraftClient;
        blockRenderDispatcher = blockRenderManager;
        itemModelResolver = itemModelManager;
        CustomPetRendererBridge.mapRenderer = mapRenderer;
        CustomPetRendererBridge.atlasManager = atlasManager;
        font = textRenderer;
        modelSetSupplier = supplier;
        equipmentAssetManager = equipmentModelLoader;
        playerSkinRenderCache = playerSkinCache;
        renderer = null;
    }

    public static void reload() {
        renderer = null;
    }

    private static EntityRenderer<? super CustomPetEntity, ?> getOrCreateRenderer() {
        if (renderer == null) {
            EntityRendererFactory.Context context = new EntityRendererFactory.Context(dispatcher, itemModelResolver, mapRenderer, blockRenderDispatcher, minecraft.getResourceManager(), modelSetSupplier.get(), equipmentAssetManager, atlasManager, font, playerSkinRenderCache);
            renderer = new CustomPetRenderer(context);
        }
        return renderer;
    }

    @SuppressWarnings("unchecked")
    public static <S extends EntityRenderState> EntityRenderer<?, ? super S> getCustomRenderer(S s) {
        if (dispatcher == null || minecraft == null) {
            return null;
        }
        if (s instanceof CustomPetRenderState) {
            return (EntityRenderer<?, ? super S>)(Object)CustomPetRendererBridge.getOrCreateRenderer();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> EntityRenderer<? super T, ?> getCustomRenderer(T t) {
        if (dispatcher == null || minecraft == null) {
            return null;
        }
        if (t instanceof CustomPetEntity) {
            return (EntityRenderer<? super T, ?>)(Object)CustomPetRendererBridge.getOrCreateRenderer();
        }
        return null;
    }
}

