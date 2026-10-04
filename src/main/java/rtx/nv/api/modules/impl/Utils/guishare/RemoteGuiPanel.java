package rtx.nv.api.modules.impl.Utils.guishare;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.modules.Category;
import rtx.nv.api.ui.window.GuiShatterAnimation;

public class RemoteGuiPanel {
    private GuiShareRemoteState state;
    private RemoteTheme theme;
    private final Set<String> enabledSet = new HashSet<>();
    private boolean closed;
    private long closedAtNanos;
    private long smoothAtNanos;
    private Vec3d anchor = Vec3d.ZERO;
    private float yaw, pitch, scroll, eventsScroll, themesScroll, popupScroll;

    public RemoteGuiPanel(GuiShareRemoteState state) {
        this.state = state;
        this.theme = RemoteTheme.fromState(state != null ? state.theme() : null);
        if (state != null) {
            anchor = state.anchor() == null ? Vec3d.ZERO : state.anchor();
            yaw = state.yaw(); pitch = state.pitch(); scroll = state.listScroll();
            eventsScroll = state.eventsScroll(); themesScroll = state.themesScroll(); popupScroll = state.popupScroll();
        }
        smoothAtNanos = System.nanoTime();
        if (state != null && state.enabledModules() != null) {
            enabledSet.addAll(state.enabledModules());
        }
    }

    public void update(GuiShareRemoteState newState) {
        this.state = newState;
        this.theme = RemoteTheme.fromState(newState != null ? newState.theme() : null);
        this.enabledSet.clear();
        if (newState != null && newState.enabledModules() != null) {
            enabledSet.addAll(newState.enabledModules());
        }
    }

    public void markClosed() {
        if (!closed) closedAtNanos = System.nanoTime();
        this.closed = true;
    }

    public boolean isFinished() {
        return this.closed && closeAlpha() <= 0.001f;
    }

    public GuiShareRemoteState state() { return state; }
    public RemoteTheme theme() { return theme; }
    public Set<String> enabledSet() { return enabledSet; }

    public float animScale() { return 0.96f + 0.04f * closeAlpha(); }
    public boolean closeActive() { return closed; }
    public float closeAlpha() { return closed ? Math.max(0f, 1f - (System.nanoTime()-closedAtNanos)/200_000_000f) : 1f; }
    public float contentAlpha() { return closeAlpha(); }
    public Category contentCategory() {
        if (state == null || state.category() == null) return Category.VISUALS;
        try {
            return Category.valueOf(state.category().toUpperCase());
        } catch (Exception e) {
            return Category.VISUALS;
        }
    }
    public Category targetCategory() { return contentCategory(); }
    public float categoryAnimT() { return 1.0f; }
    public float categoryAnimT(Category cat) { return 1.0f; }
    public float categoryT() { return 1.0f; }
    public float eventsHeaderT() { return 1.0f; }
    public float eventsSubT() { return 1.0f; }
    public float eventsSubT(int sub) { return 1.0f; }
    public float modulesHeaderT() { return 1.0f; }
    public float placeholderT() { return 0.0f; }
    public String popupDisplayed() { return state != null ? state.popupModule() : null; }
    public float popupHoldX() { return state != null ? state.popupX() : 0.0f; }
    public float popupHoldY() { return state != null ? state.popupY() : 0.0f; }
    public List<GuiSharePopupRow> popupRows() { return state != null ? state.popupRows() : List.of(); }
    public float popupT() { return (state != null && state.popupModule() != null && !state.popupModule().isEmpty()) ? 1.0f : 0.0f; }
    public void resolveAnchor() { if (state != null) resolveAnchor(state.anchor()); }
    public void resolveAnchor(Vec3d anchor) {
        if (anchor != null && Double.isFinite(anchor.x) && Double.isFinite(anchor.y) && Double.isFinite(anchor.z)) this.anchor = anchor;
    }
    public Vec3d resolvedAnchor() { return anchor; }
    public float resolvedPitch() { return pitch; }
    public float resolvedYaw() { return yaw; }
    public float smoothPitch() { return resolvedPitch(); }
    public float smoothYaw() { return resolvedYaw(); }
    public float smoothScroll() { return scroll; }
    public float smoothEventsScroll() { return eventsScroll; }
    public float smoothThemesScroll() { return themesScroll; }
    public float smoothPopupScroll() { return popupScroll; }
    public float rowAppear(int index) { return 1.0f; }
    public void markAppearFrameDone() {}
    public void updateSmoothing(long time) {
        long now = System.nanoTime();
        float t = (float)(1.0 - Math.exp(-Math.min(.1,(now-smoothAtNanos)/1e9) * 18.0));
        smoothAtNanos = now;
        if (state == null) return;
        yaw += net.minecraft.util.math.MathHelper.wrapDegrees(state.yaw()-yaw) * t;
        pitch += (state.pitch()-pitch) * t;
        scroll += (state.listScroll()-scroll) * t;
        eventsScroll += (state.eventsScroll()-eventsScroll) * t;
        themesScroll += (state.themesScroll()-themesScroll) * t;
        popupScroll += (state.popupScroll()-popupScroll) * t;
    }

    public boolean isRenderable() {
        return this.state != null;
    }

    public boolean shatterActive() { return false; }
    public float shatterProgressForRender() { return 0.0f; }
    public float[] shatterRect() { return new float[]{0, 0, 430, 290}; }
    public GuiShatterAnimation.State shatterState() { return null; }
}
