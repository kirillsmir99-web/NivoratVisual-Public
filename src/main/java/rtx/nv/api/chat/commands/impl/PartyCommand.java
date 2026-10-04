package rtx.nv.api.chat.commands.impl;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Formatting;
import rtx.nv.api.chat.commands.Command;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Utils.Party;
import rtx.nv.api.party.PartyChat;
import rtx.nv.api.party.PartyClient;
import rtx.nv.api.party.PartyMember;
import rtx.nv.api.party.PartySnapshot;
import rtx.nv.utils.storage.friend.FriendUtils;

public final class PartyCommand
extends Command {
    public PartyCommand() {
        super("party", "\u0421\u0438\u0441\u0442\u0435\u043c\u0430 Party: create / invite / info / kick / leave / disband.", "p");
    }

    @Override
    public void execute(String string, String[] stringArray) {
        if (stringArray.length == 0) {
            this.usage();
            return;
        }
        switch (stringArray[0].toLowerCase()) {
            case "create": {
                this.handleCreate(stringArray);
                break;
            }
            case "invite": {
                this.handleInvite(stringArray);
                break;
            }
            case "leave": {
                this.handleLeave();
                break;
            }
            case "disband": {
                this.handleDisband();
                break;
            }
            case "info": {
                this.handleInfo();
                break;
            }
            case "chat": 
            case "c": 
            case "msg": {
                this.handleChat(stringArray);
                break;
            }
            case "kick": {
                this.handleKick(stringArray);
                break;
            }
            case "accept": {
                this.handleRespond(stringArray, true);
                break;
            }
            case "decline": {
                this.handleRespond(stringArray, false);
                break;
            }
            default: {
                this.usage();
            }
        }
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("\u0421\u0438\u0441\u0442\u0435\u043c\u0430 Party \u0434\u043b\u044f \u0434\u0440\u0443\u0437\u0435\u0439 (\u0434\u043e 10 \u0447\u0435\u043b\u043e\u0432\u0435\u043a).", "> party create <\u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435>", "> party invite <\u043d\u0438\u043a>", "> party info", "> party chat <\u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435>", "> party kick <\u043d\u0438\u043a>", "> party leave", "> party disband");
    }

    @Override
    public Stream<String> tabComplete(String string3, String[] stringArray) {
        if (stringArray.length == 1) {
            return Stream.of("create", "invite", "info", "chat", "kick", "leave", "disband").filter(string -> string.startsWith(stringArray[0].toLowerCase()));
        }
        if (stringArray.length == 2) {
            String string4 = stringArray[0].toLowerCase();
            String string5 = stringArray[1].toLowerCase();
            if (string4.equals("invite")) {
                return this.inviteCandidates().stream().filter(string2 -> string2.toLowerCase().startsWith(string5));
            }
            if (string4.equals("kick")) {
                return this.memberNames().stream().filter(string2 -> string2.toLowerCase().startsWith(string5));
            }
        }
        return Stream.empty();
    }

    private void handleDisband() {
        if (this.ensureLink()) {
            PartyClient.INSTANCE.disband();
        }
    }

    private List<String> memberNames() {
        ArrayList<String> arrayList = new ArrayList<String>();
        PartySnapshot partySnapshot = PartyClient.INSTANCE.snapshot();
        for (PartyMember partyMember : partySnapshot.members()) {
            if (partyMember.leader()) continue;
            arrayList.add(partyMember.name());
        }
        return arrayList;
    }

    private void handleRespond(String[] stringArray, boolean bl) {
        if (stringArray.length < 2) {
            this.logDirect("\u041d\u0435\u0442 \u0438\u0434\u0435\u043d\u0442\u0438\u0444\u0438\u043a\u0430\u0442\u043e\u0440\u0430 \u043f\u0440\u0438\u0433\u043b\u0430\u0448\u0435\u043d\u0438\u044f.", Formatting.RED);
            return;
        }
        if (this.ensureLink()) {
            PartyClient.INSTANCE.respondInvite(stringArray[1].trim(), bl);
        }
    }

    private void handleInvite(String[] stringArray) {
        if (stringArray.length < 2) {
            this.logDirect("\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435: party invite <\u043d\u0438\u043a>", Formatting.RED);
            return;
        }
        String string = stringArray[1].trim();
        if (string.isEmpty()) {
            this.logDirect("\u0423\u043a\u0430\u0436\u0438\u0442\u0435 \u043d\u0438\u043a \u0438\u0433\u0440\u043e\u043a\u0430.", Formatting.RED);
            return;
        }
        if (this.ensureLink()) {
            PartyClient.INSTANCE.inviteUser(string);
        }
    }

    private void handleCreate(String[] stringArray) {
        if (stringArray.length < 2) {
            this.logDirect("\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435: party create <\u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435>", Formatting.RED);
            return;
        }
        String string = String.join((CharSequence)" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)).trim();
        if (string.isEmpty()) {
            this.logDirect("\u0423\u043a\u0430\u0436\u0438\u0442\u0435 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 Party.", Formatting.RED);
            return;
        }
        if (this.ensureLink()) {
            PartyClient.INSTANCE.create(string);
        }
    }

    private List<String> inviteCandidates() {
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>();
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.player != null && minecraftClient.player.networkHandler != null) {
            for (PlayerListEntry playerListEntry : minecraftClient.player.networkHandler.getPlayerList()) {
                GameProfile gameProfile = playerListEntry.getProfile();
                if (gameProfile == null || gameProfile.name() == null) continue;
                linkedHashSet.add(gameProfile.name());
            }
        }
        linkedHashSet.addAll(FriendUtils.getFriendNames());
        if (minecraftClient.getSession() != null && minecraftClient.getSession().getUsername() != null) {
            linkedHashSet.remove(minecraftClient.getSession().getUsername());
        }
        return new ArrayList<String>(linkedHashSet);
    }

    private void handleLeave() {
        if (this.ensureLink()) {
            PartyClient.INSTANCE.leave();
        }
    }

    private void handleInfo() {
        PartyChat.renderInfo((PartySnapshot)PartyClient.INSTANCE.snapshot());
    }

    private void handleChat(String[] stringArray) {
        if (stringArray.length < 2) {
            this.logDirect("\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435: party chat <\u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435>", Formatting.RED);
            return;
        }
        String string = String.join((CharSequence)" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)).trim();
        if (string.isEmpty()) {
            this.logDirect("\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435.", Formatting.RED);
            return;
        }
        if (this.ensureLink()) {
            PartyClient.INSTANCE.sendChat(string);
        }
    }

    private void handleKick(String[] stringArray) {
        if (stringArray.length < 2) {
            this.logDirect("\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435: party kick <\u043d\u0438\u043a>", Formatting.RED);
            return;
        }
        String string = stringArray[1].trim();
        if (string.isEmpty()) {
            this.logDirect("\u0423\u043a\u0430\u0436\u0438\u0442\u0435 \u043d\u0438\u043a \u0438\u0433\u0440\u043e\u043a\u0430.", Formatting.RED);
            return;
        }
        if (this.ensureLink()) {
            PartyClient.INSTANCE.kick(string);
        }
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

