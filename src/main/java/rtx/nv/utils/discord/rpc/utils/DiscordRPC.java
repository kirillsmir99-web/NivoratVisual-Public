package rtx.nv.utils.discord.rpc.utils;
import com.sun.jna.Library;
import com.sun.jna.Native;
import rtx.nv.utils.discord.rpc.utils.DiscordEventHandlers;
import rtx.nv.utils.discord.rpc.utils.DiscordRichPresence;

public interface DiscordRPC
extends Library {
    public static final DiscordRPC INSTANCE = (DiscordRPC)Native.load((String)"discord-rpc", DiscordRPC.class);

    public void Discord_RegisterSteamGame(String var1, String var2);

    public void Discord_UpdateConnection();

    public void Discord_Register(String var1, String var2);

    public void Discord_Respond(String var1, int var2);

    public void Discord_UpdateHandlers(DiscordEventHandlers var1);

    public void Discord_ClearPresence();

    public void Discord_Initialize(String var1, DiscordEventHandlers var2, boolean var3, String var4);

    public void Discord_UpdatePresence(DiscordRichPresence var1);

    public void Discord_Shutdown();

    public void Discord_RunCallbacks();
}

