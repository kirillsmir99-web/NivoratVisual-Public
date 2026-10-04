package rtx.nv.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.util.PlayerInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.input.InputEvent;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionWheelScreen;
import rtx.nv.api.ui.UI;
import rtx.nv.api.ui.crosshair.CrosshairEditorScreen;

@Mixin(net.minecraft.client.input.KeyboardInput.class)

public abstract class KeyboardInputMixin {
    @ModifyExpressionValue(method="tick", at={@At(value="NEW", target="(ZZZZZZZ)Lnet/minecraft/class_10185;")}, require = 0)
    private PlayerInput nv_tickHook(PlayerInput original) {
        EventBus bus;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.currentScreen != null) {
            original = UI.isOpen() && !UI.isInputActive() || mc.currentScreen instanceof CrosshairEditorScreen || mc.currentScreen instanceof EmotionWheelScreen ? this.nv_readRawMovement() : new PlayerInput(false, false, false, false, false, false, false);
        }
        if (!(bus = EventBus.get()).hasListeners(InputEvent.class)) {
            return original;
        }
        InputEvent event = bus.post(new InputEvent(original));
        return event.getInput();
    }

    @Unique
    private PlayerInput nv_readRawMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return new PlayerInput(false, false, false, false, false, false, false);
        }
        return new PlayerInput(this.nv_isRawKeyDown(mc, mc.options.forwardKey), this.nv_isRawKeyDown(mc, mc.options.backKey), this.nv_isRawKeyDown(mc, mc.options.leftKey), this.nv_isRawKeyDown(mc, mc.options.rightKey), this.nv_isRawKeyDown(mc, mc.options.jumpKey), this.nv_isRawKeyDown(mc, mc.options.sneakKey), this.nv_isRawKeyDown(mc, mc.options.sprintKey));
    }

    @Unique
    private boolean nv_isRawKeyDown(MinecraftClient mc, KeyBinding mapping) {
        InputUtil.Key key = InputUtil.fromTranslationKey((String)mapping.getBoundKeyTranslationKey());
        return switch (key.getCategory()) {
            case InputUtil.Type.KEYSYM -> InputUtil.isKeyPressed((Window)mc.getWindow(), (int)key.getCode());
            case InputUtil.Type.MOUSE -> {
                if (GLFW.glfwGetMouseButton((long)mc.getWindow().getHandle(), (int)key.getCode()) == 1) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }
}

