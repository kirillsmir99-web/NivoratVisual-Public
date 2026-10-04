package rtx.nv.api.party;

import rtx.nv.utils.chat.ChatMessage;

public final class PartyChat {
    public static void renderInfo(PartySnapshot snapshot) {
        if (snapshot == null || !snapshot.inParty()) {
            ChatMessage.brandmessage("You are not in a party.");
            return;
        }
        ChatMessage.brandmessage("Party: " + snapshot.name() + " (Leader: " + snapshot.leader() + ")");
    }

    public static void printInvite(String from, String party, String inviteId) {
        ChatMessage.brandmessage("Invite from " + from + " to party " + party + " (ID: " + inviteId + ")");
    }

    public static void printNotice(String level, String message) {
        ChatMessage.brandmessage("[" + level + "] " + message);
    }

    public static void printChat(String from, String text) {
        ChatMessage.brandmessage("<" + from + "> " + text);
    }
}