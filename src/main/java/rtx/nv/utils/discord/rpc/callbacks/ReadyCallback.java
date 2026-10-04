package rtx.nv.utils.discord.rpc.callbacks;
import com.sun.jna.Callback;
import rtx.nv.utils.discord.rpc.utils.DiscordUser;

public interface ReadyCallback
extends Callback {
    public void apply(DiscordUser var1);
}

