package rtx.nv.api.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rtx.nv.api.modules.impl.Visuals.CustomPet;
import rtx.nv.api.modules.impl.Visuals.custompet.CustomPetVariant;
import rtx.nv.api.modules.impl.Visuals.custompet.entity.CustomPetEntity;
import rtx.nv.api.modules.impl.Visuals.custompet.model.CustomPetModel;
import rtx.nv.api.modules.impl.Visuals.custompet.render.CustomPetRenderer;
import rtx.nv.utils.render.render2d.Render2D;

/** Detached companion: no world registration, movement, sounds or sync traffic. */
public final class PetPreviewRenderer {
    public static final float WIDTH = WingPreviewRenderer.WIDTH;
    private static CustomPetEntity preview;

    private static boolean dragging = false;
    private static float yaw = (float) Math.PI;
    private static float pitch = 0.10f;
    private static float zoom = 1.0f;
    private static long lastClickTime = 0L;
    private static float lastClickX = 0.0f;
    private static float lastClickY = 0.0f;

    private PetPreviewRenderer() {}

    public static void reset() {
        preview = null;
    }

    public static void resetPose() {
        yaw = (float) Math.PI;
        pitch = 0.10f;
        zoom = 1.0f;
        dragging = false;
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

    public static void render(DrawContext context, float targetX, float y, float height, float alpha, CustomPet module) {
        var client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || alpha < 0.01f) return;
        CustomPetVariant variant = module.getSelectedVariant();
        if (preview == null || preview.getEntityWorld() != client.world || preview.getPetVariant() != variant) {
            preview = new CustomPetEntity(client.world);
            preview.setPetVariant(variant);
            preview.setSilent(true);
            preview.snapTo(Vec3d.ZERO, 180);
        }
        preview.age = client.player.age;

        float progress = Math.clamp(alpha, 0.0f, 1.0f);
        // Smooth slide animation: slides out to targetX when opening, slides back behind main panel when closing
        float slide = 1.0f - (float) Math.pow(1.0f - progress, 3.0);
        float baseX = targetX + WIDTH + 8.0f;
        float x = baseX - (WIDTH + 8.0f) * slide;

        WingPreviewRenderer.drawPanel(context, x, y, height, progress, "Предпросмотр " + variant.getSettingValue(), "ЛКМ вращение, колесико зум, 2x клик сброс");

        // Pet smoothly scales down and disappears before the panel closes, preventing ghost entity hovering
        float petT = Math.clamp((progress - 0.25f) / 0.75f, 0.0f, 1.0f);
        float petScaleMult = petT * petT * (3.0f - 2.0f * petT);
        if (petScaleMult <= 0.005f) {
            return;
        }

        var renderer = (CustomPetRenderer)(Object)client.getEntityRenderDispatcher().getRenderer(preview);
        var state = renderer.createRenderState();
        renderer.updateRenderState(preview, state, client.getRenderTickCounter().getTickProgress(false));
        state.displayName = null;
        state.invisible = false;
        state.invisibleToPlayer = false;
        state.bodyYaw = 0;
        state.relativeHeadYaw = 0;
        state.pitch = 0;
        state.light = 0x00F000F0;
        state.addGeckolibData(rtx.nv.api.mods.geckolib.constant.DataTickets.ENTITY_BODY_YAW, state.bodyYaw);
        state.addGeckolibData(rtx.nv.api.mods.geckolib.constant.DataTickets.ENTITY_PITCH, 0.0f);
        state.addGeckolibData(CustomPetModel.AIRBORNE, true);
        state.addGeckolibData(CustomPetModel.VARIANT, variant.name());

        var pose = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateY(yaw)
                .rotateX(pitch);

        float baseScale = switch (variant) {
            case MASCOT -> 92.0f;
            case SPIRIT -> 88.0f;
            case MOTH -> 72.0f;
            case DRAGON -> 66.0f;
            default -> 72.0f;
        };
        float yOffset = switch (variant) {
            case DRAGON -> 0.45f;
            case MOTH -> 0.50f;
            case SPIRIT -> 0.52f;
            case MASCOT -> 0.55f;
            default -> 0.55f;
        };

        var bounds = GuiEntityBounds.from(context, x + 4, y + 32, WIDTH - 8, height - 56);
        context.addEntity(state, baseScale * bounds.scale() * petScaleMult * zoom, new Vector3f(0.0f, yOffset, 0.0f), pose, null,
                bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
        Render2D.beginFrame(context);
    }
}
