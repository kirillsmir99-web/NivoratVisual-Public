package rtx.nv.api.modules.impl.Interface;
import rtx.nv.api.events.EventHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import rtx.nv.api.config.ConfigManager;
import rtx.nv.api.localization.Lang;
import rtx.nv.api.drags.Position;
import rtx.nv.api.drags.hud.InfoHud;
import rtx.nv.api.events.impl.module.ModuleToggleEvent;
import rtx.nv.api.events.impl.render.HudRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InfoModule;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.Render2D;

public class NotificationsModule
extends Module {
    private static final String FONT = "montserrat-bold";
    private static final float TEXT_SIZE = 7.0f;
    private static final float PAD_X = 7.0f;
    private static final float PAD_Y = 4.0f;
    private static final float EDGE = 6.0f;
    private static final float GAP = 4.0f;
    private static final float MAX_RADIUS = 6.0f;
    private static final int MAX_VISIBLE = 6;
    private static final long IN_MS = 250L;
    private static final long OUT_MS = 250L;
    private static final long DEFAULT_HOLD_MS = 2500L;
    private static final long TOGGLE_HOLD_MS = 1500L;
    private static final String METRIC_REF = "\u0410\u041dmT";
    private static final int WHITE = -1;
    private static final int GREEN = -11141291;
    private static final int RED = -43691;
    private static final float OUTLINE = 0.5f;
    private static final float OUTLINE_DARK = 0.25f;
    private static NotificationsModule instance;
    private final List<NotificationsModule.Notif> notifs = new ArrayList<NotificationsModule.Notif>();
    private final ConcurrentLinkedQueue<Pending> pending = new ConcurrentLinkedQueue();
    private NotificationsModule.Notif example;
    private float chatOffset;
    private static final int FRAME_CAP = 32;
    private final NotificationsModule.Notif[] frameNotif = new NotificationsModule.Notif[32];
    private final float[] frameSlotBottom = new float[32];
    private final float[] frameEnv = new float[32];
    private final float[] glowSpans = new float[128];
    private static float cachedInkMid;

    public NotificationsModule() {
        super("Notifications", "\u0422\u043e\u0441\u0442\u044b-\u0443\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f \u0432 \u043f\u0440\u0430\u0432\u043e\u043c \u043d\u0438\u0436\u043d\u0435\u043c \u0443\u0433\u043b\u0443.", Category.DISPLAY);
        instance = this;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    static {
        cachedInkMid = Float.NaN;
    }

    public static void notify(String string, long l) {
        NotificationsModule notificationsModule;
        if (ConfigManager.isLoading()) {
            return;
        }
        NotificationsModule notificationsModule2 = notificationsModule = instance != null ? instance : ModuleManager.get().get(NotificationsModule.class);
        if (notificationsModule != null && notificationsModule.isEnabled() && string != null && !string.isBlank()) {
            notificationsModule.pending.add(new Pending(new Seg[]{new Seg(string, -1)}, Math.max(500L, l)));
        }
    }

    private static float easeInQuad(float f) {
        float f2 = NotificationsModule.clamp01(f);
        return f2 * f2;
    }

    private static float segsWidth(Seg[] segArray) {
        float f = 0.0f;
        for (Seg seg : segArray) {
            f += Render2D.msdfWidth(FONT, seg.text(), 7.0f);
        }
        return f;
    }

    private static float clamp01(float f) {
        return f < 0.0f ? 0.0f : Math.min(f, 1.0f);
    }

    private static float inkMid() {
        if (Float.isNaN(cachedInkMid)) {
            float[] fArray = Render2D.msdfBounds(FONT, METRIC_REF, 7.0f);
            cachedInkMid = (fArray[1] + fArray[3]) * 0.5f;
        }
        return cachedInkMid;
    }

    private static int darken(int n, float f) {
        int n2 = n >>> 24 & 0xFF;
        int n3 = Math.round((float)(n >> 16 & 0xFF) * f);
        int n4 = Math.round((float)(n >> 8 & 0xFF) * f);
        int n5 = Math.round((float)(n & 0xFF) * f);
        return n2 << 24 | n3 << 16 | n4 << 8 | n5;
    }

    private void render(DrawContext drawContext, NotificationsModule.Notif notif, float f, float f2, float f3, float f4) {
        float f5 = NotificationsModule.clamp01(f4);
        if (f5 <= 0.004f) {
            return;
        }
        float f6 = NotificationsModule.segsWidth(notif.segs);
        float f7 = f6 + 14.0f;
        float f8 = f - f7 + 18f * (1f - f5);
        float f9 = f2 - f3;
        float f10 = Math.min(6.0f, Math.min(f7, f3) * 0.5f);
        drawContext.getMatrices().pushMatrix();

        RectUtil.drawClientRectNoGlow(f8, f9, f7, f3, f10, f5);
        float f12 = f9 + f3 * 0.5f - NotificationsModule.inkMid();
        float f13 = f8 + 7.0f;
        for (Seg seg : notif.segs) {
            NotificationsModule.drawOutlined(seg.text(), f13, f12, ColorUtil.multAlpha(seg.color(), f5));
            f13 += Render2D.msdfWidth(FONT, seg.text(), 7.0f);
        }
        drawContext.getMatrices().popMatrix();
    }

    @EventHandler
    private void onHud(HudRenderEvent hudRenderEvent) {
        boolean bl;
        Pending pending;
        long l = System.currentTimeMillis();
        while ((pending = this.pending.poll()) != null) {
            this.spawn(pending.segs(), pending.duration(), l, false);
        }
        this.manageExample(l);
        if (this.notifs.isEmpty()) {
            return;
        }
        float f = Math.max(1.0f, Position.screenWidth());
        float f2 = Math.max(1.0f, Position.screenHeight());
        boolean bl2 = this.mc.currentScreen instanceof ChatScreen;
        float f3 = 15.0f;
        float f4 = f3 + 4.0f;
        float f5 = f - 6.0f;
        float f6 = 6.0f;
        InfoModule infoModule = ModuleManager.get().get(InfoModule.class);
        boolean bl3 = bl = infoModule != null && infoModule.isEnabled();
        if (bl) {
            f6 = Math.max(f6, InfoHud.reservedRightHeight());
        }
        float f7 = bl2 && !bl ? 14.0f : 0.0f;
        this.chatOffset += (f7 - this.chatOffset) * 0.25f;
        float f8 = f2 - f6 - this.chatOffset;
        DrawContext drawContext = hudRenderEvent.getGraphics();
        int n = 0;
        int n2 = 0;
        float f9 = Float.MAX_VALUE;
        float f10 = Float.MAX_VALUE;
        float f11 = -3.4028235E38f;
        float f12 = -3.4028235E38f;
        float f13 = 0.0f;
        Iterator<NotificationsModule.Notif> iterator = this.notifs.iterator();
        while (iterator.hasNext()) {
            float f14;
            NotificationsModule.Notif notif = iterator.next();
            long l2 = l - notif.born;
            if (!notif.removing && !notif.example && l2 >= notif.hold) {
                notif.removing = true;
                notif.removeStart = l;
            }
            float f15 = NotificationsModule.clamp01((float)l2 / 250.0f);
            float f16 = f14 = notif.removing ? NotificationsModule.clamp01((float)(l - notif.removeStart) / 250.0f) : 0.0f;
            if (f14 >= 1.0f) {
                iterator.remove();
                continue;
            }
            float f17 = NotificationsModule.easeOutQuad(f15) * (1.0f - NotificationsModule.easeInQuad(f14));
            float f18 = f8;
            f8 -= f4 * f17;
            if (f17 <= 0.004f || n >= 32) continue;
            this.frameNotif[n] = notif;
            this.frameSlotBottom[n] = f18;
            this.frameEnv[n] = f17;
            ++n;
            float f19 = NotificationsModule.segsWidth(notif.segs) + 14.0f;
            float f20 = f5 - f19 + 18f * (1f - f17);
            float f22 = f18 - f3;
            int n3 = n2 * 4;
            this.glowSpans[n3] = f20;
            this.glowSpans[n3 + 1] = f20 + f19;
            this.glowSpans[n3 + 2] = f22;
            this.glowSpans[n3 + 3] = f18;
            ++n2;
            f9 = Math.min(f9, f20);
            f11 = Math.max(f11, f20 + f19);
            f10 = Math.min(f10, f22);
            f12 = Math.max(f12, f18);
            f13 = Math.max(f13, f17);
        }
        Render2D.beginFrame(drawContext);
        if (n2 > 0) {
            for (int i = 0; i < n2; ++i) {
                int n4;
                int n5 = n4 = i * 4;
                this.glowSpans[n5] = this.glowSpans[n5] - f9;
                int n6 = n4 + 1;
                this.glowSpans[n6] = this.glowSpans[n6] - f9;
                int n7 = n4 + 2;
                this.glowSpans[n7] = this.glowSpans[n7] - f10;
                int n8 = n4 + 3;
                this.glowSpans[n8] = this.glowSpans[n8] - f10;
            }
            RectUtil.drawClientGlowSpans(f9, f10, f11 - f9, f12 - f10, f13, this.glowSpans, n2);
        }
        for (int i = 0; i < n; ++i) {
            this.render(drawContext, this.frameNotif[i], f5, this.frameSlotBottom[i], f3, this.frameEnv[i]);
            this.frameNotif[i] = null;
        }
        Render2D.flush();
    }

    @Override
    protected void onDisable() {
        this.notifs.clear();
        this.pending.clear();
        this.example = null;
    }

    @EventHandler
    private void onModuleToggle(ModuleToggleEvent moduleToggleEvent) {
        if (ConfigManager.isLoading()) {
            return;
        }
        Module module = moduleToggleEvent.getModule();
        if (module == this) {
            return;
        }
        boolean bl = moduleToggleEvent.isEnabled();
        Seg[] segArray = new Seg[]{new Seg(module.getDisplayName(), -1), new Seg(" " + Lang.get(bl ? "notification.module.enabled" : "notification.module.disabled", bl ? "enabled" : "disabled"), bl ? -11141291 : -43691)};
        this.pending.add(new Pending(segArray, 1500L));
    }

    private void spawn(Seg[] segArray, long l, long l2, boolean bl) {
        int n = 0;
        for (NotificationsModule.Notif notif : this.notifs) {
            if (notif.removing) continue;
            ++n;
        }
        for (int i = this.notifs.size() - 1; i >= 0 && n >= 6; --i) {
            NotificationsModule.Notif notif;
            notif = this.notifs.get(i);
            if (notif.removing) continue;
            notif.removing = true;
            notif.removeStart = l2;
            --n;
        }
        NotificationsModule.Notif notif = new NotificationsModule.Notif(segArray, l2, l);
        notif.example = bl;
        this.notifs.add(0, notif);
        if (bl) {
            this.example = notif;
        }
    }

    private static void drawOutlined(String string, float f, float f2, int n) {
        int n2 = NotificationsModule.darken(n, 0.25f);
        Render2D.msdfText(FONT, string, f, f2 + 0.5f, 7.0f, n2);
        Render2D.msdfText(FONT, string, f, f2 - 0.5f, 7.0f, n2);
        Render2D.msdfText(FONT, string, f + 0.5f, f2, 7.0f, n2);
        Render2D.msdfText(FONT, string, f - 0.5f, f2, 7.0f, n2);
        Render2D.msdfText(FONT, string, f, f2, 7.0f, n);
    }

    private void manageExample(long l) {
        boolean bl;
        boolean bl2 = this.mc.currentScreen instanceof ChatScreen;
        int n = 0;
        for (NotificationsModule.Notif notif : this.notifs) {
            if (notif.example || notif.removing) continue;
            ++n;
        }
        boolean bl3 = bl = this.example != null && this.notifs.contains(this.example) && !this.example.removing;
        if (bl2 && n == 0) {
            if (!bl) {
                this.spawn(new Seg[]{new Seg("Module", -1), new Seg(" enabled", -11141291)}, 2500L, l, true);
            }
        } else if (bl) {
            this.example.removing = true;
            this.example.removeStart = l;
        }
    }

    private static float easeOutQuad(float f) {
        float f2 = 1.0f - NotificationsModule.clamp01(f);
        return 1.0f - f2 * f2;
    }


    public static record Seg(String text, int color) {
    }

    public static record Pending(Seg[] segs, long duration) {
    }

    public static final class Notif {
        final NotificationsModule.Seg[] segs;
        final long born;
        final long hold;
        boolean removing;
        boolean example;
        long removeStart;

        Notif(NotificationsModule.Seg[] segArray, long l, long l2) {
            this.segs = segArray;
            this.born = l;
            this.hold = l2;
        }
    }
}

