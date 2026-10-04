package rtx.nv.api.modules.impl.Visuals.emotions;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import rtx.nv.api.modules.impl.Visuals.Emotions;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.render2d.Render2D;

public class EmotionWheel {
    public static final int PAGE_SIZE = 8;
    private final List<Emotion> emotions;
    private boolean finished;
    private int selectedPageItem = -1;
    private int currentPage = 0;
    private float animAlpha = 0.0f;
    private final long openTime = System.currentTimeMillis();

    public EmotionWheel(List<Emotion> emotions) {
        this.emotions = emotions;
    }

    public void close() {
        this.finished = true;
    }

    public void finish() {
        this.finished = true;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public int getPageCount() {
        if (this.emotions == null || this.emotions.isEmpty()) {
            return 1;
        }
        return (this.emotions.size() + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    public void nextPage() {
        int pages = getPageCount();
        if (pages > 1) {
            this.currentPage = (this.currentPage + 1) % pages;
            this.selectedPageItem = -1;
        }
    }

    public void prevPage() {
        int pages = getPageCount();
        if (pages > 1) {
            this.currentPage = (this.currentPage - 1 + pages) % pages;
            this.selectedPageItem = -1;
        }
    }

    public void render(DrawContext drawContext, boolean interactive) {
        if (drawContext == null || this.emotions == null || this.emotions.isEmpty()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) {
            return;
        }
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float cx = screenWidth * 0.5f;
        float cy = screenHeight * 0.5f;

        long elapsed = System.currentTimeMillis() - this.openTime;
        this.animAlpha = MathHelper.clamp(elapsed / 150.0f, 0.0f, 1.0f);

        double mouseX = mc.mouse.getX() * (double) screenWidth / (double) mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * (double) screenHeight / (double) mc.getWindow().getHeight();

        float dx = (float) (mouseX - cx);
        float dy = (float) (mouseY - cy);
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        int pages = getPageCount();
        if (this.currentPage >= pages) {
            this.currentPage = 0;
        }

        int startIdx = this.currentPage * PAGE_SIZE;
        int endIdx = Math.min(startIdx + PAGE_SIZE, this.emotions.size());
        int currentCount = endIdx - startIdx;
        if (currentCount <= 0) {
            return;
        }

        float sectorAngle = (float) (Math.PI * 2.0 / currentCount);
        float radius = Math.min(screenHeight * 0.38f, 85.0f);
        float btnW = 62.0f;
        float btnH = 20.0f;
        float fontSize = 5.5f;

        this.selectedPageItem = -1;
        if (interactive && dist > 26.0f && dist < (radius + btnW * 0.7f)) {
            float mouseAngle = (float) Math.atan2(dy, dx);
            if (mouseAngle < 0) {
                mouseAngle += (float) (Math.PI * 2.0);
            }
            int idx = (int) (mouseAngle / sectorAngle);
            if (idx >= 0 && idx < currentCount) {
                this.selectedPageItem = idx;
            }
        }

        int centerBg = ColorUtil.rgba(20, 20, 26, (int)(210 * this.animAlpha));
        int centerOutline = ColorUtil.rgba(255, 255, 255, (int)(45 * this.animAlpha));
        Render2D.rect(cx - 26.0f, cy - 26.0f, 52.0f, 52.0f, 26.0f, centerBg);
        Render2D.outline(cx - 26.0f, cy - 26.0f, 52.0f, 52.0f, 26.0f, 1.0f, centerOutline);

        String centerTitle = "Эмоции";
        float titleW = Fonts.MONTSERRAT_SEMIBOLD.width(centerTitle, 6.0f);
        if (pages > 1) {
            Fonts.MONTSERRAT_SEMIBOLD.draw(centerTitle, cx - titleW * 0.5f, cy - 7.0f, 6.0f, ColorUtil.rgba(230, 230, 230, (int)(255 * this.animAlpha)));
            String pageInfo = (this.currentPage + 1) + " / " + pages;
            float pageW = Fonts.MONTSERRAT_MEDIUM.width(pageInfo, 5.0f);
            Fonts.MONTSERRAT_MEDIUM.draw(pageInfo, cx - pageW * 0.5f, cy + 1.0f, 5.0f, ColorUtil.rgba(160, 160, 175, (int)(255 * this.animAlpha)));

            Fonts.MONTSERRAT_MEDIUM.draw("<", cx - 20.0f, cy - 3.0f, 6.0f, ColorUtil.rgba(200, 200, 210, (int)(180 * this.animAlpha)));
            Fonts.MONTSERRAT_MEDIUM.draw(">", cx + 16.0f, cy - 3.0f, 6.0f, ColorUtil.rgba(200, 200, 210, (int)(180 * this.animAlpha)));
        } else {
            Fonts.MONTSERRAT_SEMIBOLD.draw(centerTitle, cx - titleW * 0.5f, cy - 3.0f, 6.0f, ColorUtil.rgba(230, 230, 230, (int)(255 * this.animAlpha)));
        }

        for (int i = 0; i < currentCount; i++) {
            int globalIdx = startIdx + i;
            Emotion emotion = this.emotions.get(globalIdx);
            float angle = i * sectorAngle + sectorAngle * 0.5f;
            float itemX = cx + (float) Math.cos(angle) * radius;
            float itemY = cy + (float) Math.sin(angle) * radius;

            boolean isHovered = (i == this.selectedPageItem);
            int itemBg = isHovered
                ? ColorUtil.rgba(127, 242, 255, (int)(180 * this.animAlpha))
                : ColorUtil.rgba(30, 30, 38, (int)(180 * this.animAlpha));
            int itemBorder = isHovered
                ? ColorUtil.rgba(255, 255, 255, (int)(230 * this.animAlpha))
                : ColorUtil.rgba(255, 255, 255, (int)(30 * this.animAlpha));
            int textColor = isHovered
                ? ColorUtil.rgba(10, 15, 25, (int)(255 * this.animAlpha))
                : ColorUtil.rgba(230, 230, 230, (int)(255 * this.animAlpha));

            Render2D.rect(itemX - btnW * 0.5f, itemY - btnH * 0.5f, btnW, btnH, 6.0f, itemBg);
            Render2D.outline(itemX - btnW * 0.5f, itemY - btnH * 0.5f, btnW, btnH, 6.0f, 1.0f, itemBorder);
            String name = emotion.displayName();
            float textW = Fonts.MONTSERRAT_MEDIUM.width(name, fontSize);
            Fonts.MONTSERRAT_MEDIUM.draw(name, itemX - textW * 0.5f, itemY - fontSize * 0.5f, fontSize, textColor);
        }
    }

    public void selectHovered() {
        if (this.selectedPageItem >= 0) {
            int globalIdx = this.currentPage * PAGE_SIZE + this.selectedPageItem;
            if (globalIdx >= 0 && globalIdx < this.emotions.size()) {
                Emotions module = Emotions.getInstance();
                if (module != null) {
                    module.playEmotion(this.emotions.get(globalIdx));
                }
            }
        }
    }

    public int getSelectedPageItem() {
        return this.selectedPageItem;
    }

    public List<Emotion> getEmotions() {
        return this.emotions;
    }
}
