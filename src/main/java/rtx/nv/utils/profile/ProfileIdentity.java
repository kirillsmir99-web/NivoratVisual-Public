package rtx.nv.utils.profile;
import rtx.nv.api.profile.Profile;

public final class ProfileIdentity {
    private ProfileIdentity() {
    }

    public static int uid() {
        return Math.max(0, Profile.getUid());
    }

    public static String username(String string) {
        String string2 = Profile.getUsername();
        if (string2 != null && !string2.isBlank() && !"Guest".equalsIgnoreCase(string2)) {
            return string2;
        }
        return string;
    }

    public static String avatarUrl() {
        String string = Profile.getAvatarUrl();
        if (string != null && !string.isBlank()) {
            return string;
        }
        return null;
    }
}

