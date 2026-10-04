package rtx.nv.api.mods.accountswitcher.ias.utils;

public final class IUtils {
    public static boolean isNullOrBlank(String str) {
        return str == null || str.isBlank();
    }

    public static boolean canUseSunServer() {
        return true;
    }
}