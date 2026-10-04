package rtx.nv.utils.net;

import java.net.http.WebSocket;

public final class WebSocketMessages {
    public static final int MAX_TEXT_CHARS = 1024 * 1024;
    private WebSocketMessages() {}

    public static boolean append(StringBuilder buffer, WebSocket socket, CharSequence fragment) {
        if (fragment.length() > MAX_TEXT_CHARS - buffer.length()) {
            buffer.setLength(0);
            socket.sendClose(1009, "message too large");
            return false;
        }
        buffer.append(fragment);
        return true;
    }
}
