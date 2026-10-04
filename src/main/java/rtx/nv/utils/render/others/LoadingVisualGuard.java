package rtx.nv.utils.render.others;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.ReconfiguringScreen;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;

public final class LoadingVisualGuard {
    private LoadingVisualGuard() {
    }

    public static boolean shouldSuppressHud(MinecraftClient minecraftClient) {
        if (minecraftClient == null) {
            return false;
        }
        if (minecraftClient.getOverlay() instanceof SplashOverlay) {
            return true;
        }
        if (minecraftClient.currentScreen instanceof LevelLoadingScreen
                || minecraftClient.currentScreen instanceof ConnectScreen
                || minecraftClient.currentScreen instanceof ProgressScreen
                || minecraftClient.currentScreen instanceof ReconfiguringScreen) {
            return true;
        }
        return false;
    }
}
