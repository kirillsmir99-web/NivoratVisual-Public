package rtx.nv.api.modules.impl.Visuals.emotions;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.IMinecraft;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionWheel;
import rtx.nv.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import rtx.nv.utils.render.render2d.Render2D;

public final class EmotionWheelOverlay {
    private static EmotionWheel closing;

    private EmotionWheelOverlay() {
    }

    public static void begin(EmotionWheel emotionWheel) {
        if (emotionWheel == null) {
            return;
        }
        emotionWheel.close();
        closing = emotionWheel;
    }

    public static void cancel() {
        if (closing != null) {
            closing.finish();
            closing = null;
        }
    }

    public static void render(DrawContext drawContext) {
        EmotionWheel emotionWheel = closing;
        if (emotionWheel == null) {
            return;
        }
        if (IMinecraft.mc.currentScreen != null || IMinecraft.mc.world == null || IMinecraft.mc.options.hudHidden || emotionWheel.isFinished()) {
            emotionWheel.finish();
            closing = null;
            return;
        }
        Render2D.beginFrame(drawContext);
        emotionWheel.render(drawContext, false);
        Render2D.flush();
        GuiLayerBlurRenderer.markPanelEnd(drawContext);
    }

    public static EmotionWheel claim() {
        EmotionWheel emotionWheel = closing;
        closing = null;
        return emotionWheel;
    }
}

