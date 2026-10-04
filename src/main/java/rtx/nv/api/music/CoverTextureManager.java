package rtx.nv.api.music;

import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class CoverTextureManager {
    public static final Identifier DEFAULT_COVER = Identifier.of("nv", "textures/gui/default_cover.png");
    private static final Identifier COVER_TEXTURE_ID = Identifier.of("nv", "dynamic/album_cover");

    private final ExecutorService decoder = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1), r -> {
        Thread thread = new Thread(r, "NV-CoverDecoder");
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.DiscardOldestPolicy());
    private static final int MAX_ENCODED_LENGTH = 6 * 1024 * 1024;
    private static final int MAX_DIMENSION = 2048;
    private final AtomicLong generation = new AtomicLong();
    private volatile boolean stopped;

    private volatile Identifier currentCoverId = DEFAULT_COVER;
    private volatile boolean hasCustomCover = false;
    private volatile int dominantColor = 0xFF5588DD;

    public Identifier getCurrentCoverId() {
        return this.currentCoverId;
    }

    public int getDominantColor() {
        return this.dominantColor;
    }

    public boolean hasCustomCover() {
        return this.hasCustomCover;
    }

    public synchronized void updateCover(String base64) {
        if (this.stopped) return;
        if (base64 == null || base64.isEmpty()) {
            this.resetToDefault();
            return;
        }
        if (base64.length() > MAX_ENCODED_LENGTH) {
            this.resetToDefault();
            return;
        }
        long request = this.generation.incrementAndGet();
        this.decoder.submit(() -> {
            try {
                if (this.stopped || request != this.generation.get()) return;
                byte[] bytes = Base64.getDecoder().decode(base64);
                // Inspect dimensions before STB allocates native pixel memory.
                if (!safeDimensions(bytes)) {
                    this.resetIfCurrent(request);
                    return;
                }
                NativeImage image = NativeImage.read(bytes);
                if (image == null) {
                    this.resetIfCurrent(request);
                    return;
                }

                int color = extractDominantColor(image);

                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.execute(() -> {
                        if (this.stopped || request != this.generation.get()) {
                            image.close();
                            return;
                        }
                        NativeImageBackedTexture texture = null;
                        try {
                            texture = new NativeImageBackedTexture(() -> "nv_cover", image);
                            texture.upload();
                            mc.getTextureManager().registerTexture(COVER_TEXTURE_ID, texture);
                            rtx.nv.utils.render.render2d.image.ImageRenderer.getInstance().invalidateTexture(COVER_TEXTURE_ID);
                            this.currentCoverId = COVER_TEXTURE_ID;
                            this.hasCustomCover = true;
                            this.dominantColor = color;
                        } catch (Exception e) {
                            if (texture != null) texture.close(); else image.close();
                            this.resetIfCurrent(request);
                        }
                    });
                } else {
                    image.close();
                }
            } catch (Exception e) {
                this.resetIfCurrent(request);
            }
        });
    }

    private static boolean safeDimensions(byte[] bytes) throws java.io.IOException {
        try (var stream = javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
            var readers = javax.imageio.ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) return false;
            var reader = readers.next();
            try {
                reader.setInput(stream);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                return w > 0 && h > 0 && w <= MAX_DIMENSION && h <= MAX_DIMENSION;
            } finally {
                reader.dispose();
            }
        }
    }

    private synchronized void resetIfCurrent(long request) {
        if (request == this.generation.get()) this.resetToDefault();
    }

    private static int extractDominantColor(NativeImage image) {
        if (image == null) {
            return 0xFF5588DD;
        }
        try {
            int w = image.getWidth();
            int h = image.getHeight();
            long rSum = 0L, gSum = 0L, bSum = 0L, count = 0L;
            int step = Math.max(1, w / 16);

            for (int y = 0; y < h; y += step) {
                for (int x = 0; x < w; x += step) {
                    int argb = image.getColorArgb(x, y);
                    int a = (argb >> 24) & 0xFF;
                    if (a < 128) continue;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int brightness = (r + g + b) / 3;
                    if (brightness >= 35 && brightness <= 230) {
                        rSum += r;
                        gSum += g;
                        bSum += b;
                        count++;
                    }
                }
            }

            if (count > 0L) {
                int rAvg = (int) (rSum / count);
                int gAvg = (int) (gSum / count);
                int bAvg = (int) (bSum / count);
                return 0xFF000000 | (rAvg << 16) | (gAvg << 8) | bAvg;
            }
        } catch (Exception ignored) {
        }
        return 0xFF5588DD;
    }

    public synchronized void resetToDefault() {
        long request = this.generation.incrementAndGet();
        this.dominantColor = 0xFF5588DD;
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                mc.execute(() -> {
                    if (request != this.generation.get()) return;
                    this.hasCustomCover = false;
                    this.currentCoverId = DEFAULT_COVER;
                    rtx.nv.utils.render.render2d.image.ImageRenderer.getInstance().invalidateTexture(COVER_TEXTURE_ID);
                    mc.getTextureManager().destroyTexture(COVER_TEXTURE_ID);
                });
            } else {
                this.hasCustomCover = false;
                this.currentCoverId = DEFAULT_COVER;
            }
        }
    }

    public synchronized void shutdown() {
        this.stopped = true;
        this.decoder.shutdownNow();
        this.resetToDefault();
    }
}
