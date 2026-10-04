package rtx.nv.api.drags.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import rtx.nv.api.drags.Position;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.render.HudRenderEvent;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InfoModule;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.impl.Utils.StreamerMode;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.network.Network;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.Render2D;

public final class InfoHud {
    private static final String FONT = "montserrat-semibold";
    private static final float MARGIN = 6.0f;
    private static final float GAP = 3.0f;

    private static final int TEXT_WHITE = -1;
    private static final int TEXT_MUTED = 0x99FFFFFF;
    private static final int TEXT_SEP = 0x55FFFFFF;
    private static final int COLOR_GOOD = 0xFF6BE28A;
    private static final int COLOR_WARN = 0xFFFFD166;
    private static final int COLOR_BAD = 0xFFFF6B6B;

    private static final float[][] SHADOW_DIRS = new float[][]{
        {-0.5f, 0.0f}, {0.5f, 0.0f}, {0.0f, -0.5f}, {0.0f, 0.5f}
    };

    public InfoHud() {
        EventBus.get().subscribe(this);
    }

    private static int primaryAccent() {
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        return interfaceModule == null ? 0xFF8AA4FF : interfaceModule.clientPrimaryColorOpaque();
    }

    private static float width(List<Seg> segs, float size) {
        float w = 0.0f;
        for (Seg seg : segs) {
            w += Render2D.msdfWidth(FONT, seg.text, size);
        }
        return w;
    }

    @EventHandler
    private void onHud(HudRenderEvent hudRenderEvent) {
        InfoModule infoModule = ModuleManager.get().get(InfoModule.class);
        if (infoModule == null || !infoModule.isEnabled()) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ClientPlayerEntity clientPlayerEntity = minecraftClient.player;
        if (clientPlayerEntity == null || minecraftClient.world == null) {
            return;
        }

        float scaleFactor = Math.max(0.7f, Math.min(1.5f, infoModule.scale.getFloat()));
        float baseH = infoModule.pillHeight.getFloat();
        float pillH = baseH * scaleFactor;
        float fontSize = Math.max(5.5f, baseH * 0.53f) * scaleFactor;
        float padX = Math.max(4.0f, baseH * 0.35f) * scaleFactor;
        float radius = Math.max(2.5f, baseH * 0.28f) * scaleFactor;

        boolean withBg = infoModule.background.getValue();
        boolean dynColors = infoModule.dynamicColors.getValue();
        int accent = primaryAccent();

        float screenW = Position.screenWidth();
        float screenH = Position.screenHeight();

        DrawContext drawContext = hudRenderEvent.getGraphics();

        // 1. Build Left Side (XYZ with live Nether Calculator)
        List<Seg> xyzSegs = new ArrayList<>();
        xyzSegs.add(new Seg("XYZ ", accent));
        xyzSegs.add(new Seg(coords(clientPlayerEntity.getX(), clientPlayerEntity.getY(), clientPlayerEntity.getZ()), TEXT_WHITE));
        if (infoModule.showNether.getValue()) {
            String netherCalc = netherCalculator(clientPlayerEntity);
            if (!netherCalc.isEmpty()) {
                xyzSegs.add(new Seg(" " + netherCalc, TEXT_MUTED));
            }
        }

        // BPS
        List<Seg> bpsSegs = null;
        if (infoModule.showBps.getValue()) {
            bpsSegs = new ArrayList<>();
            bpsSegs.add(new Seg("BPS ", accent));
            bpsSegs.add(new Seg(bps(clientPlayerEntity), TEXT_WHITE));
        }

        // 2. Build Right Side (FPS • TPS • PING)
        int fpsVal = minecraftClient.getCurrentFps();
        int fpsCol = TEXT_WHITE;
        if (dynColors) {
            fpsCol = fpsVal >= 60 ? TEXT_WHITE : (fpsVal >= 30 ? COLOR_WARN : COLOR_BAD);
        }

        float tpsVal = Network.getTPS();
        int tpsCol = TEXT_WHITE;
        if (dynColors) {
            tpsCol = tpsVal >= 19.0f ? COLOR_GOOD : (tpsVal >= 15.0f ? COLOR_WARN : COLOR_BAD);
        }

        int pingVal = Network.getRealTimePing();
        int pingCol = TEXT_WHITE;
        if (dynColors) {
            pingCol = pingVal < 60 ? COLOR_GOOD : (pingVal < 120 ? COLOR_WARN : COLOR_BAD);
        }

        List<Seg> statusSegs = new ArrayList<>();
        statusSegs.add(new Seg("FPS ", accent));
        statusSegs.add(new Seg(Integer.toString(fpsVal), fpsCol));
        statusSegs.add(new Seg(" • ", TEXT_SEP));
        statusSegs.add(new Seg("TPS ", accent));
        statusSegs.add(new Seg(String.format(Locale.ROOT, "%.1f", tpsVal), tpsCol));
        statusSegs.add(new Seg(" • ", TEXT_SEP));
        statusSegs.add(new Seg("PING ", accent));
        statusSegs.add(new Seg(pingVal + "ms", pingCol));

        // Render pass
        Render2D.beginFrame(drawContext);

        float leftY = screenH - MARGIN - pillH;
        drawPill(xyzSegs, MARGIN, leftY, padX, pillH, fontSize, radius, withBg);

        if (bpsSegs != null) {
            float bpsY = leftY - GAP - pillH;
            drawPill(bpsSegs, MARGIN, bpsY, padX, pillH, fontSize, radius, withBg);
        }

        float statusTextW = width(statusSegs, fontSize);
        float statusPillW = statusTextW + padX * 2.0f;
        float statusX = screenW - MARGIN - statusPillW;
        float statusY = screenH - MARGIN - pillH;
        drawPill(statusSegs, statusX, statusY, padX, pillH, fontSize, radius, withBg);

        Render2D.flush();
    }

    private static void drawPill(List<Seg> segs, float x, float y, float padX, float h, float fontSize, float radius, boolean withBg) {
        float textW = width(segs, fontSize);
        float pillW = textW + padX * 2.0f;

        if (withBg) {
            RectUtil.drawClientRect(x, y, pillW, h, radius, 1.0f);
        }

        float textX = x + padX;
        float textY = y + (h - fontSize) * 0.5f - 0.5f;

        for (Seg seg : segs) {
            if (!withBg) {
                int shadow = ColorUtil.multAlpha(0xFF000000, 0.75f);
                for (float[] dir : SHADOW_DIRS) {
                    Render2D.msdfText(FONT, seg.text, textX + dir[0], textY + dir[1], fontSize, shadow);
                }
            }
            Render2D.msdfText(FONT, seg.text, textX, textY, fontSize, seg.color);
            textX += Render2D.msdfWidth(FONT, seg.text, fontSize);
        }
    }

    private static String netherCalculator(ClientPlayerEntity player) {
        if (StreamerMode.hideCoords()) {
            return "[#, #, #]";
        }
        if (player.getEntityWorld() == null) {
            return "";
        }
        if (player.getEntityWorld().getRegistryKey() == World.NETHER) {
            // In Nether: multiply X and Z by 8 for Overworld
            int ox = (int)Math.floor(player.getX() * 8.0);
            int oy = (int)Math.floor(player.getY());
            int oz = (int)Math.floor(player.getZ() * 8.0);
            return "[Мир: " + ox + ", " + oy + ", " + oz + "]";
        } else if (player.getEntityWorld().getRegistryKey() == World.OVERWORLD) {
            // In Overworld: divide X and Z by 8 for Nether
            int nx = (int)Math.floor(player.getX() / 8.0);
            int ny = (int)Math.floor(player.getY());
            int nz = (int)Math.floor(player.getZ() / 8.0);
            return "[Ад: " + nx + ", " + ny + ", " + nz + "]";
        }
        return "";
    }

    private static String bps(PlayerEntity playerEntity) {
        double d = playerEntity.getX() - playerEntity.lastRenderX;
        double d2 = playerEntity.getZ() - playerEntity.lastRenderZ;
        double d3 = Math.sqrt(d * d + d2 * d2) * 20.0;
        return String.format(Locale.ROOT, "%.2f", d3);
    }

    private static String coords(double d, double d2, double d3) {
        if (StreamerMode.hideCoords()) {
            return "#, #, #";
        }
        return (int)Math.floor(d) + ", " + (int)Math.floor(d2) + ", " + (int)Math.floor(d3);
    }

    public static float reservedRightHeight() {
        return 28.0f;
    }

    public record Seg(String text, int color) {
    }
}
