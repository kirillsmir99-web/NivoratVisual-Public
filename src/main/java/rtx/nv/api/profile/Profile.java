package rtx.nv.api.profile;

public final class Profile {
    private static int uid = 1;
    private static String username = null;
    private static String avatarUrl = null;
    private static Role role = Role.USER;

    private Profile() {
    }

    public static int getUid() {
        return uid;
    }

    public static void setUid(int newUid) {
        uid = newUid;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String newUsername) {
        username = newUsername;
    }

    public static String getAvatarUrl() {
        return avatarUrl;
    }

    public static void setAvatarUrl(String newAvatarUrl) {
        avatarUrl = newAvatarUrl;
    }

    public static Role getRole() {
        return role;
    }

    public static void setRole(Role newRole) {
        role = newRole;
    }
}
