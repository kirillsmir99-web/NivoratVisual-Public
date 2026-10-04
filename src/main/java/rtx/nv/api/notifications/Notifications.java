package rtx.nv.api.notifications;

import net.minecraft.sound.SoundEvent;

public final class Notifications {
    private Notifications() {}

    public static void push(String title, String message, long durationMs, SoundEvent sound) {
        String text = (title != null && !title.isBlank() ? title + ": " : "") + (message != null ? message : "");
        rtx.nv.api.modules.impl.Interface.NotificationsModule.notify(text, durationMs);
    }

    public static void push(String title, String message, long durationMs) {
        push(title, message, durationMs, null);
    }
}
