package rtx.nv.api.chat.commands.impl;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rtx.nv.api.chat.commands.Command;
import rtx.nv.api.chat.commands.CommandManager;
import rtx.nv.api.chat.commands.helpers.CommandDividers;
import rtx.nv.api.chat.commands.helpers.Paginator;
import rtx.nv.api.chat.commands.helpers.TabCompleteHelper;

public final class HelpCommand
extends Command {
    public HelpCommand() {
        super("help", "Shows available commands", new String[0]);
    }

    @Override
    public void execute(String string, String[] stringArray) {
        CommandManager commandManager = CommandManager.getInstance();
        if (stringArray.length == 0 || this.isInteger(stringArray[0])) {
            int n = stringArray.length > 0 && this.isInteger(stringArray[0]) ? Integer.parseInt(stringArray[0]) : 1;
            List<Command> list = commandManager.getCommands().stream().filter(command -> !command.hiddenFromHelp()).collect(Collectors.toList());
            List<String> list2 = list.stream().map(command -> commandManager.getPrefix() + command.getName() + " - " + command.getShortDesc()).collect(Collectors.toList());
            String string2 = "COMMANDS";
            int n2 = CommandDividers.calcLineCountForContent((String)"COMMANDS", list2);
            Paginator<Command> paginator = new Paginator<>(list);
            paginator.setPage(n);
            int n3 = n2;
            paginator.display(() -> this.logDirectRaw(CommandDividers.header((String)"COMMANDS", (int)n3)), (Command cmd) -> {
                String cmdName = commandManager.getPrefix() + cmd.getName();
                MutableText mutableText = Text.literal(cmdName).formatted(Formatting.WHITE).append(Text.literal("\n" + cmd.getShortDesc()).formatted(Formatting.GRAY));
                return Text.literal(cmdName).formatted(Formatting.WHITE).append(Text.literal(" - " + cmd.getShortDesc()).formatted(Formatting.GRAY)).styled(style -> style.withHoverEvent(new HoverEvent.ShowText(mutableText)).withClickEvent(new ClickEvent.RunCommand(commandManager.getPrefix() + string + " " + cmd.getName())));
            }, commandManager.getPrefix() + string);
            this.logDirectRaw(CommandDividers.footer((String)"COMMANDS", (int)n3));
            return;
        }
        Command command2 = commandManager.getCommand(stringArray[0]);
        if (command2 == null) {
            this.logDirect("Command '" + stringArray[0] + "' not found.", Formatting.RED);
            return;
        }
        String string3 = command2.getName().toUpperCase();
        List<String> list = command2.getLongDesc();
        int n = CommandDividers.calcLineCountForContent((String)string3, list);
        this.logDirectRaw(CommandDividers.header((String)string3, (int)n));
        String string4 = commandManager.getPrefix();
        for (int i = 0; i < list.size(); ++i) {
            String string5 = list.get(i);
            if (string5.isEmpty()) continue;
            if (i == 0) {
                this.logDirectRaw(Text.literal((String)("Desc: " + string5)).formatted(Formatting.GRAY));
                continue;
            }
            if (string5.startsWith("> ")) {
                this.logDirectRaw(Text.literal((String)(string4 + string5.substring(2))).formatted(Formatting.WHITE));
                continue;
            }
            this.logDirectRaw(Text.literal((String)string5).formatted(Formatting.WHITE));
        }
        this.logDirectRaw(CommandDividers.footer((String)string3, (int)n));
    }

    private boolean isInteger(String string) {
        try {
            Integer.parseInt(string);
            return true;
        }
        catch (NumberFormatException numberFormatException) {
            return false;
        }
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("Shows command help.", "Usage:", "> help", "> help <command>");
    }

    @Override
    public Stream<String> tabComplete(String string, String[] stringArray) {
        if (stringArray.length == 1) {
            return new TabCompleteHelper().filterPrefix(stringArray[0]).addCommands(CommandManager.getInstance()).stream();
        }
        return Stream.empty();
    }
}

