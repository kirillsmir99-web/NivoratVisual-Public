package rtx.nv.utils.net;

import java.util.Set;

public final class NvHosts {
    public static final String SOCIAL_HOST = "185.56.162.195";
    public static final Set<String> KNOWN_ELARION_HOSTS = Set.of("213.171.18.144", "185.56.162.195", "5.83.140.252");

    private NvHosts() {
    }

    public static boolean isKnownElarionHost(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        for (String known : KNOWN_ELARION_HOSTS) {
            if (matchesHost(host, known)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesHost(String text, String targetIp) {
        int idx = 0;
        while ((idx = text.indexOf(targetIp, idx)) != -1) {
            boolean validStart = (idx == 0) || !isIpChar(text.charAt(idx - 1));
            int end = idx + targetIp.length();
            boolean validEnd = (end == text.length()) || !isIpChar(text.charAt(end));
            if (validStart && validEnd) {
                return true;
            }
            idx += targetIp.length();
        }
        return false;
    }

    private static boolean isIpChar(char c) {
        return (c >= '0' && c <= '9') || c == '.';
    }
}
