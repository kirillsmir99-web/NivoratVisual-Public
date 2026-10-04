package rtx.nv.api.chat.commands.suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import rtx.nv.api.chat.commands.CommandManager;
import rtx.nv.mixin.accessor.CommandSuggestionsAccessor;

public class ClientCommandSuggestions
extends ChatInputSuggestor {
    private final TextFieldWidget editBox;

    public ClientCommandSuggestions(MinecraftClient minecraftClient, Screen screen, TextFieldWidget textFieldWidget, TextRenderer textRenderer, boolean bl, boolean bl2, int n, int n2, boolean bl3, int n3) {
        super(minecraftClient, screen, textFieldWidget, textRenderer, bl, bl2, n, n2, bl3, n3);
        this.editBox = textFieldWidget;
    }

    public void refresh() {
        int n;
        String string = this.editBox.getText();
        String string3 = CommandManager.get().getPrefix();
        CommandSuggestionsAccessor commandSuggestionsAccessor = (CommandSuggestionsAccessor)((Object)this);
        if (commandSuggestionsAccessor.isKeepSuggestions()) {
            return;
        }
        if (!string.startsWith(string3)) {
            super.refresh();
            return;
        }
        CompletableFuture<Suggestions> completableFuture = commandSuggestionsAccessor.getPendingSuggestions();
        if (completableFuture != null) {
            completableFuture.cancel(false);
        }
        commandSuggestionsAccessor.setPendingSuggestions(null);
        commandSuggestionsAccessor.setCurrentParse(null);
        this.editBox.setSuggestion("");
        String string4 = string.substring(string3.length());
        int n2 = string.lastIndexOf(60);
        if (n2 >= 0) {
            int n3 = 1;
            int n4 = n2;
            int n5 = n4 + (n4 * n4 << -67);
            n4 = n5 - (n5 * n5 << -67);
            n5 = n3 + (n3 * n3 << -67);
            n3 = n5 - (n5 * n5 << -67);
            n = (n4 ^ n3) + ((n4 & n3) << 1);
        } else {
            n = 0;
        }
        int n6 = n;
        SuggestionsBuilder suggestionsBuilder = new SuggestionsBuilder(string, n6);
        Stream<String> stream = CommandManager.get().tabComplete(string4);
        if (n2 < 0) {
            stream.map(string2 -> string3 + string2).forEach(arg_0 -> ((SuggestionsBuilder)suggestionsBuilder).suggest(arg_0));
        } else {
            stream.forEach(arg_0 -> ((SuggestionsBuilder)suggestionsBuilder).suggest(arg_0));
        }
        Suggestions suggestions = suggestionsBuilder.build();
        if (suggestions.isEmpty()) {
            this.clearWindow();
            return;
        }
        commandSuggestionsAccessor.setPendingSuggestions(CompletableFuture.completedFuture(suggestions));
        commandSuggestionsAccessor.invokeShowSuggestions(false);
    }
}

