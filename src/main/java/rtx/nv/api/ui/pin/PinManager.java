package rtx.nv.api.ui.pin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import rtx.nv.api.config.ConfigManager;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.utils.animations.Decelerate;
import rtx.nv.utils.animations.Direction;
import rtx.nv.utils.sounds.Sounds;

public final class PinManager {
    private static final Set<String> PINNED = new LinkedHashSet<>();
    private static final Map<String, Decelerate> LATCH_ANIMS = new HashMap<>();

    private PinManager() {
    }

    public static boolean isPinned(String moduleName) {
        return moduleName != null && PINNED.contains(moduleName);
    }

    public static boolean isPinned(Module module) {
        return module != null && isPinned(module.getName());
    }

    public static void toggle(Module module) {
        if (module == null) {
            return;
        }
        if (isPinned(module)) {
            unpin(module);
        } else {
            pin(module);
        }
    }

    public static void pin(Module module) {
        if (module == null) {
            return;
        }
        PINNED.add(module.getName());
        Decelerate anim = LATCH_ANIMS.computeIfAbsent(module.getName(), k -> createLatchAnim());
        anim.setDirection(Direction.FORWARDS);
        anim.counter.resetCounter();
        try {
            Sounds.play("pin");
        } catch (Throwable ignored) {
        }
        ConfigManager.markDirty();
    }

    public static void unpin(Module module) {
        if (module == null) {
            return;
        }
        PINNED.remove(module.getName());
        Decelerate anim = LATCH_ANIMS.computeIfAbsent(module.getName(), k -> createLatchAnim());
        anim.setDirection(Direction.BACKWARDS);
        try {
            Sounds.play("unpin");
        } catch (Throwable ignored) {
        }
        ConfigManager.markDirty();
    }

    public static float latchProgress(String moduleName) {
        if (moduleName == null) {
            return 0.0f;
        }
        Decelerate anim = LATCH_ANIMS.get(moduleName);
        if (anim == null) {
            return isPinned(moduleName) ? 1.0f : 0.0f;
        }
        return anim.getOutput().floatValue();
    }

    public static int getPinnedCount() {
        return PINNED.size();
    }

    public static List<String> getPinnedNames() {
        return new ArrayList<>(PINNED);
    }

    public static void setPinnedNames(List<String> names) {
        PINNED.clear();
        if (names != null) {
            for (String name : names) PINNED.add("China Hat".equalsIgnoreCase(name) ? "Crown" : name);
            for (String name : PINNED) {
                Decelerate anim = LATCH_ANIMS.computeIfAbsent(name, k -> createLatchAnim());
                anim.setDirection(Direction.FORWARDS);
                anim.counter.setTime(System.currentTimeMillis() - 10000L);
            }
        }
    }

    public static List<Module> getPinnedModules() {
        if (PINNED.isEmpty()) {
            return Collections.emptyList();
        }
        ModuleManager manager = ModuleManager.get();
        if (manager == null) {
            return Collections.emptyList();
        }
        List<Module> list = new ArrayList<>();
        for (String name : PINNED) {
            Module module = manager.findByName(name);
            if (module != null) {
                list.add(module);
            }
        }
        return list;
    }

    private static Decelerate createLatchAnim() {
        Decelerate decelerate = (Decelerate) new Decelerate().setMs(200).setValue(1.0);
        decelerate.setDirection(Direction.BACKWARDS);
        decelerate.counter.setTime(System.currentTimeMillis() - 10000L);
        return decelerate;
    }
}
