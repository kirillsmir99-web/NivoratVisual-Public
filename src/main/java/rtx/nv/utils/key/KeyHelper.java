package rtx.nv.utils.key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KeyHelper {
    private static final Map<String, Integer> NAME_TO_CODE = KeyHelper.buildNameMap();
    private static final Map<Integer, String> CODE_TO_NAME = KeyHelper.buildCodeMap();
    private static final Map<Integer, String> CODE_TO_SHORT = KeyHelper.buildShortMap();

    private KeyHelper() {
    }

    public static List<String> getAllKeyNames() {
        return new ArrayList<String>(NAME_TO_CODE.keySet());
    }

    public static String getShortName(int n) {
        if (n == -1 || n < 0) {
            return "NONE";
        }
        if (n >= 0 && n <= 7) {
            return "M" + (n - 0 + 1);
        }
        if (n >= 290 && n <= 314) {
            return "F" + (n - 290 + 1);
        }
        if (n >= 320 && n <= 329) {
            return "NUM" + (n - 320);
        }
        String string = CODE_TO_SHORT.get(n);
        return string != null ? string : KeyHelper.getKeyName(n);
    }

    public static int getKeyCode(String string) {
        if (string == null || string.isBlank()) {
            return -1;
        }
        String string2 = string.trim().toUpperCase();
        Integer n = NAME_TO_CODE.get(string2);
        return n != null ? n : -1;
    }

    public static String getKeyName(int n) {
        if (n == -1) {
            return "NONE";
        }
        if (n == 1000) {
            return "SCROLL_UP";
        }
        if (n == 1001) {
            return "SCROLL_DOWN";
        }
        if (n == 1002) {
            return "MMB";
        }
        String string = CODE_TO_NAME.get(n);
        return string != null ? string : "KEY_" + n;
    }

    public static boolean isMouse(int n) {
        return (n >= 0 && n <= 7) || n == 1002;
    }

    private static Map<String, Integer> buildNameMap() {
        int n;
        LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<String, Integer>();
        linkedHashMap.put("MOUSE1", 0);
        linkedHashMap.put("MOUSE2", 1);
        linkedHashMap.put("MOUSE3", 2);
        linkedHashMap.put("MOUSE4", 3);
        linkedHashMap.put("MOUSE5", 4);
        linkedHashMap.put("MOUSE6", 5);
        linkedHashMap.put("MOUSE7", 6);
        linkedHashMap.put("MOUSE8", 7);
        linkedHashMap.put("LMB", 0);
        linkedHashMap.put("RMB", 1);
        linkedHashMap.put("MMB", 2);
        linkedHashMap.put("MIDDLE_MOUSE", 2);
        linkedHashMap.put("MOUSE_MIDDLE", 2);
        linkedHashMap.put("WHEEL", 2);
        linkedHashMap.put("MOUSE_WHEEL", 2);
        linkedHashMap.put("SCROLL_UP", 1000);
        linkedHashMap.put("SCROLLUP", 1000);
        linkedHashMap.put("WHEEL_UP", 1000);
        linkedHashMap.put("MOUSE_WHEEL_UP", 1000);
        linkedHashMap.put("SCROLL_DOWN", 1001);
        linkedHashMap.put("SCROLLDOWN", 1001);
        linkedHashMap.put("WHEEL_DOWN", 1001);
        linkedHashMap.put("MOUSE_WHEEL_DOWN", 1001);
        for (n = 0; n < 26; ++n) {
            String string = String.valueOf((char)(65 + n));
            linkedHashMap.put(string, 65 + n);
        }
        linkedHashMap.put("0", 48);
        linkedHashMap.put("1", 49);
        linkedHashMap.put("2", 50);
        linkedHashMap.put("3", 51);
        linkedHashMap.put("4", 52);
        linkedHashMap.put("5", 53);
        linkedHashMap.put("6", 54);
        linkedHashMap.put("7", 55);
        linkedHashMap.put("8", 56);
        linkedHashMap.put("9", 57);
        for (n = 1; n <= 25; ++n) {
            linkedHashMap.put("F" + n, 290 + n - 1);
        }
        linkedHashMap.put("SPACE", 32);
        linkedHashMap.put("ESCAPE", 256);
        linkedHashMap.put("ESC", 256);
        linkedHashMap.put("ENTER", 257);
        linkedHashMap.put("TAB", 258);
        linkedHashMap.put("BACKSPACE", 259);
        linkedHashMap.put("INSERT", 260);
        linkedHashMap.put("DELETE", 261);
        linkedHashMap.put("DEL", 261);
        linkedHashMap.put("RIGHT", 262);
        linkedHashMap.put("LEFT", 263);
        linkedHashMap.put("DOWN", 264);
        linkedHashMap.put("UP", 265);
        linkedHashMap.put("PAGE_UP", 266);
        linkedHashMap.put("PAGE_DOWN", 267);
        linkedHashMap.put("HOME", 268);
        linkedHashMap.put("END", 269);
        linkedHashMap.put("CAPS_LOCK", 280);
        linkedHashMap.put("SCROLL_LOCK", 281);
        linkedHashMap.put("NUM_LOCK", 282);
        linkedHashMap.put("PRINT_SCREEN", 283);
        linkedHashMap.put("PAUSE", 284);
        linkedHashMap.put("LEFT_SHIFT", 340);
        linkedHashMap.put("LSHIFT", 340);
        linkedHashMap.put("LEFT_CONTROL", 341);
        linkedHashMap.put("LCTRL", 341);
        linkedHashMap.put("LEFT_ALT", 342);
        linkedHashMap.put("LALT", 342);
        linkedHashMap.put("RIGHT_SHIFT", 344);
        linkedHashMap.put("RSHIFT", 344);
        linkedHashMap.put("RIGHT_CONTROL", 345);
        linkedHashMap.put("RCTRL", 345);
        linkedHashMap.put("RIGHT_ALT", 346);
        linkedHashMap.put("RALT", 346);
        linkedHashMap.put("LEFT_SUPER", 343);
        linkedHashMap.put("LSUPER", 343);
        linkedHashMap.put("RIGHT_SUPER", 347);
        linkedHashMap.put("RSUPER", 347);
        linkedHashMap.put("MENU", 348);
        linkedHashMap.put("GRAVE", 96);
        linkedHashMap.put("MINUS", 45);
        linkedHashMap.put("EQUAL", 61);
        linkedHashMap.put("SEMICOLON", 59);
        linkedHashMap.put("APOSTROPHE", 39);
        linkedHashMap.put("COMMA", 44);
        linkedHashMap.put("PERIOD", 46);
        linkedHashMap.put("SLASH", 47);
        linkedHashMap.put("BACKSLASH", 92);
        linkedHashMap.put("NUM0", 320);
        linkedHashMap.put("NUM1", 321);
        linkedHashMap.put("NUM2", 322);
        linkedHashMap.put("NUM3", 323);
        linkedHashMap.put("NUM4", 324);
        linkedHashMap.put("NUM5", 325);
        linkedHashMap.put("NUM6", 326);
        linkedHashMap.put("NUM7", 327);
        linkedHashMap.put("NUM8", 328);
        linkedHashMap.put("NUM9", 329);
        linkedHashMap.put("NUM_DECIMAL", 330);
        linkedHashMap.put("NUM_DIVIDE", 331);
        linkedHashMap.put("NUM_MULTIPLY", 332);
        linkedHashMap.put("NUM_SUBTRACT", 333);
        linkedHashMap.put("NUM_ADD", 334);
        linkedHashMap.put("NUM_ENTER", 335);
        linkedHashMap.put("NUM_EQUAL", 336);
        return linkedHashMap;
    }

    private static Map<Integer, String> buildCodeMap() {
        HashMap<Integer, String> hashMap = new HashMap<Integer, String>();
        KeyHelper.buildNameMap().forEach((string, n) -> hashMap.putIfAbsent((Integer)n, (String)string));
        hashMap.put(256, "ESC");
        hashMap.put(257, "ENT");
        hashMap.put(259, "BKSP");
        hashMap.put(260, "INS");
        hashMap.put(261, "DEL");
        hashMap.put(266, "PGUP");
        hashMap.put(267, "PGDN");
        hashMap.put(280, "CAPS");
        hashMap.put(281, "SCRLK");
        hashMap.put(282, "NUMLK");
        hashMap.put(283, "PRTSC");
        hashMap.put(343, "LWIN");
        hashMap.put(347, "RWIN");
        hashMap.put(96, "`");
        hashMap.put(45, "-");
        hashMap.put(61, "=");
        hashMap.put(91, "[");
        hashMap.put(93, "]");
        hashMap.put(92, "\\");
        hashMap.put(59, ";");
        hashMap.put(39, "'");
        hashMap.put(44, ",");
        hashMap.put(46, ".");
        hashMap.put(47, "/");
        hashMap.put(330, "NUM.");
        hashMap.put(331, "NUM/");
        hashMap.put(332, "NUM*");
        hashMap.put(333, "NUM-");
        hashMap.put(334, "NUM+");
        hashMap.put(335, "NUMENT");
        hashMap.put(336, "NUM=");
        hashMap.put(161, "WORLD1");
        hashMap.put(162, "WORLD2");
        hashMap.put(1000, "SU");
        hashMap.put(1001, "SD");
        hashMap.put(1002, "M3");
        return hashMap;
    }

    private static Map<Integer, String> buildShortMap() {
        HashMap<Integer, String> hashMap = new HashMap<Integer, String>();
        hashMap.put(344, "RSH");
        hashMap.put(340, "LSH");
        hashMap.put(345, "RCT");
        hashMap.put(341, "LCT");
        hashMap.put(346, "RALT");
        hashMap.put(342, "LALT");
        hashMap.put(32, "SPC");
        hashMap.put(1000, "SCR\u2191");
        hashMap.put(1001, "SCR\u2193");
        hashMap.put(1000, "SU");
        hashMap.put(1001, "SD");
        hashMap.put(1002, "M3");
        return hashMap;
    }

    public static boolean isScroll(int n) {
        return n == 1000 || n == 1001;
    }
}

