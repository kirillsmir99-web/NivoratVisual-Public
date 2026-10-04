package rtx.nv.api.modules.impl.Visuals.emotions;

import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import rtx.nv.api.modules.impl.Visuals.Emotions;
import rtx.nv.api.ui.BaseScreen;

public final class EmotionWheelScreen extends BaseScreen {
    private final Emotions module;
    private final EmotionWheel wheel;

    public EmotionWheelScreen(Emotions emotions, List<Emotion> list) {
        super(Text.literal("Эмоции"));
        this.module = emotions;
        this.wheel = new EmotionWheel(list);
    }

    @Override
    protected void renderScreen(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (this.wheel != null) {
            this.wheel.render(drawContext, true);
        }
        if (this.module != null && !this.module.isMenuKeyDown()) {
            if (this.wheel != null) {
                this.wheel.selectHovered();
            }
            this.close();
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click != null && click.button() == 0 && this.wheel != null) {
            if (this.client != null && this.client.getWindow() != null) {
                int screenWidth = this.client.getWindow().getScaledWidth();
                int screenHeight = this.client.getWindow().getScaledHeight();
                float cx = screenWidth * 0.5f;
                float cy = screenHeight * 0.5f;
                double mouseX = this.client.mouse.getX() * (double) screenWidth / (double) this.client.getWindow().getWidth();
                double mouseY = this.client.mouse.getY() * (double) screenHeight / (double) this.client.getWindow().getHeight();
                float dx = (float) (mouseX - cx);
                float dy = (float) (mouseY - cy);
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist <= 26.0f && this.wheel.getPageCount() > 1) {
                    if (dx < 0) {
                        this.wheel.prevPage();
                    } else {
                        this.wheel.nextPage();
                    }
                    return true;
                }
            }

            if (this.wheel.getSelectedPageItem() >= 0) {
                this.wheel.selectHovered();
                this.close();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.wheel != null && this.wheel.getPageCount() > 1) {
            if (verticalAmount > 0) {
                this.wheel.prevPage();
                return true;
            } else if (verticalAmount < 0) {
                this.wheel.nextPage();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.wheel != null && this.wheel.getPageCount() > 1 && input != null) {
            int key = input.key();
            if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_Q) {
                this.wheel.prevPage();
                return true;
            }
            if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D || key == GLFW.GLFW_KEY_E) {
                this.wheel.nextPage();
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
