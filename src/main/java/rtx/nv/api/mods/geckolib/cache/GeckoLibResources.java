package rtx.nv.api.mods.geckolib.cache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.cache.BakedAnimationCache;
import rtx.nv.api.mods.geckolib.cache.BakedModelCache;
import rtx.nv.api.mods.geckolib.cache.animation.Animation;
import rtx.nv.api.mods.geckolib.cache.animation.Animation.KeyframeMarkers;
import rtx.nv.api.mods.geckolib.cache.model.BakedGeoModel;
import rtx.nv.api.mods.geckolib.loading.definition.geometry.GeometryDescription;
import rtx.nv.api.mods.geckolib.loading.json.ModelFormatVersion;
import rtx.nv.api.mods.geckolib.loading.json.raw.Bone;
import rtx.nv.api.mods.geckolib.loading.json.raw.Cube;
import rtx.nv.api.mods.geckolib.loading.json.raw.FaceUV;
import rtx.nv.api.mods.geckolib.loading.json.raw.LocatorClass;
import rtx.nv.api.mods.geckolib.loading.json.raw.LocatorValue;
import rtx.nv.api.mods.geckolib.loading.json.raw.MinecraftGeometry;
import rtx.nv.api.mods.geckolib.loading.json.raw.Model;
import rtx.nv.api.mods.geckolib.loading.json.raw.PolyMesh;
import rtx.nv.api.mods.geckolib.loading.json.raw.PolysUnion;
import rtx.nv.api.mods.geckolib.loading.json.raw.TextureMesh;
import rtx.nv.api.mods.geckolib.loading.json.raw.UVFaces;
import rtx.nv.api.mods.geckolib.loading.json.raw.UVUnion;
import rtx.nv.api.mods.geckolib.loading.json.typeadapter.BakedAnimationsAdapter;
import rtx.nv.api.mods.geckolib.loading.json.typeadapter.KeyFrameMarkersAdapter;
import rtx.nv.api.mods.geckolib.loading.math.value.Constant;
import rtx.nv.api.mods.geckolib.loading.object.BakedAnimations;
import rtx.nv.api.mods.geckolib.loading.object.BakedModelFactory;
import rtx.nv.api.mods.geckolib.loading.object.GeometryTree;
import rtx.nv.api.mods.geckolib.object.CompoundException;

public final class GeckoLibResources {
    public static final Identifier RELOAD_LISTENER_ID = GeckoLibConstants.id("geckolib_resources");
    public static final Identifier ANIMATIONS_PATH = GeckoLibConstants.id("geckolib/animations");
    public static final Identifier MODELS_PATH = GeckoLibConstants.id("geckolib/models");
    public static final Pattern SUFFIX_STRIPPER = Pattern.compile("((\\.geo)|((\\.animation)s?))?(\\.json)$");
    public static final Pattern PREFIX_STRIPPER = Pattern.compile("^(geckolib/)((animations/)|(models/))?");
    public static final Gson GSON = new GsonBuilder().setStrictness(Strictness.LENIENT).registerTypeAdapter(Bone.class, Bone.deserializer()).registerTypeAdapter(Cube.class, Cube.deserializer()).registerTypeAdapter(FaceUV.class, FaceUV.deserializer()).registerTypeAdapter(LocatorClass.class, LocatorClass.deserializer()).registerTypeAdapter(LocatorValue.class, LocatorValue.deserializer()).registerTypeAdapter(MinecraftGeometry.class, MinecraftGeometry.deserializer()).registerTypeAdapter(Model.class, Model.deserializer()).registerTypeAdapter(GeometryDescription.class, GeometryDescription.gsonDeserializer()).registerTypeAdapter(PolyMesh.class, PolyMesh.deserializer()).registerTypeAdapter(PolysUnion.class, PolysUnion.deserializer()).registerTypeAdapter(TextureMesh.class, TextureMesh.deserializer()).registerTypeAdapter(UVFaces.class, UVFaces.deserializer()).registerTypeAdapter(UVUnion.class, UVUnion.deserializer()).registerTypeAdapter(Animation.KeyframeMarkers.class, KeyFrameMarkersAdapter.deserializer()).registerTypeAdapter(BakedAnimations.class, BakedAnimationsAdapter.deserializer()).create();
    private static BakedAnimationCache ANIMATIONS = new BakedAnimationCache(Collections.emptyMap());
    private static BakedModelCache MODELS = new BakedModelCache(Collections.emptyMap());

    public static CompletableFuture<Void> reload(ResourceReloader.Store store, Executor executor, ResourceReloader.Synchronizer synchronizer, Executor executor2) {
        CompletableFuture<Map<Identifier, BakedAnimations>> completableFuture = GeckoLibResources.loadAnimations(executor, store.getResourceManager());
        CompletableFuture<Map<Identifier, BakedGeoModel>> completableFuture2 = GeckoLibResources.loadModels(executor, store.getResourceManager());
        return CompletableFuture.runAsync(() -> {
            BakedAnimationsAdapter.COMPRESSION_CACHE = new ConcurrentHashMap<Double, Constant>();
        }, executor).thenCompose(void_ -> ((CompletableFuture)CompletableFuture.allOf(completableFuture, completableFuture2).thenCompose(arg_0 -> ((ResourceReloader.Synchronizer)synchronizer).whenPrepared(arg_0))).thenRunAsync(() -> {
            ANIMATIONS = new BakedAnimationCache((Map)completableFuture.join());
            MODELS = new BakedModelCache((Map)completableFuture2.join());
            BakedAnimationsAdapter.COMPRESSION_CACHE = null;
        }, executor2));
    }

    public static Identifier stripPrefixAndSuffix(Identifier identifier) {
        String string = identifier.getPath();
        Matcher matcher = PREFIX_STRIPPER.matcher(string);
        string = matcher.find() ? string.substring(matcher.end()) : string;
        Matcher matcher2 = SUFFIX_STRIPPER.matcher(string);
        string = matcher2.find() ? string.substring(0, matcher2.start()) : string;
        return string.length() == identifier.getPath().length() ? identifier : identifier.withPath(string);
    }

    private static BakedAnimations bakeAnimations(Identifier identifier, JsonObject jsonObject) {
        if (identifier.getPath().endsWith(".geo.json")) {
            throw new RuntimeException("Found model file in animations folder! '" + String.valueOf(identifier) + "'");
        }
        try {
            return (BakedAnimations)GSON.fromJson((JsonElement)JsonHelper.getObject((JsonObject)jsonObject, (String)"animations"), BakedAnimations.class);
        }
        catch (CompoundException compoundException) {
            throw compoundException.withMessage(String.valueOf(identifier) + ": Error building animations from JSON");
        }
        catch (Exception exception) {
            throw GeckoLibConstants.exception(identifier, "Error building animations from JSON", exception);
        }
    }

    private static <BAKED> CompletableFuture<Map<Identifier, BAKED>> bakeJsonResources(Executor executor, ResourceManager resourceManager, String string, BiFunction<Identifier, JsonObject, BAKED> biFunction, Function<Throwable, BAKED> function) {
        return GeckoLibResources.loadResources(executor, resourceManager, string, "json", GeckoLibResources::readJsonFile).thenCompose(list -> {
            List<CompletableFuture<Pair<Identifier, BAKED>>> futures = new ObjectArrayList<>(list.size());
            for (Pair<Identifier, JsonObject> pair : list) {
                futures.add(CompletableFuture.supplyAsync(() -> Pair.of(GeckoLibResources.stripPrefixAndSuffix(pair.left()), biFunction.apply(pair.left(), pair.right())), executor).exceptionally(throwable -> {
                    rtx.nv.NV.LOGGER.error("Error baking JSON resource", throwable);
                    return Pair.of(pair.left(), function.apply(throwable));
                }));
            }
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
                return futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).collect(Collectors.toMap(p -> (Identifier) p.left(), p -> (BAKED) p.right()));
            });
        });
    }

    public static BakedModelCache getBakedModels() {
        return MODELS;
    }

    private static CompletableFuture<Map<Identifier, BakedAnimations>> loadAnimations(Executor executor, ResourceManager resourceManager) {
        return GeckoLibResources.bakeJsonResources(executor, resourceManager, ANIMATIONS_PATH.getPath(), GeckoLibResources::bakeAnimations, throwable -> new BakedAnimations(new Object2ObjectOpenHashMap<>()));
    }

    private static <UNBAKED> CompletableFuture<List<Pair<Identifier, UNBAKED>>> loadResources(Executor executor, ResourceManager resourceManager, String string, String string2, BiFunction<Identifier, Resource, UNBAKED> biFunction) {
        String string3 = "." + string2;
        return CompletableFuture.supplyAsync(() -> resourceManager.findResources(string, identifier -> identifier.getPath().endsWith(string3)), executor).thenCompose(map -> {
            List<CompletableFuture<Pair<Identifier, UNBAKED>>> futures = new ObjectArrayList<>(map.size());
            map.forEach((identifier, resource) -> {
                futures.add(CompletableFuture.supplyAsync(() -> Pair.of(identifier, biFunction.apply(identifier, resource)), executor));
            });
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
                return futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList();
            });
        });
    }

    private static JsonObject readJsonFile(Identifier identifier, Resource resource) {
        try (BufferedReader reader = resource.getReader()) {
            return JsonHelper.deserialize(reader);
        } catch (Exception e) {
            throw GeckoLibConstants.exception(identifier, "Error reading JSON file", e);
        }
    }

    public static BakedAnimationCache getBakedAnimations() {
        return ANIMATIONS;
    }

    private static CompletableFuture<Map<Identifier, BakedGeoModel>> loadModels(Executor executor, ResourceManager resourceManager) {
        return GeckoLibResources.bakeJsonResources(executor, resourceManager, MODELS_PATH.getPath(), GeckoLibResources::bakeModel, throwable -> null);
    }

    private static BakedGeoModel bakeModel(Identifier identifier, JsonObject jsonObject) {
        if (identifier.getPath().endsWith(".animation.json")) {
            throw new RuntimeException("Found animation file found in models folder! '" + String.valueOf(identifier) + "'");
        }
        Model model = (Model)GSON.fromJson((JsonElement)jsonObject, Model.class);
        ModelFormatVersion modelFormatVersion = ModelFormatVersion.match(model.formatVersion());
        if (modelFormatVersion == null) {
            GeckoLibConstants.LOGGER.warn("{}: Unknown geo model format version: '{}'. This may not work correctly", (Object)identifier, (Object)model.formatVersion());
        } else if (!modelFormatVersion.isSupported()) {
            GeckoLibConstants.LOGGER.error("{}: Unsupported geo model format version: '{}'. {}", (Object)identifier, (Object)model.formatVersion(), (Object)modelFormatVersion.getErrorMessage());
        }
        return BakedModelFactory.getForNamespace(identifier.getNamespace()).constructGeoModel(GeometryTree.fromModel(model));
    }
}

