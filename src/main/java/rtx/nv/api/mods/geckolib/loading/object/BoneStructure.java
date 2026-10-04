package rtx.nv.api.mods.geckolib.loading.object;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import rtx.nv.api.mods.geckolib.loading.json.raw.Bone;

public record BoneStructure(Bone self, Map<String, BoneStructure> children) {
    public BoneStructure(Bone bone) {
        this(bone, (Map<String, BoneStructure>)new Object2ObjectOpenHashMap());
    }
}

