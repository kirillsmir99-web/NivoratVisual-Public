package rtx.nv.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.impl.Visuals.Ambience;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.SelectSetting;
import rtx.nv.api.ui.UI;

public final class RenderBenchmarkTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            ModuleManager.get().getAll().forEach(m -> m.disable());
            InterfaceModule.getInstance().enable();
            client.getWindow().setWindowedSize(1280,720);
            client.options.getViewDistance().setValue(2);
            client.options.getMaxFps().setValue(120);
            client.options.getEnableVsync().setValue(false);
            client.options.getInactivityFpsLimit().setValue(net.minecraft.client.option.InactivityFpsLimit.MINIMIZED);
            client.options.getGuiScale().setValue(2);
        });
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            context.waitTicks(200);
            sample(context,"world");
            context.setScreen(() -> UI.INSTANCE);
            context.runOnClient(client -> {
                SelectSetting setting = InterfaceModule.getInstance().rectStyle;
                setting.selected(setting.getOptions().get(0));
            });
            sample(context,"gui-glass");
            context.runOnClient(client -> {
                SelectSetting setting = InterfaceModule.getInstance().rectStyle;
                setting.selected(setting.getOptions().get(1));
            });
            sample(context,"gui-mosaic");
            context.setScreen(() -> null);
            context.runOnClient(client -> {
                Ambience ambience = Ambience.getInstance();
                ambience.getSettings().all().stream().filter(s -> s.getName().equals("Своё небо")).forEach(s -> ((BooleanSetting)s).setValue(true));
                ambience.enable();
                client.player.setPitch(-45);
            });
            for (String type : new String[]{"Северное сияние","Чёрная дыра","Звездопад"}) {
                context.runOnClient(client -> Ambience.getInstance().getSettings().all().stream().filter(s -> s.getName().equals("Тип неба")).forEach(s -> ((SelectSetting)s).selected(type)));
                sample(context,"sky-"+Math.abs(type.hashCode()));
            }
            // Present only in the new build: separate resolution savings from renderer changes.
            for (String type : new String[]{"Северное сияние","Чёрная дыра","Звездопад"}) {
                boolean hasQuality = context.computeOnClient(client -> {
                    boolean[] found = {false};
                    Ambience.getInstance().getSettings().all().stream().filter(s -> s.getName().equals("Качество неба")).forEach(s -> {
                        ((SelectSetting)s).selected("Высокий"); found[0] = true;
                    });
                    Ambience.getInstance().getSettings().all().stream().filter(s -> s.getName().equals("Тип неба")).forEach(s -> ((SelectSetting)s).selected(type));
                    return found[0];
                });
                if (hasQuality) sample(context,"sky-high-"+Math.abs(type.hashCode()));
            }
            context.runOnClient(client -> { Ambience.getInstance().disable(); FrameMetrics.save(); });
        }
    }
    private void sample(ClientGameTestContext context, String name) {
        context.waitTicks(120); // Resource compilation and transition are excluded.
        context.runOnClient(client -> FrameMetrics.start(name));
        context.waitTicks(300);
        context.runOnClient(client -> FrameMetrics.finish());
    }
}
