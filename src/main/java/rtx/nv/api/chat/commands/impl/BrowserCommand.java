package rtx.nv.api.chat.commands.impl;

import java.util.Arrays;
import java.util.List;
import net.minecraft.util.Formatting;
import rtx.nv.api.chat.commands.Command;

public final class BrowserCommand extends Command {
    public BrowserCommand() {
        super("browser", "Отправить команду /browser на сервер", new String[0]);
    }

    @Override
    public void execute(String name, String[] args) {
        if (this.mc.player != null && this.mc.player.networkHandler != null) {
            this.mc.player.networkHandler.sendChatCommand("browser");
            this.logDirect("Команда /browser отправлена на сервер.", Formatting.GREEN);
        } else {
            this.logDirect("Вы не находитесь на сервере.", Formatting.RED);
        }
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("Отправка /browser на сервер.", "Использование:", "> browser");
    }
}
