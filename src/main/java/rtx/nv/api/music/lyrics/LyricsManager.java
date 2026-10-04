package rtx.nv.api.music.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtx.nv.api.music.TrackState;
import rtx.nv.api.music.lyrics.LyricLine.SubLine;
import rtx.nv.api.music.lyrics.LyricLine.Word;
import rtx.nv.api.music.lyrics.LyricsConsensusEngine.Candidate;
import rtx.nv.api.music.lyrics.StudioCascadeAligner.AcousticEnergyProfile;

public class LyricsManager {
   public static rtx.nv.api.music.lyrics.LyricsManager INSTANCE;
   private static final Logger LOGGER = LoggerFactory.getLogger("MusicSubtitles");
   private static final Pattern LRC_LINE_PATTERN = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)$");
   private static final Pattern ENHANCED_WORD_PATTERN = Pattern.compile("<(\\d+):(\\d+(?:\\.\\d+)?>)([^<]*)");
   private static final Pattern OFFSET_PATTERN = Pattern.compile("^\\[offset:([+-]?\\d+)\\]", 2);
   private static final Pattern NOISE_PATTERN = Pattern.compile(
      "(?i)\\s*[\\[\\(]?(?:official\\s*(?:music\\s*)?(?:video|audio|lyric\\s*video|visualizer|clip)|премьера\\s*клипа|клип|лирик\\s*видео|slowed\\s*\\+?\\s*reverb|speed\\s*up|nightcore|sped\\s*up|mood\\s*video|remastered|deluxe|prod\\.?\\s+by|4k|hd|hq|live|acoustic|remix)[\\]\\)]?.*"
   );
   private static final Pattern SPLIT_PATTERN = Pattern.compile("^(.*?)\\s+[-—–]\\s+(.*)$");
   private static final Pattern SPEED_UP_PATTERN = Pattern.compile(
      "(?i)(?:speed\\s*[-_]?\\s*up|sped\\s*[-_]?\\s*up|nightcore|fast\\s*version|fast\\s*ver|ускоренн(?:ая|ый|ое|о))"
   );
   private static final Pattern SLOWED_PATTERN = Pattern.compile(
      "(?i)(?:slowed\\s*(?:(?:\\+|&|and)?\\s*reverb)?|slowed\\s*[-_]?\\s*down|daycore|chopped\\s*(?:and|&)?\\s*screwed|замедленн(?:ая|ый|ое|о))"
   );
   private static final double SPEED_UP_FACTOR = 0.75;
   private static final double SLOWED_FACTOR = 1.3;
   private static final Path LOCAL_LYRICS_DIR = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve("lyrics");
   public static final long PERCEPTUAL_LEAD_MS = 0L;
   private final Map<String, List<LyricLine>> lyricsCache = new ConcurrentHashMap<>();
   private final List<SubtitleBubble> activeBubbles = new CopyOnWriteArrayList<>();
   private final ExecutorService executor;
   private final Random random = new Random();
   private int sideCounter = 0;
   private LiveCaptionServer captionServer;
   private String currentTrackKey = "";
   private LyricLine lastSpawnedLine = null;
   private String lastRenderMode = "";
   private long nextFetchAttemptMs;
   private LyricLine cachedSprintLine = null;
   private List<LyricLine> cachedSprintChunks = new ArrayList<>();
   private LyricLine lastSpawnedSprintChunk = null;
   private int sprintChunkIndex = 0;
   private final AtomicLong currentFetchId = new AtomicLong(0L);
   private volatile String activeFetchTrackKey = null;
   private long lastBlockRaycastTime = 0L;
   private final Set<String> disabledTracks = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private final Set<String> promptedMismatchTracks = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private final AtomicInteger acousticMismatchStrikes = new AtomicInteger(0);
   private volatile long lastAcousticCheckTime = 0L;
   private volatile boolean isAcousticChecking = false;
   private final Set<String> autoShutoffTracks = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private static final Pattern BROWSER_SUFFIX_PATTERN = Pattern.compile(
      "(?i)\\s*[-–—|/]\\s*(?:YouTube\\s*(?:Music)?|Яндекс\\s*Музыка|Yandex\\s*Music|SoundCloud|Spotify|Google\\s*Chrome|Microsoft\\s*Edge|Opera|Firefox|Brave|Vivaldi)\\s*$"
   );
   private static final Pattern PIPE_NOISE_PATTERN = Pattern.compile(
      "(?i)\\s*[|/]\\s*(?:Official|Премьера|Клип|Video|Audio|Visualizer|Lyric|Remastered|Mood|Soundtrack|OST).*$"
   );
   private static final Pattern BRACKET_TAGS_PATTERN = Pattern.compile(
      "(?i)\\s*[\\[\\(]\\s*(?:official(?:\\s+(?:music\\s+)?video|\\s+audio|\\s+visualizer|\\s+live|\\s*\\d+k)?|music\\s+video|video|audio|visualizer|lyric\\s+video|lyrics|клип|премьера(?:\\s+клипа|\\s+трека)?(?:\\s*\\d{4})?|официальный\\s+клип|видеоклип|mood\\s+video|live(?:\\s+performance|\\s+session)?|acoustic(?:\\s+version)?|remastered(?:\\s*\\d{4})?|deluxe(?:\\s+edition)?|prod(?:\\.|\\s+by)?\\s+[^)\\]]+|feat\\.?\\s+[^)\\]]+|ft\\.?\\s+[^)\\]]+|slowed(?:\\s*\\+?\\s*reverb)?|speed\\s*up|sped\\s*up|nightcore|hd|4k|hq|4k\\s*60fps|\\d{4}|ost(?:\\s+.*)?|soundtrack(?:\\s+.*)?)\\s*[\\]\\)]"
   );
   private static final Pattern TRAILING_NOISE_PATTERN = Pattern.compile(
      "(?i)\\s*[-–—]\\s*(?:Премьера\\s+клипа|Официальный\\s+клип|Official\\s+Video|Lyric\\s+Video)(?:\\s*\\d{4})?$"
   );

   public static rtx.nv.api.music.lyrics.LyricsManager get() {
      if (INSTANCE == null) {
         INSTANCE = new rtx.nv.api.music.lyrics.LyricsManager();
      }

      return INSTANCE;
   }

   public void disableCurrentTrack() {
      if (this.currentTrackKey != null && !this.currentTrackKey.isEmpty()) {
         this.disabledTracks.add(this.currentTrackKey);
         this.activeBubbles.clear();
         this.lastSpawnedLine = null;
         this.lastSpawnedSprintChunk = null;
         this.cachedSprintLine = null;
         this.cachedSprintChunks.clear();
      }
   }

   public void enableCurrentTrack() {
      if (this.currentTrackKey != null && !this.currentTrackKey.isEmpty()) {
         this.disabledTracks.remove(this.currentTrackKey);
         this.promptedMismatchTracks.remove(this.currentTrackKey);
         this.autoShutoffTracks.remove(this.currentTrackKey);
         this.acousticMismatchStrikes.set(0);
         this.activeBubbles.clear();
         this.lastSpawnedLine = null;
         this.lastSpawnedSprintChunk = null;
         this.cachedSprintLine = null;
         this.cachedSprintChunks.clear();
      }
   }

   public boolean isCurrentTrackDisabled() {
      return this.currentTrackKey != null && this.disabledTracks.contains(this.currentTrackKey);
   }

   public void reloadCurrentTrack() {
      this.lyricsCache.clear();
      this.promptedMismatchTracks.clear();
      this.autoShutoffTracks.clear();
      this.acousticMismatchStrikes.set(0);
      this.activeBubbles.clear();
      this.lastSpawnedLine = null;
      this.lastSpawnedSprintChunk = null;
      this.cachedSprintLine = null;
      this.cachedSprintChunks.clear();
      this.currentTrackKey = "";
   }

   public LyricsManager() {
      this.captionServer = new LiveCaptionServer(this);
      this.captionServer.start();
      this.executor = Executors.newFixedThreadPool(8, var0 -> {
         Thread var1 = new Thread(var0, "MusicSubtitles-LyricsWorker");
         var1.setDaemon(true);
         return var1;
      });

      try {
         Files.createDirectories(LOCAL_LYRICS_DIR);
      } catch (Exception var2) {
      }
   }

   public String getCurrentTrackKey() {
      return this.currentTrackKey;
   }

   public void init() {
      if (this.captionServer == null) {
         this.captionServer = new LiveCaptionServer(this);
         this.captionServer.start();
      }
      net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
         this.activeBubbles.clear();
         this.lastSpawnedLine = null;
         this.lastSpawnedSprintChunk = null;
         this.cachedSprintLine = null;
         this.cachedSprintChunks.clear();
      });
      net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
         this.activeBubbles.clear();
         this.lastSpawnedLine = null;
         this.lastSpawnedSprintChunk = null;
         this.cachedSprintLine = null;
         this.cachedSprintChunks.clear();
      });
   }

   public void shutdown() {
      if (this.captionServer != null) {
         this.captionServer.stop();
      }

      this.executor.shutdownNow();
   }

   public List<SubtitleBubble> getActiveBubbles() {
      return this.activeBubbles;
   }

   public LyricLine getCurrentLine(TrackState var1) {
      if (var1 != null && var1.isActive()) {
         String var2 = this.getTrackKey(var1);
         List<LyricLine> var3 = this.lyricsCache.get(var2);
         if (var3 != null && !var3.isEmpty()) {
            long var4 = var1.getInterpolatedPositionMs();

            for (LyricLine var6 : var3) {
               if (var4 >= var6.startMs && var4 < var6.endMs) {
                  return var6;
               }
            }

            return null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public void update(TrackState var1, int var2, int var3) {
      String mode = MusicSubtitlesConfig.INSTANCE.subtitlesMode;
      if (!mode.equals(lastRenderMode)) {
         lastRenderMode = mode;
         activeBubbles.clear(); lastSpawnedLine = null;
      }
      if (!MusicSubtitlesConfig.INSTANCE.subtitlesEnabled) {
         this.activeBubbles.clear();
         this.lastSpawnedLine = null;
      } else {
         long var5 = System.currentTimeMillis();
         if (var1 != null && var1.isActive() && !var1.title().isEmpty()) {
            String var7 = this.getTrackKey(var1);
            if (!var7.equals(this.currentTrackKey)) {
               this.currentTrackKey = var7;
               this.lastSpawnedLine = null;
               this.lastSpawnedSprintChunk = null;
               this.cachedSprintLine = null;
               this.cachedSprintChunks.clear();
               this.activeBubbles.removeIf(b -> !b.isLiveCaption);
               this.acousticMismatchStrikes.set(0);
               this.lastAcousticCheckTime = System.currentTimeMillis();
               this.nextFetchAttemptMs = var5 + 30000L;
               this.fetchLyricsAsync(var1);
            } else if (var5 >= this.nextFetchAttemptMs && !this.lyricsCache.containsKey(var7) && (this.activeFetchTrackKey == null || !this.activeFetchTrackKey.equals(var7))) {
               this.nextFetchAttemptMs = var5 + 30000L;
               this.fetchLyricsAsync(var1);
            }

            if (this.disabledTracks.contains(var7)) {
               this.activeBubbles.removeIf(b -> !b.isLiveCaption);
               this.lastSpawnedLine = null;
            } else if (!var1.isPlaying() && !var1.isPaused()) {
               // A transient stopped state during a track switch must not restart a caption.
            } else {
               List var8 = this.lyricsCache.get(var7);
               if (var8 != null && !var8.isEmpty() && !this.promptedMismatchTracks.contains(var7) && this.isLyricsMismatched(var8, var1)) {
                  this.promptedMismatchTracks.add(var7);
                  this.sendMismatchNotification();
               }

               long var9 = var1.getInterpolatedPositionMs();
               int var11 = MusicSubtitlesConfig.INSTANCE.getTrackSyncOffset(var1.artist(), var1.title());
               long var12 = Math.max(0L, var9 + MusicSubtitlesConfig.INSTANCE.subtitlesOffsetMs + var11 + 0L);
               this.activeBubbles.removeIf(var4x -> var4x.isExpired(var12, var5));
               if (var8 != null && !var8.isEmpty()) {
                  LyricLine var14 = null;

                  for (int var15 = var8.size() - 1; var15 >= 0; var15--) {
                     LyricLine var16 = (LyricLine)var8.get(var15);
                     if (var12 >= var16.startMs && var12 < var16.endMs) {
                        var14 = var16;
                        break;
                     }
                  }

                  boolean var20 = "WORLD_3D".equalsIgnoreCase(MusicSubtitlesConfig.INSTANCE.subtitlesMode);
                  MinecraftClient var21 = MinecraftClient.getInstance();
                  boolean isSprinting = var21.player != null && var21.player.isSprinting();
                  if (var20 && MusicSubtitlesConfig.INSTANCE.sprintWordsEnabled && isSprinting) {
                     this.lastSpawnedLine = null;
                     this.activeBubbles.removeIf(var0 -> !var0.isFastPaced);
                     this.updateSprint3DBubbles(var14, var12);
                  } else {
                     this.lastSpawnedSprintChunk = null;
                     this.cachedSprintLine = null;
                     this.cachedSprintChunks.clear();
                     this.activeBubbles.removeIf(var0 -> var0.isFastPaced);
                     if (var14 == null) {
                        this.lastSpawnedLine = null;
                     } else {
                        boolean var18 = "WORLD_BLOCK".equalsIgnoreCase(MusicSubtitlesConfig.INSTANCE.subtitlesMode)
                           || "WORLD_SURFACE".equalsIgnoreCase(MusicSubtitlesConfig.INSTANCE.subtitlesMode)
                           || "WORLD_3D".equalsIgnoreCase(MusicSubtitlesConfig.INSTANCE.subtitlesMode);
                        if (var14 != this.lastSpawnedLine || this.activeBubbles.isEmpty()) {
                           this.lastSpawnedLine = var14;
                           this.spawnBubble(var14, var2, var3);
                        } else if (var18 && !this.hasBlockAttachedBubble() && var5 - this.lastBlockRaycastTime > 250L) {
                           this.lastBlockRaycastTime = var5;
                           this.tryAttachToBlock(var14);
                        }
                     }
                  }

                  // Audio capture is not supplied by the media-metadata bridge.
               }
            }
         } else {
            this.activeBubbles.removeIf(b -> !b.isLiveCaption || b.isExpired(0L, var5));
            this.lastSpawnedLine = null;
         }
      }
   }

   private void updateSprint3DBubbles(LyricLine var1, long var2) {
      if (var1 == null) {
         this.cachedSprintLine = null;
         this.cachedSprintChunks.clear();
         this.lastSpawnedSprintChunk = null;
      } else {
         if (var1 != this.cachedSprintLine) {
            this.cachedSprintLine = var1;
            int var4 = Math.max(1, Math.min(3, MusicSubtitlesConfig.INSTANCE.sprintWordsChunkSize));
            this.cachedSprintChunks = this.splitLineIntoChunks(var1, var4);
            this.lastSpawnedSprintChunk = null;
            this.sprintChunkIndex = 0;
         }

         LyricLine var7 = null;

         for (int var5 = this.cachedSprintChunks.size() - 1; var5 >= 0; var5--) {
            LyricLine var6 = this.cachedSprintChunks.get(var5);
            if (var2 >= var6.startMs && var2 < var6.endMs) {
               var7 = var6;
               break;
            }
         }

         if (var7 != null && var7 != this.lastSpawnedSprintChunk) {
            this.lastSpawnedSprintChunk = var7;
            this.spawn3DSprintBubble(var7);
         }
      }
   }

   private List<LyricLine> splitLineIntoChunks(LyricLine var1, int var2) {
      ArrayList var3 = new ArrayList();
      if (var1 != null && !var1.text.isEmpty()) {
         if (var1.words != null && !var1.words.isEmpty()) {
            List var24 = var1.words;
            int var25 = 0;

            while (var25 < var24.size()) {
               int var27 = Math.min(var25 + var2, var24.size());
               List var28 = var24.subList(var25, var27);
               StringBuilder var30 = new StringBuilder();
               ArrayList var31 = new ArrayList();

               for (int var32 = 0; var32 < var28.size(); var32++) {
                  Word var34 = (Word)var28.get(var32);
                  if (var32 > 0) {
                     var30.append(" ");
                  }

                  int var35 = var30.length();
                  var30.append(var34.text());
                  int var37 = var30.length();
                  var31.add(new Word(var34.startMs(), var34.endMs(), var34.text(), var35, var37));
               }

               long var33 = ((Word)var28.get(0)).startMs();
               long var36 = ((Word)var28.get(var28.size() - 1)).endMs();
               long var38 = Math.max(70L, var36 - var33);
               long var39 = Math.max(160L, Math.min(420L, (long)((float)var38 * 1.15F)));
               long var40 = var36 + var39;
               var3.add(new LyricLine(var33, var40, var36, var30.toString(), var31));
               var25 += var2;
            }
         } else {
            String[] var4 = var1.text.split("\\s+");
            ArrayList var5 = new ArrayList();

            for (String var9 : var4) {
               if (!var9.isEmpty()) {
                  var5.add(var9);
               }
            }

            if (var5.isEmpty()) {
               return var3;
            }

            long var26 = var1.vocalEndMs - var1.startMs;
            long var29 = Math.max(var26, 200L);
            int var10 = 0;

            while (var10 < var5.size()) {
               int var11 = Math.min(var10 + var2, var5.size());
               List var12 = var5.subList(var10, var11);
               String var13 = String.join(" ", var12);
               long var14 = var1.startMs + (long)((double)var10 / var5.size() * var29);
               long var16 = var1.startMs + (long)((double)var11 / var5.size() * var29);
               long var18 = Math.max(70L, var16 - var14);
               long var20 = Math.max(160L, Math.min(420L, (long)((float)var18 * 1.15F)));
               long var22 = var16 + var20;
               var3.add(new LyricLine(var14, var22, var16, var13, List.of()));
               var10 += var2;
            }
         }

         return var3;
      } else {
         return var3;
      }
   }

   private void spawn3DSprintBubble(LyricLine var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null && var2.gameRenderer != null) {
         Camera var3 = var2.gameRenderer.getCamera();
         if (var3 != null) {
            Vec3d var4 = var3.getCameraPos();
            float baseDist = Math.max(2.0F, Math.min(12.0F, MusicSubtitlesConfig.INSTANCE.subtitlesDistance));
            double vel = var2.player.getVelocity().horizontalLength();
            float dist = baseDist + (float)Math.min(2.0, Math.sqrt(vel) * 2.5) + this.random.nextFloat() * 0.6F;
            this.sideCounter++;
            float sideSign = this.sideCounter % 2 == 0 ? 1.0F : -1.0F;
            float lateralDeg = sideSign * MusicSubtitlesConfig.INSTANCE.sprintWordsSpread * (18.0F + this.random.nextFloat() * 4.0F);
            float camPitch = var3.getPitch();
            float pitchDeg = MathHelper.clamp(camPitch * 0.4F + (this.random.nextFloat() * 6.0F - 1.0F), -18.0F, 18.0F);
            float targetYaw = var3.getYaw() + lateralDeg;
            Vec3d dir = Vec3d.fromPolar(pitchDeg, targetYaw).normalize();
            Vec3d bubblePos = var4.add(dir.multiply(dist));
            double playerY = var2.player.getY();
            if (bubblePos.y < playerY + 0.4) {
               bubblePos = new Vec3d(bubblePos.x, playerY + 0.4 + this.random.nextFloat() * 0.3, bubblePos.z);
            }

            double dx = var4.x - bubblePos.x;
            double dy = var4.y - bubblePos.y;
            double dz = var4.z - bubblePos.z;
            float facingYaw = (float)Math.atan2(dx, dz);
            float distXZ = (float)Math.sqrt(dx * dx + dz * dz);
            float facingPitch = (float)(-Math.atan2(dy, distXZ));
            float slantYaw = -sideSign * (float)Math.toRadians(18.0 + this.random.nextFloat() * 6.0);
            Quaternionf rot = new Quaternionf().rotationY(facingYaw);
            rot.rotateX(facingPitch * 0.5F);
            rot.rotateZ(sideSign * (float)Math.toRadians(3.0));
            int var30 = Math.max(1, Math.min(MusicSubtitlesConfig.INSTANCE.sprintWordsTrailLength, 4));

            while (this.activeBubbles.size() >= var30) {
               this.activeBubbles.remove(0);
            }

            this.activeBubbles.add(new SubtitleBubble(var1, bubblePos, rot, false, true));
         }
      }
   }

   private void spawnBubble(LyricLine var1, int var2, int var3) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      boolean var5 = false; // Walking must never select the separate sprint-caption lifecycle.
      String var6 = MusicSubtitlesConfig.INSTANCE.subtitlesMode;
      if ("WORLD_BLOCK".equalsIgnoreCase(var6) || "WORLD_SURFACE".equalsIgnoreCase(var6) || "WORLD_3D".equalsIgnoreCase(var6)) {
         this.spawnBlockAttachedBubble(var1, var5);
      } else if ("HUD_BOTTOM".equalsIgnoreCase(var6)) {
         this.spawn2DBottomBubble(var1, var2, var3, var5);
      } else {
         this.spawn2DRandomBubble(var1, var2, var3, var5);
      }
   }

   private void spawnLiveBubble(LyricLine var1, int var2, int var3, long var4) {
      MinecraftClient var6 = MinecraftClient.getInstance();
      boolean var7 = false;
      String var8 = MusicSubtitlesConfig.INSTANCE.subtitlesMode;
      if ("WORLD_BLOCK".equalsIgnoreCase(var8) || "WORLD_SURFACE".equalsIgnoreCase(var8) || "WORLD_3D".equalsIgnoreCase(var8)) {
         SubtitleBubble var9 = this.createBlockBubble(var1, var7);
         if (var9 != null) {
            var9.isLiveCaption = true;
            var9.liveDurationMs = var4;
            this.activeBubbles.clear();
            this.activeBubbles.add(var9);
            return;
         }
         if (!"WORLD_3D".equalsIgnoreCase(var8)) {
            return;
         }
      }
      if ("WORLD_3D".equalsIgnoreCase(var8)) {
         this.spawn3DLiveBubble(var1, var7, var4);
      } else if ("HUD_BOTTOM".equalsIgnoreCase(var8)) {
         this.spawn2DBottomLiveBubble(var1, var2, var3, var7, var4);
      } else {
         this.spawn2DRandomLiveBubble(var1, var2, var3, var7, var4);
      }
   }

   private void spawn3DLiveBubble(LyricLine line, boolean fastPaced, long duration) {
      SubtitleBubble bubble = createWorldBubble(line);
      if (bubble == null) return;
      bubble.isLiveCaption = true;
      bubble.liveDurationMs = duration;
      showBubble(bubble);
   }

   private void spawn2DBottomLiveBubble(LyricLine var1, int var2, int var3, boolean var4, long var5) {
      MinecraftClient var7 = MinecraftClient.getInstance();
      TextRenderer var8 = var7.textRenderer;
      int var9 = (int)(var2 * 0.65F);
      List<SubLine> var10 = var1.getSubLines(var8, var9);
      int var11 = 0;

      for (SubLine var13 : var10) {
         if (var13.width() > var11) {
            var11 = var13.width();
         }
      }

      byte var21 = 12;
      int var22 = var10.size() * var21;
      double var14 = MusicSubtitlesConfig.INSTANCE.sub2DPositionX;
      double var16 = MusicSubtitlesConfig.INSTANCE.sub2DPositionY;
      float var18 = (float)(var2 * var14) - var11 * 0.5F;
      float var19 = (float)(var3 * var16) - var22 * 0.5F;
      SubtitleBubble var20 = new SubtitleBubble(var1, var18, var19, var4);
      var20.isLiveCaption = true;
      var20.liveDurationMs = var5;
      this.activeBubbles.clear();
      this.activeBubbles.add(var20);
   }

   private void spawn2DRandomLiveBubble(LyricLine var1, int var2, int var3, boolean var4, long var5) {
      byte var7 = 80;
      byte var8 = 80;
      int var10 = Math.max(var7 + 1, var2 - var7 - 160);
      int var12 = Math.max(var8 + 1, var3 - var8 - 40);
      float var13 = var7 + this.random.nextInt(Math.max(1, var10 - var7));
      float var14 = var8 + this.random.nextInt(Math.max(1, var12 - var8));
      SubtitleBubble var15 = new SubtitleBubble(var1, var13, var14, var4);
      var15.isLiveCaption = true;
      var15.liveDurationMs = var5;
      this.activeBubbles.clear();
      this.activeBubbles.add(var15);
   }

   public SubtitleBubble createBlockBubble(LyricLine var1, boolean var2) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      if (var4.player != null && var4.world != null && var4.gameRenderer != null) {
         Camera var5 = var4.gameRenderer.getCamera();
         if (var5 == null) {
            return null;
         } else {
            Vec3d var6 = var5.getCameraPos();
            double reach = Math.max(25.0, MusicSubtitlesConfig.INSTANCE.subtitlesDistance);
            RaycastContext var7 = new RaycastContext(
               var6, var6.add(var4.player.getRotationVec(1.0F).multiply(reach)), ShapeType.COLLIDER, FluidHandling.NONE, var4.player
            );
            BlockHitResult var8 = var4.world.raycast(var7);
            if (var8.getType() != Type.BLOCK) {
               for (float angleOff : new float[]{0.0f, -15.0f, 15.0f, -28.0f, 28.0f}) {
                  for (float pitchOff : new float[]{0.0f, -8.0f, 8.0f, -18.0f, 18.0f}) {
                     Vec3d coneDir = Vec3d.fromPolar(MathHelper.clamp(var4.player.getPitch() + pitchOff, -85.0f, 85.0f), var4.player.getYaw() + angleOff).normalize();
                     BlockHitResult hit = var4.world.raycast(new RaycastContext(var6, var6.add(coneDir.multiply(reach)), ShapeType.COLLIDER, FluidHandling.NONE, var4.player));
                     if (hit.getType() == Type.BLOCK) {
                        var8 = hit;
                        break;
                     }
                  }
                  if (var8.getType() == Type.BLOCK) break;
               }
            }

            if (var8.getType() == Type.BLOCK) {
               net.minecraft.util.math.BlockPos blockPos = var8.getBlockPos();
               Direction var9 = var8.getSide();
               Vec3d var10 = var8.getPos();
               double var11 = 0.015;
               Vec3d var13 = Vec3d.of(var9.getVector());
               Vec3d var14 = var10.add(var13.multiply(var11));
               Quaternionf var15 = new Quaternionf();
               float var16 = MathHelper.wrapDegrees(var4.player.getYaw());
               switch (var9) {
                  case UP: {
                     var14 = var10.add(0.0, 0.28, 0.0);
                     Vec3d toward = var6.subtract(var14);
                     float yaw = (float) Math.atan2(toward.x, toward.z);
                     float pitch = (float) -Math.atan2(toward.y, toward.horizontalLength());
                     var15.rotationY(yaw).rotateX(pitch);
                     break;
                  }
                  case DOWN: {
                     var14 = var10.add(0.0, -0.28, 0.0);
                     Vec3d toward = var6.subtract(var14);
                     float yaw = (float) Math.atan2(toward.x, toward.z);
                     float pitch = (float) -Math.atan2(toward.y, toward.horizontalLength());
                     var15.rotationY(yaw).rotateX(pitch);
                     break;
                  }
                  case NORTH:
                     var15.rotationY((float)Math.toRadians(180.0));
                     break;
                  case SOUTH:
                     var15.rotationY(0.0F);
                     break;
                  case WEST:
                     var15.rotateY((float)Math.toRadians(-90.0));
                     break;
                  case EAST:
                     var15.rotateY((float)Math.toRadians(90.0));
                     break;
               }

               SubtitleBubble bubble = new SubtitleBubble(var1, var14, var15, true, var2);
               bubble.attachedBlockPos = blockPos;
               return bubble;
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private void spawnBlockAttachedBubble(LyricLine var1, boolean var2) {
      SubtitleBubble var3 = this.createBlockBubble(var1, var2);
      if (var3 != null) {
         this.showBubble(var3);
      } else {
         this.spawn3DWorldBubble(var1, var2);
      }
   }

   private void tryAttachToBlock(LyricLine var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      boolean var3 = false;
      SubtitleBubble var4 = this.createBlockBubble(var1, var3);
      if (var4 != null) {
         this.showBubble(var4);
      }
   }

   private boolean hasBlockAttachedBubble() {
      for (SubtitleBubble var2 : this.activeBubbles) {
         if (var2.isSurfaceAttached) {
            return true;
         }
      }

      return false;
   }

   private void spawn3DWorldBubble(LyricLine line, boolean fastPaced) {
      SubtitleBubble bubble = createWorldBubble(line);
      if (bubble != null) showBubble(bubble);
   }

   public SubtitleBubble createWorldBubble(LyricLine line) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null || mc.gameRenderer == null) return null;
      Camera camera = mc.gameRenderer.getCamera();
      if (camera == null) return null;

      float baseDistance = MathHelper.clamp(MusicSubtitlesConfig.INSTANCE.subtitlesDistance, 3.2f, 5.2f);
      Vec3d vel = mc.player.getVelocity();
      double speed = vel != null ? vel.horizontalLength() : 0.0;
      float speedLead = (float) Math.min(1.6, speed * 2.2);
      float totalDist = baseDistance + speedLead;

      this.sideCounter++;
      float sideSign = (this.sideCounter % 2 == 0) ? 1.0f : -1.0f;
      int hash = Math.abs(line.getText().hashCode());

      float lateralAngle = sideSign * (18.0f + (hash % 4) * 3.5f);
      float targetYaw = mc.player.getYaw() + lateralAngle;

      float targetPitch = (hash % 3 == 0 ? 1.5f : (hash % 3 == 1 ? -2.5f : -0.5f));
      targetPitch += (float) MathHelper.clamp(mc.player.getPitch() * 0.35f, -12.0f, 12.0f);

      Vec3d direction = Vec3d.fromPolar(targetPitch, targetYaw).normalize();
      Vec3d eyePos = mc.player.getEyePos();
      Vec3d position = eyePos.add(direction.multiply(totalDist));
      double eyeY = mc.player.getEyeY();
      // Ensure subtitles are strictly at eye level on the side, never down at feet or ground
      position = new Vec3d(position.x, Math.max(eyeY - 0.25, Math.min(eyeY + 0.65, position.y)), position.z);

      float worldScale = 1.0f;
      Vec3d toward = eyePos.subtract(position);
      float rotYaw = (float)Math.atan2(toward.x, toward.z);
      float tilt = (float)-Math.atan2(toward.y, toward.horizontalLength());
      Quaternionf rotation = new Quaternionf().rotationY(rotYaw).rotateX(tilt);
      SubtitleBubble bubble = new SubtitleBubble(line, position, rotation, false, false);
      bubble.worldScale = worldScale;
      return bubble;
   }

   private void showBubble(SubtitleBubble bubble) {
      long now = System.currentTimeMillis();
      for (SubtitleBubble old : activeBubbles) old.retire(now);
      while (activeBubbles.size() >= 2) activeBubbles.remove(0);
      activeBubbles.add(bubble);
   }

   private void spawn2DRandomBubble(LyricLine var1, int var2, int var3, boolean var4) {
      byte var5 = 80;
      byte var6 = 80;
      int var8 = Math.max(var5 + 1, var2 - var5 - 160);
      int var10 = Math.max(var6 + 1, var3 - var6 - 40);
      float var11 = var5 + this.random.nextInt(Math.max(1, var8 - var5));
      float var12 = var6 + this.random.nextInt(Math.max(1, var10 - var6));
      this.showBubble(new SubtitleBubble(var1, var11, var12, false));
   }

   private void spawn2DBottomBubble(LyricLine var1, int var2, int var3, boolean var4) {
      MinecraftClient var5 = MinecraftClient.getInstance();
      TextRenderer var6 = var5.textRenderer;
      int var7 = (int)(var2 * 0.65F);
      List<SubLine> var8 = var1.getSubLines(var6, var7);
      int var9 = 0;

      for (SubLine var11 : var8) {
         if (var11.width() > var9) {
            var9 = var11.width();
         }
      }

      byte var18 = 12;
      int var19 = var8.size() * var18;
      double var12 = MusicSubtitlesConfig.INSTANCE.sub2DPositionX;
      double var14 = MusicSubtitlesConfig.INSTANCE.sub2DPositionY;
      float var16 = (float)(var2 * var12) - var9 * 0.5F;
      float var17 = (float)(var3 * var14) - var19 * 0.5F;
      this.showBubble(new SubtitleBubble(var1, var16, var17, false));
   }

   private String getTrackKey(TrackState var1) {
      return var1.artist() + "___" + var1.title();
   }

   private void fetchLyricsAsync(TrackState var1) {
      String var2 = this.getTrackKey(var1);
      if (!this.lyricsCache.containsKey(var2)) {
         long var3 = this.currentFetchId.incrementAndGet();
         this.activeFetchTrackKey = var2;
         CompletableFuture.runAsync(() -> {
            try {
               if (var3 != this.currentFetchId.get()) {
                  return;
               }

               String var6 = var1.title();
               String var7 = var1.artist();
               long var8 = var1.durationMs() / 1000L;
               rtx.nv.api.music.lyrics.LyricsManager.FetchResult var10 = this.searchMultiProviderLyrics(var6, var7, var8, var3);
               if (var3 != this.currentFetchId.get()) {
                  return;
               }

               List var11 = var10 != null && var10.lines() != null ? var10.lines() : Collections.emptyList();
               boolean var5 = var10 != null && var10.possibleMismatch() || this.isLyricsMismatched(var11, var1);
               if (var11.isEmpty()) {
                  this.lyricsCache.remove(var2);
                  return;
               }

               this.lyricsCache.put(var2, var11);
               if (!var5 || this.promptedMismatchTracks.contains(var2)) {
                  return;
               }

               this.promptedMismatchTracks.add(var2);
               this.sendMismatchNotification();
            } catch (Exception var15) {
               if (var3 == this.currentFetchId.get()) {
                  this.lyricsCache.remove(var2);
               }

               return;
            } finally {
               if (var3 == this.currentFetchId.get()) {
                  this.activeFetchTrackKey = null;
               }
            }
         }, this.executor);
      }
   }

   private void sendMismatchNotification() {
   }

   public void pushLiveCaption(String var1, long var2) {
      this.pushLiveCaptionWithWords(var1, var2, null);
   }

   public void pushLiveCaptionWithWords(String var1, long var2, List<Word> var4) {
      if (var1 != null && !var1.trim().isEmpty()) {
         long var5 = var2 > 0L ? var2 : 3500L;
         LyricLine var7 = new LyricLine(0L, var5, var1.trim(), var4);
         MinecraftClient var8 = MinecraftClient.getInstance();
         if (var8 != null && var8.getWindow() != null) {
            int var9 = var8.getWindow().getScaledWidth();
            int var10 = var8.getWindow().getScaledHeight();
            var8.execute(() -> this.spawnLiveBubble(var7, var9, var10, var5));
         }
      }
   }

   public void pushTranscript(String var1, List<LyricLine> var2) {
      if (var2 != null && !var2.isEmpty()) {
         if (var1 != null && !var1.isEmpty()) {
            this.lyricsCache.put(var1, var2);
         }

         if (this.currentTrackKey != null && !this.currentTrackKey.isEmpty()) {
            this.lyricsCache.put(this.currentTrackKey, var2);
         }

         rtx.nv.NV.LOGGER.info("[MusicSubtitles] Received live transcript with {} lines for: {}", var2.size(), var1);
      }
   }

   private void triggerLiveAcousticSupervisorIfNeeded(TrackState var1, List<LyricLine> var2) {
      if (var1 != null && var1.isActive() && !var1.isPaused() && var2 != null && !var2.isEmpty()) {
         String var3 = this.currentTrackKey;
         if (var3 != null && !var3.isEmpty() && !this.disabledTracks.contains(var3) && !this.autoShutoffTracks.contains(var3) && !this.isAcousticChecking) {
            long var4 = System.currentTimeMillis();
            if (var4 - this.lastAcousticCheckTime >= 6500L) {
               long var6 = var1.getInterpolatedPositionMs();
               long var8 = var1.durationMs();
               if (var8 <= 20000L || var6 >= 4000L && var6 <= var8 - 5000L) {
                  this.lastAcousticCheckTime = var4;
                  this.isAcousticChecking = true;
                  this.executor.submit(() -> {
                     try {
                        this.checkLiveAcousticMatchAsync(var1, var2, var3);
                     } catch (Exception var8x) {
                     } finally {
                        this.isAcousticChecking = false;
                     }
                  });
               }
            }
         }
      }
   }

   private void checkLiveAcousticMatchAsync(TrackState var1, List<LyricLine> var2, String var3) {
      if (var3.equals(this.currentTrackKey) && !this.disabledTracks.contains(var3)) {
         int var4 = this.resolveHelperPort();
         if (var4 > 0) {
            String var5 = "http://127.0.0.1:" + var4;
            short var6 = 2500;
            long var7 = var1.getInterpolatedPositionMs();
            long var9 = var7 + var6;
            String var11 = httpPost(var5 + "/capture/start", "{\"durationMs\":" + var6 + "}", 2000);
            if (var11 != null && !var11.isEmpty()) {
               try {
                  Thread.sleep(var6 + 250);
               } catch (InterruptedException var17) {
                  Thread.currentThread().interrupt();
                  return;
               }

               if (var3.equals(this.currentTrackKey) && !this.disabledTracks.contains(var3)) {
                  byte[] var12 = httpGetBytes(var5 + "/capture/result", 3000);
                  if (var12 != null && var12.length > 44) {
                     AcousticEnergyProfile var13 = StudioCascadeAligner.analyzeAcousticEnergy(var12);
                     if (var13.isPlayingSound()) {
                        boolean var14 = false;

                        for (LyricLine var16 : var2) {
                           if (Math.max(var7, var16.startMs) < Math.min(var9, var16.endMs)) {
                              var14 = true;
                              break;
                           }
                        }

                        if (var14 && !var13.hasVocalPresence()) {
                           int var19 = this.acousticMismatchStrikes.incrementAndGet();
                           if (var19 == 3) {
                              rtx.nv.NV.LOGGER.info("[MusicSubtitles] Acoustic diagnostic: lyrics expect vocals, but live audio is instrumental/quiet at {}ms", var7);
                           }
                        } else if (!var14 && var13.hasVocalPresence() && var13.rmsVocal() > 0.025 && var13.vocalRatio() > 0.35) {
                           int var18 = this.acousticMismatchStrikes.incrementAndGet();
                           if (var18 == 3) {
                              rtx.nv.NV.LOGGER.info("[MusicSubtitles] Acoustic diagnostic: lyrics expect pause, but live audio has loud vocals at {}ms", var7);
                           }
                        } else if (var14 && var13.hasVocalPresence()) {
                           this.acousticMismatchStrikes.updateAndGet(var0 -> Math.max(0, var0 - 1));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private int resolveHelperPort() {
      int configured = MusicSubtitlesConfig.INSTANCE.helperPort;
      if (configured > 0 && configured <= 65535) return configured;
      return rtx.nv.api.music.MusicHelperManager.get().getPort();
   }

   private rtx.nv.api.music.lyrics.LyricsManager.FetchResult searchMultiProviderLyrics(String var1, String var2, long var3, long var5) {
      if (var5 != this.currentFetchId.get()) {
         return null;
      } else {
         boolean var7 = SPEED_UP_PATTERN.matcher(var1).find() || SPEED_UP_PATTERN.matcher(var2).find();
         boolean var8 = SLOWED_PATTERN.matcher(var1).find() || SLOWED_PATTERN.matcher(var2).find();
         List var9 = this.checkLocalLyrics(var1, var2);
         if (var9 != null && !var9.isEmpty()) {
            return !var7 && !var8
               ? new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(var9, false)
               : new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(this.applySpeedAdjustment(var9, var3, var7, var8), false);
         } else {
            List var10 = this.lyricsCache.get(var1);
            if (var10 == null && !var1.isEmpty()) {
               var10 = this.lyricsCache.get(this.cleanForSearch(var1));
            }

            if (var10 != null && !var10.isEmpty()) {
               return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(var10, false);
            } else {
               String var11 = this.cleanForSearch(var1);
               String var12 = this.cleanForSearch(var2);
               Matcher var13 = SPLIT_PATTERN.matcher(var11);
               if (var13.find()) {
                  String var14 = this.cleanForSearch(var13.group(1));
                  String var15 = this.cleanForSearch(var13.group(2));
                  if (!var14.isEmpty() && !var15.isEmpty()) {
                     var12 = var14;
                     var11 = var15;
                  }
               }

               String var29 = var11;
               String var30 = var12;
               String var16 = this.normalizeTrackTitle(var29);
               String var17 = this.extractPrimaryArtist(var30);
               long var18 = !var7 && !var8 ? var3 : 0L;
               List<CompletableFuture<Candidate>> var20 = new ArrayList<>();
               var20.add(CompletableFuture.supplyAsync(() -> {
                  if (var5 != this.currentFetchId.get()) {
                     return null;
                  } else {
                     Candidate var8x = this.wrapSyncedResult("YANDEX_SYNCED", this.queryYandexMusic(var29, var30, var18, var5));
                     if (var8x == null && !var16.equalsIgnoreCase(var29)) {
                        var8x = this.wrapSyncedResult("YANDEX_SYNCED", this.queryYandexMusic(var16, var30, var18, var5));
                     }

                     if (var8x == null && !var30.isEmpty()) {
                        var8x = this.wrapSyncedResult("YANDEX_SYNCED", this.queryYandexMusic(var16, "", var18, var5));
                     }

                     return var8x;
                  }
               }, this.executor));
               if (!var16.isEmpty() && !var16.equalsIgnoreCase(var29)) {
                  var20.add(
                     CompletableFuture.supplyAsync(
                        () -> var5 != this.currentFetchId.get()
                           ? null
                           : this.wrapSyncedResult("LRCLIB_SYNCED", this.queryLrclibSearchSynced(var16, var18, var16, "", var5)),
                        this.executor
                     )
                  );
                  var20.add(
                     CompletableFuture.supplyAsync(
                        () -> var5 != this.currentFetchId.get() ? null : this.wrapSyncedResult("NETEASE", this.queryNeteaseLyrics(var16, var16, "", var5)),
                        this.executor
                     )
                  );
               }

               var20.add(
                  CompletableFuture.supplyAsync(() -> var5 != this.currentFetchId.get() ? null : this.queryGeniusLyrics(var29, var30, var5), this.executor)
               );
               if (!var17.equals(var30)) {
                  var20.add(
                     CompletableFuture.supplyAsync(() -> var5 != this.currentFetchId.get() ? null : this.queryGeniusLyrics(var29, var17, var5), this.executor)
                  );
               }

               if (!var29.isEmpty() && !var30.isEmpty()) {
                  var20.add(
                     CompletableFuture.supplyAsync(
                        () -> var5 != this.currentFetchId.get() ? null : this.queryLrclibGetCandidate(var29, var30, var18, var5), this.executor
                     )
                  );
                  var20.add(
                     CompletableFuture.supplyAsync(
                        () -> var5 != this.currentFetchId.get()
                           ? null
                           : this.wrapSyncedResult("LRCLIB_SYNCED", this.queryLrclibSearchSynced(var30 + " " + var29, var18, var29, var30, var5)),
                        this.executor
                     )
                  );
                  var20.add(
                     CompletableFuture.supplyAsync(
                        () -> var5 != this.currentFetchId.get()
                           ? null
                           : this.wrapSyncedResult("NETEASE", this.queryNeteaseLyrics(var30 + " " + var29, var29, var30, var5)),
                        this.executor
                     )
                  );
                  if (!var17.equals(var30)) {
                     var20.add(
                        CompletableFuture.supplyAsync(
                           () -> var5 != this.currentFetchId.get() ? null : this.queryLrclibGetCandidate(var29, var17, var18, var5), this.executor
                        )
                     );
                     var20.add(
                        CompletableFuture.supplyAsync(
                           () -> var5 != this.currentFetchId.get()
                              ? null
                              : this.wrapSyncedResult("LRCLIB_SYNCED", this.queryLrclibSearchSynced(var17 + " " + var29, var18, var29, var17, var5)),
                           this.executor
                        )
                     );
                     var20.add(
                        CompletableFuture.supplyAsync(
                           () -> var5 != this.currentFetchId.get()
                              ? null
                              : this.wrapSyncedResult("NETEASE", this.queryNeteaseLyrics(var17 + " " + var29, var29, var17, var5)),
                           this.executor
                        )
                     );
                  }
               }

               if (!var29.isEmpty()) {
                  var20.add(
                     CompletableFuture.supplyAsync(
                        () -> var5 != this.currentFetchId.get()
                           ? null
                           : this.wrapSyncedResult("LRCLIB_SYNCED", this.queryLrclibSearchSynced(var29, var18, var29, var30, var5)),
                        this.executor
                     )
                  );
                  var20.add(
                     CompletableFuture.supplyAsync(
                        () -> var5 != this.currentFetchId.get() ? null : this.wrapSyncedResult("NETEASE", this.queryNeteaseLyrics(var29, var29, var30, var5)),
                        this.executor
                     )
                  );
               }

               if (!var29.isEmpty() && !var30.isEmpty()) {
                  var20.add(
                     CompletableFuture.supplyAsync(
                        () -> var5 != this.currentFetchId.get()
                           ? null
                           : this.wrapSyncedResult("LRCLIB_SYNCED", this.queryLrclibSearchSynced(var29 + " " + var30, var18, var29, var30, var5)),
                        this.executor
                     )
                  );
                  var20.add(
                     CompletableFuture.supplyAsync(
                        () -> var5 != this.currentFetchId.get()
                           ? null
                           : this.wrapSyncedResult("NETEASE", this.queryNeteaseLyrics(var29 + " " + var30, var29, var30, var5)),
                        this.executor
                     )
                  );
               }

               if (!var1.equals(var29) || !var2.equals(var30)) {
                  var20.add(
                     CompletableFuture.supplyAsync(
                        () -> var5 != this.currentFetchId.get()
                           ? null
                           : this.wrapSyncedResult("LRCLIB_SYNCED", this.queryLrclibSearchSynced(var2 + " " + var1, var18, var29, var30, var5)),
                        this.executor
                     )
                  );
                  var20.add(
                     CompletableFuture.supplyAsync(
                        () -> var5 != this.currentFetchId.get()
                           ? null
                           : this.wrapSyncedResult("NETEASE", this.queryNeteaseLyrics(var2 + " " + var1, var29, var30, var5)),
                        this.executor
                     )
                  );
               }

               var20.add(
                  CompletableFuture.supplyAsync(
                     () -> var5 != this.currentFetchId.get() ? null : this.queryLrclibSearchPlainCandidate(var30 + " " + var29, var18, var29, var30, var5),
                     this.executor
                  )
               );
               var20.add(
                  CompletableFuture.supplyAsync(
                     () -> var5 != this.currentFetchId.get() ? null : this.queryLyricsOvhCandidate(var30, var29, var5), this.executor
                  )
               );
               List<Candidate> var21 = new CopyOnWriteArrayList<>();
               CountDownLatch var22 = new CountDownLatch(1);
               AtomicInteger var23 = new AtomicInteger(var20.size());

               for (CompletableFuture<Candidate> var25 : var20) {
                  var25.thenAccept(
                     var3x -> {
                        if (var3x != null) {
                           var21.add(var3x);
                           if (var3x.isSynced) {
                              for (Candidate var5x : var21) {
                                 if (var3x != var5x
                                    && ("GENIUS".equalsIgnoreCase(var5x.provider) || var5x.isSynced)
                                    && LyricsConsensusEngine.computePhraseOverlap(var3x.phrases, var5x.phrases) >= 0.6) {
                                    var22.countDown();
                                    break;
                                 }
                              }
                           }
                        }

                        if (var23.decrementAndGet() == 0) {
                           var22.countDown();
                        }
                     }
                  );
               }

               try {
                  var22.await(8000L, TimeUnit.MILLISECONDS);
               } catch (Exception var28) {
               }

               if (var5 != this.currentFetchId.get()) {
                  return null;
               } else {
                  Candidate var31 = LyricsConsensusEngine.selectBest(List.copyOf(var21), var3);
                  if (var31 == null) {
                     return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(Collections.emptyList(), false);
                  } else {
                     rtx.nv.api.music.lyrics.LyricsManager.FetchResult var32;
                     if (var31.isSynced && !var31.lines.isEmpty()) {
                        var32 = new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(var31.lines, var31.possibleMismatch);
                     } else if (!var31.rawText.isBlank()) {
                        long var26 = var3 > 0L ? var3 * 1000L : 180000L;
                        var32 = this.parsePlainLyrics(var31.rawText, var26, var5);
                     } else {
                        var32 = new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(Collections.emptyList(), false);
                     }

                     if (var32 != null && var32.lines() != null && !var32.lines().isEmpty()) {
                        List var33 = var32.lines();
                        if (var7 || var8) {
                           var33 = this.applySpeedAdjustment(var33, var3, var7, var8);
                        }

                        return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(var33, var32.possibleMismatch());
                     } else {
                        return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(Collections.emptyList(), false);
                     }
                  }
               }
            }
         }
      }
   }

   private List<LyricLine> applySpeedAdjustment(List<LyricLine> var1, long var2, boolean var4, boolean var5) {
      if (var1 != null && !var1.isEmpty()) {
         long var8 = var2 > 0L ? var2 * 1000L : 0L;
         long var10 = var1.get(var1.size() - 1).endMs;
         double var6;
         if (var8 > 30000L && var10 > 30000L) {
            double var12 = (double)var8 / var10;
            if (Math.abs(var12 - 1.0) < 0.05) {
               return var1;
            }

            if (var12 >= 0.5 && var12 <= 0.94) {
               var6 = var12;
            } else if (var12 >= 1.06 && var12 <= 1.85) {
               var6 = var12;
            } else if (var4) {
               var6 = 0.75;
            } else {
               if (!var5) {
                  return var1;
               }

               var6 = 1.3;
            }
         } else if (var4) {
            var6 = 0.75;
         } else {
            if (!var5) {
               return var1;
            }

            var6 = 1.3;
         }

         ArrayList var15 = new ArrayList(var1.size());

         for (LyricLine var14 : var1) {
            var15.add(var14.scaled(var6));
         }

         return var15;
      } else {
         return var1;
      }
   }

   private List<LyricLine> checkLocalLyrics(String var1, String var2) {
      try {
         if (!Files.exists(LOCAL_LYRICS_DIR)) {
            return null;
         }

         File[] var3 = LOCAL_LYRICS_DIR.toFile().listFiles((var0, var1x) -> var1x.toLowerCase().endsWith(".lrc"));
         if (var3 == null || var3.length == 0) {
            return null;
         }

         String var4 = normalizeForComparison(var1);
         String var5 = normalizeForComparison(var2);

         for (File var9 : var3) {
            String var12 = var9.getName().toLowerCase().replace(".lrc", "");
            String var13x = normalizeForComparison(var12);
            List var11;
            if ((var13x.contains(var4) || !var5.isEmpty() && var13x.contains(var5) && var13x.contains(var4))
               && !(var11 = this.parseLrc(Files.readString(var9.toPath(), StandardCharsets.UTF_8))).isEmpty()) {
               return var11;
            }
         }
      } catch (Exception var131) {
      }

      return null;
   }

   private static String httpGet(String var0, Map<String, String> var1, int var2) {
      HttpURLConnection var3 = null;

      String var26;
      try {
         URL var4 = URI.create(var0).toURL();
         var3 = (HttpURLConnection)var4.openConnection();
         var3.setRequestMethod("GET");
         var3.setConnectTimeout(var2);
         var3.setReadTimeout(var2);
         var3.setUseCaches(false);
         var3.setInstanceFollowRedirects(true);
         if (var1 != null) {
            for (Entry var6 : var1.entrySet()) {
               var3.setRequestProperty((String)var6.getKey(), (String)var6.getValue());
            }
         }

         if (var3.getResponseCode() != 200) {
            Object var24 = null;
            return (String)var24;
         }

         try (InputStream var25 = var3.getInputStream()) {
            var26 = new String(rtx.nv.utils.net.BoundedInput.read(var25, 2 * 1024 * 1024), StandardCharsets.UTF_8);
         }
      } catch (Exception var22) {
         return null;
      } finally {
         if (var3 != null) {
            try {
               var3.disconnect();
            } catch (Exception var19) {
            }
         }
      }

      return var26;
   }

   private rtx.nv.api.music.lyrics.LyricsManager.FetchResult queryYandexMusic(String var1, String var2, long var3, long var5) {
      if (var5 != this.currentFetchId.get()) {
         return null;
      } else {
         try {
            String var8 = (var2 + " " + var1).trim();
            String var9 = "https://api.music.yandex.net/search?text=" + URLEncoder.encode(var8, StandardCharsets.UTF_8) + "&type=track&page=0";
            HashMap var10 = new HashMap();
            var10.put("User-Agent", "YandexMusic/2024.01.1 (Android 14; Pixel 8 Pro)");
            var10.put("Accept", "application/json");
            if (MusicSubtitlesConfig.INSTANCE.yandexMusicToken != null && !MusicSubtitlesConfig.INSTANCE.yandexMusicToken.isBlank()) {
               var10.put("Authorization", "OAuth " + MusicSubtitlesConfig.INSTANCE.yandexMusicToken.trim());
            }

            String var7;
            if ((var7 = httpGet(var9, var10, 2500)) == null) {
               return null;
            }

            if (var5 != this.currentFetchId.get()) {
               return null;
            }

            JsonObject var11 = JsonParser.parseString(var7).getAsJsonObject();
            if (!var11.has("result")) {
               return null;
            }

            JsonObject var12 = var11.getAsJsonObject("result");
            if (!var12.has("tracks")) {
               return null;
            }

            JsonObject var13 = var12.getAsJsonObject("tracks");
            if (!var13.has("results")) {
               return null;
            }

            JsonArray var14 = var13.getAsJsonArray("results");
            if (var14.isEmpty()) {
               return null;
            }

            int var15 = Math.min(var14.size(), 6);

            for (int var16 = 0; var16 < var15; var16++) {
               if (var5 != this.currentFetchId.get()) {
                  return null;
               }

               JsonElement var17 = var14.get(var16);
               if (var17.isJsonObject()) {
                  JsonObject var18 = var17.getAsJsonObject();
                  if (var16 <= 0 || !var18.has("lyricsAvailable") || var18.get("lyricsAvailable").getAsBoolean()) {
                     String var19 = var18.has("title") && !var18.get("title").isJsonNull() ? var18.get("title").getAsString() : "";
                     double var20 = computeStringSimilarity(var1, var19);
                     if (!(var20 < 0.35)) {
                        boolean var22 = var20 < 0.75;
                        String var23 = var18.get("id").getAsString();
                        long var24 = var18.has("durationMs") ? var18.get("durationMs").getAsLong() : 0L;
                        String var26 = "https://api.music.yandex.net/tracks/" + var23 + "/supplement";
                        String var27 = httpGet(var26, var10, 2500);
                        if (var27 != null) {
                           if (var5 != this.currentFetchId.get()) {
                              return null;
                           }

                           JsonObject var28 = JsonParser.parseString(var27).getAsJsonObject();
                           if (var28.has("result")) {
                              JsonObject var29 = var28.getAsJsonObject("result");
                              if (var29.has("lyrics") && !var29.get("lyrics").isJsonNull()) {
                                 JsonObject var31 = var29.getAsJsonObject("lyrics");
                                 List var30;
                                 if (var31.has("syncedLyrics")
                                    && !var31.get("syncedLyrics").isJsonNull()
                                    && !(var30 = this.parseLrc(var31.get("syncedLyrics").getAsString())).isEmpty()) {
                                    return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(var30, var22);
                                 }

                                 if (var31.has("fullLyrics") && !var31.get("fullLyrics").isJsonNull()) {
                                    long var32 = var3 > 0L ? var3 * 1000L : (var24 > 0L ? var24 : 180000L);
                                    rtx.nv.api.music.lyrics.LyricsManager.FetchResult var34 = this.parsePlainLyrics(
                                       var31.get("fullLyrics").getAsString(), var32, var5
                                    );
                                    if (var34 != null && var34.lines() != null && !var34.lines().isEmpty()) {
                                       return var34;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         } catch (Exception var35) {
         }

         return null;
      }
   }

   private rtx.nv.api.music.lyrics.LyricsManager.FetchResult queryNeteaseLyrics(String var1, String var2, String var3, long var4) {
      if (var4 != this.currentFetchId.get()) {
         return null;
      } else {
         try {
            String var8 = "https://music.163.com/api/search/get/web?s="
               + URLEncoder.encode(var1, StandardCharsets.UTF_8)
               + "&type=1&offset=0&total=true&limit=3";
            Map var9 = Map.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)", "Referer", "https://music.163.com/");
            String var10 = httpGet(var8, var9, 2500);
            if (var10 == null) {
               return null;
            }

            if (var4 != this.currentFetchId.get()) {
               return null;
            }

            JsonObject var11 = JsonParser.parseString(var10).getAsJsonObject();
            if (!var11.has("result") || var11.get("result").isJsonNull()) {
               return null;
            }

            JsonObject var12 = var11.getAsJsonObject("result");
            if (!var12.has("songs") || var12.get("songs").isJsonNull()) {
               return null;
            }

            JsonArray var13 = var12.getAsJsonArray("songs");
            if (var13.isEmpty()) {
               return null;
            }

            JsonObject var14 = null;
            double var15 = 0.0;

            for (JsonElement var18 : var13) {
               if (var18.isJsonObject()) {
                  JsonObject var19 = var18.getAsJsonObject();
                  String var20 = var19.has("name") && !var19.get("name").isJsonNull() ? var19.get("name").getAsString() : "";
                  double var22 = computeStringSimilarity(var2, var20);
                  if (var22 > var15) {
                     var15 = var22;
                     var14 = var19;
                  }
               }
            }

            if (var14 == null || var15 < 0.45) {
               return null;
            }

            boolean var25 = var15 < 0.75;
            long var26 = var14.get("id").getAsLong();
            String var27 = "https://music.163.com/api/song/lyric?os=pc&id=" + var26 + "&lv=-1&kv=-1&tv=-1";
            String var21 = httpGet(var27, var9, 2500);
            if (var21 == null) {
               return null;
            }

            if (var4 != this.currentFetchId.get()) {
               return null;
            }

            JsonObject var28 = JsonParser.parseString(var21).getAsJsonObject();
            List var6;
            JsonObject var7;
            if (var28.has("lrc")
               && !var28.get("lrc").isJsonNull()
               && (var7 = var28.getAsJsonObject("lrc")).has("lyric")
               && !var7.get("lyric").isJsonNull()
               && !(var6 = this.parseLrc(var7.get("lyric").getAsString())).isEmpty()) {
               return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(var6, var25);
            }
         } catch (Exception var23) {
         }

         return null;
      }
   }

   private Candidate wrapSyncedResult(String var1, rtx.nv.api.music.lyrics.LyricsManager.FetchResult var2) {
      if (var2 != null && var2.lines() != null && !var2.lines().isEmpty()) {
         Candidate var3 = new Candidate(var1, var2.lines(), null, true);
         var3.possibleMismatch = var2.possibleMismatch();
         return var3;
      } else {
         return null;
      }
   }

   private Candidate queryGeniusLyrics(String var1, String var2, long var3) {
      if (var3 != this.currentFetchId.get()) {
         return null;
      } else {
         try {
            String var5 = GeniusLyricsProvider.fetchLyrics(var1, var2, 3000L);
            if (var5 != null && !var5.isBlank()) {
               return new Candidate("GENIUS", null, var5, false);
            }
         } catch (Exception var6) {
         }

         return null;
      }
   }

   private Candidate queryLrclibGetCandidate(String var1, String var2, long var3, long var5) {
      if (var5 != this.currentFetchId.get()) {
         return null;
      } else {
         try {
            StringBuilder var9 = new StringBuilder("https://lrclib.net/api/get?");
            var9.append("track_name=").append(URLEncoder.encode(var1, StandardCharsets.UTF_8));
            var9.append("&artist_name=").append(URLEncoder.encode(var2, StandardCharsets.UTF_8));
            if (var3 > 0L) {
               var9.append("&duration=").append(var3);
            }

            Map var10 = Map.of("User-Agent", "MusicSubtitles/2.3.0 (Minecraft Fabric)");
            String var11 = httpGet(var9.toString(), var10, 2500);
            if (var11 == null || var5 != this.currentFetchId.get()) {
               return null;
            }

            JsonObject var12 = JsonParser.parseString(var11).getAsJsonObject();
            List var8;
            if (var12.has("syncedLyrics")
               && !var12.get("syncedLyrics").isJsonNull()
               && !(var8 = this.parseLrc(var12.get("syncedLyrics").getAsString())).isEmpty()) {
               return new Candidate("LRCLIB_SYNCED", var8, null, true);
            }

            String var7;
            if (var12.has("plainLyrics") && !var12.get("plainLyrics").isJsonNull() && !(var7 = var12.get("plainLyrics").getAsString()).isBlank()) {
               return new Candidate("LRCLIB_PLAIN", null, var7, false);
            }
         } catch (Exception var13) {
         }

         return null;
      }
   }

   private Candidate queryLrclibSearchPlainCandidate(String var1, long var2, String var4, String var5, long var6) {
      if (var6 != this.currentFetchId.get()) {
         return null;
      } else {
         try {
            String var9 = "https://lrclib.net/api/search?q=" + URLEncoder.encode(var1, StandardCharsets.UTF_8);
            Map var10 = Map.of("User-Agent", "MusicSubtitles/2.3.0 (Minecraft Fabric)");
            String var11 = httpGet(var9, var10, 2500);
            if (var11 == null || var6 != this.currentFetchId.get()) {
               return null;
            }

            JsonArray var12 = JsonParser.parseString(var11).getAsJsonArray();
            if (var12.isEmpty()) {
               return null;
            }

            JsonObject var13 = null;
            int var14 = 40;

            for (JsonElement var16 : var12) {
               int var17;
               JsonObject var18x;
               if (var16.isJsonObject()
                  && (var18x = var16.getAsJsonObject()).has("plainLyrics")
                  && !var18x.get("plainLyrics").isJsonNull()
                  && (var17 = this.calculateMatchScore(var18x, var2, var4, var5)) > var14) {
                  var14 = var17;
                  var13 = var18x;
               }
            }

            String var8;
            if (var13 != null && !(var8 = var13.get("plainLyrics").getAsString()).isBlank()) {
               return new Candidate("LRCLIB_PLAIN", null, var8, false);
            }
         } catch (Exception var181) {
         }

         return null;
      }
   }

   private Candidate queryLyricsOvhCandidate(String var1, String var2, long var3) {
      if (var3 != this.currentFetchId.get()) {
         return null;
      } else {
         try {
            if (var1.isEmpty() || var2.isEmpty()) {
               return null;
            }

            String var7 = "https://api.lyrics.ovh/v1/"
               + URLEncoder.encode(var1, StandardCharsets.UTF_8)
               + "/"
               + URLEncoder.encode(var2, StandardCharsets.UTF_8);
            String var8 = httpGet(var7, Map.of("User-Agent", "MusicSubtitles/2.3.0"), 2500);
            if (var8 == null || var3 != this.currentFetchId.get()) {
               return null;
            }

            JsonObject var9x = JsonParser.parseString(var8).getAsJsonObject();
            String var5;
            if (var9x.has("lyrics") && !var9x.get("lyrics").isJsonNull() && !(var5 = var9x.get("lyrics").getAsString()).isBlank()) {
               return new Candidate("OVH", null, var5, false);
            }
         } catch (Exception var91) {
         }

         return null;
      }
   }

   private rtx.nv.api.music.lyrics.LyricsManager.FetchResult queryLyricsOvh(String var1, String var2, long var3, long var5) {
      if (var5 != this.currentFetchId.get()) {
         return null;
      } else {
         try {
            if (var1.isEmpty() || var2.isEmpty()) {
               return null;
            }

            String var8 = "https://api.lyrics.ovh/v1/"
               + URLEncoder.encode(var1, StandardCharsets.UTF_8)
               + "/"
               + URLEncoder.encode(var2, StandardCharsets.UTF_8);
            String var9 = httpGet(var8, Map.of("User-Agent", "MusicSubtitles/2.2.5"), 2500);
            if (var9 == null) {
               return null;
            }

            if (var5 != this.currentFetchId.get()) {
               return null;
            }

            JsonObject var10 = JsonParser.parseString(var9).getAsJsonObject();
            if (var10.has("lyrics") && !var10.get("lyrics").isJsonNull()) {
               long var11 = var3 > 0L ? var3 * 1000L : 180000L;
               return this.parsePlainLyrics(var10.get("lyrics").getAsString(), var11, var5);
            }
         } catch (Exception var12) {
         }

         return null;
      }
   }

   private rtx.nv.api.music.lyrics.LyricsManager.FetchResult queryLrclibGet(String var1, String var2, long var3, long var5) {
      if (var5 != this.currentFetchId.get()) {
         return null;
      } else {
         try {
            StringBuilder var8 = new StringBuilder("https://lrclib.net/api/get?");
            var8.append("track_name=").append(URLEncoder.encode(var1, StandardCharsets.UTF_8));
            var8.append("&artist_name=").append(URLEncoder.encode(var2, StandardCharsets.UTF_8));
            if (var3 > 0L) {
               var8.append("&duration=").append(var3);
            }

            Map var9 = Map.of("User-Agent", "MusicSubtitles/2.2.5 (Minecraft Fabric)");
            String var10 = httpGet(var8.toString(), var9, 2500);
            if (var10 == null) {
               return null;
            }

            if (var5 != this.currentFetchId.get()) {
               return null;
            }

            JsonObject var11 = JsonParser.parseString(var10).getAsJsonObject();
            List var7;
            if (var11.has("syncedLyrics")
               && !var11.get("syncedLyrics").isJsonNull()
               && !(var7 = this.parseLrc(var11.get("syncedLyrics").getAsString())).isEmpty()) {
               return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(var7, false);
            }

            if (var11.has("plainLyrics") && !var11.get("plainLyrics").isJsonNull()) {
               long var12 = var3 > 0L ? var3 * 1000L : 180000L;
               return this.parsePlainLyrics(var11.get("plainLyrics").getAsString(), var12, var5);
            }
         } catch (Exception var14) {
         }

         return null;
      }
   }

   private rtx.nv.api.music.lyrics.LyricsManager.FetchResult queryLrclibSearchSynced(String var1, long var2, String var4, String var5, long var6) {
      if (var6 != this.currentFetchId.get()) {
         return null;
      } else {
         try {
            String var9 = "https://lrclib.net/api/search?q=" + URLEncoder.encode(var1, StandardCharsets.UTF_8);
            Map var10 = Map.of("User-Agent", "MusicSubtitles/2.2.5 (Minecraft Fabric)");
            String var11 = httpGet(var9, var10, 2500);
            if (var11 == null) {
               return null;
            }

            if (var6 != this.currentFetchId.get()) {
               return null;
            }

            JsonArray var12 = JsonParser.parseString(var11).getAsJsonArray();
            if (var12.isEmpty()) {
               return null;
            }

            JsonObject var13 = null;
            int var14 = 40;

            for (JsonElement var16 : var12) {
               int var17;
               JsonObject var18x;
               if (var16.isJsonObject()
                  && (var18x = var16.getAsJsonObject()).has("syncedLyrics")
                  && !var18x.get("syncedLyrics").isJsonNull()
                  && (var17 = this.calculateMatchScore(var18x, var2, var4, var5)) > var14) {
                  var14 = var17;
                  var13 = var18x;
               }
            }

            List var8;
            if (var13 != null && !(var8 = this.parseLrc(var13.get("syncedLyrics").getAsString())).isEmpty()) {
               boolean var20 = var14 < 250;
               return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(var8, var20);
            }
         } catch (Exception var181) {
         }

         return null;
      }
   }

   private rtx.nv.api.music.lyrics.LyricsManager.FetchResult queryLrclibSearchPlain(String var1, long var2, String var4, String var5, long var6) {
      if (var6 != this.currentFetchId.get()) {
         return null;
      } else {
         try {
            String var8 = "https://lrclib.net/api/search?q=" + URLEncoder.encode(var1, StandardCharsets.UTF_8);
            Map var9 = Map.of("User-Agent", "MusicSubtitles/2.2.5 (Minecraft Fabric)");
            String var10 = httpGet(var8, var9, 2500);
            if (var10 == null) {
               return null;
            }

            if (var6 != this.currentFetchId.get()) {
               return null;
            }

            JsonArray var11 = JsonParser.parseString(var10).getAsJsonArray();
            if (var11.isEmpty()) {
               return null;
            }

            JsonObject var12 = null;
            int var13 = 40;

            for (JsonElement var15 : var11) {
               int var16;
               JsonObject var17;
               if (var15.isJsonObject()
                  && (var17 = var15.getAsJsonObject()).has("plainLyrics")
                  && !var17.get("plainLyrics").isJsonNull()
                  && (var16 = this.calculateMatchScore(var17, var2, var4, var5)) > var13) {
                  var13 = var16;
                  var12 = var17;
               }
            }

            if (var12 != null) {
               long var19 = var2 > 0L ? var2 * 1000L : 180000L;
               return this.parsePlainLyrics(var12.get("plainLyrics").getAsString(), var19, var6);
            }
         } catch (Exception var18) {
         }

         return null;
      }
   }

   private int calculateMatchScore(JsonObject var1, long var2, String var4, String var5) {
      String var8 = var1.has("trackName") && !var1.get("trackName").isJsonNull() ? var1.get("trackName").getAsString() : "";
      String var9 = var1.has("artistName") && !var1.get("artistName").isJsonNull() ? var1.get("artistName").getAsString() : "";
      double var10 = computeStringSimilarity(var4, var8);
      double var12 = computeStringSimilarity(var5, var9);
      // Some community records swap artist and title fields. Accept only a strong two-way match.
      double swappedTitle = computeStringSimilarity(var4, var9);
      double swappedArtist = computeStringSimilarity(var5, var8);
      if (swappedTitle >= .9 && swappedArtist >= .75 && swappedTitle + swappedArtist > var10 + var12) {
         var10 = swappedTitle; var12 = swappedArtist;
      }
      if (var10 < 0.35) {
         return 0;
      } else {
         int var14 = 0;
         if (var10 >= 0.9) {
            var14 += 300;
         } else {
            int var21;
            int var14x;
            var14 = var10 >= 0.5 ? (var21 = var14 + (int)(240.0 * var10)) : (var14x = var14 + (int)(150.0 * var10));
         }

         if (var12 >= 0.85) {
            var14 += 150;
         } else if (var12 >= 0.3) {
            var14 += (int)(120.0 * var12);
         } else if (!var5.isEmpty()) {
            var14 -= 100;
         }

         long var6 = var1.has("duration") && !var1.get("duration").isJsonNull() ? (long)var1.get("duration").getAsDouble() : 0L;
         if (var2 > 0L && var6 > 0L) {
            long var18 = Math.abs(var6 - var2);
            if (var18 <= 2L) {
               var14 += 500;
            } else if (var18 <= 5L) {
               var14 += 250;
            } else if (var18 <= 10L) {
               var14 += 100;
            } else if (var18 > 15L) {
               return 0; // A different edit/version is not a reliable synchronized transcript.
            }
         }

         return var14;
      }
   }

   private static double computeStringSimilarity(String var0, String var1) {
      if (var0 != null && var1 != null) {
         String var2 = normalizeForComparison(var0);
         String var3 = normalizeForComparison(var1);
         if (var2.isEmpty() || var3.isEmpty()) {
            return 0.0;
         } else if (var2.equals(var3)) {
            return 1.0;
         } else if (!var2.contains(var3) && !var3.contains(var2)) {
            HashSet<String> var4 = new HashSet<>(Arrays.asList(var2.split("\\s+")));
            HashSet<String> var5 = new HashSet<>(Arrays.asList(var3.split("\\s+")));
            var4.removeIf(var0x -> var0x.length() < 2);
            var5.removeIf(var0x -> var0x.length() < 2);
            if (!var4.isEmpty() && !var5.isEmpty()) {
               HashSet<String> var6 = new HashSet<>(var4);
               var6.retainAll(var5);
               HashSet<String> var7 = new HashSet<>(var4);
               var7.addAll(var5);
               return (double)var6.size() / var7.size();
            } else {
               return 0.0;
            }
         } else {
            return (double)Math.min(var2.length(), var3.length()) / Math.max(var2.length(), var3.length());
         }
      } else {
         return 0.0;
      }
   }

   private boolean isLyricsMismatched(List<LyricLine> var1, TrackState var2) {
      if (var1 != null && !var1.isEmpty() && var2 != null) {
         long var3 = var2.durationMs();
         if (var3 > 30000L) {
            long var6 = var1.get(var1.size() - 1).endMs;
            long var8 = var1.get(0).startMs;
            if (var8 > var3 * 0.55) {
               return true;
            }

            if (var6 > var3 + 25000L) {
               return true;
            }

            boolean var5 = SPEED_UP_PATTERN.matcher(var2.title()).find() || SPEED_UP_PATTERN.matcher(var2.artist()).find();
            if (!var5 && var6 < var3 * 0.35 && var1.size() > 2) {
               return true;
            }

            if (!var5 && var3 - var6 > 55000L && var1.size() > 3) {
               return true;
            }
         }

         String var13 = (var2.artist() + " " + var2.title()).toLowerCase();
         int var14 = countCyrillicChars(var13);
         int var7 = countCjkChars(var13);
         StringBuilder var15 = new StringBuilder();
         int var9 = Math.min(10, var1.size());

         for (int var10 = 0; var10 < var9; var10++) {
            var15.append(var1.get(var10).text).append(" ");
         }

         String var16 = var15.toString();
         int var11 = countCjkChars(var16);
         int var12 = countCyrillicChars(var16);
         return var14 >= 3 && var12 == 0 && var11 >= 5 ? true : var7 == 0 && var11 > 15 && var14 >= 2;
      } else {
         return false;
      }
   }

   private static int countCyrillicChars(String var0) {
      if (var0 == null) {
         return 0;
      } else {
         int var1 = 0;

         for (int var2 = 0; var2 < var0.length(); var2++) {
            char var3 = var0.charAt(var2);
            if (var3 >= 1024 && var3 <= 1279 || var3 >= 1280 && var3 <= 1327) {
               var1++;
            }
         }

         return var1;
      }
   }

   private static int countCjkChars(String var0) {
      if (var0 == null) {
         return 0;
      } else {
         int var1 = 0;

         for (int var2 = 0; var2 < var0.length(); var2++) {
            char var3 = var0.charAt(var2);
            if (var3 >= 19968 && var3 <= '鿿' || var3 >= 13312 && var3 <= 19903) {
               var1++;
            }
         }

         return var1;
      }
   }

   private static String normalizeForComparison(String var0) {
      return var0 == null ? "" : var0.toLowerCase().replaceAll("[^a-zа-я0-9\\s]", "").replaceAll("\\s+", " ").trim();
   }

   private List<LyricLine> parseLrc(String input) {
      return LrcParser.parse(input);
   }

   private List<LyricLine> splitLongLyricLine(long var1, long var3, String var5, List<Word> var6) {
      ArrayList var7 = new ArrayList();
      if (var5 != null && !var5.trim().isEmpty()) {
         var5 = var5.trim();
         String[] var8 = var5.split("\\s+");
         long var9 = var3 - var1;
         if (var8.length <= 5) {
            var7.add(new LyricLine(var1, var3, var5, var6));
            return var7;
         } else {
            byte var11 = 5;
            int var12 = (int)Math.ceil(var8.length / 5.0);
            int var13 = (int)Math.ceil((double)var8.length / var12);
            if (var13 > 5) {
               var13 = 5;
            }

            int var14 = 0;
            long var15 = var1;
            int var17 = var5.length();

            for (int var18 = 0; var18 < var12 && var14 < var8.length; var18++) {
               int var19 = var12 - var18;
               int var20 = var8.length - var14;
               int var21 = var19 == 1 ? Math.min(5, var20) : Math.min(var13, var20);
               CharSequence[] var22 = Arrays.copyOfRange(var8, var14, var14 + var21);
               String var23 = String.join(" ", var22);
               long var24 = var19 != 1 && (var14 += var21) < var8.length ? (long)(var9 * ((double)var23.length() / var17)) : var3 - var15;
               if (var24 < 700L) {
                  var24 = Math.min(var9, 700L);
               }

               long var28 = var19 != 1 && var14 < var8.length ? Math.min(var3, var15 + var24) : var3;
               var7.add(new LyricLine(var15, var28, var23, null));
               var15 = var28;
            }

            return var7;
         }
      } else {
         return var7;
      }
   }

   private List<Word> parseEnhancedLrcWords(String var1, long var2, long var4) {
      ArrayList var6 = new ArrayList();
      Matcher var7 = ENHANCED_WORD_PATTERN.matcher(var1);
      int var8 = 0;

      while (var7.find()) {
         long var11 = Long.parseLong(var7.group(1));
         double var13 = Double.parseDouble(var7.group(2));
         long var15 = var11 * 60000L + (long)(var13 * 1000.0);
         String var17 = var7.group(3).trim();
         if (!var17.isEmpty()) {
            int var19 = var8 + var17.length();
            var6.add(new Word(var15, var15 + 500L, var17, var8, var19));
            var8 = var19 + 1;
            long var17x = var15 + 500L;
         }
      }

      for (int var20 = 0; var20 < var6.size(); var20++) {
         Word var12 = (Word)var6.get(var20);
         long var21 = var20 < var6.size() - 1 ? ((Word)var6.get(var20 + 1)).startMs() : Math.min(var4, var12.startMs() + 800L);
         var6.set(var20, new Word(var12.startMs(), var21, var12.text(), var12.charStart(), var12.charEnd()));
      }

      return var6;
   }

   private String stripEnhancedLrcTags(String var1) {
      return var1.replaceAll("<\\d+:\\d+(?:\\.\\d+)?>", "").replaceAll("\\[\\d+:\\d+(?:\\.\\d+)?\\]", "").trim();
   }

   private rtx.nv.api.music.lyrics.LyricsManager.FetchResult parsePlainLyrics(String var1, long var2, long var4) {
      if (var1 != null && !var1.isEmpty()) {
         String[] var6 = var1.split("\\r?\\n");
         ArrayList var7 = new ArrayList();

         for (String var11 : var6) {
            String var12 = var11.trim();
            if (!var12.isEmpty() && !var12.startsWith("[") && !var12.endsWith("]")) {
               var7.add(var12);
            }
         }

         if (var7.isEmpty()) {
            return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(Collections.emptyList(), false);
         } else {
            rtx.nv.api.music.lyrics.LyricsManager.FetchResult var18 = this.trySmartAlignment(var7, var2, var4);
            if (var18 != null && var18.lines() != null && !var18.lines().isEmpty()) {
               return var18;
            } else if (var4 != this.currentFetchId.get()) {
               return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(Collections.emptyList(), false);
            } else {
               List var19 = StudioCascadeAligner.align(var1, null, var2);
               if (var19 != null && !var19.isEmpty()) {
                  return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(var19, false);
               } else {
                  ArrayList var20 = new ArrayList();
                  long var21 = Math.max(3000L, var2 / var7.size());

                  for (int var13 = 0; var13 < var7.size(); var13++) {
                     long var14 = var13 * var21;
                     long var16 = var14 + var21;
                     var20.addAll(this.splitLongLyricLine(var14, var16, (String)var7.get(var13), null));
                  }

                  return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(var20, true);
               }
            }
         }
      } else {
         return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(Collections.emptyList(), false);
      }
   }

   private rtx.nv.api.music.lyrics.LyricsManager.FetchResult trySmartAlignment(List<String> var1, long var2, long var4) {
      try {
         if (var4 != this.currentFetchId.get()) {
            return null;
         } else {
            int var8 = this.resolveHelperPort();
            if (var8 <= 0) {
               return null;
            } else {
               String var9 = "http://127.0.0.1:" + var8;
               short var10 = 4000;
               if (httpPost(var9 + "/capture/start", "{\"durationMs\":" + var10 + "}", 2000) == null) return null;

               for (short var11 = 0; var11 < var10 + 400; var11 = (short)(var11 + 150)) {
                  if (var4 != this.currentFetchId.get()) {
                     httpPost(var9 + "/capture/stop", "", 1000);
                     return null;
                  }

                  Thread.sleep(150L);
               }

               if (var4 != this.currentFetchId.get()) {
                  return null;
               } else {
                  String var44 = null;
                  byte[] var12 = httpGetBytes(var9 + "/capture/result", 3000);
                  if (var12 != null && var12.length > 44) {
                     List var13 = StudioCascadeAligner.align(String.join("\n", var1), var12, var2);
                     if (var13 != null && !var13.isEmpty()) {
                        return new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(var13, false);
                     }

                     var44 = Base64.getEncoder().encodeToString(var12);
                  }

                  StringBuilder var45 = new StringBuilder();
                  var45.append("{\"lines\":[");

                  for (int var14 = 0; var14 < var1.size(); var14++) {
                     if (var14 > 0) {
                        var45.append(",");
                     }

                     var45.append("\"").append(var1.get(var14).replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
                  }

                  var45.append("],\"durationMs\":").append(var2);
                  if (var44 != null) {
                     var45.append(",\"wavBase64\":\"").append(var44).append("\"");
                  }

                  var45.append("}");
                  if (var4 != this.currentFetchId.get()) {
                     return null;
                  } else {
                     String var46 = httpPost(var9 + "/smart-align", var45.toString(), 5000);
                     if (var46 == null || var46.isEmpty()) {
                        return null;
                     } else if (var4 != this.currentFetchId.get()) {
                        return null;
                     } else {
                        JsonObject var15 = JsonParser.parseString(var46).getAsJsonObject();
                        if (var15.has("success") && var15.get("success").getAsBoolean()) {
                           if (var15.has("lines") && !var15.get("lines").isJsonNull()) {
                              boolean var16 = false;
                              if (var15.has("possibleMismatch") && var15.get("possibleMismatch").getAsBoolean()) {
                                 var16 = true;
                              }

                              double var6 = var15.has("confidence") ? var15.get("confidence").getAsDouble() : 1.0;
                              if (var6 < 0.35) {
                                 var16 = true;
                              }

                              JsonArray var19 = var15.getAsJsonArray("lines");
                              ArrayList var20 = new ArrayList();

                              for (JsonElement var22 : var19) {
                                 if (var22.isJsonObject()) {
                                    JsonObject var24 = var22.getAsJsonObject();
                                    long var25 = var24.has("startMs") ? var24.get("startMs").getAsLong() : 0L;
                                    long var27 = var24.has("endMs") ? var24.get("endMs").getAsLong() : var25 + 4000L;
                                    String var23 = var24.has("text") ? var24.get("text").getAsString() : "";
                                    if (!var23.isEmpty()) {
                                       ArrayList var30 = null;
                                       if (var24.has("words") && var24.get("words").isJsonArray()) {
                                          JsonArray var31 = var24.getAsJsonArray("words");
                                          var30 = new ArrayList();

                                          for (JsonElement var33 : var31) {
                                             if (var33.isJsonObject()) {
                                                JsonObject var35 = var33.getAsJsonObject();
                                                long var36 = var35.has("startMs") ? var35.get("startMs").getAsLong() : var25;
                                                long var38 = var35.has("endMs") ? var35.get("endMs").getAsLong() : var36 + 500L;
                                                String var40 = var35.has("text") ? var35.get("text").getAsString() : "";
                                                int var41 = var35.has("charStart") ? var35.get("charStart").getAsInt() : 0;
                                                int var34 = var35.has("charEnd") ? var35.get("charEnd").getAsInt() : var40.length();
                                                if (!var40.isEmpty()) {
                                                   var30.add(new Word(var36, var38, var40, var41, var34));
                                                }
                                             }
                                          }

                                          if (var30.isEmpty()) {
                                             var30 = null;
                                          }
                                       }

                                       long var47 = var30 != null && !var30.isEmpty() ? ((Word)var30.get(var30.size() - 1)).endMs() : var27;
                                       var20.add(new LyricLine(var25, var27, var47, var23, var30));
                                    }
                                 }
                              }

                              if (!var20.isEmpty()) {
                                 rtx.nv.NV.LOGGER.info("[MusicSubtitles] Smart alignment successful: {} lines aligned (confidence: {})", var20.size(), var6);
                              }

                              return var20.isEmpty() ? null : new rtx.nv.api.music.lyrics.LyricsManager.FetchResult(var20, var16);
                           } else {
                              return null;
                           }
                        } else {
                           return null;
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var39) {
         rtx.nv.NV.LOGGER.warn("[MusicSubtitles] Smart alignment failed, using fallback: {}", var39.getMessage());
         return null;
      }
   }

   public static String httpPost(String var0, String var1, int var2) {
      HttpURLConnection var3 = null;

      String var9;
      try {
         URL var4 = URI.create(var0).toURL();
         var3 = (HttpURLConnection)var4.openConnection();
         var3.setRequestMethod("POST");
         var3.setConnectTimeout(var2);
         var3.setReadTimeout(var2);
         var3.setDoOutput(true);
         var3.setRequestProperty("Content-Type", "application/json; charset=utf-8");
         byte[] var25 = var1.getBytes(StandardCharsets.UTF_8);
         var3.setFixedLengthStreamingMode(var25.length);
         var3.getOutputStream().write(var25);
         var3.getOutputStream().flush();
         int var6 = var3.getResponseCode();
         if (var6 != 200) {
            Object var7 = null;
            return (String)var7;
         }

         try (InputStream var26 = var3.getInputStream()) {
            var9 = new String(rtx.nv.utils.net.BoundedInput.read(var26, 2 * 1024 * 1024), StandardCharsets.UTF_8);
         }
      } catch (Exception var24) {
         return null;
      } finally {
         if (var3 != null) {
            try {
               var3.disconnect();
            } catch (Exception var21) {
            }
         }
      }

      return var9;
   }

   public static byte[] httpGetBytes(String var0, int var1) {
      HttpURLConnection var2 = null;

      byte[] var7;
      try {
         URL var3 = URI.create(var0).toURL();
         var2 = (HttpURLConnection)var3.openConnection();
         var2.setRequestMethod("GET");
         var2.setConnectTimeout(var1);
         var2.setReadTimeout(var1);
         int var23 = var2.getResponseCode();
         if (var23 != 200) {
            Object var5 = null;
            return (byte[])var5;
         }

         try (InputStream var24 = var2.getInputStream()) {
            var7 = rtx.nv.utils.net.BoundedInput.read(var24, 8 * 1024 * 1024);
         }
      } catch (Exception var22) {
         return null;
      } finally {
         if (var2 != null) {
            try {
               var2.disconnect();
            } catch (Exception var19) {
            }
         }
      }

      return var7;
   }

   private String cleanForSearch(String var1) {
      if (var1 == null) {
         return "";
      } else {
         String var2 = var1.trim();
         var2 = BROWSER_SUFFIX_PATTERN.matcher(var2).replaceAll("");
         var2 = PIPE_NOISE_PATTERN.matcher(var2).replaceAll("");

         for (int var3 = 0; var3 < 3; var3++) {
            var2 = BRACKET_TAGS_PATTERN.matcher(var2).replaceAll("");
         }

         var2 = TRAILING_NOISE_PATTERN.matcher(var2).replaceAll("");
         var2 = var2.replaceAll("[►◄\"'«»“”„]", "");
         return var2.replaceAll("\\s+", " ").trim();
      }
   }

   private String normalizeTrackTitle(String var1) {
      if (var1 == null) {
         return "";
      } else {
         String var2 = var1.trim();
         String var3 = var2.toLowerCase(Locale.ROOT);
         return !var3.contains("carmelldansen") && !var3.contains("carmell dansen") ? var2 : var2.replaceAll("(?i)carmell\\s*dansen", "Caramelldansen");
      }
   }

   private String extractPrimaryArtist(String var1) {
      if (var1 == null) {
         return "";
      } else {
         String var2 = var1.replaceAll("(?i)\\s*(?:feat\\.?|ft\\.?|featuring)\\s+.*$", "").trim();
         String[] var3 = var2.split("(?i)\\s*(?:,|&|x|\\+)\\s*");
         if (var3.length > 0 && !var3[0].trim().isEmpty()) {
            String var4 = var3[0].trim();
            return var4.toLowerCase(Locale.ROOT).startsWith("prodby") && var3.length > 1 ? var3[1].trim() : var4;
         } else {
            return var2;
         }
      }
   }

   public record FetchResult(List<LyricLine> lines, boolean possibleMismatch) {
   }

   private record RawLine(long timeMs, String text) {
   }

    public List<LyricLine> getChunks(LyricLine line, int wordsPerChunk) {
        if (line == null || line.getText().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> words = line.getWords();
        if (words == null || words.isEmpty()) {
            return Collections.singletonList(line);
        }
        int chunkCount = (int) Math.ceil((double) words.size() / (double) Math.max(1, wordsPerChunk));
        if (chunkCount <= 1) {
            return Collections.singletonList(line);
        }
        long lineDur = line.getEndMs() - line.getStartMs();
        long stepDur = Math.max(800L, lineDur / chunkCount);
        List<LyricLine> chunks = new ArrayList<>();
        for (int c = 0; c < chunkCount; c++) {
            int startIdx = c * wordsPerChunk;
            int endIdx = Math.min(words.size(), startIdx + wordsPerChunk);
            StringBuilder sb = new StringBuilder();
            for (int i = startIdx; i < endIdx; i++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(words.get(i));
            }
            long cStart = line.getStartMs() + c * stepDur;
            long cEnd = (c == chunkCount - 1) ? line.getEndMs() : cStart + stepDur;
            chunks.add(new LyricLine(cStart, cEnd, sb.toString()));
        }
        return chunks;
    }
}
