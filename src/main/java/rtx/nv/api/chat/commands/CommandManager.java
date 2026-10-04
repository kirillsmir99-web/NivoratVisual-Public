package rtx.nv.api.chat.commands;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rtx.nv.NV;
import rtx.nv.api.chat.commands.Command;
import rtx.nv.api.chat.commands.impl.BindCommand;
import rtx.nv.api.chat.commands.impl.ConfigCommand;
import rtx.nv.api.chat.commands.impl.HelpCommand;
import rtx.nv.api.chat.commands.impl.MacroCommand;
import rtx.nv.api.chat.commands.impl.PartyChatCommand;
import rtx.nv.api.chat.commands.impl.PartyCommand;
import rtx.nv.api.chat.commands.impl.BoostCommand;
import rtx.nv.api.chat.commands.impl.BrowserCommand;
import rtx.nv.api.chat.commands.impl.MusicCommand;
import rtx.nv.api.chat.commands.impl.PrefixCommand;
import rtx.nv.api.chat.commands.impl.ViewModelCommand;
import rtx.nv.api.chat.commands.impl.GuiCommand;
import rtx.nv.utils.chat.ChatMessage;
import rtx.nv.utils.sounds.Sounds;
import rtx.nv.utils.storage.RepositoryStorage;

public final class CommandManager {
    private static CommandManager instance;
    private final List<Command> commands = new CopyOnWriteArrayList<Command>();
    private String prefix = ".";
    private Runnable onStateRefresh;

    private CommandManager() {
        instance = this;
    }

    public static CommandManager get() {
        if (instance == null) {
            new CommandManager();
        }
        return instance;
    }

    public static CommandManager getInstance() {
        return CommandManager.get();
    }

    public void register(Command command) {
        this.commands.add(command);
    }

    public Command find(String string) {
        return this.getCommand(string);
    }

    public void init() {
        JsonObject jsonObject = RepositoryStorage.readObject("prefix");
        this.prefix = jsonObject.has("prefix") ? jsonObject.get("prefix").getAsString() : ".";
        this.register(new HelpCommand());
        this.register(new ConfigCommand());
        this.register(new MacroCommand());
        this.register(new BindCommand());
        this.register(new PrefixCommand());
        if (!rtx.nv.ClientEdition.isTrial()) {
            this.register(new PartyCommand());
            this.register(new PartyChatCommand());
            this.register(new MusicCommand());
        }
        this.register(new ViewModelCommand());
        this.register(new GuiCommand());
        this.register(new BrowserCommand());
        this.register(new BoostCommand());
        NV.LOGGER.info("[CommandManager] Initialized with {} commands, prefix '{}'", (Object)this.commands.size(), (Object)this.prefix);
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void refreshRuntimeState() {
        if (this.onStateRefresh != null) {
            this.onStateRefresh.run();
        }
    }

    public boolean isClientCommand(String string) {
        return string != null && string.startsWith(this.prefix);
    }

    public void sendSuccess(String string) {
        this.sendRaw((Text)ChatMessage.brandmessage().copy().append((Text)Text.literal((String)"\u2192 ").formatted(Formatting.DARK_GRAY)).append((Text)Text.literal((String)string).formatted(Formatting.WHITE)));
    }

    public Stream<String> tabComplete(String string) {
        String[] stringArray;
        if (string == null) {
            string = "";
        }
        if ((stringArray = string.split("\\s+", -1)).length <= 1) {
            String string2 = stringArray.length == 0 ? "" : stringArray[0].toLowerCase();
            return this.getCommandSuggestions(string2);
        }
        Command command = this.getCommand(stringArray[0]);
        if (command != null) {
            return command.tabComplete(stringArray[0], Arrays.copyOfRange(stringArray, 1, stringArray.length));
        }
        return Stream.empty();
    }

    private Stream<String> getCommandSuggestions(String string) {
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>();
        for (Command command : this.commands) {
            if (command.getName().toLowerCase().startsWith(string)) {
                linkedHashSet.add(command.getName());
            }
            for (String string2 : command.getAliases()) {
                if (!string2.toLowerCase().startsWith(string)) continue;
                linkedHashSet.add(string2);
            }
        }
        return linkedHashSet.stream().sorted();
    }

    public void setStateRefreshCallback(Runnable runnable) {
        this.onStateRefresh = runnable;
    }

    public List<Command> getCommands() {
        return new ArrayList<Command>(this.commands);
    }

    public List<Command> getAll() {
        return Collections.unmodifiableList(this.commands);
    }

    public Command getCommand(String string) {
        return this.commands.stream().filter(command -> command.matches(string)).findFirst().orElse(null);
    }

    public void sendMessage(String string) {
        ChatMessage.brandmessage((String)string);
    }

    public void executeRaw(String string) {
        if (string == null || string.isBlank()) {
            this.executeRaw("help");
            return;
        }
        String[] stringArray = string.trim().split("\\s+", 2);
        String string2 = stringArray[0];
        String[] stringArray2 = stringArray.length > 1 ? stringArray[1].split("\\s+") : new String[]{};
        Command command = this.getCommand(string2);
        if (command == null) {
            this.sendError("Unknown command. Use " + this.prefix + "help.");
            return;
        }
        try {
            command.execute(string2, stringArray2);
        }
        catch (Exception exception) {
            this.sendError("Command error: " + exception.getMessage());
            NV.LOGGER.error("[CommandManager] Error executing command '{}'", (Object)string2, (Object)exception);
        }
    }

    public void sendError(String string) {
        Sounds.play("command_error");
        this.sendRaw((Text)ChatMessage.brandmessage().copy().append((Text)Text.literal((String)"\u2192 ").formatted(Formatting.DARK_GRAY)).append((Text)Text.literal((String)string).formatted(Formatting.RED)));
    }

    public void sendRaw(Text text) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(text, false);
        }
    }

    public void setPrefix(String string) {
        this.prefix = string == null || string.isBlank() ? "." : string;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("prefix", this.prefix);
        RepositoryStorage.write("prefix", jsonObject);
    }
}

