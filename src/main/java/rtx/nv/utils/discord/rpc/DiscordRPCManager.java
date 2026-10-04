package rtx.nv.utils.discord.rpc;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtx.nv.api.modules.impl.Utils.Discord;
import rtx.nv.api.profile.Profile;
import rtx.nv.utils.discord.rpc.DiscordNativeLoader;
import rtx.nv.utils.discord.rpc.utils.DiscordEventHandlers;
import rtx.nv.utils.discord.rpc.utils.DiscordRPC;
import rtx.nv.utils.discord.rpc.utils.DiscordRichPresence;
import rtx.nv.utils.discord.rpc.utils.DiscordUser;
import rtx.nv.utils.discord.rpc.utils.RPCButton;
import rtx.nv.utils.profile.ProfileIdentity;

public final class DiscordRPCManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("NV/DiscordRPC");
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final String CLIENT_ID = "1553432646166515764";
    private static final long CALLBACK_DELAY_MS = 15000L;
    private static final long DETAILS_ANIM_INTERVAL_MS = 3500L;
    private static volatile boolean running;
    private static volatile long startedAt;
    private static volatile Thread callbackThread;
    private static volatile Thread shutdownHook;
    private static final AtomicInteger detailsFrame = new AtomicInteger(0);
    private static volatile ScheduledExecutorService detailsScheduler;
    private static volatile String avatarUrl = "";
    private static volatile String username = "";

    private DiscordRPCManager() {
    }

    public static void start() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        if (isLinux()) {
            STARTED.set(false);
            return;
        }
        try {
            DiscordNativeLoader.prepare();
            startedAt = System.currentTimeMillis() / 1000L;
            DiscordEventHandlers discordEventHandlers = new DiscordEventHandlers.Builder()
                    .ready(DiscordRPCManager::onReady)
                    .build();
            DiscordRPC.INSTANCE.Discord_Initialize(CLIENT_ID, discordEventHandlers, true, "");
            running = true;
            callbackThread = new Thread(DiscordRPCManager::runCallbacksLoop, "NV-Discord-RPC");
            callbackThread.setDaemon(true);
            callbackThread.start();
            shutdownHook = new Thread(DiscordRPCManager::stop, "NV-Discord-RPC-Shutdown");
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (Throwable throwable) {
            running = false;
            STARTED.set(false);
            LOGGER.debug("Failed to start Discord RPC", throwable);
        }
    }

    private static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    }

    public static void stop() {
        if (!STARTED.compareAndSet(true, false)) {
            return;
        }
        running = false;
        ScheduledExecutorService scheduledExecutorService = detailsScheduler;
        detailsScheduler = null;
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
        Thread thread = callbackThread;
        callbackThread = null;
        if (thread != null) {
            thread.interrupt();
        }
        Thread thread2 = shutdownHook;
        shutdownHook = null;
        if (thread2 != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(thread2);
            } catch (Throwable ignored) {
            }
        }
        try {
            DiscordRPC.INSTANCE.Discord_ClearPresence();
        } catch (Throwable ignored) {
        }
        try {
            DiscordRPC.INSTANCE.Discord_Shutdown();
        } catch (Throwable ignored) {
        }
    }

    public static String username() {
        String string = username;
        return string == null || string.isEmpty() ? null : string;
    }

    public static String avatarUrl() {
        String string = avatarUrl;
        return string == null || string.isEmpty() ? null : string;
    }

    private static void onReady(DiscordUser discordUser) {
        try {
            avatarUrl = "";
            if (discordUser != null && discordUser.userId != null && discordUser.avatar != null && !discordUser.userId.isBlank() && !discordUser.avatar.isBlank()) {
                avatarUrl = "https://cdn.discordapp.com/avatars/" + discordUser.userId + "/" + discordUser.avatar + ".png";
            }
            username = discordUser != null && discordUser.username != null ? discordUser.username : "";
            detailsFrame.set(0);
            updatePresenceImmediate();
            startDetailsAnimation();
        } catch (Throwable throwable) {
            LOGGER.debug("Failed to initialize Discord presence on ready", throwable);
        }
    }

    public static void updatePresenceImmediate() {
        if (!running) {
            return;
        }
        Discord discord = Discord.getInstance();
        if (discord != null && !discord.isEnabled()) {
            clearPresence();
            return;
        }
        try {
            DiscordRichPresence presence = buildPresence(detailsFrame.get());
            if (presence != null) {
                DiscordRPC.INSTANCE.Discord_UpdatePresence(presence);
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to update Discord presence immediately", t);
        }
    }

    public static void clearPresence() {
        if (!running) {
            return;
        }
        try {
            DiscordRPC.INSTANCE.Discord_ClearPresence();
        } catch (Throwable ignored) {
        }
    }

    public static DiscordRichPresence buildPresence(int frameIndex) {
        Discord discord = Discord.getInstance();
        if (discord != null && !discord.isEnabled()) {
            return null;
        }

        String mode = discord != null ? discord.mode.getValue() : "Авто";
        boolean showServer = discord == null || discord.showServer.getValue();
        boolean showNick = discord == null || discord.showNick.getValue();
        boolean anim = discord == null || discord.animation.getValue();
        String serverName = detectServerName();
        String nick = profileUsername();

        boolean inMenu = "В главном меню".equals(serverName) || "Одиночный мир".equals(serverName);

        String details;
        String state;

        if ("Свой текст".equals(mode) && discord != null) {
            List<String> activePages = discord.activePages.getSelected();
            if (activePages == null || activePages.isEmpty()) {
                activePages = List.of("Страница 1");
            }
            int pageIndex = anim ? Math.floorMod(frameIndex, activePages.size()) : 0;
            String currentPage = activePages.get(pageIndex);

            String cLine1;
            String cLine2;
            if ("Страница 2".equals(currentPage)) {
                cLine1 = discord.page2Line1.getValue();
                cLine2 = discord.page2Line2.getValue();
            } else if ("Страница 3".equals(currentPage)) {
                cLine1 = discord.page3Line1.getValue();
                cLine2 = discord.page3Line2.getValue();
            } else {
                cLine1 = discord.page1Line1.getValue();
                cLine2 = discord.page1Line2.getValue();
            }

            if (cLine1 == null || cLine1.isBlank()) {
                cLine1 = "NV";
            }
            if (cLine2 == null || cLine2.isBlank()) {
                cLine2 = "Лучший визуал";
            }

            cLine1 = cLine1.replace("{server}", serverName).replace("{сервер}", serverName)
                           .replace("{nick}", nick).replace("{ник}", nick).trim();
            cLine2 = cLine2.replace("{server}", serverName).replace("{сервер}", serverName)
                           .replace("{nick}", nick).replace("{ник}", nick).trim();

            details = cLine1;

            StringBuilder stateBuilder = new StringBuilder(cLine2);
            boolean alreadyHasServer = cLine1.contains(serverName) || cLine2.contains(serverName);
            boolean alreadyHasNick = cLine1.contains(nick) || cLine2.contains(nick);

            if (showServer && !inMenu && !alreadyHasServer) {
                if (stateBuilder.length() > 0) {
                    stateBuilder.append(" • ");
                }
                stateBuilder.append(serverName);
            }
            if (showNick && !alreadyHasNick) {
                if (stateBuilder.length() > 0) {
                    stateBuilder.append(" • ");
                }
                stateBuilder.append(nick);
            }
            state = stateBuilder.toString();
        } else {
            // "Авто" (динамические сменяющиеся фразы, компактные и эстетичные)
            int frame = anim ? Math.floorMod(frameIndex, 4) : 0;
            switch (frame) {
                case 1:
                    details = "NV • " + (inMenu ? serverName : (showServer ? serverName : "В игре"));
                    state = showNick ? ("Игрок: " + nick) : "Лучший визуал";
                    break;
                case 2:
                    details = "NV • " + (inMenu ? serverName : (showServer ? ("Сервер: " + serverName) : "В игре"));
                    state = showNick ? (nick + " (NV)") : "Nivorat Visual";
                    break;
                case 3:
                    details = "NV • Лучший визуал";
                    state = inMenu ? serverName : (showServer ? ("Сервер: " + serverName) : (showNick ? ("Игрок: " + nick) : "В игре"));
                    break;
                case 0:
                default:
                    details = "NV • " + (inMenu ? serverName : (showServer ? serverName : "В игре"));
                    state = showNick ? (nick + (showServer && !inMenu ? (" • " + serverName) : "")) : "Разработка: Nivorat";
                    break;
            }
        }

        DiscordRichPresence.Builder builder = new DiscordRichPresence.Builder()
                .setStartTimestamp(startedAt)
                .setDetails(details)
                .setState(state)
                .setButtons(RPCButton.create("Telegram", "https://t.me/virionDEV"));

        String smallImg = smallImageUrl();
        if (smallImg != null && !smallImg.isEmpty()) {
            builder.setSmallImage(smallImg, nick);
        }

        return builder.build();
    }

    public static String detectServerName() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.world == null) {
                return "В главном меню";
            }
            if (mc.isInSingleplayer() || mc.isIntegratedServerRunning()) {
                return "Одиночный мир";
            }

            String serverListName = "";
            String address = "";
            String brand = "";
            ClientPlayNetworkHandler handler = mc.getNetworkHandler();
            if (handler != null) {
                ServerInfo info = handler.getServerInfo();
                if (info != null) {
                    if (info.name != null && !info.name.isBlank()) {
                        serverListName = info.name.trim();
                    }
                    if (info.address != null && !info.address.isBlank()) {
                        address = info.address.trim();
                    }
                }
                String b = handler.getBrand();
                if (b != null) {
                    brand = b.trim();
                }
            }
            if (mc.getCurrentServerEntry() != null) {
                ServerInfo entry = mc.getCurrentServerEntry();
                if (serverListName.isEmpty() && entry.name != null && !entry.name.isBlank()) {
                    serverListName = entry.name.trim();
                }
                if (address.isEmpty() && entry.address != null && !entry.address.isBlank()) {
                    address = entry.address.trim();
                }
            }

            String sbTitle = "";
            try {
                if (mc.world != null) {
                    net.minecraft.scoreboard.Scoreboard scoreboard = mc.world.getScoreboard();
                    if (scoreboard != null) {
                        net.minecraft.scoreboard.ScoreboardObjective objective = scoreboard.getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR);
                        if (objective != null && objective.getDisplayName() != null) {
                            sbTitle = objective.getDisplayName().getString().trim();
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            String combined = (serverListName + " " + address + " " + brand + " " + sbTitle).toLowerCase(Locale.ROOT);

            if (combined.contains("elarion") || combined.contains("ellarium") ||
                rtx.nv.utils.net.NvHosts.isKnownElarionHost(combined)) {
                return "Elarion";
            }
            if (combined.contains("holyworld")) {
                return "HolyWorld";
            }
            if (combined.contains("funtime") || combined.contains("botfilter")) {
                return "FunTime";
            }
            if (combined.contains("reallyworld")) {
                return "ReallyWorld";
            }
            if (combined.contains("spooky")) {
                return "SpookyTime";
            }
            if (combined.contains("hypixel")) {
                return "Hypixel";
            }
            if (combined.contains("vimeworld")) {
                return "VimeWorld";
            }
            if (combined.contains("mst")) {
                return "MST Network";
            }
            if (combined.contains("mcfun")) {
                return "McFun";
            }
            if (combined.contains("sunmc")) {
                return "SunMC";
            }
            if (combined.contains("aresmine")) {
                return "AresMine";
            }
            if (combined.contains("dexland")) {
                return "DexLand";
            }
            if (combined.contains("prostocraft")) {
                return "ProstoCraft";
            }
            if (combined.contains("gommehd")) {
                return "GommeHD";
            }

            if (!serverListName.isEmpty() &&
                !serverListName.equalsIgnoreCase("Minecraft Server") &&
                !serverListName.equalsIgnoreCase("Сервер Minecraft") &&
                !serverListName.equalsIgnoreCase("Direct Connect") &&
                !serverListName.equalsIgnoreCase("Прямое подключение") &&
                !serverListName.equalsIgnoreCase(address)) {
                return serverListName;
            }

            if (!sbTitle.isEmpty()) {
                String cleanSb = sbTitle.replaceAll("[§&][0-9a-fk-or]", "").trim();
                if (!cleanSb.isEmpty() && cleanSb.length() <= 16 && !cleanSb.equalsIgnoreCase("Scoreboard") && !cleanSb.equalsIgnoreCase("Скорборд")) {
                    return cleanSb;
                }
            }

            if (address.isEmpty()) {
                return "В игре";
            }

            int colonIndex = address.indexOf(':');
            String cleanHost = colonIndex != -1 ? address.substring(0, colonIndex).trim() : address.trim();
            if (cleanHost.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
                return "Сервер";
            }
            if (cleanHost.startsWith("mc.")) {
                cleanHost = cleanHost.substring(3);
            } else if (cleanHost.startsWith("play.")) {
                cleanHost = cleanHost.substring(5);
            }
            return cleanHost.isEmpty() ? "Сервер" : cleanHost;
        } catch (Throwable t) {
            return "В игре";
        }
    }

    private static String profileUsername() {
        String string = Profile.getUsername();
        if (string != null && !string.isBlank()) {
            return string;
        }
        return currentUsername();
    }

    private static String smallImageUrl() {
        String string = ProfileIdentity.avatarUrl();
        if (string != null) {
            return string;
        }
        return avatarUrl != null && !avatarUrl.isEmpty() ? avatarUrl : null;
    }

    private static void runCallbacksLoop() {
        while (running) {
            try {
                DiscordRPC.INSTANCE.Discord_RunCallbacks();
                Thread.sleep(CALLBACK_DELAY_MS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable throwable) {
                LOGGER.debug("Discord RPC callback loop error", throwable);
                break;
            }
        }
    }

    private static void tickDetails() {
        if (!running) {
            return;
        }
        Discord discord = Discord.getInstance();
        if (discord != null && !discord.isEnabled()) {
            clearPresence();
            return;
        }
        try {
            int n2 = detailsFrame.incrementAndGet();
            DiscordRichPresence presence = buildPresence(n2);
            if (presence != null) {
                DiscordRPC.INSTANCE.Discord_UpdatePresence(presence);
            }
        } catch (Throwable throwable) {
            LOGGER.debug("Discord RPC details animation error", throwable);
        }
    }

    private static String currentUsername() {
        try {
            String string;
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient != null && minecraftClient.getSession() != null && (string = minecraftClient.getSession().getUsername()) != null && !string.isBlank()) {
                return string;
            }
        } catch (Throwable ignored) {
        }
        return "Player";
    }

    private static void startDetailsAnimation() {
        if (detailsScheduler != null) {
            return;
        }
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "NV-Discord-RPC-Anim");
            thread.setDaemon(true);
            return thread;
        });
        detailsScheduler = scheduledExecutorService;
        scheduledExecutorService.scheduleAtFixedRate(DiscordRPCManager::tickDetails, DETAILS_ANIM_INTERVAL_MS, DETAILS_ANIM_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
}
