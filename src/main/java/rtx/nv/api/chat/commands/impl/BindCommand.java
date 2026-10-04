package rtx.nv.api.chat.commands.impl;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;
import rtx.nv.api.chat.commands.Command;
import rtx.nv.api.chat.commands.helpers.CommandDividers;
import rtx.nv.api.chat.commands.helpers.TabCompleteHelper;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.ClickGui;
import rtx.nv.utils.chat.ChatMessage;
import rtx.nv.utils.key.KeyBind;
import rtx.nv.utils.key.KeyHelper;

public final class BindCommand
extends Command {
    public BindCommand() {
        super("bind", "Manage module binds", "b");
    }

    @Override
    public void execute(String string, String[] stringArray) {
        String string2;
        ModuleManager moduleManager = ModuleManager.get();
        if (stringArray.length == 0) {
            this.usage();
            return;
        }
        switch (string2 = stringArray[0].toLowerCase(Locale.ROOT)) {
            case "add": 
            case "set": {
                this.setBind(moduleManager, stringArray);
                break;
            }
            case "remove": 
            case "del": 
            case "delete": {
                this.clearBind(moduleManager, stringArray);
                break;
            }
            case "clear": {
                this.clearAll(moduleManager);
                break;
            }
            case "list": {
                this.listBinds(moduleManager);
                break;
            }
            default: {
                this.usage();
            }
        }
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("Manages module key binds.", "Usage:", "> bind add <module> <key>", "> bind remove <module>", "> bind list", "> bind clear");
    }

    @Override
    public Stream<String> tabComplete(String string, String[] stringArray) {
        if (stringArray.length == 1) {
            return new TabCompleteHelper().append(new String[]{"add", "remove", "list", "clear"}).sortAlphabetically().filterPrefix(stringArray[0]).stream();
        }
        if (stringArray.length >= 2 && BindCommand.isAddAction(stringArray[0])) {
            String string2 = BindCommand.joinArgs(stringArray, 1, stringArray.length - 1);
            if (stringArray.length >= 3 && ModuleManager.get().findByName(string2) != null) {
                return new TabCompleteHelper().append(KeyHelper.getAllKeyNames().toArray(new String[0])).filterPrefix(stringArray[stringArray.length - 1]).stream();
            }
            String string3 = BindCommand.joinArgs(stringArray, 1, stringArray.length);
            return new TabCompleteHelper().append((String[])ModuleManager.get().getAll().stream().map(Module::getName).toArray(String[]::new)).filterPrefix(string3).stream();
        }
        if (stringArray.length >= 2 && BindCommand.isRemoveAction(stringArray[0])) {
            String string4 = BindCommand.joinArgs(stringArray, 1, stringArray.length);
            return new TabCompleteHelper().append((String[])ModuleManager.get().getAll().stream().filter(module -> module.getBind().isBound() && !(module instanceof ClickGui)).map(Module::getName).toArray(String[]::new)).filterPrefix(string4).stream();
        }
        return Stream.empty();
    }

    private static boolean isAddAction(String string) {
        return "add".equalsIgnoreCase(string) || "set".equalsIgnoreCase(string);
    }

    private static boolean isRemoveAction(String string) {
        return "remove".equalsIgnoreCase(string) || "del".equalsIgnoreCase(string) || "delete".equalsIgnoreCase(string);
    }

    private void setBind(ModuleManager moduleManager, String[] stringArray) {
        if (stringArray.length < 3) {
            this.logDirect("Usage: bind add <module> <key>", Formatting.RED);
            return;
        }
        String string = stringArray[stringArray.length - 1];
        String string2 = BindCommand.joinArgs(stringArray, 1, stringArray.length - 1);
        Module module = moduleManager.findByName(string2);
        if (module == null) {
            this.logDirect("Module not found: " + string2, Formatting.RED);
            return;
        }
        int n = KeyHelper.getKeyCode(string);
        if (n == -1 || n < 0) {
            this.logDirect("Unknown key: " + string, Formatting.RED);
            return;
        }
        if (module instanceof ClickGui && KeyHelper.isMouse(n)) {
            this.logDirect("ClickGui can only be bound to a keyboard key.", Formatting.RED);
            return;
        }
        module.setBind(KeyHelper.isMouse(n) ? KeyBind.mouse(n) : KeyBind.keyboard(n));
        this.logDirect(ChatMessage.accentGradient((String)(module.getName() + " bound to " + module.getBind().getDisplayName())));
    }

    private void listBinds(ModuleManager moduleManager) {
        List<Module> list = moduleManager.getAll().stream().filter(module -> module.getBind().isBound()).sorted(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        if (list.isEmpty()) {
            this.logDirect("No module binds.", Formatting.RED);
            return;
        }
        String string = "BINDS";
        List<String> list2 = list.stream().map(module -> module.getName() + " -> " + module.getBind().getDisplayName()).toList();
        int n = CommandDividers.calcLineCountForContent((String)"BINDS", list2);
        this.logDirectRaw(CommandDividers.header((String)"BINDS", (int)n));
        for (Module module2 : list) {
            this.logDirect(module2.getName() + " -> " + module2.getBind().getDisplayName());
        }
        this.logDirectRaw(CommandDividers.footer((String)"BINDS", (int)n));
    }

    private static String joinArgs(String[] stringArray, int n, int n2) {
        if (n >= n2) {
            return "";
        }
        return String.join((CharSequence)" ", Arrays.copyOfRange(stringArray, n, n2));
    }

    private void clearBind(ModuleManager moduleManager, String[] stringArray) {
        if (stringArray.length < 2) {
            this.logDirect("Usage: bind remove <module>", Formatting.RED);
            return;
        }
        String string = BindCommand.joinArgs(stringArray, 1, stringArray.length);
        Module module = moduleManager.findByName(string);
        if (module == null) {
            this.logDirect("Module not found: " + string, Formatting.RED);
            return;
        }
        if (module instanceof ClickGui) {
            this.logDirect("ClickGui bind is protected and cannot be removed.", Formatting.RED);
            return;
        }
        module.setBind(KeyBind.NONE);
        this.logDirect(ChatMessage.accentGradient((String)(module.getName() + " bind removed.")));
    }

    private void clearAll(ModuleManager moduleManager) {
        for (Module module : moduleManager.getAll()) {
            if (module instanceof ClickGui) continue;
            module.setBind(KeyBind.NONE);
        }
        this.logDirect("All binds removed.");
    }
}

