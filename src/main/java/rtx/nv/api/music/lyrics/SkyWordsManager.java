package rtx.nv.api.music.lyrics;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.MusicHudModule;
import rtx.nv.api.music.MusicManager;
import rtx.nv.api.music.TrackState;

public final class SkyWordsManager {
    private static final SkyWordsManager INSTANCE = new SkyWordsManager();
    private static final int MAX_BUBBLES = 1;

    private final List<SkyWordBubble> activeBubbles = new CopyOnWriteArrayList<>();
    private LyricLine lastSpawnedChunk = null;
    private long lastSpawnTime = 0L;

    public static SkyWordsManager get() {
        return INSTANCE;
    }

    public void init() {
        this.activeBubbles.clear();
        this.lastSpawnedChunk = null;
    }

    public List<SkyWordBubble> getActiveBubbles() {
        return Collections.unmodifiableList(this.activeBubbles);
    }

    public void onTick() {
        long now = System.currentTimeMillis();
        this.activeBubbles.removeIf(b -> b.isExpired(now));

        MusicHudModule mod = ModuleManager.get().get(MusicHudModule.class);
        if (mod == null || !mod.isEnabled() || !mod.skyWords.getValue()) {
            this.activeBubbles.clear();
            this.lastSpawnedChunk = null;
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) {
            return;
        }

        TrackState state = MusicManager.get().getClient().getState();
        if (!state.isActive() || !state.isPlaying()) {
            this.activeBubbles.clear();
            this.lastSpawnedChunk = null;
            return;
        }

        LyricLine currentLine = LyricsManager.get().getCurrentLine(state);
        if (currentLine == null || currentLine.getText().isEmpty()) {
            return;
        }

        List<LyricLine> chunks = LyricsManager.get().getChunks(currentLine, 2);
        long pos = state.currentPositionMs();
        LyricLine targetChunk = null;
        for (LyricLine chunk : chunks) {
            if (pos >= chunk.getStartMs() && pos < chunk.getEndMs()) {
                targetChunk = chunk;
                break;
            }
        }

        if (targetChunk == null) {
            return;
        }

        if (targetChunk == this.lastSpawnedChunk) {
            return;
        }

        this.lastSpawnedChunk = targetChunk;
        this.lastSpawnTime = now;
        this.spawnBubble(mc, mod, targetChunk);
    }

    private boolean sideToggle = false;

    private void spawnBubble(MinecraftClient mc, MusicHudModule mod, LyricLine chunk) {
        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null || mc.player == null) {
            return;
        }

        Vec3d camPos = camera.getCameraPos();
        float yaw = camera.getYaw();
        double yawRad = Math.toRadians(yaw);

        // Horizontal forward vector (ignoring pitch so words stay in the sky and never on the grass)
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);

        // Horizontal right vector
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        float dist = mod.wordsDistance.getFloat();
        Vec3d vel = mc.player.getVelocity();
        double horizontalSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        float sprintLead = (float) Math.min(3.5, horizontalSpeed * 3.8);
        float spawnDist = dist + sprintLead;

        // Alternate side: slightly left or right of view
        this.sideToggle = !this.sideToggle;
        float sideSign = this.sideToggle ? 1.0f : -1.0f;
        int hash = Math.abs(chunk.getText().hashCode());
        float sideSpread = 1.8f + (hash % 3) * 0.4f;

        // Height: ALWAYS in the sky/air above eye level (camPos.y + 1.8 to 2.4 blocks)
        double spawnY = camPos.y + 1.8 + (hash % 3) * 0.3;
        double spawnX = camPos.x + fwdX * spawnDist + rightX * (sideSign * sideSpread);
        double spawnZ = camPos.z + fwdZ * spawnDist + rightZ * (sideSign * sideSpread);

        Vec3d spawnPos = new Vec3d(spawnX, spawnY, spawnZ);
        long chunkDuration = Math.max(2800L, Math.min(5000L, chunk.getEndMs() - chunk.getStartMs() + 1200L));
        SkyWordBubble bubble = new SkyWordBubble(spawnPos, chunk.getText(), chunkDuration);

        this.activeBubbles.add(bubble);
        while (this.activeBubbles.size() > 2) {
            this.activeBubbles.remove(0);
        }
    }
}
