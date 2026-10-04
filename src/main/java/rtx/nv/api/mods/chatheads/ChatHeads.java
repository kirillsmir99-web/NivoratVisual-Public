package rtx.nv.api.mods.chatheads;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix3x2fStack;
import rtx.nv.api.mods.chatheads.ComponentProcessor;
import rtx.nv.api.mods.chatheads.HeadData;
import rtx.nv.api.mods.chatheads.config.ChatHeadsConfig;
import rtx.nv.api.mods.chatheads.config.ChatHeadsConfigDefaults;
import rtx.nv.api.mods.chatheads.config.RenderPosition;
import rtx.nv.api.mods.chatheads.config.SenderDetection;
import rtx.nv.api.mods.chatheads.mixininterface.HeadRenderable;
import rtx.nv.api.mods.chatheads.mixininterface.Ownable;

public class ChatHeads {
    public static final String MOD_ID = "chat_heads";
    public static final Pattern FORMAT_REGEX = Pattern.compile("\u00a7.");
    public static final Logger LOGGER = LogManager.getLogger((String)"chat_heads");
    public static final Identifier DISABLE_RESOURCE = Identifier.of((String)"chat_heads", (String)"disable");
    public static ChatHeadsConfig CONFIG = new ChatHeadsConfigDefaults();
    public static HeadData lastSenderData = HeadData.EMPTY;
    public static boolean refreshing;
    public static HeadData lineData;
    public static HeadData refreshingLineData;
    public static volatile boolean serverSentUuid;
    public static volatile boolean serverDisabledChatHeads;
    public static final Set<Identifier> blendedHeadTextures;
    public static DrawContext guiGraphics;
    public static ChatHud.Backend chatGraphicsAccess;
    public static boolean customHeadRendering;

    static {
        lineData = HeadData.EMPTY;
        refreshingLineData = HeadData.EMPTY;
        serverSentUuid = false;
        serverDisabledChatHeads = false;
        blendedHeadTextures = new HashSet<Identifier>();
        guiGraphics = null;
        chatGraphicsAccess = null;
    }

    public static void init() {
    }

    public static PlayerListEntry getOwner(SignedMessage signedMessage) {
        return ((Ownable)(Object)signedMessage).chatheads_getOwner();
    }

    public static void setOwner(SignedMessage signedMessage, PlayerListEntry playerListEntry) {
        ((Ownable)(Object)signedMessage).chatheads_setOwner(playerListEntry);
    }

    public static boolean offsetChat(HeadData headData) {
        if (CONFIG.renderPosition() != RenderPosition.BEFORE_LINE) {
            return false;
        }
        return headData != HeadData.EMPTY || CONFIG.offsetNonPlayerText() && !serverDisabledChatHeads;
    }

    public static int headWidth() {
        return ChatHeads.headWidth(CONFIG.drawShadow());
    }

    public static int headWidth(boolean bl) {
        return 10 + (bl ? 1 : 0);
    }

    public static HeadData getHeadData(ChatHudLine chatHudLine) {
        return ((HeadRenderable)(Object)chatHudLine).chatheads_getHeadData();
    }

    public static HeadData getHeadData(ChatHudLine.Visible visible) {
        return ((HeadRenderable)(Object)visible).chatheads_getHeadData();
    }

    public static void setHeadData(ChatHudLine chatHudLine, HeadData headData) {
        ((HeadRenderable)(Object)chatHudLine).chatheads_setHeadData(headData);
    }

    public static int getChatOffset(HeadData headData) {
        return ChatHeads.offsetChat(headData) ? ChatHeads.headWidth() : 0;
    }

    public static int getTextWidthDifference(HeadData headData) {
        if (CONFIG.renderPosition() != RenderPosition.BEFORE_LINE) {
            return 0;
        }
        return headData != HeadData.EMPTY || ChatHeads.offsetChat(headData) ? ChatHeads.headWidth() : 0;
    }

    public static int getTextWidthDifference(ChatHudLine.Visible visible) {
        return ChatHeads.getTextWidthDifference(ChatHeads.getHeadData(visible));
    }

    public static HeadData scanForPlayerName(String string, ChatHeads.PlayerInfoCache playerInfoCache) {
        Map<Integer, List<String>> map = playerInfoCache.createNamesByFirstCharacterMap();
        boolean bl = false;
        int[] nArray = string.codePoints().toArray();
        for (int i = 0; i < nArray.length; ++i) {
            int n = nArray[i];
            if (bl && ChatHeads.isWordCharacter(n)) continue;
            for (String string2 : map.getOrDefault(n, List.of())) {
                boolean bl2;
                int[] nArray2 = string2.codePoints().toArray();
                if (i + nArray2.length - 1 >= nArray.length) continue;
                boolean bl3 = ChatHeads.isWordCharacter(nArray2[nArray2.length - 1]);
                boolean bl4 = bl2 = i + nArray2.length < nArray.length && ChatHeads.isWordCharacter(nArray[i + nArray2.length]);
                if (bl3 && bl2 || !ChatHeads.containsSubsequenceAt(nArray, i, nArray2)) continue;
                return new HeadData(playerInfoCache.get(string2), i);
            }
            bl = ChatHeads.isWordCharacter(n);
        }
        return HeadData.EMPTY;
    }

    private static boolean isWordCharacter(int n) {
        return Character.isLetterOrDigit(n) || n == 95 || Character.getNumericValue(n) != -1;
    }

    public static void autoDetectAlias(Text text) {
        String string = text.getString();
        int n = string.indexOf(" ");
        if (n == -1) {
            return;
        }
        String string2 = string.substring(0, n);
        if (!string.substring(n).startsWith(" is ")) {
            return;
        }
        String string3 = string.substring(n + " is ".length());
        if (string3.contains(" ")) {
            return;
        }
        CONFIG.addNameAlias(string2, string3);
    }

    public static void setLineData(HeadData headData) {
        if (refreshing) {
            refreshingLineData = headData;
        } else {
            lineData = headData;
        }
    }

    public static Text handleAddedMessage(Text text, PlayerListEntry playerListEntry) {
        HeadData headData;
        if (CONFIG.detectNameAliases()) {
            ChatHeads.autoDetectAlias(text);
        }
        lastSenderData = HeadData.EMPTY;
        if (serverDisabledChatHeads) {
            return text;
        }
        boolean bl = ChatHeads.isShowcaseItemMessage(text);
        if (CONFIG.senderDetection() == SenderDetection.HEURISTIC_ONLY || bl) {
            playerListEntry = null;
        } else if (playerListEntry != null) {
            serverSentUuid = true;
        } else if (CONFIG.senderDetection() == SenderDetection.UUID_ONLY || serverSentUuid && CONFIG.smartHeuristics()) {
            return text;
        }
        Pair<Text, HeadData> pair = ChatHeads.detectPlayerAndAddChatHead(text, playerListEntry);
        if (pair == null) {
            return text;
        }
        Text text2 = (Text)pair.getFirst();
        lastSenderData = headData = (HeadData)pair.getSecond();
        if (CONFIG.renderPosition() == RenderPosition.BEFORE_LINE) {
            return text;
        }
        return text2;
    }

    private static boolean isShowcaseItemMessage(Text text) {
        TranslatableTextContent translatableTextContent;
        TextContent textContent = text.getContent();
        return textContent instanceof TranslatableTextContent && Objects.equals((translatableTextContent = (TranslatableTextContent)textContent).getKey(), "showcaseitem.misc.shared_item");
    }

    public static Pair<Text, HeadData> detectPlayerAndAddChatHead(Text text, PlayerListEntry playerListEntry) {
        ClientPlayNetworkHandler clientPlayNetworkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (clientPlayNetworkHandler == null) {
            return null;
        }
        ChatHeads.PlayerInfoCache playerInfoCache = new ChatHeads.PlayerInfoCache(clientPlayNetworkHandler);
        if (playerListEntry != null) {
            playerInfoCache.addProfileName(playerListEntry);
        } else {
            playerInfoCache.collectProfileNames();
        }
        ArrayList<Text> arrayList = ComponentProcessor.split(text);
        if (ComponentProcessor.containsPlayerSprite(arrayList)) {
            return null;
        }
        PlayerListEntry playerListEntry2 = ComponentProcessor.addChatHeadForClickTellCommand(arrayList, playerInfoCache);
        if (playerListEntry2 != null) {
            return new Pair((Object)ComponentProcessor.join(arrayList), (Object)HeadData.of(playerListEntry2));
        }
        if (playerListEntry != null) {
            playerInfoCache.add(playerListEntry);
        } else {
            playerInfoCache.collectAllNames();
        }
        playerListEntry2 = ComponentProcessor.addChatHeadForPlayerName(arrayList, playerInfoCache);
        if (playerListEntry2 != null) {
            return new Pair((Object)ComponentProcessor.join(arrayList), (Object)HeadData.of(playerListEntry2));
        }
        if (playerListEntry != null) {
            MutableText mutableText = ComponentProcessor.createChatHeadComponent(playerListEntry, text);
            MutableText mutableText2 = Text.empty().append((Text)mutableText).append(text);
            return new Pair((Object)mutableText2, (Object)HeadData.of(playerListEntry));
        }
        return null;
    }

    public static HeadData getLineData() {
        return refreshing ? refreshingLineData : lineData;
    }

    public static Identifier getBlendedHeadLocation(Identifier identifier) {
        return Identifier.of((String)MOD_ID, (String)identifier.getPath());
    }

    public static NativeImage extractBlendedHead(NativeImage nativeImage) {
        boolean bl = nativeImage.getWidth() / 2 == nativeImage.getHeight();
        int n = nativeImage.getWidth() / 64;
        int n2 = nativeImage.getHeight() / (bl ? 32 : 64);
        NativeImage nativeImage2 = new NativeImage(8 * n, 8 * n2, false);
        for (int i = 0; i < nativeImage2.getHeight(); ++i) {
            for (int j = 0; j < nativeImage2.getWidth(); ++j) {
                int n3 = nativeImage.getColorArgb(8 * n + j, 8 * n2 + i);
                int n4 = nativeImage.getColorArgb(40 * n + j, 8 * n2 + i);
                nativeImage2.setColorArgb(j, i, ChatHeads.blendColors(n3, n4));
            }
        }
        return nativeImage2;
    }

    private static boolean containsSubsequenceAt(int[] nArray, int n, int[] nArray2) {
        for (int i = 0; i < nArray2.length; ++i) {
            if (nArray[n + i] == nArray2[i]) continue;
            return false;
        }
        return true;
    }

    public static int blendColors(int n, int n2) {
        float f = (float)ColorHelper.getAlpha((int)n) / 255.0f;
        float f2 = (float)ColorHelper.getRed((int)n) / 255.0f;
        float f3 = (float)ColorHelper.getGreen((int)n) / 255.0f;
        float f4 = (float)ColorHelper.getBlue((int)n) / 255.0f;
        float f5 = (float)ColorHelper.getAlpha((int)n2) / 255.0f;
        float f6 = (float)ColorHelper.getRed((int)n2) / 255.0f;
        float f7 = (float)ColorHelper.getGreen((int)n2) / 255.0f;
        float f8 = (float)ColorHelper.getBlue((int)n2) / 255.0f;
        float f9 = f5 * f5 + (1.0f - f5) * f;
        float f10 = f5 * f6 + (1.0f - f5) * f2;
        float f11 = f5 * f7 + (1.0f - f5) * f3;
        float f12 = f5 * f8 + (1.0f - f5) * f4;
        return ColorHelper.getArgb((int)((int)Math.clamp(f9 * 255.0f, 0.0f, 255.0f)), (int)((int)Math.clamp(f10 * 255.0f, 0.0f, 255.0f)), (int)((int)Math.clamp(f11 * 255.0f, 0.0f, 255.0f)), (int)((int)Math.clamp(f12 * 255.0f, 0.0f, 255.0f)));
    }

    public static void renderChatHead(DrawContext drawContext, int n, int n2, PlayerListEntry playerListEntry, float f) {
        ChatHeads.renderChatHead(drawContext, n, n2, playerListEntry, f, CONFIG.drawShadow());
    }

    public static void renderChatHead(DrawContext drawContext, int n3, int n4, PlayerListEntry playerListEntry, float f, boolean bl) {
        boolean bl2;
        Identifier identifier = playerListEntry.getSkinTextures().body().texturePath();
        int n5 = ColorHelper.getWhite((float)f);
        int n6 = ColorHelper.scaleRgb((int)n5, (float)0.25f);
        int n7 = bl ? -1 : 0;
        ClientWorld clientWorld = MinecraftClient.getInstance().world;
        PlayerEntity playerEntity = clientWorld != null ? clientWorld.getPlayerByUuid(playerListEntry.getProfile().id()) : null;
        boolean bl3 = playerEntity != null && PlayerEntityRenderer.shouldFlipUpsideDown((PlayerEntity)playerEntity);
        boolean bl4 = playerListEntry.shouldShowHat();
        int n8 = bl3 ? 8 : 0;
        int n9 = bl3 ? -1 : 1;
        boolean bl5 = bl2 = CONFIG.threeDeeNess() != 0.0f;
        if (bl4 && !bl2 && blendedHeadTextures.contains(identifier)) {
            if (bl) {
                drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, ChatHeads.getBlendedHeadLocation(identifier), n3 + 1, n4, 0.0f, (float)n8, 8, 8, 8, n9 * 8, 8, 8, n6);
            }
            drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, ChatHeads.getBlendedHeadLocation(identifier), n3, n4 + n7, 0.0f, (float)n8, 8, 8, 8, n9 * 8, 8, 8, n5);
        } else {
            Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
            BiConsumer<Integer, Integer> biConsumer = (n, n2) -> matrix3x2fStack.pushMatrix().scaleAround(1.0f + CONFIG.threeDeeNess() * 0.25f, (float)n.intValue() + 4.0f, (float)n2.intValue() + 4.0f);
            if (bl) {
                drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, identifier, n3 + 1, n4, 8.0f, (float)(8 + n8), 8, 8, 8, n9 * 8, 64, 64, n6);
                if (bl4) {
                    if (bl2) {
                        biConsumer.accept(n3 + 1, n4);
                    }
                    drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, identifier, n3 + 1, n4, 40.0f, (float)(8 + n8), 8, 8, 8, n9 * 8, 64, 64, n6);
                    if (bl2) {
                        matrix3x2fStack.popMatrix();
                    }
                }
            }
            drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, identifier, n3, n4 + n7, 8.0f, (float)(8 + n8), 8, 8, 8, n9 * 8, 64, 64, n5);
            if (bl4) {
                if (bl2) {
                    biConsumer.accept(n3, n4 + n7);
                }
                drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, identifier, n3, n4 + n7, 40.0f, (float)(8 + n8), 8, 8, 8, n9 * 8, 64, 64, n5);
                if (bl2) {
                    matrix3x2fStack.popMatrix();
                }
            }
        }
    }


    public static class PlayerInfoCache {
        private final ClientPlayNetworkHandler connection;
        private final Map<String, PlayerListEntry> playerInfos = new HashMap<String, PlayerListEntry>();
        private boolean collectedProfileNames = false;
        private boolean collectedEverything = false;
    
        public PlayerInfoCache(ClientPlayNetworkHandler clientPlayNetworkHandler) {
            this.connection = clientPlayNetworkHandler;
        }
    
        public void add(PlayerListEntry playerListEntry) {
            this.addProfileName(playerListEntry);
            this.addDisplayName(playerListEntry);
            this.addNameAliases();
        }
    
        public PlayerListEntry get(String string) {
            return this.playerInfos.get(string);
        }
    
        public Set<String> getNames() {
            return this.playerInfos.keySet();
        }
    
        public Map<Integer, List<String>> createNamesByFirstCharacterMap() {
            HashMap<Integer, List<String>> hashMap = new HashMap<Integer, List<String>>();
            for (String string : this.playerInfos.keySet()) {
                hashMap.compute(string.codePointAt(0), (n, arrayList) -> {
                    if (arrayList == null) {
                        arrayList = new ArrayList<String>();
                    }
                    arrayList.add(string);
                    return arrayList;
                });
            }
            return hashMap;
        }
    
        public void collectProfileNames() {
            if (this.collectedProfileNames) {
                return;
            }
            this.collectedProfileNames = true;
            for (PlayerListEntry playerListEntry : this.connection.getPlayerList()) {
                this.addProfileName(playerListEntry);
            }
        }
    
        private void addProfileName(PlayerListEntry playerListEntry) {
            String string = ChatHeads.FORMAT_REGEX.matcher(playerListEntry.getProfile().name()).replaceAll("");
            if (string.isEmpty()) {
                return;
            }
            this.playerInfos.put(string, playerListEntry);
        }
    
        public void collectAllNames() {
            if (this.collectedEverything) {
                return;
            }
            this.collectedEverything = true;
            this.collectProfileNames();
            for (PlayerListEntry playerListEntry : this.connection.getPlayerList()) {
                this.addDisplayName(playerListEntry);
            }
            this.addNameAliases();
        }
    
        private void addNameAliases() {
            for (Map.Entry<String, String> entry : ChatHeads.CONFIG.getNameAliases().entrySet()) {
                PlayerListEntry playerListEntry = this.playerInfos.get(entry.getValue());
                if (playerListEntry == null) continue;
                this.playerInfos.putIfAbsent(entry.getKey(), playerListEntry);
            }
        }
    
        private void addDisplayName(PlayerListEntry playerListEntry) {
            if (playerListEntry.getDisplayName() != null) {
                String string = ChatHeads.FORMAT_REGEX.matcher(playerListEntry.getDisplayName().getString()).replaceAll("");
                if (string.isEmpty()) {
                    return;
                }
                this.playerInfos.putIfAbsent(string, playerListEntry);
            }
        }
    }
}

