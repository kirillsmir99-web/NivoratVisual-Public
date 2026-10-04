package rtx.nv.api.modules.impl.Visuals;
import rtx.nv.api.events.EventHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import rtx.nv.api.drags.Position;
import rtx.nv.api.drags.RenderUnderHand;
import rtx.nv.api.events.impl.render.HudRenderEvent;
import rtx.nv.api.events.impl.render.UnderHandRenderEvent;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Utils.Globals;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.net.ClientPresence;
import rtx.nv.utils.network.Network;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.msdf.GlyphNormalizer;
import rtx.nv.utils.render.render2d.msdf.MsdfFont;
import rtx.nv.utils.render.render2d.msdf.MsdfFonts;
import rtx.nv.utils.render.underhand.UnderHand2D;

public final class NameTags
extends Module {
    private static final boolean UNDER_HAND = NameTags.class.isAnnotationPresent(RenderUnderHand.class);
    private static final String NAME_FONT = "montserrat-semibold";
    private static final String HEALTH_FONT = "montserrat-medium";
    private static final String HEART_FONT = "heart";
    private static final String HEART_GLYPH = "A";
    private static final String ICON_FONT = "nv";
    private static final String LIGHTNING_GLYPH = "L";
    private static final int LIGHTNING_CP = 9889;
    private static final String LOGO_GLYPH = "x";
    private static final float BADGE_GAP = 2.0f;
    private static final float NAME_SIZE = 7.0f;
    private static final float HEALTH_SIZE = 6.0f;
    private static final float HEART_SIZE = 6.0f;
    private static final float HEART_GAP = 2.0f;
    private static final int HEART_COLOR = -42386;
    private static final float NATIVE_CAP_H = 7.5f;
    private static final StyleSpriteSource DEFAULT_FONT = StyleSpriteSource.DEFAULT;
    private static final float PAD_X = 3.0f;
    private static final float PAD_Y = 2.0f;
    private static final float HEALTH_GAP = 3.0f;
    private static final float RADIUS = 4.0f;
    private static final int NAME_COLOR = -1;
    private static final int HEALTH_COLOR = -5196096;
    private static final int BACKGROUND_COLOR = -1778384896;
    private static final int BADGE_LEFT_COLOR = -7695373;
    private static final int BADGE_RIGHT_COLOR = -4938241;
    private static final double MAX_DISTANCE_SQR = 4096.0;
    private static final long VISIBILITY_REFRESH_MS = 150L;
    private static final long VISIBILITY_PRUNE_MS = 1000L;
    private static final float HIDDEN_SCALE = 0.85f;
    private static final float ANIM_MS = 150.0f;
    private static NameTags instance;
    private final SeparatorSetting displaySeparator = this.register(new SeparatorSetting("Отображение"));
    private final BooleanSetting otherPlayers = this.register(new BooleanSetting("Другие игроки", "Отображать таблички имён над другими игроками.", true));
    private final BooleanSetting showHealth = this.register(new BooleanSetting("Здоровье", "Показывать здоровье игрока рядом с именем.", true));
    private final SliderSetting scale = this.register(new SliderSetting("Масштаб", "Общий размер табличек.").range(0.5f, 2.0f).setValue(1.0f));

    private final SeparatorSetting selfSeparator = this.register(new SeparatorSetting("Собственный ник"));
    private final BooleanSetting showSelfTag = this.register(new BooleanSetting("Отображать свой ник", "Отображать табличку с собственным ником при виде от третьего лица.", true));
    private final ModeSetting selfTagStyle = this.register(new ModeSetting("Стиль своего ника", "Формат таблички собственного ника.", "Client", "Client", "Vanilla").visibleWhen(this.showSelfTag::getValue));
    private final List<Tag> tags = new ArrayList<Tag>();
    private final Map<Integer, VisibilityCache> visibilityCache = new HashMap<Integer, VisibilityCache>();
    private final Vector4f projectionScratch = new Vector4f();
    private long lastFrameMs = System.currentTimeMillis();
    private long frameDeltaMs = 1L;
    private static final long LAYOUT_TTL_MS = 500L;
    private boolean lastNameConfident;
    private static final Pattern EMPTY_TOKEN;
    private boolean underHandDrawn;

    public NameTags() {
        super("NameTags", "\u0420\u0438\u0441\u0443\u0435\u0442 \u0442\u0430\u0431\u043b\u0438\u0447\u043a\u0438 \u0441 \u0438\u043c\u0435\u043d\u0430\u043c\u0438 \u0442\u043e\u043b\u044c\u043a\u043e \u043d\u0430\u0434 \u0432\u0438\u0434\u0438\u043c\u044b\u043c\u0438 \u0438\u0433\u0440\u043e\u043a\u0430\u043c\u0438.", Category.VISUALS);
        instance = this;
    }

    static {
        EMPTY_TOKEN = Pattern.compile("[<\\[({\u2039\u3008\u00ab\uff1c][\\s]*empty[\\s]*[>\\])}\u203a\u3009\u00bb\uff1e]", 2);
    }

    private static String stripLeading(String string) {
        int n;
        for (n = 0; n < string.length() && Character.isWhitespace(string.charAt(n)); ++n) {
        }
        return string.substring(n);
    }

    private static String stripTrailing(String string) {
        int n;
        for (n = string.length(); n > 0 && Character.isWhitespace(string.charAt(n - 1)); --n) {
        }
        return string.substring(0, n);
    }

    private Text resolveName(PlayerEntity playerEntity) {
        Text text = NameTags.resolveDisplayName(playerEntity);
        this.lastNameConfident = text != null;
        return text != null ? text : Text.literal((String)"NPC");
    }

    public static NameTags getInstance() {
        NameTags nameTags = ModuleManager.get().get(NameTags.class);
        return nameTags != null ? nameTags : instance;
    }

    public void migrateLegacySelfTag(boolean enabled) {
        this.showSelfTag.setValue(enabled);
        if (enabled) {
            this.selfTagStyle.setSelected("Vanilla");
        }
    }

    public static boolean shouldShowSelfTagVanilla(Entity entity) {
        NameTags nt = NameTags.getInstance();
        if (nt == null) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (entity != mc.player) return false;
        if (mc.options.getPerspective().isFirstPerson()) return false;
        String val = nt.selfTagStyle.getValue();
        return nt.showSelfTag.getValue() && ("Vanilla".equals(val) || "Ванильный".equals(val));
    }

    private boolean isValid(PlayerEntity playerEntity) {
        if (playerEntity == this.mc.player) {
            String val = this.selfTagStyle.getValue();
            return this.showSelfTag.getValue() && ("Client".equals(val) || "NV".equals(val)) && !this.mc.options.getPerspective().isFirstPerson();
        }
        if (!this.otherPlayers.getValue()) {
            return false;
        }
        return playerEntity.isAlive() && !playerEntity.isRemoved() && playerEntity.getHealth() > 0.0f && !playerEntity.isInvisible();
    }

    @EventHandler
    private void onHud(HudRenderEvent hudRenderEvent) {
        boolean bl = this.underHandDrawn;
        this.underHandDrawn = false;
        if (UNDER_HAND && bl || this.tags.isEmpty()) {
            return;
        }
        DrawContext drawContext = hudRenderEvent.getGraphics();
        for (Tag tag : this.tags) {
            boolean bl2;
            boolean bl3 = bl2 = !NameTags.isSettled(tag);
            if (bl2) {
                float f = tag.boxX + tag.boxWidth * 0.5f;
                float f2 = tag.boxY + tag.boxHeight * 0.5f;
                drawContext.getMatrices().pushMatrix();
                drawContext.getMatrices().translate(f, f2);
                drawContext.getMatrices().scale(tag.scale, tag.scale);
                drawContext.getMatrices().translate(-f, -f2);
            }
            Render2D.beginFrame(drawContext);
            this.drawTagBackground(tag);
            Render2D.flush();
            Render2D.beginFrame(drawContext);
            this.drawTagText(drawContext, tag);
            Render2D.flush();
            if (!bl2) continue;
            drawContext.getMatrices().popMatrix();
        }
    }

    @Override
    protected void onDisable() {
        this.tags.clear();
        this.visibilityCache.clear();
    }

    private boolean isVisible(PlayerEntity playerEntity, float f, long l) {
        float f2;
        VisibilityCache visibilityCache = this.visibilityCache.get(playerEntity.getId());
        if (visibilityCache == null) {
            visibilityCache = new VisibilityCache();
            this.visibilityCache.put(playerEntity.getId(), visibilityCache);
        }
        if (l - visibilityCache.checkedAtMs >= 150L) {
            visibilityCache.visible = this.computeVisible(playerEntity, f);
            visibilityCache.checkedAtMs = l;
        }
        visibilityCache.touchedAtMs = l;
        float f3 = visibilityCache.visible ? 1.0f : -1.0f;
        visibilityCache.anim = MathHelper.clamp((float)(visibilityCache.anim + f3 * ((float)this.frameDeltaMs / 150.0f)), (float)0.0f, (float)1.0f);
        visibilityCache.alpha = f2 = visibilityCache.anim * visibilityCache.anim * (3.0f - 2.0f * visibilityCache.anim);
        visibilityCache.scale = 0.85f + 0.14999998f * f2;
        return visibilityCache.visible;
    }

    public static boolean hidesVanillaNameTag(PlayerEntity playerEntity) {
        NameTags nameTags = NameTags.getInstance();
        if (nameTags == null || playerEntity == null) {
            return false;
        }
        if (nameTags.mc.player == null) {
            return false;
        }
        if (playerEntity == nameTags.mc.player) {
            if (nameTags.mc.options.getPerspective().isFirstPerson()) {
                return true;
            }
            String val = nameTags.selfTagStyle.getValue();
            if (nameTags.showSelfTag.getValue() && ("Vanilla".equals(val) || "Ванильный".equals(val))) {
                return false;
            }
            return true;
        }
        if (!nameTags.isEnabled() || !nameTags.otherPlayers.getValue()) {
            return false;
        }
        if (!nameTags.isValid(playerEntity)) {
            return false;
        }
        return nameTags.mc.player.squaredDistanceTo((Entity)playerEntity) <= 4096.0;
    }

    public static boolean hidesNameTagFor(Entity entity) {
        if (entity instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity)entity;
            return NameTags.hidesVanillaNameTag(playerEntity);
        }
        NameTags nameTags = NameTags.getInstance();
        if (nameTags == null || !nameTags.isEnabled() || entity == null) {
            return false;
        }
        if (nameTags.mc.player == null || nameTags.mc.world == null) {
            return false;
        }
        double d = entity.getX();
        double d2 = entity.getY();
        double d3 = entity.getZ();
        for (PlayerEntity playerEntity : nameTags.mc.world.getPlayers()) {
            double d4;
            double d5;
            double d6;
            if (!nameTags.isValid(playerEntity) || nameTags.mc.player.squaredDistanceTo((Entity)playerEntity) > 4096.0 || (d6 = d - playerEntity.getX()) * d6 + (d5 = d3 - playerEntity.getZ()) * d5 > 2.25 || !(d2 >= (d4 = playerEntity.getY() + (double)playerEntity.getHeight()) - 1.0) || !(d2 <= d4 + 7.0)) continue;
            return true;
        }
        return false;
    }

    private void ensureLayout(VisibilityCache visibilityCache, Text text, int n) {
        String string = text.getString();
        String string2 = text.toString();
        long l = System.currentTimeMillis();
        if (visibilityCache.segs != null && visibilityCache.builtSize == n && string2.equals(visibilityCache.nameSig) && l - visibilityCache.layoutAtMs < 500L) {
            return;
        }
        visibilityCache.nameSig = string2;
        visibilityCache.builtSize = n;
        visibilityCache.layoutAtMs = l;
        List<Seg> list = this.buildSegments(NameTags.parseRuns(text, string), n);
        float f = 0.0f;
        for (Seg seg : list) {
            f += seg.width;
        }
        visibilityCache.segs = list;
        visibilityCache.nameWidth = f;
    }

    private static String formatHealth(float f) {
        return Integer.toString(MathHelper.ceil((float)f));
    }

    private void ensureHealthLayout(VisibilityCache visibilityCache, String string, int n, int n2) {
        int n3 = n << 16 | n2;
        if (n3 == visibilityCache.healthBuiltSize && string.equals(visibilityCache.healthSig)) {
            return;
        }
        visibilityCache.healthSig = string;
        visibilityCache.healthBuiltSize = n3;
        visibilityCache.heartWidth = Render2D.msdfWidth(HEART_FONT, HEART_GLYPH, n2);
        visibilityCache.healthNumberWidth = Render2D.msdfWidth(HEALTH_FONT, string, n);
    }

    private static String normalizeCodePoint(MsdfFont msdfFont, StyleSpriteSource styleSpriteSource, int n) {
        int n2;
        if (msdfFont == null || styleSpriteSource != null && !styleSpriteSource.equals((Object)DEFAULT_FONT) || msdfFont.hasGlyph(n)) {
            return null;
        }
        String string = GlyphNormalizer.normalize((int)n);
        if (string == null) {
            return null;
        }
        for (int i = 0; i < string.length(); i += Character.charCount(n2)) {
            n2 = string.codePointAt(i);
            if (Character.isWhitespace(n2) || msdfFont.hasGlyph(n2)) continue;
            return null;
        }
        return string;
    }

    private void drawTagText(DrawContext drawContext, Tag tag) {
        float f = tag.boxY + (float)tag.padY;
        float f2 = f - (float)tag.nudge;
        if (tag.badge) {
            Render2D.msdfText(ICON_FONT, LOGO_GLYPH, tag.boxX + (float)tag.padX, f2 + 1.0f, (float)tag.nameSize, NameTags.badgeLeft(tag.alpha), NameTags.badgeRight(tag.alpha), NameTags.badgeRight(tag.alpha), NameTags.badgeLeft(tag.alpha));
        }
        float f3 = tag.boxX + (float)tag.padX + tag.badgeWidth;
        for (Seg seg : tag.segs) {
            int n = ColorUtil.multAlpha(seg.color, tag.alpha);
            if (seg.kind == 0) {
                boolean bl = ICON_FONT.equals(seg.msdfFont);
                float f4 = bl ? 2.0f : 0.0f;
                float f5 = bl ? 1.0f : 0.0f;
                Render2D.msdfText(seg.msdfFont, seg.text, f3 + f4, f2 + f5, tag.nameSize, n);
            } else {
                float f6 = Render2DCoordinateSpace.guiIndependentScale();
                drawContext.getMatrices().pushMatrix();
                drawContext.getMatrices().scale(f6, f6);
                drawContext.getMatrices().translate(f3, f2 + 1.0f);
                drawContext.getMatrices().scale(seg.nativeScale, seg.nativeScale);
                if (seg.nativeFont == null || seg.nativeFont.equals((Object)DEFAULT_FONT)) {
                    drawContext.drawText(this.mc.textRenderer, seg.text, 0, 0, n, false);
                } else {
                    drawContext.drawText(this.mc.textRenderer, (Text)Text.literal((String)seg.text).setStyle(Style.EMPTY.withFont(seg.nativeFont)), 0, 0, n, false);
                }
                drawContext.getMatrices().popMatrix();
            }
            f3 += seg.width;
        }
        if (tag.health != null) {
            float f7 = tag.boxX + (float)tag.padX + tag.badgeWidth + tag.nameWidth + (float)tag.healthGap;
            Render2D.msdfText(HEART_FONT, HEART_GLYPH, f7, f + (float)tag.nudge, tag.heartSize, ColorUtil.multAlpha(-42386, tag.alpha));
            Render2D.msdfText(HEALTH_FONT, tag.health, f7 += tag.heartWidth + (float)tag.heartGap, f, tag.healthSize, ColorUtil.multAlpha(-5196096, tag.alpha));
        }
    }

    private boolean computeVisible(PlayerEntity playerEntity, float f) {
        Vec3d vec3d = this.mc.player.getCameraPosVec(f);
        if (this.rayClear(vec3d, playerEntity.getCameraPosVec(f), playerEntity)) {
            return true;
        }
        Vec3d vec3d2 = playerEntity.getLerpedPos(f).add(0.0, (double)playerEntity.getHeight() * 0.5, 0.0);
        return this.rayClear(vec3d, vec3d2, playerEntity);
    }

    private static Text firstReadableLine(Text text) {
        if (text == null) {
            return null;
        }
        ArrayList<MutableText> arrayList = new ArrayList<MutableText>();
        MutableText[] mutableTextArray = new MutableText[]{Text.empty()};
        text.visit((style, string) -> {
            int n = 0;
            for (int i = 0; i < string.length(); ++i) {
                if (string.charAt(i) != '\n') continue;
                String string2 = string.substring(n, i);
                if (!string2.isEmpty()) {
                    mutableTextArray[0].append((Text)Text.literal((String)string2).setStyle(style));
                }
                arrayList.add(mutableTextArray[0]);
                mutableTextArray[0] = Text.empty();
                n = i + 1;
            }
            String string3 = string.substring(n);
            if (!string3.isEmpty()) {
                mutableTextArray[0].append((Text)Text.literal((String)string3).setStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        arrayList.add(mutableTextArray[0]);
        for (MutableText mutableText : arrayList) {
            if (NameTags.isMissingName((Text)mutableText)) continue;
            return mutableText;
        }
        return null;
    }

    private static Text buildPlayerName(Text text, String string) {
        MutableText mutableText = Text.empty();
        if (text != null && !text.getString().isEmpty()) {
            mutableText.append((Text)text.copy());
            if (!text.getString().endsWith(" ")) {
                mutableText.append((Text)Text.literal((String)" "));
            }
        }
        mutableText.append((Text)Text.literal((String)NameTags.stripEmptyTokens(string)).formatted(Formatting.WHITE));
        return mutableText;
    }

    public static Text resolveDisplayName(PlayerEntity playerEntity) {
        return NameTags.resolveDisplayName0(playerEntity);
    }

    public static int copyUnderHandBounds(float[] fArray) {
        NameTags nameTags = NameTags.getInstance();
        if (fArray == null || fArray.length < 4 || nameTags == null || !nameTags.isEnabled() || !nameTags.underHandDrawn) {
            return 0;
        }
        int n = 0;
        for (Tag tag : nameTags.tags) {
            if (tag.alpha <= 0.01f || n * 4 + 3 >= fArray.length) continue;
            float f = NameTags.isSettled(tag) ? 1.0f : tag.scale;
            float f2 = tag.boxX + tag.boxWidth * 0.5f;
            float f3 = tag.boxY + tag.boxHeight * 0.5f;
            int n2 = n * 4;
            fArray[n2] = NameTags.sx(f2, tag.boxX, f) - 2.0f;
            fArray[n2 + 1] = NameTags.sy(f3, tag.boxY, f) - 2.0f;
            fArray[n2 + 2] = tag.boxWidth * f + 4.0f;
            fArray[n2 + 3] = tag.boxHeight * f + 4.0f;
            ++n;
        }
        return n;
    }

    private static boolean isMissingName(String string) {
        if (string == null) {
            return true;
        }
        String string2 = string.replaceAll("(?i)\u00a7[0-9a-fk-or]", "");
        if ((string2 = NameTags.stripEmptyTokens(string2.trim())).isEmpty()) {
            return true;
        }
        String string3 = string2.replaceAll("^[\\p{P}\\p{S}\\s]+", "").replaceAll("[\\p{P}\\p{S}\\s]+$", "");
        return string3.isEmpty() || string3.equalsIgnoreCase("empty");
    }

    private static boolean isMissingName(Text text) {
        return text == null || NameTags.isMissingName(text.getString());
    }

    private void drawTagBackground(Tag tag) {
        Render2D.rect(tag.boxX, tag.boxY, tag.boxWidth, tag.boxHeight, tag.radius, ColorUtil.multAlpha(-1778384896, tag.alpha));
    }

    private List<Seg> buildSegments(List<TextRun> list, int n) {
        ArrayList<Seg> arrayList = new ArrayList<Seg>();
        MsdfFont msdfFont = MsdfFonts.get(NAME_FONT);
        float f = Math.max(1.0f, (float)Math.round((float)n / 7.5f));
        for (TextRun textRun : list) {
            if (textRun.iconFont() != null) {
                float f2 = Render2D.msdfWidth(textRun.iconFont(), textRun.text(), n);
                arrayList.add(new Seg(0, textRun.text(), textRun.iconFont(), null, textRun.color(), f2, 1.0f));
                continue;
            }
            String string = textRun.text();
            int n2 = 0;
            while (n2 < string.length()) {
                int n3 = n2;
                boolean bl = NameTags.covered(textRun.font(), msdfFont, string.codePointAt(n2));
                while (n2 < string.length() && NameTags.covered(textRun.font(), msdfFont, string.codePointAt(n2)) == bl) {
                    n2 += Character.charCount(string.codePointAt(n2));
                }
                String string2 = string.substring(n3, n2);
                if (bl) {
                    arrayList.add(new Seg(0, string2, NAME_FONT, null, textRun.color(), Render2D.msdfWidth(NAME_FONT, string2, n), 1.0f));
                    continue;
                }
                arrayList.add(new Seg(1, string2, null, textRun.font(), textRun.color(), (float)this.nativeWidth(string2, textRun.font()) * f, f));
            }
        }
        return arrayList;
    }

    private int nativeWidth(String string, StyleSpriteSource styleSpriteSource) {
        if (styleSpriteSource == null || styleSpriteSource.equals((Object)DEFAULT_FONT)) {
            return this.mc.textRenderer.getWidth(string);
        }
        return this.mc.textRenderer.getWidth((StringVisitable)Text.literal((String)string).setStyle(Style.EMPTY.withFont(styleSpriteSource)));
    }

    public static String displayGlyphCovered(int n, boolean bl) {
        int n2;
        String string = NameTags.displayGlyph(n, bl);
        MsdfFont msdfFont = MsdfFonts.get(NAME_FONT);
        if (msdfFont == null) {
            return string;
        }
        for (int i = 0; i < string.length(); i += Character.charCount(n2)) {
            n2 = string.codePointAt(i);
            if (Character.isWhitespace(n2) || msdfFont.hasGlyph(n2)) continue;
            return "";
        }
        return string;
    }

    private static Text resolveDisplayName0(PlayerEntity playerEntity) {
        Text text;
        Team team = playerEntity.getScoreboardTeam();
        if (team instanceof Team) {
            Team team2 = team;
            text = team2.getPrefix();
        } else {
            text = null;
        }
        Text text2 = text;
        boolean bl = text2 != null && !NameTags.isMissingName(text2.getString());
        String string = playerEntity.getName().getString();
        if (!NameTags.isMissingName(string)) {
            return NameTags.buildPlayerName((Text)(bl ? text2 : null), string);
        }
        if (bl) {
            return text2.copy();
        }
        Text text3 = NameTags.findTopHologramLine(playerEntity);
        if (text3 != null) {
            return text3;
        }
        Text text4 = playerEntity.getCustomName();
        if (!NameTags.isMissingName(text4)) {
            return text4.copy();
        }
        String string2 = playerEntity.getGameProfile().name();
        if (!NameTags.isMissingName(string2)) {
            return NameTags.buildPlayerName(null, string2);
        }
        String string3 = playerEntity.getNameForScoreboard();
        if (!NameTags.isMissingName(string3)) {
            return NameTags.buildPlayerName(null, string3);
        }
        return null;
    }

    public static String displayGlyph(int n, boolean bl) {
        String string = NameTags.normalizeCodePoint(MsdfFonts.get(NAME_FONT), DEFAULT_FONT, n);
        if (string != null) {
            return string;
        }
        return new String(Character.toChars(bl ? Character.toUpperCase(n) : n));
    }

    private void drawTagUnderHand(UnderHand2D underHand2D, Tag tag, float f) {
        float f2 = tag.boxX + tag.boxWidth * 0.5f;
        float f3 = tag.boxY + tag.boxHeight * 0.5f;
        underHand2D.rect(NameTags.sx(f2, tag.boxX, f), NameTags.sy(f3, tag.boxY, f), tag.boxWidth * f, tag.boxHeight * f, (float)tag.radius * f, ColorUtil.multAlpha(-1778384896, tag.alpha));
        float f4 = tag.boxY + (float)tag.padY;
        float f5 = f4 - (float)tag.nudge;
        if (tag.badge) {
            underHand2D.msdfText(ICON_FONT, LOGO_GLYPH, NameTags.sx(f2, tag.boxX + (float)tag.padX, f), NameTags.sy(f3, f5 + 1.0f, f), (float)tag.nameSize * f, NameTags.badgeLeft(tag.alpha), NameTags.badgeRight(tag.alpha), NameTags.badgeRight(tag.alpha), NameTags.badgeLeft(tag.alpha));
        }
        float f6 = tag.boxX + (float)tag.padX + tag.badgeWidth;
        for (Seg seg : tag.segs) {
            int n = ColorUtil.multAlpha(seg.color, tag.alpha);
            if (seg.kind == 0) {
                boolean bl = ICON_FONT.equals(seg.msdfFont);
                float f7 = bl ? 2.0f : 0.0f;
                float f8 = bl ? 1.0f : 0.0f;
                underHand2D.msdfText(seg.msdfFont, seg.text, NameTags.sx(f2, f6 + f7, f), NameTags.sy(f3, f5 + f8, f), (float)tag.nameSize * f, n);
            } else {
                MutableText mutableText = seg.nativeFont == null || seg.nativeFont.equals((Object)DEFAULT_FONT) ? Text.literal((String)seg.text) : Text.literal((String)seg.text).setStyle(Style.EMPTY.withFont(seg.nativeFont));
                underHand2D.nativeText((Text)mutableText, NameTags.sx(f2, f6, f), NameTags.sy(f3, f5 + 1.0f, f), seg.nativeScale * f, n);
            }
            f6 += seg.width;
        }
        if (tag.health != null) {
            float f9 = tag.boxX + (float)tag.padX + tag.badgeWidth + tag.nameWidth + (float)tag.healthGap;
            underHand2D.msdfText(HEART_FONT, HEART_GLYPH, NameTags.sx(f2, f9, f), NameTags.sy(f3, f4 + (float)tag.nudge, f), (float)tag.heartSize * f, ColorUtil.multAlpha(-42386, tag.alpha));
            underHand2D.msdfText(HEALTH_FONT, tag.health, NameTags.sx(f2, f9 += tag.heartWidth + (float)tag.heartGap, f), NameTags.sy(f3, f4, f), (float)tag.healthSize * f, ColorUtil.multAlpha(-5196096, tag.alpha));
        }
    }

    @EventHandler
    private void onUnderHand(UnderHandRenderEvent underHandRenderEvent) {
        if (!UNDER_HAND || this.tags.isEmpty()) {
            return;
        }
        this.underHandDrawn = true;
        UnderHand2D underHand2D = underHandRenderEvent.ctx();
        for (Tag tag : this.tags) {
            float f = NameTags.isSettled(tag) ? 1.0f : tag.scale;
            this.drawTagUnderHand(underHand2D, tag, f);
            underHand2D.barrier();
        }
    }

    private static String stripEmptyTokens(String string) {
        return EMPTY_TOKEN.matcher(string).replaceAll("").trim();
    }

    private static Text findTopHologramLine(PlayerEntity playerEntity) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.world == null) {
            return null;
        }
        Text bestText = null;
        double d = Double.NEGATIVE_INFINITY;
        double d2 = playerEntity.getY() + (double)playerEntity.getHeight();
        for (Entity entity : minecraftClient.world.getEntities()) {
            if (entity == playerEntity || entity instanceof PlayerEntity) continue;
            double d3 = entity.getX() - playerEntity.getX();
            double d4 = entity.getZ() - playerEntity.getZ();
            double d5 = entity.getY();
            if (d3 * d3 + d4 * d4 > 2.25 || d5 < d2 - 1.0 || d5 > d2 + 7.0) continue;
            Text text2 = entity instanceof DisplayEntity.TextDisplayEntity textDisplay ? textDisplay.getText() : entity.getCustomName();
            Text readableLine = NameTags.firstReadableLine(text2);
            if (readableLine == null || !(d5 > d)) continue;
            bestText = readableLine;
            d = d5;
        }
        return bestText;
    }

    private static float sy(float f, float f2, float f3) {
        return f + (f2 - f) * f3;
    }

    private static float sx(float f, float f2, float f3) {
        return f + (f2 - f) * f3;
    }

    private void pruneCache(long l) {
        if (this.visibilityCache.isEmpty()) {
            return;
        }
        this.visibilityCache.values().removeIf(visibilityCache -> l - visibilityCache.touchedAtMs > 1000L);
    }

    private static int roundSize(float f) {
        return Math.max(1, Math.round(f));
    }

    private static List<TextRun> parseRuns(Text text, String string2) {
        ArrayList<TextRun> arrayList = new ArrayList<TextRun>();
        MsdfFont msdfFont = MsdfFonts.get(NAME_FONT);
        boolean bl = string2.codePoints().anyMatch(GlyphNormalizer::isSmallCap);
        StringBuilder stringBuilder = new StringBuilder();
        int[] nArray = new int[]{-1};
        StyleSpriteSource[] styleSpriteSourceArray = new StyleSpriteSource[]{DEFAULT_FONT};
        text.visit((style2, string) -> {
            TextVisitFactory.visitFormatted((String)string, (Style)style2, (n, style, n2) -> {
                int n3 = style.getColor() != null ? 0xFF000000 | style.getColor().getRgb() : -1;
                StyleSpriteSource styleSpriteSource = DEFAULT_FONT;
                if (n2 == 9889) {
                    if (!stringBuilder.isEmpty()) {
                        arrayList.add(new TextRun(stringBuilder.toString(), nArray[0], styleSpriteSourceArray[0]));
                        stringBuilder.setLength(0);
                    }
                    arrayList.add(new TextRun(LIGHTNING_GLYPH, n3, DEFAULT_FONT, ICON_FONT));
                    return true;
                }
                if (!(n3 == nArray[0] && styleSpriteSource.equals((Object)styleSpriteSourceArray[0]) || stringBuilder.isEmpty())) {
                    arrayList.add(new TextRun(stringBuilder.toString(), nArray[0], styleSpriteSourceArray[0]));
                    stringBuilder.setLength(0);
                }
                nArray[0] = n3;
                styleSpriteSourceArray[0] = styleSpriteSource;
                String norm = NameTags.normalizeCodePoint(msdfFont, styleSpriteSource, n2);
                if (norm != null) {
                    stringBuilder.append(norm);
                } else if (bl) {
                    stringBuilder.appendCodePoint(Character.toUpperCase(n2));
                } else {
                    stringBuilder.appendCodePoint(n2);
                }
                return true;
            });
            return Optional.empty();
        }, Style.EMPTY);
        if (!stringBuilder.isEmpty()) {
            arrayList.add(new TextRun(stringBuilder.toString(), nArray[0], styleSpriteSourceArray[0]));
        }
        NameTags.trimRuns(arrayList);
        if (arrayList.isEmpty()) {
            arrayList.add(new TextRun(string2, -1, DEFAULT_FONT));
        }
        return arrayList;
    }

    private static int badgeLeft(float f) {
        return ColorUtil.multAlpha(-7695373, f);
    }

    private static boolean isSettled(Tag tag) {
        return Math.abs(tag.scale - 1.0f) < 0.001f;
    }

    public static boolean isStylized(String string) {
        return string != null && string.codePoints().anyMatch(GlyphNormalizer::isSmallCap);
    }

    private boolean rayClear(Vec3d vec3d, Vec3d vec3d2, PlayerEntity playerEntity) {
        BlockHitResult blockHitResult = this.mc.world.raycast(new RaycastContext(vec3d, vec3d2, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, (Entity)playerEntity));
        return blockHitResult == null || blockHitResult.getType() == HitResult.Type.MISS;
    }

    private static int badgeRight(float f) {
        return ColorUtil.multAlpha(-4938241, f);
    }

    private static void trimRuns(List<TextRun> list) {
        String string;
        int n;
        TextRun textRun;
        String string2;
        while (!list.isEmpty() && NameTags.edgeBlank(list.get(0))) {
            list.remove(0);
        }
        while (!list.isEmpty() && NameTags.edgeBlank(list.get(list.size() - 1))) {
            list.remove(list.size() - 1);
        }
        if (list.isEmpty()) {
            return;
        }
        TextRun textRun2 = list.get(0);
        if (NameTags.plainText(textRun2) && !(string2 = NameTags.stripLeading(textRun2.text())).equals(textRun2.text())) {
            list.set(0, new TextRun(string2, textRun2.color(), textRun2.font(), textRun2.iconFont()));
        }
        if (NameTags.plainText(textRun = list.get(n = list.size() - 1)) && !(string = NameTags.stripTrailing(textRun.text())).equals(textRun.text())) {
            list.set(n, new TextRun(string, textRun.color(), textRun.font(), textRun.iconFont()));
        }
        while (!list.isEmpty() && NameTags.edgeBlank(list.get(0))) {
            list.remove(0);
        }
        while (!list.isEmpty() && NameTags.edgeBlank(list.get(list.size() - 1))) {
            list.remove(list.size() - 1);
        }
    }

    private static boolean edgeBlank(TextRun textRun) {
        if (textRun.iconFont() != null) {
            return false;
        }
        if (textRun.font() != null && !textRun.font().equals((Object)DEFAULT_FONT)) {
            return textRun.text().isEmpty();
        }
        return textRun.text().isBlank();
    }

    private static boolean covered(StyleSpriteSource styleSpriteSource, MsdfFont msdfFont, int n) {
        if (styleSpriteSource != null && !styleSpriteSource.equals((Object)DEFAULT_FONT)) {
            return false;
        }
        if (n == 32 || Character.isWhitespace(n)) {
            return true;
        }
        return msdfFont != null && msdfFont.hasGlyph(n);
    }

    private static boolean plainText(TextRun textRun) {
        return textRun.iconFont() == null && (textRun.font() == null || textRun.font().equals((Object)DEFAULT_FONT));
    }

    private Vector4f project(Matrix4f matrix4f, Matrix4f matrix4f2, Vec3d vec3d, double d, double d2, double d3) {
        Vector4f vector4f = this.projectionScratch.set((float)(d - vec3d.x), (float)(d2 - vec3d.y), (float)(d3 - vec3d.z), 1.0f);
        matrix4f.transform(vector4f);
        matrix4f2.transform(vector4f);
        if (vector4f.w <= 1.0E-4f) {
            return null;
        }
        return vector4f;
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent worldRenderEvent) {
        this.tags.clear();
        if (this.mc.player == null || this.mc.world == null) {
            return;
        }
        long l = System.currentTimeMillis();
        this.frameDeltaMs = Math.max(1L, Math.min(l - this.lastFrameMs, 100L));
        this.lastFrameMs = l;
        float f = worldRenderEvent.getPartialTicks();
        Vec3d vec3d = worldRenderEvent.getCamera() == null ? this.mc.gameRenderer.getCamera().getCameraPos() : worldRenderEvent.getCamera().getCameraPos();
        float f2 = Position.screenWidth();
        float f3 = Position.screenHeight();
        Matrix4f matrix4f = worldRenderEvent.getPositionMatrix();
        Matrix4f matrix4f2 = worldRenderEvent.getProjectionMatrix();
        boolean bl = this.showHealth.getValue();
        boolean bl2 = Globals.tagsBadge();
        float f4 = this.scale.getFloat();
        int n = NameTags.roundSize(7.0f * f4);
        int n2 = NameTags.roundSize(6.0f * f4);
        int n3 = NameTags.roundSize(6.0f * f4);
        int n4 = Math.round(3.0f * f4);
        int n5 = Math.round(2.0f * f4);
        int n6 = Math.round(2.0f * f4);
        int n7 = Math.round(3.0f * f4);
        int n8 = Math.round(4.0f * f4);
        int n9 = Math.max(1, Math.round(f4));
        for (PlayerEntity playerEntity : this.mc.world.getPlayers()) {
            float f5;
            if (!this.isValid(playerEntity) || this.mc.player.squaredDistanceTo((Entity)playerEntity) > 4096.0) continue;
            boolean bl3 = this.isVisible(playerEntity, f, l);
            VisibilityCache visibilityCache = this.visibilityCache.get(playerEntity.getId());
            float f6 = f5 = visibilityCache == null ? 0.0f : visibilityCache.alpha;
            if (f5 <= 0.01f && !bl3) continue;
            Vec3d vec3d2 = playerEntity.getLerpedPos(f);
            double d = vec3d2.y + (double)playerEntity.getHeight() + 0.35;
            Vector4f vector4f = this.project(matrix4f, matrix4f2, vec3d, vec3d2.x, d, vec3d2.z);
            if (vector4f == null) continue;
            float f7 = (vector4f.x / vector4f.w * 0.5f + 0.5f) * f2;
            float f8 = (1.0f - (vector4f.y / vector4f.w * 0.5f + 0.5f)) * f3;
            if (Float.isNaN(f7) || Float.isNaN(f8)) continue;
            Text text = this.resolveName(playerEntity);
            if (this.lastNameConfident) {
                visibilityCache.resolvedName = text;
            } else {
                if (visibilityCache.resolvedName == null) continue;
                text = visibilityCache.resolvedName;
            }
            this.ensureLayout(visibilityCache, text, n);
            String string = bl ? NameTags.formatHealth(Network.getResolvedHealth((LivingEntity)playerEntity, true)) : null;
            float f9 = 0.0f;
            float f10 = 0.0f;
            if (string != null) {
                this.ensureHealthLayout(visibilityCache, string, n2, n3);
                f9 = visibilityCache.heartWidth;
                f10 = visibilityCache.healthNumberWidth;
            }
            Tag tag3 = new Tag();
            tag3.segs = visibilityCache.segs;
            tag3.nameWidth = visibilityCache.nameWidth;
            tag3.health = string;
            tag3.heartWidth = f9;
            tag3.healthNumberWidth = f10;
            tag3.nameSize = n;
            tag3.healthSize = n2;
            tag3.heartSize = n3;
            tag3.padX = n4;
            tag3.padY = n5;
            tag3.heartGap = n6;
            tag3.healthGap = n7;
            tag3.radius = n8;
            tag3.nudge = n9;
            tag3.alpha = f5;
            tag3.scale = visibilityCache.scale;
            boolean bl4 = bl2 && ClientPresence.INSTANCE.isNvUser(playerEntity.getGameProfile().name());
            float f11 = bl4 ? Render2D.msdfWidth(ICON_FONT, LOGO_GLYPH, n) + 2.0f : 0.0f;
            tag3.badge = bl4;
            tag3.badgeWidth = f11;
            float f12 = string == null ? 0.0f : f9 + (float)n6 + f10;
            tag3.boxWidth = (float)n4 * 2.0f + f11 + visibilityCache.nameWidth + (string == null ? 0.0f : (float)n7) + f12;
            tag3.boxHeight = (float)n + (float)n5 * 2.0f;
            tag3.boxX = f7 - tag3.boxWidth * 0.5f;
            tag3.boxY = f8 - tag3.boxHeight;
            tag3.depth = vector4f.w;
            this.tags.add(tag3);
        }
        this.tags.sort((tag, tag2) -> Float.compare(tag2.depth, tag.depth));
        this.pruneCache(l);
    }


    public static final class Tag {
        public List<Seg> segs;
        public float nameWidth;
        public String health;
        public float heartWidth, healthNumberWidth;
        public int nameSize, healthSize, heartSize;
        public int padX, padY, heartGap, healthGap, radius, nudge;
        public float alpha, scale;
        public boolean badge;
        public float badgeWidth, boxWidth, boxHeight, boxX, boxY, depth;
    }

    public static final class TextRun {
        private final String text;
        private final int color;
        private final StyleSpriteSource font;
        private final String iconFont;

        public TextRun(String text, int color, StyleSpriteSource font, String iconFont) {
            this.text = text;
            this.color = color;
            this.font = font;
            this.iconFont = iconFont;
        }
        public TextRun(String text, int color, StyleSpriteSource font) { this(text, color, font, null); }
        public TextRun(String text, int color) { this(text, color, DEFAULT_FONT, null); }
        public String text() { return text; }
        public int color() { return color; }
        public String iconFont() { return iconFont; }
        public StyleSpriteSource font() { return font; }
    }

    public static final class VisibilityCache {
        public List<Seg> segs;
        public float nameWidth, heartWidth, healthNumberWidth, scale, alpha, anim;
        public long checkedAtMs, touchedAtMs, layoutAtMs;
        public boolean visible;
        public int builtSize, healthBuiltSize;
        public String nameSig, healthSig;
        public Text resolvedName;
    }

    public static record Seg(int kind, String text, String msdfFont, StyleSpriteSource nativeFont, int color, float width, float nativeScale) {} {
    }
}