package rtx.nv.utils.network;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import rtx.nv.mixin.accessor.BossHealthOverlayAccessor;

public final class Network {
    private static final long SERVER_DETECT_INTERVAL_MS = 1000L;
    private static final long PVP_SCAN_INTERVAL_MS = 250L;
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static long lastPvpMs;
    private static long lastPvpScanMs;
    private static long lastTpsPacketNs;
    private static long lastServerDetectMs;
    private static volatile boolean singleplayerWorld;
    private static volatile float tps;
    private static volatile String server;
    private static volatile int realTimePing;
    private static volatile long lastPingRequestMs;
    private static volatile long lastPingUpdateMs;

    private Network() {
    }

    static {
        singleplayerWorld = true;
        tps = 20.0f;
        server = "Vanilla";
    }

    private static String clean(String string) {
        return Network.normalizeServerToken(string).replace('\u00a0', ' ').trim();
    }

    public static void handlePacket(Packet<?> packet) {
        if (singleplayerWorld) {
            Network.resetTpsState();
            return;
        }
        if (packet instanceof WorldTimeUpdateS2CPacket) {
            Network.handleTimePacket();
        } else if (packet instanceof PingResultS2CPacket pingResult) {
            long diff = Util.getMeasuringTimeMs() - pingResult.startTime();
            if (diff >= 0 && diff < 10000) {
                realTimePing = (int) diff;
                lastPingUpdateMs = System.currentTimeMillis();
            }
        } else if (packet instanceof CommonPingS2CPacket) {
            lastPingUpdateMs = System.currentTimeMillis();
        }
    }

    public static float getResolvedHealth(LivingEntity livingEntity, boolean bl) {
        if (livingEntity == null) {
            return 0.0f;
        }
        if (livingEntity instanceof PlayerEntity) {
            float f;
            PlayerEntity playerEntity = (PlayerEntity)livingEntity;
            if (Network.MC.player != null && playerEntity != Network.MC.player && !singleplayerWorld && (f = Network.resolveServerHealth(playerEntity)) >= 0.0f) {
                return f;
            }
        }
        float f = Math.max(0.0f, livingEntity.getHealth());
        return bl ? f + Math.max(0.0f, livingEntity.getAbsorptionAmount()) : f;
    }

    private static int parseHealthFromName(String string, String string2) {
        int n;
        int n2;
        if (string == null || string.isEmpty()) {
            return -1;
        }
        String string3 = Network.stripSectionCodes(string);
        if (string2 != null && !string2.isEmpty()) {
            string3 = string3.replace(string2, " ");
        }
        String string4 = string3.toLowerCase(Locale.ROOT);
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (int i = 0; i < string3.length(); i += Character.charCount(n2)) {
            n2 = string3.codePointAt(i);
            if (!Network.isHeart(n2)) continue;
            arrayList.add(i);
        }
        for (String string5 : new String[]{"\u0437\u0434\u043e\u0440\u043e\u0432", "health", "heart", "\u0445\u043f", "hp", "\u0436\u0438\u0437\u043d"}) {
            n = string4.indexOf(string5);
            while (n >= 0) {
                arrayList.add(n);
                n = string4.indexOf(string5, n + 1);
            }
        }
        if (arrayList.isEmpty()) {
            return -1;
        }
        int n3 = -1;
        n2 = Integer.MAX_VALUE;
        int n4 = 0;
        while (n4 < string3.length()) {
            if (Character.isDigit(string3.charAt(n4))) {
                int n5;
                int n6 = n4;
                while (n4 < string3.length() && Character.isDigit(string3.charAt(n4))) {
                    ++n4;
                }
                n = (n6 + n4) / 2;
                try {
                    n5 = Integer.parseInt(string3.substring(n6, n4));
                }
                catch (NumberFormatException numberFormatException) {
                    continue;
                }
                Iterator iterator = arrayList.iterator();
                while (iterator.hasNext()) {
                    int n7 = (Integer)iterator.next();
                    int n8 = Math.abs(n - n7);
                    if (n8 >= n2) continue;
                    n2 = n8;
                    n3 = n5;
                }
                continue;
            }
            ++n4;
        }
        return n3;
    }

    private static boolean looksLikeHealth(ScoreboardObjective scoreboardObjective) {
        if (scoreboardObjective.getRenderType() == ScoreboardCriterion.RenderType.HEARTS) {
            return true;
        }
        String string = (scoreboardObjective.getName() + " " + scoreboardObjective.getDisplayName().getString()).toLowerCase(Locale.ROOT);
        if (string.contains("health") || string.contains("heart") || string.contains("hp") || string.contains("\u0437\u0434\u043e\u0440\u043e\u0432") || string.contains("\u0445\u043f") || string.contains("\u0436\u0438\u0437\u043d")) {
            return true;
        }
        for (int i = 0; i < string.length(); ++i) {
            if (!Network.isHeart(string.codePointAt(i))) continue;
            return true;
        }
        return false;
    }

    private static String stripSectionCodes(String string) {
        StringBuilder stringBuilder = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c == '\u00a7' && i + 1 < string.length()) {
                ++i;
                continue;
            }
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

    public static String formatHealthValue(float f) {
        if (f >= 100.0f) {
            return Integer.toString((int)f);
        }
        if (f >= 10.0f) {
            return String.format(Locale.ROOT, "%.1f", Float.valueOf(f));
        }
        return String.format(Locale.ROOT, "%.2f", Float.valueOf(f));
    }

    public static int getAnarchyMode() {
        if (Network.MC.world == null) {
            return -1;
        }
        Scoreboard scoreboard = Network.MC.world.getScoreboard();
        ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (scoreboardObjective == null) {
            return -1;
        }
        String string = Network.clean(scoreboardObjective.getDisplayName().getString());
        int n = Network.extractNumberAfter(string, "#");
        if (n != -1) {
            return n;
        }
        for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(scoreboardObjective)) {
            String string2 = Network.clean(Team.decorateName((AbstractTeam)scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), (Text)scoreboardEntry.name()).getString());
            int n2 = Network.extractNumberAfter(string2, "#");
            if (n2 == -1) continue;
            return n2;
        }
        return -1;
    }

    private static int extractNumberAfter(String string, String string2) {
        int n = string.indexOf(string2);
        if (n == -1) {
            return -1;
        }
        int n2 = n + string2.length();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = n2; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (Character.isDigit(c)) {
                stringBuilder.append(c);
                continue;
            }
            if (!stringBuilder.isEmpty()) break;
        }
        if (stringBuilder.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(stringBuilder.toString());
        }
        catch (NumberFormatException numberFormatException) {
            return -1;
        }
    }

    public static boolean isHolyWorld() {
        return "HolyWorld".equals(Network.getServer());
    }

    public static boolean isElarion() {
        return "Elarion".equals(Network.getServer());
    }

    private static String detectServer() {
        if (MC.getNetworkHandler() == null || MC.getNetworkHandler().getServerInfo() == null) {
            return "Vanilla";
        }
        String string = Network.safeLower(Network.MC.getNetworkHandler().getServerInfo().address);
        String string2 = Network.safeLower(MC.getNetworkHandler().getBrand());
        String string3 = Network.normalizeServerToken(MC.getNetworkHandler().getBrand());
        if (string.contains("elarion") || string.contains("ellarium") || rtx.nv.utils.net.NvHosts.isKnownElarionHost(string) || string2.contains("elarion") || string2.contains("ellarium")) {
            return "Elarion";
        }
        if (string2.contains("botfilter") || string3.contains("botfilter")) {
            return "FunTime";
        }
        if (string.contains("spooky") || string.contains("pookie") || string2.contains("spooky") || string2.contains("pookie") || string3.contains("spookycore") || string3.contains("spookytime") || string3.contains("pookietime")) {
            return "SpookyTime";
        }
        if (string.contains("funtime") || string.contains("skytime") || string.contains("space-times") || string.contains("funsky")) {
            return "CopyTime";
        }
        if (string2.contains("holyworld") || string3.contains("holyworld")) {
            return "HolyWorld";
        }
        if (string.contains("reallyworld")) {
            return "ReallyWorld";
        }
        if (string.contains("gulpvp")) {
            return "GulPvP";
        }
        return "Vanilla";
    }

    private static String normalizeServerToken(String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder(string.length());
        boolean bl = false;
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (bl) {
                bl = false;
                continue;
            }
            if (c == '\u00a7') {
                bl = true;
                continue;
            }
            if (c == '\u00c2' || Character.isWhitespace(c)) continue;
            stringBuilder.append(Character.toLowerCase(c));
        }
        return stringBuilder.toString();
    }

    public static boolean isSpookyTime() {
        return "SpookyTime".equals(Network.getServer());
    }

    private static float resolveServerHealth(PlayerEntity playerEntity) {
        ReadableScoreboardScore readableScoreboardScore;
        if (Network.MC.world == null) {
            return -1.0f;
        }
        Scoreboard scoreboard = Network.MC.world.getScoreboard();
        ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
        if (scoreboardObjective != null && Network.looksLikeHealth(scoreboardObjective) && (readableScoreboardScore = scoreboard.getScore((ScoreHolder)playerEntity, scoreboardObjective)) != null && readableScoreboardScore.getScore() > 0) {
            return readableScoreboardScore.getScore();
        }
        int n = Network.parseHealthFromName(playerEntity.getDisplayName().getString(), playerEntity.getName().getString());
        return n >= 0 ? (float)n : -1.0f;
    }

    private static boolean isSingleplayerWorld() {
        return MC.isIntegratedServerRunning() || MC.isInSingleplayer();
    }

    private static void refreshServer(long l) {
        if (l - lastServerDetectMs < 1000L) {
            return;
        }
        lastServerDetectMs = l;
        server = Network.detectServer();
    }

    private static void resetTpsState() {
        lastTpsPacketNs = 0L;
        tps = 20.0f;
    }

    private static void updatePvpState(long l) {
        if (l - lastPvpScanMs < 250L) {
            return;
        }
        lastPvpScanMs = l;
        if (Network.MC.inGameHud == null || Network.MC.inGameHud.getBossBarHud() == null) {
            return;
        }
        Map<UUID, ClientBossBar> map = ((BossHealthOverlayAccessor)Network.MC.inGameHud.getBossBarHud()).nv_getEvents();
        for (ClientBossBar clientBossBar : map.values()) {
            String string = Network.safeLower(clientBossBar.getName().getString());
            if (!string.contains("pvp") && !string.contains("\u043f\u0432\u043f")) continue;
            lastPvpMs = l;
            return;
        }
    }

    public static void handleTimePacket() {
        long l = System.nanoTime();
        if (lastTpsPacketNs != 0L) {
            float f = 20.0f * (1.0E9f / (float)Math.max(1L, l - lastTpsPacketNs));
            tps = MathHelper.clamp((float)f, (float)0.0f, (float)20.0f);
        }
        lastTpsPacketNs = l;
    }

    private static void resetRuntimeState() {
        lastPvpMs = 0L;
        lastPvpScanMs = 0L;
        lastServerDetectMs = 0L;
        singleplayerWorld = true;
        realTimePing = 0;
        lastPingRequestMs = 0L;
        lastPingUpdateMs = 0L;
        Network.resetTpsState();
        server = "Vanilla";
    }

    public static int getRealTimePing() {
        if (singleplayerWorld || isSingleplayerWorld() || MC.player == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        if (realTimePing > 0 && now - lastPingUpdateMs < 5000L) {
            return realTimePing;
        }
        if (MC.getNetworkHandler() != null && MC.player != null) {
            PlayerListEntry entry = MC.getNetworkHandler().getPlayerListEntry(MC.player.getUuid());
            if (entry != null && entry.getLatency() > 0) {
                return entry.getLatency();
            }
        }
        return realTimePing > 0 ? realTimePing : 0;
    }

    public static boolean isReallyWorld() {
        return "ReallyWorld".equals(Network.getServer());
    }

    public static String getServer() {
        if (Network.MC.player == null || Network.MC.world == null || MC.getNetworkHandler() == null || singleplayerWorld) {
            return "Vanilla";
        }
        Network.refreshServer(System.currentTimeMillis());
        return server;
    }

    public static boolean isGulPvP() {
        return "GulPvP".equals(Network.getServer());
    }

    private static String safeLower(String string) {
        return string == null ? "" : string.toLowerCase(Locale.ROOT);
    }

    private static boolean isHeart(int n) {
        return n == 9829 || n == 10084 || n == 9825 || n == 10083 || n == 10085 || n == 10086 || n == 10087 || n == 128147 || n == 128148 || n == 128149 || n == 128150 || n == 128151 || n == 128152 || n == 128153 || n == 128154 || n == 128155 || n == 128156 || n == 128157 || n == 129505 || n == 128420 || n == 129293 || n == 129294;
    }

    public static float getTPS() {
        if (Network.MC.player == null || Network.MC.world == null || MC.getNetworkHandler() == null || singleplayerWorld) {
            return 20.0f;
        }
        return tps;
    }

    public static boolean isPvp() {
        Network.updatePvpState(System.currentTimeMillis());
        return System.currentTimeMillis() - lastPvpMs <= 500L;
    }

    public static boolean isCopyTime() {
        String string = Network.getServer();
        return "CopyTime".equals(string) || "SpookyTime".equals(string) || "FunTime".equals(string);
    }

    public static boolean isFunTime() {
        return "FunTime".equals(Network.getServer());
    }

    public static boolean isVanilla() {
        return "Vanilla".equals(Network.getServer());
    }

    public static void tick() {
        if (Network.MC.player == null || Network.MC.world == null || MC.getNetworkHandler() == null) {
            Network.resetRuntimeState();
            return;
        }
        singleplayerWorld = Network.isSingleplayerWorld();
        if (singleplayerWorld) {
            Network.resetTpsState();
            server = "Vanilla";
            return;
        }
        long l = System.currentTimeMillis();
        Network.refreshServer(l);
        Network.updatePvpState(l);

        if (MC.getNetworkHandler() != null && !singleplayerWorld && l - lastPingRequestMs >= 1000L) {
            lastPingRequestMs = l;
            try {
                MC.getNetworkHandler().sendPacket(new QueryPingC2SPacket(Util.getMeasuringTimeMs()));
            } catch (Throwable ignored) {
            }
        }
    }
}

