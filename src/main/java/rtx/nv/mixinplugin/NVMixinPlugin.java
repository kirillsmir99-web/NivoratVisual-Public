package rtx.nv.mixinplugin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class NVMixinPlugin implements IMixinConfigPlugin {
    private static final Map<String, String> DISABLED_BY_MOD = Map.of(
        "rtx.nv.mixin.LevelRendererChunkFadeMixin", "sodium"
    );
    private static final Map<String, String> REQUIRED_MOD = Map.of(
        "rtx.nv.mixin.compat.IrisHandRendererHandsMixin", "iris",
        "rtx.nv.mixin.compat.IrisRenderingPipelineHandsMixin", "iris"
    );
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("NV");
    private static final Set<String> SKIPPED = new HashSet<String>();

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals("rtx.nv.mixin.cape.AnimatedCapeMixin") && NVMixinPlugin.isLoaded("waveycapes")) {
            if (SKIPPED.add(mixinClassName)) {
                LOGGER.info("[NV] Skipping mixin {} because 'waveycapes' is installed", mixinClassName);
            }
            return false;
        }

        if (mixinClassName.startsWith("rtx.nv.mixin.shulkerview.") && NVMixinPlugin.isLoaded("shulkerboxtooltip")) {
            if (SKIPPED.add(mixinClassName)) {
                LOGGER.info("[NV] Skipping mixin {} because 'shulkerboxtooltip' is installed", mixinClassName);
            }
            return false;
        }

        if (mixinClassName.startsWith("rtx.nv.mixin.chatheads.") && (NVMixinPlugin.isLoaded("chat_heads") || NVMixinPlugin.isLoaded("chatheads"))) {
            if (SKIPPED.add(mixinClassName)) {
                LOGGER.info("[NV] Skipping mixin {} because 'chat_heads' is installed", mixinClassName);
            }
            return false;
        }

        if (mixinClassName.startsWith("rtx.nv.mixin.chatanim.") && NVMixinPlugin.isLoaded("chatanimation")) {
            if (SKIPPED.add(mixinClassName)) {
                LOGGER.info("[NV] Skipping mixin {} because 'chatanimation' is installed", mixinClassName);
            }
            return false;
        }

        if (mixinClassName.equals("rtx.nv.mixin.TitleScreenAccountSwitcherMixin") && NVMixinPlugin.isLoaded("ias")) {
            if (SKIPPED.add(mixinClassName)) {
                LOGGER.info("[NV] Skipping mixin {} because 'ias' is installed", mixinClassName);
            }
            return false;
        }

        String blockedBy = DISABLED_BY_MOD.get(mixinClassName);
        if (blockedBy != null && NVMixinPlugin.isLoaded(blockedBy)) {
            if (SKIPPED.add(mixinClassName)) {
                LOGGER.info("[NV] Skipping mixin {} because '{}' is installed", mixinClassName, blockedBy);
            }
            return false;
        }

        String required = REQUIRED_MOD.get(mixinClassName);
        if (required != null && !NVMixinPlugin.isLoaded(required)) {
            return false;
        }

        return true;
    }

    private static boolean isLoaded(String modId) {
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Throwable throwable) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
