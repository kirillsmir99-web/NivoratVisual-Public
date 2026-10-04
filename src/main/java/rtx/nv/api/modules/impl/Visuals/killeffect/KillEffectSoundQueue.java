package rtx.nv.api.modules.impl.Visuals.killeffect;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.sound.SoundEvent;
import rtx.nv.utils.sounds.SoundManager;

public final class KillEffectSoundQueue {
    private final List<PendingSound> pendingSounds = new ArrayList<PendingSound>();

    public void clear() {
        this.pendingSounds.clear();
    }

    public void schedule(SoundEvent soundEvent, float f, long l) {
        if (soundEvent == null) {
            return;
        }
        if (l <= 0L) {
            SoundManager.playSoundDirect(soundEvent, f, 1.0f);
            return;
        }
        this.pendingSounds.add(new PendingSound(soundEvent, f, System.currentTimeMillis() + l));
    }

    public void tick() {
        Iterator<PendingSound> iterator = this.pendingSounds.iterator();
        long l = System.currentTimeMillis();
        while (iterator.hasNext()) {
            PendingSound pendingSound = iterator.next();
            if (l < pendingSound.playAt) continue;
            SoundManager.playSoundDirect(pendingSound.sound, pendingSound.volume, 1.0f);
            iterator.remove();
        }
    }

    public static record PendingSound(SoundEvent sound, float volume, long playAt) {}
}

