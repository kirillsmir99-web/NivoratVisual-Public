package rtx.nv.api.mods.geckolib.cache.model;
import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.render.VertexConsumer;
import rtx.nv.api.mods.geckolib.cache.model.GeoBone;
import rtx.nv.api.mods.geckolib.loading.json.raw.ModelProperties;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo;

public record BakedGeoModel(GeoBone[] topLevelBones, ModelProperties properties, Supplier<Map<String, GeoBone>> boneLookup) {
    public BakedGeoModel(GeoBone[] geoBoneArray, ModelProperties modelProperties) {
        this(geoBoneArray, modelProperties, BakedGeoModel.createBoneMap(geoBoneArray));
    }

    public <R extends GeoRenderState> void render(RenderPassInfo<R> renderPassInfo, VertexConsumer vertexConsumer, int n, int n2, int n3) {
        for (GeoBone geoBone : this.topLevelBones()) {
            geoBone.positionAndRender(renderPassInfo, vertexConsumer, n, n2, n3);
        }
    }

    public Optional<GeoBone> getBone(String string) {
        return Optional.ofNullable(this.boneLookup.get().get(string));
    }

    private static List<GeoBone> collectChildBones(GeoBone geoBone) {
        ObjectArrayList objectArrayList = new ObjectArrayList();
        for (GeoBone geoBone2 : geoBone.children()) {
            objectArrayList.add(geoBone2);
            objectArrayList.addAll(BakedGeoModel.collectChildBones(geoBone2));
        }
        return objectArrayList;
    }

    private static Supplier<Map<String, GeoBone>> createBoneMap(GeoBone[] geoBoneArray) {
        return Suppliers.memoize(() -> {
            Object2ReferenceOpenHashMap object2ReferenceOpenHashMap = new Object2ReferenceOpenHashMap();
            for (GeoBone geoBone : geoBoneArray) {
                object2ReferenceOpenHashMap.put((Object)geoBone.name(), (Object)geoBone);
                for (GeoBone geoBone2 : BakedGeoModel.collectChildBones(geoBone)) {
                    object2ReferenceOpenHashMap.put((Object)geoBone2.name(), (Object)geoBone2);
                }
            }
            return object2ReferenceOpenHashMap;
        });
    }
}

