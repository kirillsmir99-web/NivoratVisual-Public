package rtx.nv.api.music.subtitles;

import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.EndMain;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import rtx.nv.api.music.MusicManager;
import rtx.nv.api.music.NowPlayingClient;
import rtx.nv.api.music.TrackState;
import rtx.nv.api.music.lyrics.LyricLine;
import rtx.nv.api.music.lyrics.LyricsManager;
import rtx.nv.api.music.lyrics.MusicSubtitlesConfig;
import rtx.nv.api.music.lyrics.SubtitleBubble;
import rtx.nv.api.ui.theme.ThemeManager;

public class MusicSubtitlesRenderer {
    private static MusicSubtitlesRenderer INSTANCE;
    private static boolean registered = false;

    public static MusicSubtitlesRenderer get() {
        if (INSTANCE == null) {
            INSTANCE = new MusicSubtitlesRenderer(LyricsManager.get(), MusicManager.get().getClient());
        }
        return INSTANCE;
    }

    public static void init() {
        get().register();
    }

    private final LyricsManager lyricsManager;
    private final NowPlayingClient client;

    public MusicSubtitlesRenderer(LyricsManager lyricsManager, NowPlayingClient client) {
        this.lyricsManager = lyricsManager;
        this.client = client;
        INSTANCE = this;
    }

    public synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        HudRenderCallback.EVENT.register(this::render2D);
        WorldRenderEvents.END_MAIN.register((EndMain) this::render3D);
    }

    public void render3D(WorldRenderContext var1) {
        MinecraftClient var3 = MinecraftClient.getInstance();
        if (var3.player != null && var3.gameRenderer != null) {
            if (!var3.options.hudHidden || MusicSubtitlesConfig.INSTANCE.subtitlesVisibleInF1) {
                TrackState var4 = this.client.getState();
                if (MusicSubtitlesConfig.INSTANCE.subtitlesEnabled
                    && (
                        "WORLD_3D".equalsIgnoreCase(MusicSubtitlesConfig.INSTANCE.subtitlesMode)
                            || "WORLD_BLOCK".equalsIgnoreCase(MusicSubtitlesConfig.INSTANCE.subtitlesMode)
                            || "WORLD_SURFACE".equalsIgnoreCase(MusicSubtitlesConfig.INSTANCE.subtitlesMode)
                    )) {
                    boolean var5 = false;

                    for (SubtitleBubble var7 : this.lyricsManager.getActiveBubbles()) {
                        if (var7.isLiveCaption) {
                            var5 = true;
                            break;
                        }
                    }

                    boolean hasActive = !this.lyricsManager.getActiveBubbles().isEmpty();
                    if ((var4.isActive() && (var4.isPlaying() || var4.isPaused())) || var5 || hasActive) {
                        int var25 = var4.isActive() ? MusicSubtitlesConfig.INSTANCE.getTrackSyncOffset(var4.artist(), var4.title()) : 0;
                        long basePos = var4.isActive()
                            ? Math.max(0L, var4.getInterpolatedPositionMs() + MusicSubtitlesConfig.INSTANCE.subtitlesOffsetMs + var25 + LyricsManager.PERCEPTUAL_LEAD_MS)
                            : 0L;
                        long var9 = System.currentTimeMillis();
                        MatrixStack var11 = var1.matrices();
                        Object var2 = var1.consumers() != null ? var1.consumers() : var3.getBufferBuilders().getEntityVertexConsumers();
                        if (var11 != null && var2 != null) {
                            Camera var13 = var3.gameRenderer.getCamera();
                            Vec3d var14 = var13.getCameraPos();
                            TextRenderer var15 = var3.textRenderer;

                            for (SubtitleBubble var17 : this.lyricsManager.getActiveBubbles()) {
                                if (var17.worldPos != null) {
                                    long var26 = var4.isActive() ? basePos : (var17.line != null ? var17.line.startMs : 0L);
                                    float var18 = var17.getOverallAlpha(var26, var9);
                                    if (!(var18 <= 0.02F)) {
                                        float var19 = (var17.isSurfaceAttached ? 0.020F : 0.025F) * MusicSubtitlesConfig.INSTANCE.subtitlesScale * var17.worldScale;
                                        float var20 = var17.getScale(var9);
                                        if (var17.isSurfaceAttached && var17.attachedBlockPos != null && var3.world != null) {
                                            if (var3.world.isAir(var17.attachedBlockPos)) {
                                                var17.retire(var9);
                                                continue;
                                            }
                                        }

                                        Vec3d renderPos = var17.worldPos;
                                        if (!var17.isSurfaceAttached && "WORLD_3D".equalsIgnoreCase(MusicSubtitlesConfig.INSTANCE.subtitlesMode)) {
                                            Vec3d toward = var14.subtract(renderPos);
                                            double dist = toward.length();
                                            if (dist > 0.001) {
                                                float yaw = (float) Math.atan2(toward.x, toward.z);
                                                float tilt = (float) -Math.atan2(toward.y, toward.horizontalLength());
                                                var17.worldRot = new org.joml.Quaternionf().rotationY(yaw).rotateX(tilt);
                                            }

                                            if (var17.isLongLine) {
                                                double maxDist = Math.max(6.0, MusicSubtitlesConfig.INSTANCE.subtitlesDistance * 1.5);
                                                if (dist > maxDist && !var17.isRepositioning()) {
                                                    Vec3d vel = var3.player.getVelocity();
                                                    float moveHeading = vel.horizontalLengthSquared() > 0.0025
                                                        ? (float) Math.toDegrees(Math.atan2(-vel.x, vel.z))
                                                        : var3.player.getYaw();
                                                    float forwardDist = Math.max(3.0F, Math.min(8.0F, MusicSubtitlesConfig.INSTANCE.subtitlesDistance));
                                                    Vec3d forwardDir = Vec3d.fromPolar(-12.0F, moveHeading);
                                                    Vec3d newTarget = var14.add(forwardDir.multiply(forwardDist));
                                                    if (var3.world != null) {
                                                        newTarget = new Vec3d(newTarget.x, Math.max(var3.player.getY() + 0.45, newTarget.y), newTarget.z);
                                                    }
                                                    var17.startReposition(newTarget);
                                                }
                                            }
                                        }
                                        var11.push();
                                        var11.translate(
                                            (float) (renderPos.x - var14.x),
                                            (float) (renderPos.y - var14.y) + var17.getExitOffsetY(var26) * 0.02F,
                                            (float) (renderPos.z - var14.z)
                                        );
                                        if (var17.worldRot != null) {
                                            var11.multiply(var17.worldRot);
                                        }

                                        var11.scale(var19, -var19, var19);
                                        var11.scale(var20, var20, var20);
                                        double var21 = renderPos.squaredDistanceTo(var14);
                                        float var23 = var21 < 0.64 ? (float) Math.sqrt(var21) / 0.8F : 1.0F;
                                        float var24 = var18 * Math.max(0.05F, var23);
                                        this.render3DSubtitleDoubleSided(var11, (VertexConsumerProvider) var2, var15, var17, var26, var24);
                                        var11.pop();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void render3DSubtitleDoubleSided(MatrixStack var1, VertexConsumerProvider var2, TextRenderer var3, SubtitleBubble var4, long var5, float var7) {
        this.draw3DFace(var1, var2, var3, var4, var5, var7);
    }

    private void draw3DFace(MatrixStack var1, VertexConsumerProvider var2, TextRenderer var3, SubtitleBubble var4, long var5, float var7) {
        LyricLine var8 = var4.line;
        String var9 = var8.text;
        short var10 = 260;
        List<LyricLine.SubLine> var11 = var8.getSubLines(var3, var10);
        byte var12 = 9;
        int var13 = var12 + 3;
        int var14 = (var11.size() - 1) * var13 + var12;
        int var15 = 0;

        for (LyricLine.SubLine var17 : var11) {
            if (var17.width() > var15) {
                var15 = var17.width();
            }
        }

        float var53 = 5.0F;
        float var54 = 2.5F;
        float var18 = -(var14 / 2.0F);
        float var19 = -var15 / 2.0F - var53;
        float var20 = var18 - var54;
        float var21 = var15 + var53 * 2.0F;
        float var22 = var14 + var54 * 2.0F;

        // Background quad placed at z = 0.001F, text placed in front at z = 0.015F
        if (MusicSubtitlesConfig.INSTANCE.subtitlesBgEnabled) {
            int var23 = (int) (MusicSubtitlesConfig.INSTANCE.subtitlesBgAlpha * var7 * 230.0F);
            int var24 = MusicSubtitlesConfig.INSTANCE.subtitlesBgColor;
            int var25 = var24 >> 16 & 0xFF;
            int var26 = var24 >> 8 & 0xFF;
            int var27 = var24 & 0xFF;
            drawQuad(var1.peek().getPositionMatrix(), var2, var19, var20, 0.001F, var21, var22, var25, var26, var27, var23, true);
            if (var2 instanceof VertexConsumerProvider.Immediate var28) {
                var28.draw();
            }
        }

        var1.push();
        var1.translate(0.0F, 0.0F, 0.015F);
        Matrix4f var55 = var1.peek().getPositionMatrix();
        int var56 = 15728880;
        TextRenderer.TextLayerType var57 = MusicSubtitlesConfig.INSTANCE.subtitlesSeeThrough
            ? TextRenderer.TextLayerType.SEE_THROUGH
            : TextRenderer.TextLayerType.NORMAL;
        int var58 = MusicSubtitlesConfig.INSTANCE.subtitlesColor & 16777215;
        boolean var59 = MusicSubtitlesConfig.INSTANCE.subtitlesTextShadow;
        String var60 = MusicSubtitlesConfig.INSTANCE.subtitlesAnimation;
        int dynamicAccent = ThemeManager.accent(255.0F);
        int var29 = MusicSubtitlesConfig.INSTANCE.resolveActiveColor(dynamicAccent);
        long var30 = System.currentTimeMillis();
        long var32 = var4.isLiveCaption ? var30 - var4.spawnSystemTimeMs : var5;

        for (int var34 = 0; var34 < var11.size(); var34++) {
            LyricLine.SubLine var35 = var11.get(var34);
            float var36 = var18 + (float) var34 * var13;
            int var37 = -var35.width() / 2;

            for (int var38 = var35.charStart(); var38 < var35.charEnd(); var38 += Character.charCount(var9.codePointAt(var38))) {
                char var39 = var9.charAt(var38);
                String var40 = new String(Character.toChars(var9.codePointAt(var38)));
                int var41 = var3.getWidth(var40);
                float var42 = var8.getCharAlpha(var38, var32);
                float var43 = var42 * var7;
                if (var43 >= 0.04F) {
                    float var44 = var8.getCharAnimationOffsetY(var38, var32, var30, var60) + var4.getLetterEntryOffset(var38, var30);
                    int var45 = var8.getCharColor(var38, var32, var30, var60, var58, var43, var29);
                    float var46 = var36 + var44;

                    if (!var4.isSurfaceAttached) {
                        // In 3D space: launcher accent glow & deep drop shadow
                        if (var59) {
                            int shadowA = (int) (var43 * 180.0F);
                            if (shadowA > 2) {
                                int shadowCol = (shadowA << 24) | 0x080B14;
                                var3.draw(var40, var37 + 0.85F, var46 + 0.85F, shadowCol, false, var55, var2, var57, 0, var56);
                            }
                        }
                    } else if (var59) {
                        // On block surface: classic double shadow
                        int var47 = (int) (var43 * 140.0F);
                        if (var47 > 3) {
                            int var48 = var47 << 24 | 329226;
                            var3.draw(var40, var37 + 0.8F, var46 + 1.0F, var48, false, var55, var2, var57, 0, var56);
                        }

                        int var62 = (var45 >> 16 & 0xFF) * 35 / 100;
                        int var49 = (var45 >> 8 & 0xFF) * 35 / 100;
                        int var50 = (var45 & 0xFF) * 35 / 100;
                        int var51 = (int) (var43 * 180.0F);
                        if (var51 > 3) {
                            int var52 = var51 << 24 | var62 << 16 | var49 << 8 | var50;
                            var3.draw(var40, var37 + 0.2F, var46 + 0.7F, var52, false, var55, var2, var57, 0, var56);
                        }
                    }

                    var3.draw(var40, var37, var46, var45, false, var55, var2, var57, 0, var56);
                }

                var37 += var41;
            }
        }

        var1.pop();
        if (var2 instanceof VertexConsumerProvider.Immediate var61) {
            var61.draw();
        }
    }

    private static void drawQuad(
        Matrix4f var0, VertexConsumerProvider var1, float var2, float var3, float var4, float var5, float var6, int var7, int var8, int var9, int var10, boolean var11
    ) {
        RenderLayer var12 = MusicSubtitlesConfig.INSTANCE.subtitlesSeeThrough ? RenderLayers.textBackgroundSeeThrough() : RenderLayers.textBackground();
        VertexConsumer var13 = var1.getBuffer(var12);
        var13.vertex(var0, var2, var3, var4).color(var7, var8, var9, var10).light(15728880);
        var13.vertex(var0, var2 + var5, var3, var4).color(var7, var8, var9, var10).light(15728880);
        var13.vertex(var0, var2 + var5, var3 + var6, var4).color(var7, var8, var9, var10).light(15728880);
        var13.vertex(var0, var2, var3 + var6, var4).color(var7, var8, var9, var10).light(15728880);
    }

    public void render2D(DrawContext var1, RenderTickCounter var2) {
        MinecraftClient var3 = MinecraftClient.getInstance();
        if (var3.player != null) {
            if (!var3.options.hudHidden || MusicSubtitlesConfig.INSTANCE.subtitlesVisibleInF1) {
                TrackState var4 = this.client.getState();
                int var5 = var3.getWindow().getScaledWidth();
                if (MusicSubtitlesConfig.INSTANCE.subtitlesEnabled
                    && !"WORLD_3D".equalsIgnoreCase(MusicSubtitlesConfig.INSTANCE.subtitlesMode)
                    && !"WORLD_BLOCK".equalsIgnoreCase(MusicSubtitlesConfig.INSTANCE.subtitlesMode)
                    && !"WORLD_SURFACE".equalsIgnoreCase(MusicSubtitlesConfig.INSTANCE.subtitlesMode)) {
                    boolean var6 = false;

                    for (SubtitleBubble var8 : this.lyricsManager.getActiveBubbles()) {
                        if (var8.isLiveCaption) {
                            var6 = true;
                            break;
                        }
                    }

                    if ((var4.isActive() && (var4.isPlaying() || var4.isPaused())) || var6) {
                        int var19 = var4.isActive() ? MusicSubtitlesConfig.INSTANCE.getTrackSyncOffset(var4.artist(), var4.title()) : 0;
                        long var20 = var4.isActive() ? Math.max(0L, var4.getInterpolatedPositionMs() + MusicSubtitlesConfig.INSTANCE.subtitlesOffsetMs + var19 + LyricsManager.PERCEPTUAL_LEAD_MS) : 0L;
                        long var10 = System.currentTimeMillis();
                        TextRenderer var12 = var3.textRenderer;

                        for (SubtitleBubble var14 : this.lyricsManager.getActiveBubbles()) {
                            if (var14.worldPos == null) {
                                float var15 = var14.getOverallAlpha(var20, var10);
                                if (!(var15 <= 0.02F)) {
                                    float var16 = Math.max(0.2F, var14.getScale(var10) * MusicSubtitlesConfig.INSTANCE.subtitlesScale);
                                    float var17 = var14.getOffsetX(var10);
                                    float var18 = var14.getOffsetY(var10);
                                    var1.getMatrices().pushMatrix();
                                    var anchor = rtx.nv.api.drags.components.MusicSubtitlesComp.get();
                                    float dx = 0, dy = 0;
                                    if (anchor != null) {
                                        float sx = var3.getWindow().getScaledWidth() / rtx.nv.api.drags.Position.screenWidth();
                                        float sy = var3.getWindow().getScaledHeight() / rtx.nv.api.drags.Position.screenHeight();
                                        dx = anchor.centerX() * sx - (float)(var3.getWindow().getScaledWidth() * MusicSubtitlesConfig.INSTANCE.sub2DPositionX);
                                        dy = anchor.centerY() * sy - (float)(var3.getWindow().getScaledHeight() * MusicSubtitlesConfig.INSTANCE.sub2DPositionY);
                                    }
                                    var1.getMatrices().translate(var14.screenX + dx + var17, var14.screenY + dy + var18 + var14.getExitOffsetY(var20));
                                    var1.getMatrices().scale(var16, var16);
                                    this.render2DBubble(var1, var12, var14, var20, var15, var5, var10);
                                    var1.getMatrices().popMatrix();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void render2DBubble(DrawContext var1, TextRenderer var2, SubtitleBubble var3, long var4, float var6, int var7, long var8) {
        LyricLine var10 = var3.line;
        String var11 = var10.text;
        int var12 = (int) (var7 * 0.65F);
        List<LyricLine.SubLine> var13 = var10.getSubLines(var2, var12);
        byte var14 = 9;
        int var15 = var14 + 3;
        int var16 = (var13.size() - 1) * var15 + var14;
        int var17 = 0;

        for (LyricLine.SubLine var19 : var13) {
            if (var19.width() > var17) {
                var17 = var19.width();
            }
        }

        byte var45 = 6;
        byte var46 = 3;
        if (MusicSubtitlesConfig.INSTANCE.subtitlesBgEnabled) {
            int var20 = (int) (MusicSubtitlesConfig.INSTANCE.subtitlesBgAlpha * var6 * 255.0F);
            int var21 = var20 << 24 | MusicSubtitlesConfig.INSTANCE.subtitlesBgColor & 16777215;
            int var22 = Math.max(0, Math.min(MusicSubtitlesConfig.INSTANCE.subtitlesBgRadius, 15));
            renderRoundedRect(var1, -var45, -var46, var17 + var45 * 2, var16 + var46 * 2, var22, var21);
        }

        int var47 = MusicSubtitlesConfig.INSTANCE.subtitlesColor & 16777215;
        boolean var48 = MusicSubtitlesConfig.INSTANCE.subtitlesTextShadow;
        String var49 = MusicSubtitlesConfig.INSTANCE.subtitlesAnimation;
        int dynamicAccent2D = ThemeManager.accent(255.0F);
        int var23 = MusicSubtitlesConfig.INSTANCE.resolveActiveColor(dynamicAccent2D);
        long var24 = var3.isLiveCaption ? var8 - var3.spawnSystemTimeMs : var4;

        for (int var26 = 0; var26 < var13.size(); var26++) {
            LyricLine.SubLine var27 = var13.get(var26);
            int var28 = var26 * var15;
            int var29 = 0;

            for (int var30 = var27.charStart(); var30 < var27.charEnd(); var30 += Character.charCount(var11.codePointAt(var30))) {
                char var31 = var11.charAt(var30);
                String var32 = new String(Character.toChars(var11.codePointAt(var30)));
                int var33 = var2.getWidth(var32);
                float var34 = var10.getCharAlpha(var30, var24);
                float var35 = var34 * var6;
                if (var35 >= 0.05F) {
                    float var36 = var10.getCharAnimationOffsetY(var30, var24, var8, var49) + var3.getLetterEntryOffset(var30, var8);
                    int var37 = var10.getCharColor(var30, var24, var8, var49, var47, var35, var23);
                    int var38 = (int) (var28 + var36);
                    if (var48) {
                        int var39 = (int) (var35 * 145.0F);
                        if (var39 > 3) {
                            int var40 = var39 << 24 | 329226;
                            var1.drawText(var2, var32, var29 + 1, var38 + 1, var40, false);
                        }

                        int var50 = (var37 >> 16 & 0xFF) * 35 / 100;
                        int var41 = (var37 >> 8 & 0xFF) * 35 / 100;
                        int var42 = (var37 & 0xFF) * 35 / 100;
                        int var43 = (int) (var35 * 180.0F);
                        if (var43 > 3) {
                            int var44 = var43 << 24 | var50 << 16 | var41 << 8 | var42;
                            var1.drawText(var2, var32, var29, var38 + 1, var44, false);
                        }
                    }

                    var1.drawText(var2, var32, var29, var38, var37, false);
                }

                var29 += var33;
            }
        }
    }

    public static void renderRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        int alpha = color >> 24 & 0xFF;
        if (alpha != 0) {
            if (radius <= 0) {
                context.fill(x, y, x + width, y + height, color);
            } else {
                radius = Math.min(radius, Math.min(width / 2, height / 2));
                context.fill(x, y + radius, x + width, y + height - radius, color);
                for (int r = 0; r < radius; r++) {
                    double dy = (double) radius - r - 0.5;
                    int inset = (int) Math.round(radius - Math.sqrt(Math.max(0.0, radius * radius - dy * dy)));
                    context.fill(x + inset, y + r, x + width - inset, y + r + 1, color);
                    context.fill(x + inset, y + height - 1 - r, x + width - inset, y + height - r, color);
                }
            }
        }
    }
}
