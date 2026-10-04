package rtx.nv.api.chat.commands.impl;

import java.util.Arrays;
import java.util.List;
import net.minecraft.util.Formatting;
import rtx.nv.api.chat.commands.Command;

public final class BoostCommand extends Command {
    public BoostCommand() {
        super("boost", "Отправить команду /boost на сервер", new String[0]);
    }

    @Override
    public void execute(String name, String[] args) {
        if (this.mc.player != null && this.mc.player.networkHandler != null) {
            this.mc.player.networkHandler.sendChatCommand("boost");
            this.logDirect("Команда /boost отправлена на сервер.", Formatting.GREEN);
        } else {
            this.logDirect("Вы не находитесь на сервере.", Formatting.RED);
        }
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("Отправка /boost на сервер.", "Использование:", "> boost");
    }
}
