package rtx.nv.api.mods.geckolib.renderer.texture;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.texture.TextureTickListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;

public class GeckoLibAnimatedTexture
extends ResourceTexture
implements TextureTickListener {
    protected GeckoLibAnimatedTexture.AnimationInfo animatedTexture = null;
    protected int frameWidth;
    protected int frameHeight;
    protected NativeImage baseImage;

    public GeckoLibAnimatedTexture(Identifier identifier) {
        super(identifier);
    }

    public void close() {
        if (this.baseImage != null) {
            this.baseImage.close();
        }
        if (this.animatedTexture != null) {
            this.animatedTexture.close();
        }
        super.close();
    }

    public boolean isAnimated() {
        return this.animatedTexture != null;
    }

    private static /* synthetic */ boolean lambda_buildAnimatedTexture_0(IntSet intSet, int n) {
        return !intSet.contains(n);
    }

    protected void uploadFrame(GpuDevice gpuDevice, NativeImage nativeImage, int n, int n2, GpuTexture gpuTexture) {
        gpuDevice.createCommandEncoder().writeToTexture(gpuTexture, nativeImage, 0, 0, n, n2, this.frameWidth, this.frameHeight, 0, 0);
    }

    protected GeckoLibAnimatedTexture.AnimationInfo buildAnimatedTexture(AnimationResourceMetadata animationResourceMetadata) {
        if (this.baseImage == null) {
            return null;
        }
        SpriteDimensions spriteDimensions = animationResourceMetadata.getSize(this.baseImage.getWidth(), this.baseImage.getHeight());
        this.frameWidth = spriteDimensions.width();
        this.frameHeight = spriteDimensions.height();
        int n = this.baseImage.getWidth() / this.frameWidth;
        int n2 = this.baseImage.getHeight() / this.frameHeight;
        int n3 = n * n2;
        int n4 = animationResourceMetadata.defaultFrameTime();
        int n5 = animationResourceMetadata.frames().map(List::size).orElse(n3);
        if (n5 <= 1) {
            return null;
        }
        List<FrameInfo> frameList = new ObjectArrayList<>(n5);
        if (animationResourceMetadata.frames().isEmpty()) {
            for (int i = 0; i < n3; ++i) {
                frameList.add(new FrameInfo(i, n4));
            }
        } else {
            for (AnimationFrameResourceMetadata frameMeta : animationResourceMetadata.frames().get()) {
                frameList.add(new FrameInfo(frameMeta.index(), frameMeta.getTime(n4)));
            }
            int n6 = 0;
            IntOpenHashSet usedIndices = new IntOpenHashSet();
            Iterator<FrameInfo> iterator = frameList.iterator();
            while (iterator.hasNext()) {
                FrameInfo frameInfo = iterator.next();
                boolean bl = true;
                if (frameInfo.time <= 0) {
                    GeckoLibConstants.LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", (Object)this.getId(), (Object)n6, (Object)frameInfo.time);
                    bl = false;
                }
                if (frameInfo.index < 0 || frameInfo.index >= n3) {
                    GeckoLibConstants.LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", (Object)this.getId(), (Object)n6, (Object)frameInfo.index);
                    bl = false;
                }
                if (bl) {
                    usedIndices.add(frameInfo.index);
                } else {
                    iterator.remove();
                }
                ++n6;
            }
            int[] unused = IntStream.range(0, n3).filter(idx -> !usedIndices.contains(idx)).toArray();
            if (unused.length > 0) {
                GeckoLibConstants.LOGGER.warn("Unused frames in sprite {}: {}", (Object)this.getId(), (Object)Arrays.toString(unused));
            }
        }
        return new GeckoLibAnimatedTexture.AnimationInfo(this, List.copyOf(frameList), n, animationResourceMetadata.interpolate());
    }

    public void load(NativeImage image) {
        GpuDevice gpuDevice = RenderSystem.getDevice();
        Identifier identifier = this.getId();
        Objects.requireNonNull(identifier);
        this.glTexture = gpuDevice.createTexture(() -> ((Identifier)identifier).toString(), 5, TextureFormat.RGBA8, this.frameWidth, this.frameHeight, 1, 1);
        this.glTextureView = gpuDevice.createTextureView(this.glTexture);
        this.uploadFrame(gpuDevice, image, 0, 0, this.glTexture);
    }

    public void reload(TextureContents textureContents) {
        if (this.baseImage != null) {
            AddressMode addressMode = textureContents.clamp() ? AddressMode.CLAMP_TO_EDGE : AddressMode.REPEAT;
            FilterMode filterMode = textureContents.blur() ? FilterMode.LINEAR : FilterMode.NEAREST;
            this.sampler = RenderSystem.getSamplerCache().get(addressMode, addressMode, filterMode, filterMode, false);
            this.load(this.baseImage);
        }
    }

    public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
        Resource resource = resourceManager.getResourceOrThrow(this.getId());
        try (InputStream inputStream = resource.getInputStream();){
            this.baseImage = NativeImage.read((InputStream)inputStream);
        }
        this.animatedTexture = resource.getMetadata().decode(AnimationResourceMetadata.SERIALIZER).map(this::buildAnimatedTexture).orElse(null);
        return new TextureContents(this.baseImage, (TextureResourceMetadata)resource.getMetadata().decode(TextureResourceMetadata.SERIALIZER).orElse(null));
    }

    public void tick() {
        if (this.animatedTexture != null) {
            this.animatedTexture.tick();
        }
    }


    public record FrameInfo(int index, int time) {}

    public static class InterpolationData implements AutoCloseable {
        @Override
        public void close() {}
    }

    public static class AnimationInfo implements AutoCloseable {
        protected final List<FrameInfo> frames;
        protected final int frameRowSize;
        protected final boolean interpolateFrames;
        protected final InterpolationData interpolationData;
        protected final NativeImage currentFrameBuffer;
        int currentFrame;
        int subFrame;

        public AnimationInfo(GeckoLibAnimatedTexture texture, List<FrameInfo> frames, int frameRowSize, boolean interpolateFrames) {
            this(frames, frameRowSize, interpolateFrames, null, null);
        }

        public AnimationInfo(List<FrameInfo> frames, int frameRowSize, boolean interpolateFrames, InterpolationData interpolationData, NativeImage currentFrameBuffer) {
            this.frames = frames;
            this.frameRowSize = frameRowSize;
            this.interpolateFrames = interpolateFrames;
            this.interpolationData = interpolationData;
            this.currentFrameBuffer = currentFrameBuffer;
        }

        public void tick() {
        }

        int getFrameColumn(int n) {
            return n % this.frameRowSize;
        }

        @Override
        public void close() {
            if (this.currentFrameBuffer != null) {
                this.currentFrameBuffer.close();
            }
            if (this.interpolationData != null) {
                this.interpolationData.close();
            }
        }
    }
}

