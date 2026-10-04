package rtx.nv.api.liteapi;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import rtx.nv.api.liteapi.Feature;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;

public final class FeatureBlocklist {
    private static volatile Set<String> blocked = Collections.emptySet();

    private FeatureBlocklist() {
    }

    public static void clear() {
        blocked = Collections.emptySet();
    }

    public static boolean hasAny() {
        return !blocked.isEmpty();
    }

    public static boolean isModuleBlocked(Module module) {
        if (module == null) {
            return false;
        }
        Set<String> set = blocked;
        if (set.isEmpty()) {
            return false;
        }
        Feature feature = module.getClass().getAnnotation(Feature.class);
        if (feature == null) {
            return false;
        }
        for (String string : feature.value()) {
            if (string == null || !set.contains(string.toLowerCase().trim())) continue;
            return true;
        }
        return false;
    }

    public static void applyBlocklist(Collection<String> collection) {
        if (collection == null || collection.isEmpty()) {
            blocked = Collections.emptySet();
            return;
        }
        HashSet<String> hashSet = new HashSet<String>();
        for (String string : collection) {
            if (string == null || string.isBlank()) continue;
            hashSet.add(string.toLowerCase().trim());
        }
        blocked = hashSet;
    }

    public static Set<String> knownFeatures() {
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>();
        for (Module module : ModuleManager.get().getAll()) {
            Feature feature = module.getClass().getAnnotation(Feature.class);
            if (feature == null) continue;
            Collections.addAll(linkedHashSet, feature.value());
        }
        return linkedHashSet;
    }
}

