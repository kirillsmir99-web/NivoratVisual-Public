package rtx.nv.api.mods.geckolib.loading.object;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import rtx.nv.api.mods.geckolib.loading.definition.geometry.GeometryDescription;
import rtx.nv.api.mods.geckolib.loading.json.raw.Bone;
import rtx.nv.api.mods.geckolib.loading.json.raw.MinecraftGeometry;
import rtx.nv.api.mods.geckolib.loading.json.raw.Model;
import rtx.nv.api.mods.geckolib.loading.object.BoneStructure;

public record GeometryTree(Map<String, BoneStructure> topLevelBones, GeometryDescription properties) {
    public static GeometryTree fromModel(Model model) {
        Object object;
        Object2ObjectOpenHashMap object2ObjectOpenHashMap = new Object2ObjectOpenHashMap();
        MinecraftGeometry minecraftGeometry = model.minecraftGeometry()[0];
        Bone[] boneArray = minecraftGeometry.bones();
        Object2ObjectOpenHashMap object2ObjectOpenHashMap2 = new Object2ObjectOpenHashMap(boneArray.length);
        for (Bone bone : boneArray) {
            object = new BoneStructure(bone);
            object2ObjectOpenHashMap2.put(bone.name(), object);
            if (bone.parent() != null) continue;
            object2ObjectOpenHashMap.put(bone.name(), object);
        }
        for (Bone bone : boneArray) {
            object = bone.parent();
            if (object == null) continue;
            String string = bone.name();
            if (((String)object).equals(string)) {
                throw new IllegalArgumentException("Invalid model definition. Bone has defined itself as its own parent: " + string);
            }
            BoneStructure boneStructure = (BoneStructure)object2ObjectOpenHashMap2.get(object);
            if (boneStructure == null) {
                throw new IllegalArgumentException("Invalid model definition. Found bone with undefined parent (child -> parent): " + string + " -> " + (String)object);
            }
            boneStructure.children().put(string, (BoneStructure)object2ObjectOpenHashMap2.get(string));
        }
        return new GeometryTree((Map<String, BoneStructure>)object2ObjectOpenHashMap, minecraftGeometry.geometryDescription());
    }
}

