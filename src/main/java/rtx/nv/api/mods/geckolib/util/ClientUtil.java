package rtx.nv.api.mods.geckolib.util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.GlfwUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.MoonPhase;
import net.minecraft.world.World;

public final class ClientUtil {
    private ClientUtil() {
    }

    public static boolean isFirstPerson() {
        return MinecraftClient.getInstance().options.getPerspective().isFirstPerson();
    }

    public static double getCurrentTick() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        return minecraftClient.world != null ? (double)((float)minecraftClient.world.getTime() + minecraftClient.getRenderTickCounter().getTickProgress(false)) : GlfwUtil.getTime() * 20.0;
    }

    public static World getLevel() {
        return MinecraftClient.getInstance().world;
    }

    public static PlayerEntity getClientPlayer() {
        return MinecraftClient.getInstance().player;
    }

    public static Vec3d getCameraPos() {
        return MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos();
    }

    public static int getVisibleEntityCount() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.world == null) {
            return 0;
        }
        return minecraftClient.worldRenderer.worldRenderState.entityRenderStates.size();
    }

    public static boolean clientPlayerHasCape() {
        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
        return clientPlayerEntity != null && clientPlayerEntity.getSkin().cape() != null;
    }

    public static MoonPhase getClientMoonPhase() {
        return MinecraftClient.getInstance().worldRenderer.worldRenderState.skyRenderState.moonPhase;
    }
}

