package rtx.nv.api.ui.window;

import net.minecraft.client.MinecraftClient;
import rtx.nv.utils.profile.ProfileIdentity;

public final class WindowTitleAnimation {
    private static final long STEP_MS = 45L;
    private static final long HOLD_MS = 4000L;
    private static final int ERASE_PER_STEP = 2;
    private static final WindowTitleAnimation INSTANCE = new WindowTitleAnimation();
    private String visible = "";
    private String target = "";
    private boolean erasing;
    private boolean uidPhase;
    private long nextStepAt;
    private String lastPushed;

    private WindowTitleAnimation() {
    }

    public static WindowTitleAnimation get() {
        return INSTANCE;
    }

    private void push() {
        String title = this.currentTitle();
        if (title.equals(this.lastPushed)) {
            return;
        }
        this.lastPushed = title;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getWindow() != null) {
            mc.getWindow().setTitle(title);
        }
    }

    public String currentTitle() {
        return "Nivorat Visual (NV) v1.0.1  \u2758  " + this.visible;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        if (this.target.isEmpty()) {
            this.target = phaseText(this.uidPhase);
        }
        if (now < this.nextStepAt) {
            return;
        }
        if (this.erasing) {
            if (this.visible.isEmpty()) {
                this.erasing = false;
                this.uidPhase = !this.uidPhase;
                this.target = phaseText(this.uidPhase);
                this.nextStepAt = now + STEP_MS;
            } else {
                int len = Math.max(0, this.visible.length() - ERASE_PER_STEP);
                this.visible = this.visible.substring(0, len);
                this.nextStepAt = now + STEP_MS;
            }
        } else if (this.visible.equals(this.target)) {
            this.erasing = true;
            this.nextStepAt = now + HOLD_MS;
        } else {
            if (this.visible.length() < this.target.length()) {
                this.visible = this.target.substring(0, this.visible.length() + 1);
            }
            this.nextStepAt = now + STEP_MS;
        }
        this.push();
    }

    private static String profileName() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getSession() != null && mc.getSession().getUsername() != null && !mc.getSession().getUsername().isBlank()) {
                return mc.getSession().getUsername();
            }
            return ProfileIdentity.username("Player");
        } catch (Throwable throwable) {
            return "Player";
        }
    }

    private static String phaseText(boolean bl) {
        if (bl) {
            return "by Nivorat";
        }
        return profileName() + "'s Profile";
    }
}
