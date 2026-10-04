package rtx.nv.utils.render.render2d.gif;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import rtx.nv.NV;
import org.w3c.dom.Node;
import rtx.nv.utils.render.render2d.Render2D;

public final class GifRenderer {
    private static final Map<String, GifAnimation> CACHE = new HashMap<>();
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "nv-gif-loader");
        thread.setDaemon(true);
        return thread;
    });

    public static void shutdown() {
        LOADER.shutdownNow();
        for (GifAnimation gifAnimation : CACHE.values()) {
            gifAnimation.dispose();
        }
        CACHE.clear();
    }

    private static int parseInt(String string, int n) {
        if (string == null) {
            return n;
        }
        try {
            return Integer.parseInt(string);
        }
        catch (Exception exception) {
            return n;
        }
    }

    private static IIOMetadataNode findNode(IIOMetadataNode iIOMetadataNode, String string) {
        for (int n = 0; n < iIOMetadataNode.getLength(); ++n) {
            if (!iIOMetadataNode.item(n).getNodeName().equalsIgnoreCase(string)) continue;
            return (IIOMetadataNode)iIOMetadataNode.item(n);
        }
        for (int n = 0; n < iIOMetadataNode.getLength(); ++n) {
            Node node = iIOMetadataNode.item(n);
            if (node instanceof IIOMetadataNode) {
                IIOMetadataNode found = GifRenderer.findNode((IIOMetadataNode)node, string);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static BufferedImage copyImage(BufferedImage bufferedImage) {
        BufferedImage bufferedImage2 = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), 2);
        Graphics2D graphics2D = bufferedImage2.createGraphics();
        graphics2D.drawImage((Image)bufferedImage, 0, 0, null);
        graphics2D.dispose();
        return bufferedImage2;
    }

    public static void preload(String string2) {
        CACHE.computeIfAbsent(string2, string -> {
            GifAnimation anim = new GifAnimation();
            anim.startLoad(string);
            return anim;
        });
    }

    public static void draw(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, String string2, float f6) {
        GifAnimation anim = CACHE.computeIfAbsent(string2, string -> {
            GifAnimation a = new GifAnimation();
            a.startLoad(string);
            return a;
        });
        if (anim.state != State.GPU_READY) {
            return;
        }
        String string3 = anim.getCurrentFrameId();
        if (string3 == null) {
            return;
        }
        int n = Math.max(0, Math.min(255, (int)(f6 * 255.0f)));
        Render2D.image(string3, f, f2, f3, f4, f5, n << 24 | 0xFFFFFF);
    }

    public static enum State {
        IDLE,
        LOADING,
        FRAMES_READY,
        GPU_READY;
    }

    public static final class GifAnimation {
        volatile State state = State.IDLE;
        final List<BufferedImage> rawFrames = new ArrayList<>();
        final List<Integer> rawDelays = new ArrayList<>();
        final List<String> frames = new ArrayList<>();
        final List<Integer> frameDelays = new ArrayList<>();
        final List<GpuTexture> gpuTextures = new ArrayList<>();
        final List<GpuTextureView> gpuViews = new ArrayList<>();
        int totalDuration = 0;

        private GifAnimation() {
        }

        void startLoad(String string) {
            this.state = State.LOADING;
            GifRenderer.LOADER.submit(() -> {
                try {
                    this.decodeGif(string);
                }
                catch (Exception exception) {
                    NV.LOGGER.error("Failed to decode gif {}", string, exception);
                    this.state = State.IDLE;
                }
            });
        }

        private void decodeGif(String string) throws Exception {
            String string2;
            if (string.contains(":")) {
                String[] split = string.split(":", 2);
                string2 = "/assets/" + split[0] + "/" + split[1];
            } else {
                string2 = "/assets/nv/" + string;
            }
            InputStream inputStream = GifRenderer.class.getResourceAsStream(string2);
            if (inputStream == null) {
                this.state = State.IDLE;
                return;
            }
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream);
            ImageReader imageReader = ImageIO.getImageReadersByFormatName("gif").next();
            imageReader.setInput(imageInputStream, false, false);
            int n = imageReader.getWidth(0);
            int n2 = imageReader.getHeight(0);
            int n3 = imageReader.getNumImages(true);
            BufferedImage bufferedImage = new BufferedImage(n, n2, 2);
            Graphics2D graphics2D = bufferedImage.createGraphics();
            graphics2D.setBackground(new Color(0, 0, 0, 0));
            BufferedImage bufferedImage2 = null;
            for (int i = 0; i < n3; ++i) {
                int n4;
                IIOMetadata iIOMetadata = imageReader.getImageMetadata(i);
                IIOMetadataNode iIOMetadataNode = (IIOMetadataNode)iIOMetadata.getAsTree(iIOMetadata.getNativeMetadataFormatName());
                IIOMetadataNode iIOMetadataNode2 = GifRenderer.findNode(iIOMetadataNode, "GraphicControlExtension");
                IIOMetadataNode iIOMetadataNode3 = GifRenderer.findNode(iIOMetadataNode, "ImageDescriptor");
                String string3 = iIOMetadataNode2 != null ? iIOMetadataNode2.getAttribute("disposalMethod") : "none";
                int n5 = 100;
                if (iIOMetadataNode2 != null) {
                    try {
                        n5 = Integer.parseInt(iIOMetadataNode2.getAttribute("delayTime")) * 10;
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (n5 <= 0) {
                    n5 = 100;
                }
                int n6 = iIOMetadataNode3 != null ? GifRenderer.parseInt(iIOMetadataNode3.getAttribute("imageLeftPosition"), 0) : 0;
                int n7 = n4 = iIOMetadataNode3 != null ? GifRenderer.parseInt(iIOMetadataNode3.getAttribute("imageTopPosition"), 0) : 0;
                if ("restoreToPrevious".equals(string3)) {
                    bufferedImage2 = GifRenderer.copyImage(bufferedImage);
                }
                BufferedImage bufferedImage3 = imageReader.read(i);
                graphics2D.setComposite(AlphaComposite.SrcOver);
                graphics2D.drawImage((Image)bufferedImage3, n6, n4, null);
                this.rawFrames.add(GifRenderer.copyImage(bufferedImage));
                this.rawDelays.add(n5);
                if ("restoreToBackgroundColor".equals(string3)) {
                    graphics2D.setComposite(AlphaComposite.Clear);
                    graphics2D.fillRect(n6, n4, bufferedImage3.getWidth(), bufferedImage3.getHeight());
                    continue;
                }
                if (!"restoreToPrevious".equals(string3) || bufferedImage2 == null) continue;
                graphics2D.setComposite(AlphaComposite.Src);
                graphics2D.drawImage((Image)bufferedImage2, 0, 0, null);
            }
            graphics2D.dispose();
            imageReader.dispose();
            imageInputStream.close();
            inputStream.close();
            this.state = State.FRAMES_READY;
            this.scheduleGpuUploads();
        }

        void dispose() {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient != null) {
                for (String string : this.frames) {
                    try {
                        Identifier identifier = Identifier.tryParse(string);
                        if (identifier == null) continue;
                        minecraftClient.getTextureManager().destroyTexture(identifier);
                    }
                    catch (Exception exception) {}
                }
            }
            this.gpuViews.forEach(GpuTextureView::close);
            this.gpuTextures.forEach(GpuTexture::close);
            this.frames.clear();
            this.frameDelays.clear();
            this.gpuTextures.clear();
            this.gpuViews.clear();
            this.rawFrames.clear();
            this.rawDelays.clear();
        }

        private void uploadSingleFrame(int n) {
            try {
                int n2;
                if (n >= this.rawFrames.size()) {
                    return;
                }
                BufferedImage bufferedImage = this.rawFrames.get(n);
                int n3 = bufferedImage.getWidth();
                int n4 = bufferedImage.getHeight();
                NativeImage nativeImage = new NativeImage(n3, n4, false);
                for (n2 = 0; n2 < n4; ++n2) {
                    for (int i = 0; i < n3; ++i) {
                        nativeImage.setColorArgb(i, n2, bufferedImage.getRGB(i, n2));
                    }
                }
                int frameId = GifRenderer.ID_COUNTER.getAndIncrement();
                GpuTexture gpuTexture = RenderSystem.getDevice().createTexture(() -> "nv_gif_" + frameId, 5, TextureFormat.RGBA8, n3, n4, 1, 1);
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpuTexture, nativeImage);
                GpuTextureView gpuTextureView = RenderSystem.getDevice().createTextureView(gpuTexture);
                nativeImage.close();
                Identifier identifier = Identifier.of("nv", "gif/frame_" + frameId);
                MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, new GifFrameTexture(gpuTexture, gpuTextureView, n3, n4));
                this.frames.add(identifier.toString());
                this.frameDelays.add(this.rawDelays.get(n));
                this.gpuTextures.add(gpuTexture);
                this.gpuViews.add(gpuTextureView);
                this.totalDuration += this.rawDelays.get(n);
            }
            catch (Exception exception) {
                NV.LOGGER.error("Failed to upload gif frame to GPU", exception);
            }
        }

        private void scheduleGpuUploads() {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient == null) {
                return;
            }
            for (int n = 0; n < this.rawFrames.size(); ++n) {
                int n2 = n;
                minecraftClient.execute(() -> this.uploadSingleFrame(n2));
            }
            minecraftClient.execute(() -> {
                this.rawFrames.clear();
                this.state = State.GPU_READY;
            });
        }

        String getCurrentFrameId() {
            if (this.frames.isEmpty()) {
                return null;
            }
            if (this.totalDuration == 0) {
                return this.frames.get(0);
            }
            long l = System.currentTimeMillis() % (long)this.totalDuration;
            long l2 = 0L;
            for (int i = 0; i < this.frames.size(); ++i) {
                if (l >= (l2 += (long)this.frameDelays.get(i).intValue())) continue;
                return this.frames.get(i);
            }
            return this.frames.get(this.frames.size() - 1);
        }
    }

    public static final class GifFrameTexture extends AbstractTexture {
        private final GpuTexture gpuTexture;
        private final GpuTextureView gpuTextureView;
        private final int width;
        private final int height;

        public GifFrameTexture(GpuTexture gpuTexture, GpuTextureView gpuTextureView, int n, int n2) {
            this.gpuTexture = gpuTexture;
            this.gpuTextureView = gpuTextureView;
            this.width = n;
            this.height = n2;
        }

        public void load(ResourceManager resourceManager) {
        }

        public GpuTextureView getGlTextureView() {
            return this.gpuTextureView;
        }

        public GpuTexture getGlTexture() {
            return this.gpuTexture;
        }
    }
}
