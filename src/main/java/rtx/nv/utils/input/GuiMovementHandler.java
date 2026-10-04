package rtx.nv.utils.input;
import rtx.nv.api.events.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.game.TickEvent;

public final class GuiMovementHandler {
    private static final GuiMovementHandler INSTANCE = new GuiMovementHandler();
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean handledScreen;

    private GuiMovementHandler() {
    }

    public static void init() {
        EventBus.get().subscribe(INSTANCE);
    }

    private boolean isPressed(KeyBinding keyBinding) {
        if (keyBinding == null || keyBinding.isUnbound()) {
            return false;
        }
        InputUtil.Key key = InputUtil.fromTranslationKey((String)keyBinding.getBoundKeyTranslationKey());
        long l = this.mc.getWindow().getHandle();
        return switch (key.getCategory()) {
            case InputUtil.Type.KEYSYM -> InputUtil.isKeyPressed((Window)(Object)this.mc.getWindow(), (int)key.getCode());
            case InputUtil.Type.MOUSE -> {
                if (GLFW.glfwGetMouseButton((long)l, (int)key.getCode()) == 1) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    private static void setDown(KeyBinding keyBinding, boolean bl) {
        if (keyBinding != null) {
            keyBinding.setPressed(bl);
        }
    }

    @EventHandler
    private void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPre()) {
            return;
        }
        if (!this.canHandleScreen()) {
            this.finishHandledScreen();
            return;
        }
        this.handledScreen = true;
        this.syncMovementKeys();
    }

    private void syncMovementKeys() {
        GuiMovementHandler.setDown(this.mc.options.forwardKey, this.isPressed(this.mc.options.forwardKey));
        GuiMovementHandler.setDown(this.mc.options.backKey, this.isPressed(this.mc.options.backKey));
        GuiMovementHandler.setDown(this.mc.options.leftKey, this.isPressed(this.mc.options.leftKey));
        GuiMovementHandler.setDown(this.mc.options.rightKey, this.isPressed(this.mc.options.rightKey));
        GuiMovementHandler.setDown(this.mc.options.jumpKey, this.isPressed(this.mc.options.jumpKey));
        GuiMovementHandler.setDown(this.mc.options.sprintKey, this.isPressed(this.mc.options.sprintKey));
        GuiMovementHandler.setDown(this.mc.options.sneakKey, this.isPressed(this.mc.options.sneakKey));
    }

    private void releaseMovementKeys() {
        if (this.mc.options == null) {
            return;
        }
        GuiMovementHandler.setDown(this.mc.options.forwardKey, false);
        GuiMovementHandler.setDown(this.mc.options.backKey, false);
        GuiMovementHandler.setDown(this.mc.options.leftKey, false);
        GuiMovementHandler.setDown(this.mc.options.rightKey, false);
        GuiMovementHandler.setDown(this.mc.options.jumpKey, false);
        GuiMovementHandler.setDown(this.mc.options.sprintKey, false);
        GuiMovementHandler.setDown(this.mc.options.sneakKey, false);
    }

    private boolean canHandleScreen() {
        return this.mc.player != null && this.mc.world != null && this.mc.options != null && this.mc.getWindow() != null && this.mc.currentScreen != null && !(this.mc.currentScreen instanceof ChatScreen);
    }

    private void finishHandledScreen() {
        if (!this.handledScreen) {
            this.handledScreen = false;
            return;
        }
        if (this.canSyncInput()) {
            this.syncMovementKeys();
        } else {
            this.releaseMovementKeys();
        }
        this.handledScreen = false;
    }

    private boolean canSyncInput() {
        return this.mc.player != null && this.mc.world != null && this.mc.options != null && this.mc.getWindow() != null && this.mc.currentScreen == null;
    }
}

