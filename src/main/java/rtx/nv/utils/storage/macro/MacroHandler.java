package rtx.nv.utils.storage.macro;
import rtx.nv.api.events.EventHandler;
import net.minecraft.client.MinecraftClient;
import rtx.nv.api.chat.commands.CommandManager;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.input.KeyPressEvent;
import rtx.nv.api.events.impl.input.KeyPressEvent.Action;
import rtx.nv.utils.storage.macro.Macro;
import rtx.nv.utils.storage.macro.MacroRepository;

public final class MacroHandler {
    private static final MacroHandler INSTANCE = new MacroHandler();

    private MacroHandler() {
    }

    private void run(MinecraftClient minecraftClient, String string) {
        if (string == null || string.isBlank()) {
            return;
        }
        CommandManager commandManager = CommandManager.get();
        if (commandManager.isClientCommand(string)) {
            commandManager.executeRaw(string.substring(commandManager.getPrefix().length()));
        } else if (string.startsWith("/")) {
            minecraftClient.player.networkHandler.sendChatCommand(string.substring(1));
        } else {
            minecraftClient.player.networkHandler.sendChatMessage(string);
        }
    }

    public static void init() {
        EventBus.get().subscribe(INSTANCE);
    }

    @EventHandler
    public void onKey(KeyPressEvent keyPressEvent) {
        if (keyPressEvent.action != KeyPressEvent.Action.PRESS) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.player == null || minecraftClient.currentScreen != null) {
            return;
        }
        for (Macro macro : MacroRepository.getInstance().getMacroList()) {
            if (macro.key() != keyPressEvent.keyCode) continue;
            this.run(minecraftClient, macro.message());
        }
    }
}

