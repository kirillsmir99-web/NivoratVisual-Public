package rtx.nv.mixin.chatheads;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.brigadier.suggestion.Suggestion;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.Rect2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.mods.chatheads.ChatHeads;

@Mixin(targets = "net.minecraft.client.gui.screen.ChatInputSuggestor$SuggestionWindow")
public abstract class CommandSuggestionSuggestionsListMixin {
    @Shadow
    @Final
    private Rect2i area;
    @Shadow
    @Final
    private List<Suggestion> suggestions;

    @ModifyVariable(method={"render"}, at=@At(value="HEAD"), argsOnly=true, require = 0)
    public DrawContext chatheads_captureGuiGraphics(DrawContext guiGraphics, @Share(value="graphics") LocalRef<DrawContext> graphicsRef) {
        graphicsRef.set(guiGraphics);
        return guiGraphics;
    }

    @Inject(method={"<init>"}, at={@At(value="RETURN")}, require = 0)
    public void chatheads_fixOutOfBoundChatHeads(ChatInputSuggestor commandSuggestions, int x, int y, int width, List<Suggestion> suggestions, boolean narrateFirstSuggestion, CallbackInfo ci) {
        ClientPlayNetworkHandler connection = MinecraftClient.getInstance().getNetworkHandler();
        if (connection == null) {
            return;
        }
        if (this.area != null && this.area.getX() - (ChatHeads.headWidth(false) + 2) < 3) {
            for (Suggestion suggestion : this.suggestions) {
                PlayerListEntry playerInfo = connection.getPlayerListEntry(suggestion.getText());
                if (playerInfo == null) continue;
                this.area.setStartPos(3 + (ChatHeads.headWidth(false) + 2), this.area.getY());
                break;
            }
        }
    }

    @ModifyVariable(method={"render"}, at=@At(value="STORE"), ordinal=0, require = 0)
    public Suggestion chatheads_captureSuggestion(Suggestion suggestion, @Share(value="player") LocalRef<PlayerListEntry> playerRef) {
        ClientPlayNetworkHandler connection = MinecraftClient.getInstance().getNetworkHandler();
        if (connection == null) {
            return suggestion;
        }
        playerRef.set(connection.getPlayerListEntry(suggestion.getText()));
        return suggestion;
    }

    @ModifyArg(method={"render"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal=4), index=0, require = 0)
    public int chatheads_enlargeBackground(int x, @Share(value="player") LocalRef<PlayerListEntry> playerRef) {
        if (playerRef.get() != null) {
            return x - (ChatHeads.headWidth(false) + 2);
        }
        return x;
    }

    @ModifyArg(method={"render"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I", ordinal=0), index=3, require = 0)
    public int chatheads_renderChatHead(int y, @Share(value="player") LocalRef<PlayerListEntry> playerRef, @Share(value="graphics") LocalRef<DrawContext> graphicsRef) {
        int x = (this.area != null ? this.area.getX() : 0) - ChatHeads.headWidth(false);
        if (playerRef.get() != null && graphicsRef.get() != null) {
            ChatHeads.renderChatHead(graphicsRef.get(), x, y, playerRef.get(), 1.0f, false);
        }
        return y;
    }
}
