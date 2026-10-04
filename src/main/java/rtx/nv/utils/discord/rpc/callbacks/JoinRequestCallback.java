package rtx.nv.utils.discord.rpc.callbacks;
import com.sun.jna.Callback;
import rtx.nv.utils.discord.rpc.utils.DiscordUser;

public interface JoinRequestCallback
extends Callback {
    public void apply(DiscordUser var1);
}

