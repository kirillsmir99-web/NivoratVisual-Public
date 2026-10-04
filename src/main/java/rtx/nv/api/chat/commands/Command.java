package rtx.nv.api.chat.commands;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rtx.nv.api.chat.commands.CommandManager;
import rtx.nv.utils.chat.ChatMessage;
import rtx.nv.utils.sounds.Sounds;

public abstract class Command {
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    private final String name;
    private final String description;
    private final List<String> aliases;

    protected Command(String string, String string2, String ... stringArray) {
        this.name = string.toLowerCase();
        this.description = string2 == null ? "" : string2;
        this.aliases = Arrays.asList(stringArray == null ? new String[]{} : stringArray);
    }

    public String getName() {
        return this.name;
    }

    public boolean matches(String string) {
        if (this.name.equalsIgnoreCase(string)) {
            return true;
        }
        return this.aliases.stream().anyMatch(string2 -> string2.equalsIgnoreCase(string));
    }

    public abstract void execute(String var1, String[] var2);

    public String getDescription() {
        return this.description;
    }

    public String getShortDesc() {
        return this.description;
    }

    public List<String> getAllNames() {
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add(this.name);
        arrayList.addAll(this.aliases);
        return arrayList;
    }

    protected void logDirectRaw(Text text) {
        CommandManager.getInstance().sendRaw(text);
    }

    protected void logDirectRaw(MutableText mutableText) {
        CommandManager.getInstance().sendRaw((Text)mutableText);
    }

    public List<String> getLongDesc() {
        return Arrays.asList(this.description, "", "Usage:", "> " + this.name + " - " + this.description);
    }

    public Stream<String> tabComplete(String string, String[] stringArray) {
        return Stream.empty();
    }

    public boolean hiddenFromHelp() {
        return false;
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    protected void logDirect(String string) {
        ChatMessage.brandmessage((String)string);
    }

    protected void logDirect(MutableText mutableText) {
        ChatMessage.brandmessage((Text)mutableText);
    }

    protected void logDirect(Text text) {
        ChatMessage.brandmessage((Text)text);
    }

    protected void logDirect(String string, Formatting formatting) {
        CommandManager commandManager = CommandManager.getInstance();
        if (commandManager == null) {
            ChatMessage.brandmessage((String)string);
            return;
        }
        if (formatting == Formatting.RED) {
            commandManager.sendError(string);
        } else if (formatting == Formatting.GREEN) {
            commandManager.sendSuccess(string);
        } else {
            commandManager.sendMessage(string);
        }
    }

    protected void usage() {
        Sounds.play("command_error");
        CommandManager commandManager = CommandManager.getInstance();
        String string = commandManager != null ? commandManager.getPrefix() : ".";
        String string2 = string + "help " + this.name;
        MutableText mutableText = Text.literal((String)("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 " + this.name + " \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0430 \u043d\u0435\u043a\u043e\u0440\u0440\u0435\u043a\u0442\u043d\u043e. \u0418\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0439\u0442\u0435 ")).formatted(Formatting.RED).append((Text)Text.literal((String)string2).formatted(new Formatting[]{Formatting.RED, Formatting.BOLD})).append((Text)Text.literal((String)" \u0447\u0442\u043e\u0431\u044b \u043f\u043e\u0441\u043c\u043e\u0442\u0440\u0435\u0442\u044c \u0441\u043f\u0438\u0441\u043e\u043a \u043a\u043e\u043c\u0430\u043d\u0434.").formatted(Formatting.RED));
        if (commandManager == null) {
            ChatMessage.brandmessage((Text)mutableText);
            return;
        }
        commandManager.sendRaw((Text)ChatMessage.brandmessage().copy().append((Text)Text.literal((String)"\u2192 ").formatted(Formatting.DARK_GRAY)).append((Text)mutableText));
    }
}

