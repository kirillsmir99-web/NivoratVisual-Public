package rtx.nv.api.ui.crosshair;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import rtx.nv.api.modules.impl.Visuals.Crosshair;
import rtx.nv.api.ui.BaseScreen;
import rtx.nv.api.ui.window.PanelAnimation;
import rtx.nv.utils.animations.Decelerate;

public final class CrosshairEditorScreen
extends BaseScreen {
    private static final int GRID = 15;
    private static final float CELL = 10.0f;
    private static final float PITCH = 11.0f;
    private static final float PAD = 12.0f;
    private static final float GRID_PANEL = 172.0f;
    private static final float RIGHT_W = 112.0f;
    private static final float WIN_W = 320.0f;
    private static final float PANEL_TOP = 30.0f;
    private static final float WIN_H = 214.0f;
    private static final float PREVIEW_LABEL_Y = 30.0f;
    private static final float PREVIEW_Y = 39.0f;
    private static final float PREVIEW_H = 50.0f;
    private static final float MIRROR_LABEL_Y = 97.0f;
    private static final float ROW_H = 13.0f;
    private static final float ROW1_Y = 106.0f;
    private static final float ROW2_Y = 123.0f;
    private static final float BTN_H = 16.0f;
    private static final float BTN1_Y = 145.0f;
    private static final float BTN2_Y = 167.0f;
    private static final float HINT_Y = 192.5f;
    private static final float BACK_H = 14.0f;
    private final Crosshair module;
    private final Screen parent;
    private final boolean[] grid;
    private final PanelAnimation anim = new PanelAnimation(true);
    private final Decelerate mirrorXAnim = new Decelerate();
    private final Decelerate mirrorYAnim = new Decelerate();
    private boolean mirrorX = true;
    private boolean mirrorY = false;
    private int paintButton = -1;
    private float hoverBack;
    private float hoverRow1;
    private float hoverRow2;
    private float hoverBtn1;
    private float hoverBtn2;
    private long lastNs = System.nanoTime();

    public CrosshairEditorScreen(Crosshair crosshair, Screen screen) {
        super((Text)Text.literal((String)"\u0420\u0435\u0434\u0430\u043a\u0442\u043e\u0440 \u043f\u0440\u0438\u0446\u0435\u043b\u0430"));
        this.module = crosshair;
        this.parent = screen;
        this.grid = crosshair.customGrid();
        CrosshairEditorScreen.initToggleAnim((Decelerate)(Object)this.mirrorXAnim, (boolean)this.mirrorX);
        CrosshairEditorScreen.initToggleAnim((Decelerate)(Object)this.mirrorYAnim, (boolean)this.mirrorY);
    }

    @Override
    protected void onClosingOverlayDropped() {
        this.anim.finish();
    }

    @Override
    protected void renderScreen(net.minecraft.client.gui.DrawContext graphics, int mouseX, int mouseY, float delta) {
    }

    private static void initToggleAnim(Decelerate anim, boolean val) {
        if (anim != null) {
            anim.setDirection(val ? rtx.nv.utils.animations.Direction.FORWARDS : rtx.nv.utils.animations.Direction.BACKWARDS);
        }
    }
}

