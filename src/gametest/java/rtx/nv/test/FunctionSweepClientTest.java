package rtx.nv.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.SelectSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.api.ui.UI;

/** Covers every registered settings panel. Service delivery requires external acceptance. */
public final class FunctionSweepClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        context.waitTicks(20);
        var results = new ArrayList<Map<String,Object>>();
        try (var world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            context.waitTicks(20);
            var modules = context.computeOnClient(client -> List.copyOf(ModuleManager.get().getAll()));
            for (var module : modules) context.runOnClient(client -> module.disable());
            for (var module : modules) {
                boolean external = List.of("IRC", "Party", "Globals", "Discord").stream().anyMatch(n -> n.equalsIgnoreCase(module.getName()));
                context.runOnClient(client -> {
                    if (!external) module.enable();
                    client.setScreen(UI.INSTANCE);
                    UI.INSTANCE.selectCategoryFromWorkspace(module.getCategory());
                });
                context.waitTicks(3);
                context.runOnClient(client -> UI.INSTANCE.getInspector().open(module));
                int options = 0, switches = 0, ranges = 0;
                for (var setting : module.getSettings().all()) {
                    if (setting instanceof SelectSetting select) {
                        String previous = context.computeOnClient(client -> select.getSelected());
                        for (String option : select.getOptions()) {
                            context.runOnClient(client -> {
                                select.selected(option);
                                if (!select.is(option)) throw new AssertionError("Option ignored: " + module.getName()+"/"+setting.getName()+"/"+option);
                            });
                            context.waitTicks(2); options++;
                        }
                        context.runOnClient(client -> select.selected(previous));
                    } else if (setting instanceof BooleanSetting toggle) {
                        boolean previous = context.computeOnClient(client -> toggle.getValue());
                        context.runOnClient(client -> toggle.setValue(!previous));
                        context.waitTicks(1);
                        context.runOnClient(client -> toggle.setValue(previous)); switches++;
                    } else if (setting instanceof SliderSetting number) {
                        float previous = context.computeOnClient(client -> number.getValue());
                        for (float value : new float[]{number.getMin(), number.getMax(), Float.NaN}) {
                            context.runOnClient(client -> {
                                number.setValue(value);
                                if (!Float.isFinite(number.getValue())) throw new AssertionError("Nonfinite setting: " + module.getName()+"/"+setting.getName());
                            });
                            context.waitTicks(1);
                        }
                        context.runOnClient(client -> number.setValue(previous)); ranges++;
                    }
                }
                context.waitTicks(3);
                results.add(Map.of("module",module.getName(),"settings",module.getSettings().all().size(),"select_values",options,"switches",switches,"numeric_ranges",ranges,"lifecycle",external?"external delivery excluded":"enabled, rendered and disabled"));
                context.runOnClient(client -> {
                    client.setScreen(null);
                    module.disable();
                    if (module.isEnabled()) throw new AssertionError("Disable failed: " + module.getName());
                    rtx.nv.NV.LOGGER.info("[NV-SWEEP] Passed {}", module.getName());
                });
            }
        }
        try {
            Files.writeString(Path.of("function-sweep.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results));
        } catch (java.io.IOException error) { throw new AssertionError(error); }
        context.runOnClient(client -> rtx.nv.NV.LOGGER.info("[NV-SWEEP] All {} registered module panels and settings completed", results.size()));
    }
}
