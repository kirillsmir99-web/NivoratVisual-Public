package rtx.nv.api.ui;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rtx.nv.api.ui.settings.RenderHelper;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.render2d.Render2D;

/** A fresh render state keeps the preview separate from the world player pose. */
public final class WingPreviewRenderer {
    public static final float WIDTH = 190.0f;
    private static final Map<EntityRenderState, Boolean> PREVIEWS = Collections.synchronizedMap(new WeakHashMap<>());

    private static boolean dragging = false;
    private static float yaw = (float) Math.PI;
    private static float pitch = 0.08f;
    private static float zoom = 1.0f;
    private static long lastClickTime = 0L;
    private static float lastClickX = 0.0f;
    private static float lastClickY = 0.0f;

    private WingPreviewRenderer() {}

    public static boolean isPreview(EntityRenderState state) {
        return PREVIEWS.containsKey(state);
    }

    public static boolean isDragging() {
        return dragging;
    }

    public static void resetPose() {
        yaw = (float) Math.PI;
        pitch = 0.08f;
        zoom = 1.0f;
    }

    public static boolean mouseClicked(float mouseX, float mouseY, int button, boolean doubleClick, float x, float y, float height) {
        if (button != 0) return false;
        if (mouseX < x || mouseX > x + WIDTH || mouseY < y || mouseY > y + height) {
            return false;
        }

        long now = System.currentTimeMillis();
        boolean isDouble = doubleClick || (now - lastClickTime < 350L && Math.abs(mouseX - lastClickX) < 10.0f && Math.abs(mouseY - lastClickY) < 10.0f);
        lastClickTime = now;
        lastClickX = mouseX;
        lastClickY = mouseY;

        if (isDouble) {
            resetPose();
            dragging = false;
            return true;
        }

        dragging = true;
        return true;
    }

    public static boolean mouseScrolled(double mouseX, double mouseY, double amount, float x, float y, float height) {
        if (mouseX < x || mouseX > x + WIDTH || mouseY < y || mouseY > y + height) {
            return false;
        }
        zoom = Math.clamp(zoom + (float) amount * 0.12f, 0.5f, 2.5f);
        return true;
    }

    public static boolean mouseDragged(double deltaX, double deltaY, int button) {
        if (dragging && button == 0) {
            yaw += (float) deltaX * 0.018f;
            pitch = Math.clamp(pitch + (float) deltaY * 0.018f, -0.65f, 0.65f);
            return true;
        }
        return false;
    }

    public static boolean mouseReleased(int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void render(DrawContext context, float targetX, float y, float height, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || alpha < .01f) return;

        float progress = Math.clamp(alpha, 0.0f, 1.0f);
        float slide = 1.0f - (float) Math.pow(1.0f - progress, 3.0);
        float baseX = targetX + WIDTH + 8.0f;
        float x = baseX - (WIDTH + 8.0f) * slide;

        drawPanel(context, x, y, height, progress, "Предпросмотр крыльев", "ЛКМ вращение, колесико зум, 2x клик сброс");

        float entityT = Math.clamp((progress - 0.25f) / 0.75f, 0.0f, 1.0f);
        float entityScaleMult = entityT * entityT * (3.0f - 2.0f * entityT);
        if (entityScaleMult <= 0.005f) {
            return;
        }

        renderPlayer(context, client, x, y, height, entityScaleMult);
    }

    static void drawPanel(DrawContext context, float x, float y, float height, float alpha, String title, String subtitle) {
        Render2D.beginFrame(context);
        rtx.nv.utils.render.others.RectUtil.drawClientRect(x, y, WIDTH, height, 12, alpha);
        RenderHelper.drawPanelBg(x, y, WIDTH, height, 12, 0, 0, 12, alpha);
        Fonts.MONTSERRAT_MEDIUM.draw(title, x + 10, y + 12, 8, ((int)(255 * alpha) << 24) | 0xFFFFFF);
        Fonts.MONTSERRAT_MEDIUM.draw(subtitle, x + 10, y + height - 16, 6, ((int)(160 * alpha) << 24) | 0xFFFFFF);
        Render2D.flush();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void renderPlayer(DrawContext context, MinecraftClient client, float x, float y, float height, float entityScaleMult) {
        EntityRenderer renderer = client.getEntityRenderDispatcher().getRenderer(client.player);
        EntityRenderState state = renderer.createRenderState();
        renderer.updateRenderState(client.player, state, client.getRenderTickCounter().getTickProgress(false));
        state.displayName = null;
        state.invisible = false;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyYaw = 0;
            living.relativeHeadYaw = 0;
            living.pitch = 0;
            living.hurt = false;
        }
        if (state instanceof PlayerEntityRenderState player) {
            player.isGliding = false;
            player.equippedChestStack = net.minecraft.item.ItemStack.EMPTY;
        }
        PREVIEWS.put(state, true);

        // Player model occupying ~80-85% of available preview height
        float availHeight = Math.max(50.0f, height - 56.0f);
        var bounds = GuiEntityBounds.from(context, x + 4, y + 32, WIDTH - 8, availHeight);
        float effectiveHeight = 2.15f; // Player 1.8m + wing clearance
        float dynamicScale = (availHeight * 0.82f / effectiveHeight) * bounds.scale() * entityScaleMult * zoom;

        Quaternionf pose = new Quaternionf()
            .rotateZ((float) Math.PI)
            .rotateY(yaw)
            .rotateX(pitch);

        context.addEntity(state, dynamicScale, new Vector3f(0.0f, state.height * 0.48f, 0.0f), pose, null,
            bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
        Render2D.beginFrame(context);
    }
}
