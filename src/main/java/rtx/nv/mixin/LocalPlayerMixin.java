package rtx.nv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.game.CloseScreenEvent;
import rtx.nv.api.events.impl.player.PlayerMoveEvent;
import rtx.nv.api.events.impl.player.UsingItemEvent;

@Mixin(ClientPlayerEntity.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "closeScreen", at = {@At("HEAD")}, cancellable = true, require = 0)
    private void nv_closeHandledScreenHook(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = client == null ? null : client.currentScreen;
        EventBus bus = EventBus.get();
        if (!bus.hasListeners(CloseScreenEvent.class)) {
            return;
        }
        CloseScreenEvent event = bus.post(new CloseScreenEvent(screen));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "tickMovement", at = {@At("HEAD")}, require = 0)
    private void nv_onAiStep(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity)(Object)this;
        if (self.input == null || self.input.playerInput == null) {
            return;
        }
        EventBus bus = EventBus.get();
        if (!bus.hasListeners(PlayerMoveEvent.class)) {
            return;
        }
        PlayerInput current = self.input.playerInput;
        PlayerMoveEvent event = bus.post(new PlayerMoveEvent(current.forward(), current.backward(), current.left(), current.right(), current.jump(), current.sprint(), current.sneak()));
        if (event.isCancelled()) {
            self.input.playerInput = PlayerInput.DEFAULT;
        } else if (this.nv_inputChanged(current, event)) {
            self.input.playerInput = new PlayerInput(event.isForward(), event.isBackward(), event.isLeft(), event.isRight(), event.isJump(), event.isShift(), event.isSprint());
        }
    }

    @Inject(method = "tickMovement", at = {@At("HEAD")}, require = 0)
    private void nv_onUsingItemPre(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity)(Object)this;
        if (!self.isUsingItem()) {
            return;
        }
        EventBus bus = EventBus.get();
        if (bus.hasListeners(UsingItemEvent.class)) {
            bus.post(new UsingItemEvent(0));
        }
    }

    @Inject(method = "tickMovement", at = {@At("RETURN")}, require = 0)
    private void nv_onUsingItemPost(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity)(Object)this;
        if (!self.isUsingItem()) {
            return;
        }
        EventBus bus = EventBus.get();
        if (bus.hasListeners(UsingItemEvent.class)) {
            bus.post(new UsingItemEvent(1));
        }
    }

    @Unique
    private boolean nv_inputChanged(PlayerInput current, PlayerMoveEvent event) {
        return current.forward() != event.isForward() || current.backward() != event.isBackward() || current.left() != event.isLeft() || current.right() != event.isRight() || current.jump() != event.isJump() || current.sprint() != event.isSprint() || current.sneak() != event.isShift();
    }
}
