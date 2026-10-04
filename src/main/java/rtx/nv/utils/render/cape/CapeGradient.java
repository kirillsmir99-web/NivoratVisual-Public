package rtx.nv.utils.render.cape;
import java.io.InputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.system.MemoryUtil;
import rtx.nv.NV;
import rtx.nv.api.ui.theme.ClientAccent;

public final class CapeGradient {
    private static final Identifier BASE_TEXTURE = Identifier.of((String)"nv", (String)"textures/capes/cape.png");
    private static final Identifier DYNAMIC_TEXTURE = Identifier.of((String)"nv", (String)"dynamic/cape_gradient");
    private static final Identifier DYNAMIC_ASSET_ID = Identifier.of((String)"nv", (String)"capes/cape_gradient");
    public static final AssetInfo.TextureAsset STATIC_ASSET = new AssetInfo.TextureAssetInfo(Identifier.of((String)"nv", (String)"capes/cape"), BASE_TEXTURE);
    private static final int LUT_SIZE = 256;
    private static final int[] gradientLut = new int[256];
    private static AssetInfo.TextureAsset dynamicAsset;
    private static NativeImage base;
    private static NativeImage work;
    private static int[] basePixels;
    private static int baseW;
    private static int baseH;
    private static int bx0;
    private static int by0;
    private static int bx1;
    private static int by1;
    private static boolean projUseX;
    private static int[] projIdx;
    private static NativeImageBackedTexture texture;
    private static int[] packedPixels;
    private static final int[] shadeLut;
    private static long lastUpdateMs;
    private static int lastSig;
    private static boolean initialized;
    private static boolean failed;

    private CapeGradient() {
    }

    static {
        shadeLut = new int[65536];
        lastSig = Integer.MIN_VALUE;
    }

    public static AssetInfo.TextureAsset asset() {
        CapeGradient.ensureInit();
        return !failed && dynamicAsset != null ? dynamicAsset : STATIC_ASSET;
    }

    private static void ensureInit() {
        if (initialized || failed) {
            return;
        }
        initialized = true;
        try {
            int n;
            int n2;
            int n3;
            int n4;
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            Resource resource = minecraftClient.getResourceManager().getResourceOrThrow(BASE_TEXTURE);
            try (InputStream inputStream = resource.getInputStream();){
                base = NativeImage.read((InputStream)inputStream);
            }
            baseW = base.getWidth();
            baseH = base.getHeight();
            basePixels = new int[baseW * baseH];
            bx0 = baseW;
            by0 = baseH;
            bx1 = 0;
            by1 = 0;
            for (n4 = 0; n4 < baseH; ++n4) {
                for (int i = 0; i < baseW; ++i) {
                    CapeGradient.basePixels[n4 * CapeGradient.baseW + i] = n3 = base.getColorArgb(i, n4);
                    if (ColorHelper.getAlpha((int)n3) < 8 || Math.max(ColorHelper.getRed((int)n3), Math.max(ColorHelper.getGreen((int)n3), ColorHelper.getBlue((int)n3))) <= 40) continue;
                    if (i < bx0) {
                        bx0 = i;
                    }
                    if (i > bx1) {
                        bx1 = i;
                    }
                    if (n4 < by0) {
                        by0 = n4;
                    }
                    if (n4 <= by1) continue;
                    by1 = n4;
                }
            }
            if (bx1 < bx0 || by1 < by0) {
                bx0 = 0;
                by0 = 0;
                bx1 = baseW - 1;
                by1 = baseH - 1;
            }
            projUseX = bx1 - bx0 >= by1 - by0;
            n4 = Math.max(1, projUseX ? baseW : baseH);
            int[] nArray = new int[n4];
            n3 = 0;
            for (n2 = 0; n2 < baseH; ++n2) {
                for (n = 0; n < baseW; ++n) {
                    int n5 = basePixels[n2 * baseW + n];
                    if (ColorHelper.getAlpha((int)n5) < 8 || Math.max(ColorHelper.getRed((int)n5), Math.max(ColorHelper.getGreen((int)n5), ColorHelper.getBlue((int)n5))) <= 40) continue;
                    int n6 = projUseX ? n : n2;
                    nArray[n6] = nArray[n6] + 1;
                    ++n3;
                }
            }
            projIdx = new int[n4];
            if (n3 > 0) {
                n2 = 0;
                for (n = 0; n < n4; ++n) {
                    float f = ((float)(n2 += nArray[n]) - (float)nArray[n] * 0.5f) / (float)n3;
                    int n7 = Math.round(f * 255.0f);
                    CapeGradient.projIdx[n] = Math.max(0, Math.min(255, n7));
                }
            }
            CapeGradient.buildDynamicIndex();
            work = new NativeImage(baseW, baseH, false);
            texture = new NativeImageBackedTexture(() -> "nv_cape_gradient", work);
            minecraftClient.getTextureManager().registerTexture(DYNAMIC_TEXTURE, (AbstractTexture)texture);
            dynamicAsset = new AssetInfo.TextureAssetInfo(DYNAMIC_ASSET_ID, DYNAMIC_TEXTURE);
            CapeGradient.recolor(true);
        }
        catch (Throwable throwable) {
            failed = true;
            NV.LOGGER.error("[CapeGradient] init failed, falling back to plain cape", throwable);
        }
    }

    public static void tick() {
        CapeGradient.ensureInit();
        if (failed || texture == null) {
            return;
        }
        long l = System.currentTimeMillis();
        if (l - lastUpdateMs < 40L) {
            return;
        }
        lastUpdateMs = l;
        CapeGradient.recolor(false);
    }

    private static void buildDynamicIndex() {
        int n = baseW * baseH;
        packedPixels = new int[n];
        for (int i = 0; i < baseH; ++i) {
            int n2 = i * baseW;
            int n3 = projIdx == null || projIdx.length == 0 || projUseX ? 0 : projIdx[i];
            for (int j = 0; j < baseW; ++j) {
                int n4 = basePixels[n2 + j];
                int n5 = ColorHelper.getAlpha((int)n4);
                int n6 = Math.max(ColorHelper.getRed((int)n4), Math.max(ColorHelper.getGreen((int)n4), ColorHelper.getBlue((int)n4)));
                int n7 = projIdx == null || projIdx.length == 0 ? 0 : (projUseX ? projIdx[j] : n3);
                CapeGradient.packedPixels[n2 + j] = n5 << 24 | n7 << 8 | n6;
            }
        }
    }

    private static int colorSignature() {
        rtx.nv.api.modules.impl.Visuals.Cape cape = rtx.nv.api.modules.impl.Visuals.Cape.getInstance();
        if (cape != null && cape.isEnabled()) {
            String style = cape.getStyle();
            if ("Лаунчер".equalsIgnoreCase(style)) {
                return 0x7B4BFF * 31 + 0xB85EFF;
            }
            if ("Статичный".equalsIgnoreCase(style)) {
                return 0x6C72CB * 31 + 0xCB69C1;
            }
        }
        int[] nArray = ClientAccent.currentPalette();
        return (nArray[0] & 0xFFFFFF) * 31 + (nArray[nArray.length - 1] & 0xFFFFFF);
    }

    private static void buildShadeLut() {
        for (int i = 0; i < 256; ++i) {
            int n = gradientLut[i];
            int n2 = n >> 16 & 0xFF;
            int n3 = n >> 8 & 0xFF;
            int n4 = n & 0xFF;
            int n5 = i << 8;
            for (int j = 0; j < 256; ++j) {
                CapeGradient.shadeLut[n5 + j] = j * n4 / 255 << 16 | j * n3 / 255 << 8 | j * n2 / 255;
            }
        }
    }

    private static void paintPixels() {
        long l = work.imageId();
        int[] nArray = packedPixels;
        int[] nArray2 = shadeLut;
        int n = nArray.length;
        for (int i = 0; i < n; ++i) {
            int n2 = nArray[i];
            MemoryUtil.memPutInt((long)(l + ((long)i << 2)), (int)(n2 & 0xFF000000 | nArray2[n2 & 0xFFFF]));
        }
    }

    private static void recolor(boolean bl) {
        if (work == null || texture == null || basePixels == null) {
            return;
        }
        int n = CapeGradient.colorSignature();
        if (!bl && n == lastSig) {
            return;
        }
        lastSig = n;
        rtx.nv.api.modules.impl.Visuals.Cape cape = rtx.nv.api.modules.impl.Visuals.Cape.getInstance();
        int n2, n3;
        if (cape != null && cape.isEnabled() && "Лаунчер".equalsIgnoreCase(cape.getStyle())) {
            n2 = 0x7B4BFF;
            n3 = 0xB85EFF;
        } else if (cape != null && cape.isEnabled() && "Статичный".equalsIgnoreCase(cape.getStyle())) {
            n2 = 0x6C72CB;
            n3 = 0xCB69C1;
        } else {
            int[] nArray = ClientAccent.currentPalette();
            n2 = nArray[0] & 0xFFFFFF;
            n3 = nArray[nArray.length - 1] & 0xFFFFFF;
        }
        int n4 = n2 >> 16 & 0xFF;
        int n5 = n2 >> 8 & 0xFF;
        int n6 = n2 & 0xFF;
        int n7 = n3 >> 16 & 0xFF;
        int n8 = n3 >> 8 & 0xFF;
        int n9 = n3 & 0xFF;
        for (int i = 0; i < 256; ++i) {
            float f = (float)i / 255.0f;
            int n10 = Math.round((float)n4 + (float)(n7 - n4) * f);
            int n11 = Math.round((float)n5 + (float)(n8 - n5) * f);
            int n12 = Math.round((float)n6 + (float)(n9 - n6) * f);
            CapeGradient.gradientLut[i] = n10 << 16 | n11 << 8 | n12;
        }
        CapeGradient.buildShadeLut();
        try {
            CapeGradient.paintPixels();
            texture.upload();
        }
        catch (Throwable throwable) {
            failed = true;
            NV.LOGGER.error("[CapeGradient] upload failed, falling back to plain cape", throwable);
        }
    }
}

