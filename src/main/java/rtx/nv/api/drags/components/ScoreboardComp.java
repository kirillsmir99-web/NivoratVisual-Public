package rtx.nv.api.drags.components;

import java.util.Collection;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.drags.Position;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.ScoreboardModule;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.settings.SettingsFactory;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;

public final class ScoreboardComp extends Draggable {
    private static ScoreboardComp INSTANCE;

    private float width = 120.0f;
    private float height = 80.0f;
    private float vanillaX = 0.0f;
    private float vanillaY = 0.0f;
    private float vanillaWidth = 120.0f;
    private float vanillaHeight = 80.0f;
    private long lastServerScoreboardTime = 0L;
    private boolean positionInitialized = false;
    private boolean userMoved = false;

    public ScoreboardComp() {
        super("scoreboard", -1.0f, -1.0f);
        this.getDrag().setForceDirectDrag(true);
        INSTANCE = this;
    }

    public static ScoreboardComp get() {
        return INSTANCE;
    }

    public static ScoreboardModule module() {
        return ModuleManager.get().get(ScoreboardModule.class);
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return this.height;
    }

    public float getVanillaX() {
        return this.vanillaX;
    }

    public float getVanillaY() {
        return this.vanillaY;
    }

    public float getVanillaWidth() {
        return this.vanillaWidth;
    }

    public float getVanillaHeight() {
        return this.vanillaHeight;
    }

    public boolean isUserMoved() {
        return this.userMoved;
    }

    public void setUserMoved(boolean moved) {
        this.userMoved = moved;
    }

    @Override
    public void resetToDefault() {
        this.userMoved = false;
        float scale = Math.max(0.001f, Render2DCoordinateSpace.guiIndependentScale());
        this.getDrag().setTargetX(this.vanillaX / scale);
        this.getDrag().setTargetY(this.vanillaY / scale);
        this.getDrag().syncToTarget();
    }

    public boolean hasActiveServerScoreboard() {
        return System.currentTimeMillis() - this.lastServerScoreboardTime < 300L;
    }

    public void setPositionInitialized(boolean initialized) {
        this.positionInitialized = initialized;
    }

    @Override
    public boolean isInteractive() {
        ScoreboardModule m = module();
        return m == null || (m.isEnabled() && m.customPosition.getValue());
    }

    @Override
    public boolean isVisible() {
        ScoreboardModule m = module();
        if (m != null && m.isEnabled() && m.hide.getValue()) {
            return false;
        }
        return true;
    }

    @Override
    protected List<Setting> buildHudSettings() {
        List<Setting> list = super.buildHudSettings();
        ScoreboardModule m = module();
        if (m != null) {
            for (rtx.nv.api.modules.settings.Setting s : m.getSettings().all()) {
                Setting ui = SettingsFactory.create(s);
                if (ui != null) {
                    list.add(ui);
                }
            }
        }
        return list;
    }

    public void updateFromObjective(DrawContext context, ScoreboardObjective objective) {
        this.lastServerScoreboardTime = System.currentTimeMillis();

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = mc.textRenderer;
        if (textRenderer == null) return;

        Scoreboard scoreboard = objective.getScoreboard();
        Collection<ScoreboardEntry> collection = scoreboard.getScoreboardEntries(objective);

        NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.EMPTY);
        int colonWidth = textRenderer.getWidth(": ");
        int count = 0;
        int maxWidth = textRenderer.getWidth(objective.getDisplayName());

        for (ScoreboardEntry entry : collection) {
            if (entry.hidden()) continue;
            count++;
            Team team = scoreboard.getScoreHolderTeam(entry.owner());
            Text decoratedName = Team.decorateName(team, entry.name());
            int entryWidth = textRenderer.getWidth(decoratedName);
            MutableText formattedScore = entry.formatted(numberFormat);
            int scoreWidth = textRenderer.getWidth(formattedScore);
            if (scoreWidth > 0) {
                entryWidth += colonWidth + scoreWidth;
            }
            if (entryWidth > maxWidth) {
                maxWidth = entryWidth;
            }
            if (count >= 15) break;
        }

        int entryCount = Math.min(count, 15);
        int entryHeight = entryCount * 9;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        // Exact vanilla calculations for scoreboard box bounds
        int n = screenHeight / 2 + entryHeight / 3;

        this.vanillaX = screenWidth - maxWidth - 5;
        this.vanillaY = n - entryHeight - 10;
        this.vanillaWidth = maxWidth + 4;
        this.vanillaHeight = entryHeight + 10;

        float scale = Math.max(0.001f, Render2DCoordinateSpace.guiIndependentScale());
        this.width = this.vanillaWidth / scale;
        this.height = this.vanillaHeight / scale;

        if (this.getDrag().isDragging()) {
            this.userMoved = true;
        }

        if (!this.userMoved) {
            this.getDrag().setTargetX(this.vanillaX / scale);
            this.getDrag().setTargetY(this.vanillaY / scale);
            this.getDrag().syncToTarget();
            this.positionInitialized = true;
        }
    }

    @Override
    protected void render(DrawContext context) {
        // No fake scoreboard preview! Only render when an actual server scoreboard is active
        if (!this.hasActiveServerScoreboard()) {
            return;
        }

        boolean dragMode = DragSystem.get().isDragModeActive();
        if (!dragMode) {
            return;
        }

        if (this.getDrag().isDragging()) {
            this.userMoved = true;
        }

        // Draw clean, perfect outline framing the scoreboard during drag mode
        if (this.getDrag().isDragging() || this.getDrag().isHovered(Position.mouseX(), Position.mouseY(), this.width, this.height)) {
            float x = this.getDrag().getRenderX();
            float y = this.getDrag().getRenderY();
            int[] colors = ClientPalette.cornerColors(0.95f);
            Render2D.beginFrame(context);
            Render2D.outline(x, y, this.width, this.height, 2.0f, 1.25f, colors[0], colors[1], colors[2], colors[3]);
            Render2D.flush();
        }
    }
}
