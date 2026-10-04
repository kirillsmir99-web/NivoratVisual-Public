package rtx.nv.utils.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ChatMessage {
    private static final String PREFIX_STR = "[NV] ";

    public static MutableText brandmessage() {
        return Text.literal("[NV] ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD);
    }

    public static void brandmessage(String message) {
        brandmessage(Text.literal(message));
    }

    public static void brandmessage(Text text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            MutableText full = brandmessage().append(text);
            mc.player.sendMessage(full, false);
        }
    }

    public static String accentGradient(String text) {
        return text;
    }

    public static void send(String message) {
        brandmessage(message);
    }

    public static void error(String message) {
        brandmessage(Text.literal(message).formatted(Formatting.RED));
    }
}