package rtx.nv.api.chat.commands.impl;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;
import rtx.nv.api.chat.commands.Command;
import rtx.nv.api.chat.commands.helpers.CommandDividers;
import rtx.nv.api.chat.commands.helpers.TabCompleteHelper;
import rtx.nv.utils.key.KeyHelper;
import rtx.nv.utils.storage.macro.Macro;
import rtx.nv.utils.storage.macro.MacroRepository;

public final class MacroCommand
extends Command {
    public MacroCommand() {
        super("macro", "Manage macros", new String[0]);
    }

    @Override
    public void execute(String string, String[] stringArray) {
        String string2;
        MacroRepository macroRepository = MacroRepository.getInstance();
        if (stringArray.length == 0) {
            this.usage();
            return;
        }
        switch (string2 = stringArray[0].toLowerCase(Locale.ROOT)) {
            case "add": {
                if (stringArray.length < 4) {
                    this.logDirect("Usage: macro add <name> <message> <key>", Formatting.RED);
                    return;
                }
                String string3 = stringArray[1];
                String string4 = stringArray[stringArray.length - 1];
                int n = KeyHelper.getKeyCode(string4);
                if (n < 0) {
                    this.logDirect("Unknown key: " + string4, Formatting.RED);
                    return;
                }
                String string5 = String.join((CharSequence)" ", Arrays.copyOfRange(stringArray, 2, stringArray.length - 1));
                macroRepository.addMacroAndSave(string3, string5, n);
                this.logDirect("Macro " + string3 + " added [" + KeyHelper.getKeyName(n) + "]", Formatting.GREEN);
                break;
            }
            case "remove": 
            case "del": 
            case "delete": {
                if (stringArray.length < 2) {
                    this.logDirect("Usage: macro remove <name>", Formatting.RED);
                    return;
                }
                macroRepository.deleteMacroAndSave(stringArray[1]);
                this.logDirect("Macro " + stringArray[1] + " removed.", Formatting.GREEN);
                break;
            }
            case "clear": {
                int n = macroRepository.size();
                macroRepository.clearListAndSave();
                this.logDirect("Macros cleared. Removed: " + n, Formatting.GREEN);
                break;
            }
            case "list": {
                if (macroRepository.getMacroList().isEmpty()) {
                    this.logDirect("Macro list is empty.", Formatting.RED);
                    return;
                }
                String string6 = "MACROS";
                List<String> list = macroRepository.getMacroList().stream().map(macro -> macro.name() + " [" + KeyHelper.getKeyName(macro.key()) + "] " + macro.message()).toList();
                int n = CommandDividers.calcLineCountForContent((String)"MACROS", list);
                this.logDirectRaw(CommandDividers.header((String)"MACROS", (int)n));
                for (Macro macro2 : macroRepository.getMacroList()) {
                    this.logDirect("\u00a7f" + macro2.name() + " \u00a78[" + KeyHelper.getKeyName(macro2.key()) + "] \u00a77" + macro2.message());
                }
                this.logDirectRaw(CommandDividers.footer((String)"MACROS", (int)n));
                break;
            }
            default: {
                this.usage();
            }
        }
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("Manages macros.", "Usage:", "> macro add <name> <message> <key>", "> macro remove <name>", "> macro list", "> macro clear");
    }

    @Override
    public Stream<String> tabComplete(String string, String[] stringArray) {
        if (stringArray.length == 1) {
            return new TabCompleteHelper().append(new String[]{"add", "remove", "list", "clear"}).sortAlphabetically().filterPrefix(stringArray[0]).stream();
        }
        if (stringArray.length >= 4 && stringArray[0].equalsIgnoreCase("add")) {
            return new TabCompleteHelper().append(KeyHelper.getAllKeyNames().toArray(new String[0])).filterPrefix(stringArray[stringArray.length - 1]).stream();
        }
        if (stringArray.length == 2 && stringArray[0].equalsIgnoreCase("remove")) {
            return new TabCompleteHelper().append(MacroRepository.getInstance().getMacroNames().toArray(new String[0])).filterPrefix(stringArray[1]).stream();
        }
        return Stream.empty();
    }
}

