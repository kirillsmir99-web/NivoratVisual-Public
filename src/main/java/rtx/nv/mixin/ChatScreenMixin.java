package rtx.nv.mixin;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.chat.commands.CommandManager;
import rtx.nv.api.chat.commands.suggestion.ClientCommandSuggestions;
import rtx.nv.utils.chat.ChatHistory;

@Mixin(net.minecraft.client.gui.screen.ChatScreen.class)

public abstract class ChatScreenMixin
extends Screen {
    @Shadow
    protected TextFieldWidget chatField;
    @Shadow
    ChatInputSuggestor chatInputSuggestor;
    @Unique
    private static boolean nv_historySeeded;

    @Shadow
    public abstract String normalize(String var1);

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method="init", at={@At(value="TAIL")}, require = 0)
    private void nv_replaceSuggestions(CallbackInfo ci) {
        CommandManager.get().refreshRuntimeState();
        this.chatInputSuggestor = new ClientCommandSuggestions(this.client, this, this.chatField, this.client.textRenderer, false, false, 1, 10, true, -805306368);
        this.chatInputSuggestor.setCanLeave(false);
        this.chatInputSuggestor.refresh();
        this.nv_seedHistory();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Unique
    private void nv_seedHistory() {
        if (nv_historySeeded) {
            return;
        }
        nv_historySeeded = true;
        if (this.client == null || this.client.inGameHud == null) {
            return;
        }
        ChatHistory.seeding = true;
        try {
            ChatHud chat = this.client.inGameHud.getChatHud();
            for (String line : ChatHistory.entries()) {
                chat.addToMessageHistory(line);
            }
        }
        finally {
            ChatHistory.seeding = false;
        }
    }

    @Inject(method="sendMessage", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_handleClientCommand(String message, boolean addToHistory, CallbackInfo ci) {
        CommandManager.get().refreshRuntimeState();
        String normalized = this.normalize(message);
        if (!CommandManager.get().isClientCommand(normalized)) {
            return;
        }
        if (addToHistory) {
            this.client.inGameHud.getChatHud().addToMessageHistory(normalized);
        }
        String prefix = CommandManager.get().getPrefix();
        CommandManager.get().executeRaw(normalized.substring(prefix.length()));
        ci.cancel();
    }
}

