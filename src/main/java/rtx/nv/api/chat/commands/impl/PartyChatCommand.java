package rtx.nv.api.chat.commands.impl;
import java.util.Arrays;
import java.util.List;
import net.minecraft.util.Formatting;
import rtx.nv.api.chat.commands.Command;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Utils.Party;
import rtx.nv.api.party.PartyClient;

public final class PartyChatCommand
extends Command {
    public PartyChatCommand() {
        super("pc", "Party-\u0447\u0430\u0442: \u043d\u0430\u043f\u0438\u0441\u0430\u0442\u044c \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435 \u0443\u0447\u0430\u0441\u0442\u043d\u0438\u043a\u0430\u043c Party.", "pchat");
    }

    @Override
    public void execute(String string, String[] stringArray) {
        if (stringArray.length == 0) {
            this.logDirect("\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435: pc <\u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435>", Formatting.RED);
            return;
        }
        String string2 = String.join((CharSequence)" ", Arrays.copyOfRange(stringArray, 0, stringArray.length)).trim();
        if (string2.isEmpty()) {
            this.logDirect("\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435.", Formatting.RED);
            return;
        }
        if (!this.ensureLink()) {
            return;
        }
        PartyClient.INSTANCE.sendChat(string2);
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("Party-\u0447\u0430\u0442 \u2014 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435 \u0432\u0438\u0434\u044f\u0442 \u0442\u043e\u043b\u044c\u043a\u043e \u0443\u0447\u0430\u0441\u0442\u043d\u0438\u043a\u0438 \u0432\u0430\u0448\u0435\u0439 Party.", "> pc <\u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435>", "\u0410\u043d\u0430\u043b\u043e\u0433: party chat <\u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435>");
    }

    private boolean ensureLink() {
        if (PartyClient.INSTANCE.isConnected()) {
            return true;
        }
        Party party = ModuleManager.get().get(Party.class);
        if (party != null && !party.isEnabled()) {
            party.enable();
            this.logDirect("\u041c\u043e\u0434\u0443\u043b\u044c Party \u0432\u043a\u043b\u044e\u0447\u0451\u043d \u2014 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u044e\u0441\u044c \u043a \u0441\u0435\u0440\u0432\u0435\u0440\u0443. \u041f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u0435 \u043a\u043e\u043c\u0430\u043d\u0434\u0443 \u0447\u0435\u0440\u0435\u0437 \u043f\u0430\u0440\u0443 \u0441\u0435\u043a\u0443\u043d\u0434.", Formatting.YELLOW);
        } else {
            this.logDirect("\u041d\u0435\u0442 \u0441\u0432\u044f\u0437\u0438 \u0441 Party-\u0441\u0435\u0440\u0432\u0435\u0440\u043e\u043c. \u041f\u043e\u043f\u0440\u043e\u0431\u0443\u0439\u0442\u0435 \u043f\u043e\u0437\u0436\u0435.", Formatting.RED);
        }
        return false;
    }
}

