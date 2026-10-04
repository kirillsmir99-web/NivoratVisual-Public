package rtx.nv.test;

import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.MusicHudModule;
import rtx.nv.api.music.TrackState;
import rtx.nv.api.music.lyrics.*;

final class LyricsMotionChecks {
    static void verify(MinecraftClient client) {
        var parsed = LrcParser.parse("[offset:100]\n[00:01.00]<00:01.00>Плавные <00:02.00>слова<00:04.00>\n[00:05.00]Другая строка\n[00:08.00]\n[00:10.00][00:15.00]Повтор");
        var first = parsed.getFirst();
        if (first.startMs != 1100 || first.endMs != 5100 || first.words.size() != 2
            || first.words.getFirst().endMs() != 2100 || first.words.get(1).endMs() != 4100)
            throw new AssertionError("LRC provider timestamps, offsets or word bounds changed");
        if (!first.isCharActive(0, 1500) || first.isCharActive(0, 3000) || !first.isCharActive(8, 3000))
            throw new AssertionError("Karaoke highlights the wrong word");
        if (parsed.size() != 4 || parsed.get(2).startMs != 10100 || parsed.get(3).startMs != 15100)
            throw new AssertionError("Repeated LRC timestamps were lost");
        var layout = first.getSubLines(client.textRenderer, 260);
        if (layout != first.getSubLines(client.textRenderer, 260)) throw new AssertionError("Lyrics layout rebuilt every frame");
        var longWord = new LyricLine(0, 20000, "Непрерывноедлинноеслово😀продолжение");
        for (var row : longWord.getSubLines(client.textRenderer, 50))
            if (row.width() > 50 || Character.isLowSurrogate(longWord.text.charAt(row.charStart()))) throw new AssertionError("Long word layout overflow");

        var manager = LyricsManager.get();
        try {
            var cacheField = LyricsManager.class.getDeclaredField("lyricsCache"); cacheField.setAccessible(true);
            @SuppressWarnings("unchecked") var cache = (Map<String, List<LyricLine>>)cacheField.get(manager);
            var score = LyricsManager.class.getDeclaredMethod("calculateMatchScore", com.google.gson.JsonObject.class, long.class, String.class, String.class);
            score.setAccessible(true);
            var reversed = new com.google.gson.JsonObject();
            reversed.addProperty("trackName", "madk1d, VILLIAN"); reversed.addProperty("artistName", "8 миля"); reversed.addProperty("duration", 115);
            if ((int)score.invoke(manager, reversed, 115L, "8 миля", "madk1d, VILLIAN") < 900)
                throw new AssertionError("Strong reversed provider metadata was rejected");
            reversed.addProperty("duration", 95);
            if ((int)score.invoke(manager, reversed, 115L, "8 миля", "madk1d, VILLIAN") != 0)
                throw new AssertionError("Wrong-duration song edit accepted as synced lyrics");
            var one = new LyricsConsensusEngine.Candidate("LRCLIB_SYNCED", List.of(first), "", true);
            var duplicate = new LyricsConsensusEngine.Candidate("LRCLIB_SYNCED", List.of(first), "", true);
            LyricsConsensusEngine.selectBest(List.of(one, duplicate), 0);
            double originalScore = one.score;
            LyricsConsensusEngine.selectBest(List.of(one, duplicate), 0);
            if (one.hasCrossConsensus || one.score != originalScore) throw new AssertionError("Duplicate provider manufactured consensus");
            var line = new LyricLine(0, 90000, "NV сохраняет строку при движении");
            cache.put("NV___Motion fixture", List.of(line));
            var module = ModuleManager.get().get(MusicHudModule.class);
            module.wordsMode.setSelected("Перед игроком · 3D"); module.wordsAnimation.setSelected("Плавный подъём");
            MusicSubtitlesConfig.INSTANCE.sync();
            var origin = client.player.getEntityPos();
            var state = new TrackState(true, "Motion fixture", "NV", "", "playing", 10000, 90000, null, System.currentTimeMillis(), 1);
            manager.update(state, 854, 480);
            var bubble = manager.getActiveBubbles().getLast();
            Vec3d fixedPosition = bubble.worldPos;
            for (int tick = 0; tick < 80; tick++) {
                client.player.setSprinting((tick & 1) == 0);
                client.player.setVelocity(.28, 0, .04);
                client.player.setPosition(origin.add(tick * .25, 0, 0));
                manager.update(state, 854, 480);
                if (!manager.getActiveBubbles().contains(bubble) || bubble.isFastPaced)
                    throw new AssertionError("Movement recreated the subtitle at tick " + tick);
                client.player.setYaw(tick * 3);
                if (!bubble.worldPos.equals(fixedPosition))
                    throw new AssertionError("World caption followed movement or camera rotation");
            }
            var paused = new TrackState(true, "Motion fixture", "NV", "", "paused", 10000, 90000, null, System.currentTimeMillis(), 1);
            manager.update(paused, 854, 480);
            if (!manager.getActiveBubbles().contains(bubble) || bubble.isExpired(10000, bubble.spawnSystemTimeMs + 60000))
                throw new AssertionError("Pause restarted or expired the caption");
            if (bubble.getLetterEntryOffset(0, bubble.spawnSystemTimeMs + 500) != 0)
                throw new AssertionError("Letter animation never settled");
            client.player.setSprinting(false); client.player.setVelocity(Vec3d.ZERO); client.player.setPosition(origin); client.player.setYaw(0);
            manager.update(TrackState.INACTIVE, 854, 480);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        rtx.nv.NV.LOGGER.info("[NV-TEST] Lyrics motion: 80 movement/sprint updates retain one fixed world anchor independent of camera; pause, timed words, LRC offset/repeats and cached Unicode layout passed");
    }
}
