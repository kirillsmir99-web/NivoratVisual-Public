package rtx.nv.api.chat.commands.impl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rtx.nv.api.chat.commands.Command;
import rtx.nv.api.chat.commands.CommandManager;
import rtx.nv.api.chat.commands.helpers.CommandDividers;
import rtx.nv.api.chat.commands.helpers.TabCompleteHelper;
import rtx.nv.api.config.ConfigManager;

public final class ConfigCommand
extends Command {
    public ConfigCommand() {
        super("config", "Configuration management", "cfg");
    }

    private void reset() {
        ConfigManager.resetToFactory();
        this.logDirect("Client reset to factory defaults.", Formatting.GREEN);
    }

    private void load(String[] stringArray) {
        if (stringArray.length < 2) {
            this.logDirect("Usage: config load <name>", Formatting.RED);
            return;
        }
        String string = ConfigCommand.joinName(stringArray);
        if (!ConfigManager.profileExists(string)) {
            this.logDirect("Config '" + string + "' not found. Use config list.", Formatting.RED);
            return;
        }
        ConfigManager.loadProfile(string);
        this.logDirect("Config '" + string + "' loaded.", Formatting.GREEN);
    }

    private void list(String string) {
        List<String> list = ConfigManager.listProfiles();
        if (list.isEmpty()) {
            this.logDirect("No saved configs. Use config save <name>.", Formatting.RED);
            return;
        }
        String string2 = "CONFIGS";
        int n = CommandDividers.calcLineCountForContent((String)"CONFIGS", list);
        this.logDirectRaw(CommandDividers.header((String)"CONFIGS", (int)n));
        CommandManager commandManager = CommandManager.getInstance();
        String string3 = commandManager == null ? "." : commandManager.getPrefix();
        for (String string4 : list) {
            String string5 = string3 + string + " load " + string4;
            MutableText mutableText = Text.literal((String)("> " + string4)).styled(style -> style.withColor(Formatting.GREEN).withClickEvent((ClickEvent)new ClickEvent.RunCommand(string5)).withHoverEvent((HoverEvent)new HoverEvent.ShowText((Text)Text.literal((String)("\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u043a\u043e\u043d\u0444\u0438\u0433 '" + string4 + "'")).formatted(Formatting.GRAY)))).append((Text)Text.literal((String)(" [" + ConfigManager.profileEnabledCount(string4) + " enabled]")).formatted(Formatting.DARK_GRAY));
            this.logDirect((Text)mutableText);
        }
        this.logDirectRaw(CommandDividers.footer((String)"CONFIGS", (int)n));
    }

    @Override
    public void execute(String string, String[] stringArray) {
        String string2;
        switch (string2 = stringArray.length > 0 ? stringArray[0].toLowerCase(Locale.ROOT) : "") {
            case "save": {
                this.save(stringArray);
                break;
            }
            case "load": {
                this.load(stringArray);
                break;
            }
            case "reset": {
                this.reset();
                break;
            }
            case "list": {
                this.list(string);
                break;
            }
            case "dir": {
                this.openDir();
                break;
            }
            default: {
                this.usage();
            }
        }
    }

    private void save(String[] stringArray) {
        if (stringArray.length < 2) {
            this.logDirect("Usage: config save <name>", Formatting.RED);
            return;
        }
        String string = ConfigCommand.joinName(stringArray);
        ConfigManager.saveProfile(string);
        this.logDirect("Config '" + string + "' saved.", Formatting.GREEN);
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("Saves, loads or resets the client configuration.", "Usage:", "> config save <name>", "> config load <name>", "> config list", "> config reset", "> config dir");
    }

    @Override
    public Stream<String> tabComplete(String string, String[] stringArray) {
        if (stringArray.length == 1) {
            return new TabCompleteHelper().append(new String[]{"save", "load", "list", "reset", "dir"}).sortAlphabetically().filterPrefix(stringArray[0]).stream();
        }
        if (stringArray.length == 2 && stringArray[0].equalsIgnoreCase("load")) {
            return new TabCompleteHelper().append(ConfigManager.listProfiles().toArray(new String[0])).filterPrefix(stringArray[1]).stream();
        }
        return Stream.empty();
    }

    private void openDirectory(Path path) throws IOException {
        String string = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        ProcessBuilder processBuilder = string.contains("win") ? new ProcessBuilder("explorer.exe", path.toAbsolutePath().toString()) : (string.contains("mac") ? new ProcessBuilder("open", path.toAbsolutePath().toString()) : new ProcessBuilder("xdg-open", path.toAbsolutePath().toString()));
        processBuilder.start();
    }

    private static String joinName(String[] stringArray) {
        return String.join((CharSequence)" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)).trim();
    }

    private void openDir() {
        try {
            Path path = ConfigManager.profilesDirectory();
            Files.createDirectories(path, new FileAttribute[0]);
            this.openDirectory(path);
            this.logDirect("Config folder opened.", Formatting.GREEN);
        }
        catch (IOException iOException) {
            this.logDirect("Failed to open config folder: " + iOException.getMessage(), Formatting.RED);
        }
    }
}

