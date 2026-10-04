package rtx.nv.utils.render.render2d.font;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.image.BufferedImage;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;

final class GlyphAtlasPage
implements AutoCloseable {
    private static int nextId;
    private final int size;
    private final NativeImage image;
    private final NativeImageBackedTexture texture;
    private final TextureSetup textureSetup;
    private int cursorX = 1;
    private int cursorY = 1;
    private int rowHeight;
    private boolean dirty;

    GlyphAtlasPage(int n) {
        this.size = n;
        this.image = new NativeImage(n, n, true);
        this.texture = new NativeImageBackedTexture(() -> "nv_font_atlas_" + nextId++, this.image);
        this.textureSetup = TextureSetup.of((GpuTextureView)(Object)this.texture.getGlTextureView(), (GpuSampler)RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
    }

    void copy(BufferedImage bufferedImage, int n, int n2) {
        int n3 = bufferedImage.getWidth();
        int n4 = bufferedImage.getHeight();
        for (int i = 0; i < n4; ++i) {
            for (int j = 0; j < n3; ++j) {
                this.image.setColor(n + j, n2 + i, GlyphAtlasPage.argbToAbgr(bufferedImage.getRGB(j, i)));
            }
        }
        this.dirty = true;
    }

    GlyphAtlasPage.Allocation allocate(int n, int n2) {
        int n3 = n + 2;
        int n4 = n2 + 2;
        if (n3 > this.size || n4 > this.size) {
            return null;
        }
        if (this.cursorX + n3 > this.size) {
            this.cursorX = 1;
            this.cursorY += this.rowHeight;
            this.rowHeight = 0;
        }
        if (this.cursorY + n4 > this.size) {
            return null;
        }
        int n5 = this.cursorX;
        int n6 = this.cursorY;
        this.cursorX += n3;
        this.rowHeight = Math.max(this.rowHeight, n4);
        return new GlyphAtlasPage.Allocation(this, n5, n6, (float)n5 / (float)this.size, (float)n6 / (float)this.size, (float)(n5 + n) / (float)this.size, (float)(n6 + n2) / (float)this.size);
    }

    @Override
    public void close() {
        this.texture.close();
    }

    private static int argbToAbgr(int n) {
        int n2 = n >>> 24 & 0xFF;
        int n3 = n >>> 16 & 0xFF;
        int n4 = n >>> 8 & 0xFF;
        int n5 = n & 0xFF;
        return n2 << 24 | n5 << 16 | n4 << 8 | n3;
    }

    TextureSetup textureSetup() {
        return this.textureSetup;
    }

    void uploadIfDirty() {
        if (!this.dirty) {
            return;
        }
        this.texture.upload();
        this.dirty = false;
    }


    public static record Allocation(GlyphAtlasPage page, int x, int y, float u0, float v0, float u1, float v1) {
    }
}

