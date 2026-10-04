package rtx.nv.api.chat.commands.helpers;

import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class CommandDividers {
    public static int calcLineCountForContent(String title, List<String> content) {
        return content != null ? content.size() : 0;
    }

    public static Text header(String title, int count) {
        return Text.literal("------- " + title + " -------").formatted(Formatting.GRAY);
    }

    public static Text footer(String title, int count) {
        return Text.literal("---------------------------").formatted(Formatting.GRAY);
    }
}