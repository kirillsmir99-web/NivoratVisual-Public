package rtx.nv.api.ui.module;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import rtx.nv.utils.discord.rpc.DiscordRPCManager;
import rtx.nv.utils.profile.ProfileIdentity;

public final class DiscordAvatar {
    private static final AtomicInteger ID = new AtomicInteger();
    private static volatile DiscordAvatar.State state = DiscordAvatar.State.IDLE;
    private static volatile BufferedImage decoded;
    private static String loadedUrl;
    private static String textureId;

    private DiscordAvatar() {
    }

    private static void reset() {
        if (textureId != null) {
            try {
                Identifier identifier = Identifier.tryParse((String)textureId);
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                if (identifier != null && minecraftClient != null) {
                    minecraftClient.getTextureManager().destroyTexture(identifier);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        textureId = null;
        decoded = null;
        state = DiscordAvatar.State.IDLE;
    }

    public static String texture() {
        String string = DiscordAvatar.currentUrl();
        if (string == null) {
            return null;
        }
        if (!string.equals(loadedUrl)) {
            DiscordAvatar.reset();
            loadedUrl = string;
            DiscordAvatar.startDownload(string);
        }
        if (state == DiscordAvatar.State.DECODED && decoded != null) {
            BufferedImage bufferedImage = decoded;
            decoded = null;
            state = DiscordAvatar.State.UPLOADING;
            MinecraftClient.getInstance().execute(() -> DiscordAvatar.upload(bufferedImage));
        }
        return state == DiscordAvatar.State.READY ? textureId : null;
    }

    private static void upload(BufferedImage bufferedImage) {
        try {
            int n;
            int n2 = bufferedImage.getWidth();
            int n3 = bufferedImage.getHeight();
            NativeImage nativeImage = new NativeImage(n2, n3, false);
            for (n = 0; n < n3; ++n) {
                for (int i = 0; i < n2; ++i) {
                    nativeImage.setColorArgb(i, n, bufferedImage.getRGB(i, n));
                }
            }
            int avatarId = ID.getAndIncrement();
            GpuTexture gpuTexture = RenderSystem.getDevice().createTexture(() -> "nv_discord_avatar_" + avatarId, 5, TextureFormat.RGBA8, n2, n3, 1, 1);
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpuTexture, nativeImage);
            GpuTextureView gpuTextureView = RenderSystem.getDevice().createTextureView(gpuTexture);
            nativeImage.close();
            Identifier identifier = Identifier.of((String)"nv", (String)("discord/avatar_" + avatarId));
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, (AbstractTexture)new DiscordAvatar.AvatarTexture(gpuTexture, gpuTextureView));
            textureId = identifier.toString();
            state = DiscordAvatar.State.READY;
        }
        catch (Exception exception) {
            state = DiscordAvatar.State.FAILED;
        }
    }

    public static String currentUrl() {
        String avatar = ProfileIdentity.avatarUrl();
        if (avatar != null) {
            return avatar;
        }
        avatar = DiscordRPCManager.avatarUrl();
        if (avatar == null) {
            return null;
        }
        avatar = avatar.replace(".gif", ".png");
        if (!avatar.contains("?")) {
            avatar = avatar + "?size=128";
        }
        return avatar;
    }

    private static void startDownload(String string) {
        state = DiscordAvatar.State.DOWNLOADING;
        Thread thread = new Thread(() -> {
            try {
                BufferedImage bufferedImage;
                URLConnection uRLConnection = URI.create(string).toURL().openConnection();
                uRLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (NV)");
                uRLConnection.setConnectTimeout(6000);
                uRLConnection.setReadTimeout(6000);
                try (InputStream inputStream = uRLConnection.getInputStream();){
                    bufferedImage = ImageIO.read(inputStream);
                }
                if (bufferedImage != null) {
                    decoded = bufferedImage;
                    state = DiscordAvatar.State.DECODED;
                } else {
                    state = DiscordAvatar.State.FAILED;
                }
            }
            catch (Exception exception) {
                state = DiscordAvatar.State.FAILED;
            }
        }, "nv-discord-avatar");
        thread.setDaemon(true);
        thread.start();
    }


    public static final class AvatarTexture
    extends AbstractTexture {
        private final GpuTexture gpuTexture;
        private final GpuTextureView gpuTextureView;
    
        public AvatarTexture(GpuTexture gpuTexture, GpuTextureView gpuTextureView) {
            this.gpuTexture = gpuTexture;
            this.gpuTextureView = gpuTextureView;
        }
    
        public void load(ResourceManager resourceManager) {
            long l = 0L;
            AvatarTexture avatarTexture = null;
            long l2 = 0L;
            ResourceManager resourceManager2 = null;
            long l3 = 0L;
            Object var14_7 = null;
            long l4 = 0L;
            Object var17_9 = null;
            long l5 = 0L;
            Object var20_11 = null;
            long l6 = 0L;
            Object var23_13 = null;
            avatarTexture = this;
            resourceManager2 = resourceManager;
        }
    
        public GpuTextureView getGlTextureView() {
            return this.gpuTextureView;
        }
    
        public GpuTexture getGlTexture() {
            return this.gpuTexture;
        }
    }
    
    public static enum State {
        IDLE,
        DOWNLOADING,
        DECODED,
        UPLOADING,
        READY,
        FAILED
    }
}

