package rtx.nv.api.chat.commands.impl;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;
import rtx.nv.api.chat.commands.Command;
import rtx.nv.api.chat.commands.CommandManager;

public final class PrefixCommand
extends Command {
    public PrefixCommand() {
        super("prefix", "Changes command prefix", new String[0]);
    }

    @Override
    public void execute(String string, String[] stringArray) {
        CommandManager commandManager = CommandManager.getInstance();
        if (stringArray.length == 0) {
            this.logDirect("Current prefix: " + commandManager.getPrefix());
            this.usage();
            return;
        }
        if (!stringArray[0].equalsIgnoreCase("set") || stringArray.length < 2) {
            this.usage();
            return;
        }
        String string2 = stringArray[1];
        if (string2.length() > 3 || string2.contains(" ")) {
            this.logDirect("Prefix must be 1-3 chars without spaces.", Formatting.RED);
            return;
        }
        commandManager.setPrefix(string2);
        this.logDirect("Prefix changed to: " + string2, Formatting.GREEN);
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("Changes command prefix.", "Usage:", "> prefix", "> prefix set <symbol>");
    }

    @Override
    public Stream<String> tabComplete(String string2, String[] stringArray) {
        if (stringArray.length == 1) {
            return Stream.of("set").filter(string -> string.startsWith(stringArray[0].toLowerCase()));
        }
        if (stringArray.length == 2 && stringArray[0].equalsIgnoreCase("set")) {
            return Stream.of(".", "!", "$", "#", "-", "/").filter(string -> string.startsWith(stringArray[1]));
        }
        return Stream.empty();
    }
}

