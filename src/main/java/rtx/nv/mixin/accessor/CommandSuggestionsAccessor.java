package rtx.nv.mixin.accessor;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChatInputSuggestor.class)
public interface CommandSuggestionsAccessor {
    @Accessor("messages")
    public List<OrderedText> getCommandUsage();

    @Accessor("pendingSuggestions")
    public CompletableFuture<Suggestions> getPendingSuggestions();

    @Accessor("completingSuggestions")
    public boolean isKeepSuggestions();

    @Accessor("parse")
    public void setCurrentParse(ParseResults<?> var1);

    @Invoker("show")
    public void invokeShowSuggestions(boolean var1);

    @Accessor("parse")
    public ParseResults<?> getCurrentParse();

    @Accessor("pendingSuggestions")
    public void setPendingSuggestions(CompletableFuture<Suggestions> var1);
}
