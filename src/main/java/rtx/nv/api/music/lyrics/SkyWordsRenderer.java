package rtx.nv.api.music.lyrics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import rtx.nv.api.drags.Position;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.render.HudRenderEvent;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.MusicHudModule;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;

public final class SkyWordsRenderer {
    private static final SkyWordsRenderer INSTANCE = new SkyWordsRenderer();

    private final List<ProjectedWord> projected = new CopyOnWriteArrayList<>();

    private record ProjectedWord(String text, float x, float y, float size, float alpha) {}

    public static SkyWordsRenderer get() {
        return INSTANCE;
    }

    public void init() {
        EventBus.get().subscribe(this);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        this.projected.clear();

        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
        if (mod == null || !mod.isEnabled() || !mod.skyWords.getValue()) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null || event.getCamera() == null) {
            return;
        }

        List<SkyWordBubble> bubbles = SkyWordsManager.get().getActiveBubbles();
        if (bubbles.isEmpty()) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3d camPos = camera.getCameraPos();
        long now = System.currentTimeMillis();

        Matrix4f projMatrix = event.getProjectionMatrix();
        Matrix4f viewMatrix = event.getStack().peek().getPositionMatrix();
        float screenW = Position.screenWidth();
        float screenH = Position.screenHeight();

        for (SkyWordBubble bubble : bubbles) {
            float alpha = bubble.getAlpha(now);
            if (alpha <= 0.02f) {
                continue;
            }

            Vec3d targetPos = bubble.getWorldPos();
            Vector4f vec = new Vector4f((float)(targetPos.x - camPos.x), (float)(targetPos.y - camPos.y), (float)(targetPos.z - camPos.z), 1.0f);
            viewMatrix.transform(vec);
            projMatrix.transform(vec);

            if (vec.w <= 0.001f || vec.z <= 0.0f) {
                continue;
            }

            float ndcX = vec.x / vec.w;
            float ndcY = vec.y / vec.w;
            if (ndcX < -1.5f || ndcX > 1.5f || ndcY < -1.5f || ndcY > 1.5f) {
                continue;
            }

            float sx = (ndcX * 0.5f + 0.5f) * screenW;
            float sy = (1.0f - (ndcY * 0.5f + 0.5f)) * screenH;

            double dist = camPos.distanceTo(targetPos);
            float fontSize = (float) MathHelper.clamp(28.0 / (dist * 0.75 + 1.0), 5.5, 11.0);

            this.projected.add(new ProjectedWord(bubble.getText(), sx, sy, fontSize, alpha));
        }
    }

    @EventHandler
    public void onHudRender(HudRenderEvent event) {
        if (this.projected.isEmpty()) {
            return;
        }

        DrawContext drawContext = event.getGraphics();
        if (drawContext == null) {
            return;
        }

        Render2D.beginFrame(drawContext);

        for (ProjectedWord pw : this.projected) {
            float textW = Fonts.MONTSERRAT_BOLD.width(pw.text, pw.size);
            float padX = 7.0f;
            float padY = 3.5f;
            float cardW = textW + padX * 2.0f;
            float cardH = pw.size + padY * 2.0f + 1.0f;
            float cardX = pw.x - cardW * 0.5f;
            float cardY = pw.y - cardH * 0.5f;
            float rad = cardH * 0.5f;

            // Soft atmospheric glow behind words in the sky
            int glowCol = ClientAccent.accent(255);
            Render2D.glow(new BuiltGlow(
                cardX - 2.0f, cardY - 2.0f, cardW + 4.0f, cardH + 4.0f,
                new float[]{rad, rad, rad, rad},
                glowCol, 0.75f, 8.0f, 0.32f * pw.alpha
            ));

            // Ultra-subtle dark glass scrim for crisp contrast against bright sky/clouds
            int bgCol = ColorUtil.rgba(10, 12, 16, (int) (185 * pw.alpha));
            Render2D.rect(cardX, cardY, cardW, cardH, rad, bgCol);
            int borderCol = ColorUtil.multAlpha(glowCol, 0.45f * pw.alpha);
            Render2D.outline(cardX, cardY, cardW, cardH, rad, 1.0f, borderCol);

            // Deep text shadow
            Fonts.MONTSERRAT_BOLD.msdf(pw.text, cardX + padX + 0.8f, cardY + padY + 0.8f, pw.size, ColorUtil.rgba(0, 0, 0, (int) (180 * pw.alpha)));
            // Radiant pure white words
            Fonts.MONTSERRAT_BOLD.msdf(pw.text, cardX + padX, cardY + padY, pw.size, ColorUtil.rgba(255, 255, 255, (int) (255 * pw.alpha)));
        }

        Render2D.flush();
    }
}
