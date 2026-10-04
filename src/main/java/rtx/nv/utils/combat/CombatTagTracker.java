package rtx.nv.utils.combat;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.network.PacketEvent;
import rtx.nv.api.events.impl.player.AttackEntityEvent;
import rtx.nv.utils.network.Network;

public final class CombatTagTracker {
    private static final CombatTagTracker INSTANCE = new CombatTagTracker();
    private static volatile long combatEndMs = 0L;
    private static volatile long lastDetectedMs = 0L;

    private static final Pattern SECONDS_PATTERN = Pattern.compile("(?::|\\b)(\\d{1,3})\\s*(?:с|сек|s|sec|\\b)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    static {
        EventBus.get().subscribe(INSTANCE);
    }

    public static CombatTagTracker get() {
        return INSTANCE;
    }

    public static boolean isInCombat() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            combatEndMs = 0L;
            return false;
        }
        return System.currentTimeMillis() < combatEndMs;
    }

    public static long getRemainingMs() {
        long rem = combatEndMs - System.currentTimeMillis();
        return Math.max(0L, rem);
    }

    public static int getRemainingSeconds() {
        long rem = getRemainingMs();
        return (int) Math.ceil((double) rem / 1000.0);
    }

    public static String getFormattedRemaining() {
        int sec = getRemainingSeconds();
        if (sec >= 60) {
            return (sec / 60) + ":" + String.format(Locale.ROOT, "%02d", sec % 60);
        }
        return sec + "с";
    }

    public static String getFormattedCombatTag() {
        return "КТ " + getFormattedRemaining();
    }

    public static void setCombatDurationSeconds(int seconds) {
        if (seconds <= 0) {
            reset();
            return;
        }
        long now = System.currentTimeMillis();
        long newEnd = now + (long) seconds * 1000L;
        if (newEnd > combatEndMs || (now - lastDetectedMs > 1500L)) {
            combatEndMs = newEnd;
        }
        lastDetectedMs = now;
    }

    public static void reset() {
        combatEndMs = 0L;
    }

    public static void onActionBarMessage(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        parseMessage(text);
    }

    public static boolean parseMessage(String raw) {
        if (raw == null) {
            return false;
        }
        String clean = stripFormatting(raw).trim();
        if (clean.isEmpty()) {
            return false;
        }
        String lower = clean.toLowerCase(Locale.ROOT);

        if (lower.contains("вы вышли из боя") || lower.contains("режим боя окончен") || lower.contains("бой окончен")
            || lower.contains("combat ended") || lower.contains("no longer in combat")) {
            reset();
            return true;
        }

        boolean hasCombatMarker = (lower.contains("режим") && (lower.contains("боя") || lower.contains("бою") || lower.contains("бое") || lower.contains("бой")))
            || lower.contains("в бою") || lower.contains("в бoю")
            || lower.contains("кт:") || lower.contains("кт :") || lower.contains("[кт]") || lower.contains("(кт)") || lower.contains(" кт ")
            || lower.contains("бой:") || lower.contains("бой :") || lower.contains("не выходите")
            || lower.contains("combat tag") || lower.contains("in combat") || (lower.contains("combat") && !lower.contains("cooldown"))
            || lower.contains("pvp");

        if (!hasCombatMarker) {
            return false;
        }

        Matcher matcher = SECONDS_PATTERN.matcher(clean);
        int parsedSeconds = -1;
        while (matcher.find()) {
            try {
                int val = Integer.parseInt(matcher.group(1));
                if (val > 0 && val <= 300) {
                    parsedSeconds = val;
                    break;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (parsedSeconds > 0) {
            setCombatDurationSeconds(parsedSeconds);
        } else {
            if (!isInCombat()) {
                setCombatDurationSeconds(15);
            }
        }
        return true;
    }

    @EventHandler
    private void onPacket(PacketEvent event) {
        if (!event.isReceive()) {
            return;
        }
        Object packet = event.getPacket();
        if (packet instanceof OverlayMessageS2CPacket overlayPacket) {
            if (overlayPacket.text() != null) {
                onActionBarMessage(overlayPacket.text().getString());
            }
        } else if (packet instanceof GameMessageS2CPacket gamePacket) {
            if (gamePacket.content() != null) {
                parseMessage(gamePacket.content().getString());
            }
        }
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (event.isSynthetic()) {
            return;
        }
        Entity target = event.getTarget();
        if (target instanceof PlayerEntity && !Network.isVanilla()) {
            if (!isInCombat()) {
                setCombatDurationSeconds(15);
            }
        }
    }

    private static String stripFormatting(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        boolean skipNext = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (skipNext) {
                skipNext = false;
                continue;
            }
            if (c == '§' || c == '&') {
                if (i + 1 < input.length()) {
                    char next = Character.toLowerCase(input.charAt(i + 1));
                    if ((next >= '0' && next <= '9') || (next >= 'a' && next <= 'f') || (next >= 'k' && next <= 'r') || next == 'x') {
                        skipNext = true;
                        continue;
                    }
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
