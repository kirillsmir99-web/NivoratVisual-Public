package rtx.nv.api.mods.chatheads;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import rtx.nv.api.mods.chatheads.Compat;

public class MixinPlugin
implements IMixinConfigPlugin {
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("ClothConfigScreenMixin") || mixinClassName.endsWith("AbstractConfigScreenQuitSaveConsumerMixin")) {
            return Compat.isModLoaded((String)"cloth-config");
        }
        if (mixinClassName.endsWith("EmojifulMixin")) {
            return Compat.isModLoaded((String)"emojiful");
        }
        return true;
    }

    public void onLoad(String mixinPackage) {
    }

    public String getRefMapperConfig() {
        return null;
    }

    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    public List<String> getMixins() {
        return null;
    }

    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}

