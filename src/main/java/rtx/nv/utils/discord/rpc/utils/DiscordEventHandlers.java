package rtx.nv.utils.discord.rpc.utils;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
import rtx.nv.utils.discord.rpc.callbacks.DisconnectedCallback;
import rtx.nv.utils.discord.rpc.callbacks.ErroredCallback;
import rtx.nv.utils.discord.rpc.callbacks.JoinGameCallback;
import rtx.nv.utils.discord.rpc.callbacks.JoinRequestCallback;
import rtx.nv.utils.discord.rpc.callbacks.ReadyCallback;
import rtx.nv.utils.discord.rpc.callbacks.SpectateGameCallback;

public class DiscordEventHandlers
extends Structure {
    public DisconnectedCallback disconnected;
    public JoinRequestCallback joinRequest;
    public SpectateGameCallback spectateGame;
    public ReadyCallback ready;
    public ErroredCallback errored;
    public JoinGameCallback joinGame;

    protected List<String> getFieldOrder() {
        return Arrays.asList("ready", "disconnected", "errored", "joinGame", "spectateGame", "joinRequest");
    }


    public static class Builder {
        private final DiscordEventHandlers handlers = new DiscordEventHandlers();
    
        public DiscordEventHandlers build() {
            return this.handlers;
        }
    
        public Builder ready(ReadyCallback readyCallback) {
            this.handlers.ready = readyCallback;
            return this;
        }
    
        public Builder disconnected(DisconnectedCallback disconnectedCallback) {
            this.handlers.disconnected = disconnectedCallback;
            return this;
        }
    
        public Builder errored(ErroredCallback erroredCallback) {
            this.handlers.errored = erroredCallback;
            return this;
        }
    
        public Builder joinGame(JoinGameCallback joinGameCallback) {
            this.handlers.joinGame = joinGameCallback;
            return this;
        }
    
        public Builder joinRequest(JoinRequestCallback joinRequestCallback) {
            this.handlers.joinRequest = joinRequestCallback;
            return this;
        }
    
        public Builder spectateGame(SpectateGameCallback spectateGameCallback) {
            this.handlers.spectateGame = spectateGameCallback;
            return this;
        }
    }
}

