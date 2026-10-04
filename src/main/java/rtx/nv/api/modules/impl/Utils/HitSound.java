package rtx.nv.api.modules.impl.Utils;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import rtx.nv.api.config.ConfigManager;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.player.AttackEntityEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.utils.sounds.SoundManager;

public class HitSound extends Module {
    private final SeparatorSetting soundSeparator = this.register(new SeparatorSetting("Звук"));
    public final ModeSetting soundType = this.register(new ModeSetting(
            "Звук",
            "Тип звука попадания.",
            "Пузырь",
            "Пузырь",
            "Поп",
            "Импульс",
            "Кристалл",
            "Rust",
            "Колокольчик",
            "Щелчок",
            "CoD",
            "NV · Тактический",
            "NV · Стекло"
    ));
    private final NumberSetting volume = this.register(new NumberSetting("Громкость", "Громкость звука.", 1.0, 0.1, 2.0, 0.1));
    private final NumberSetting pitch = this.register(new NumberSetting("Высота тона", "Высота тона звука попадания.", 1.0, 0.5, 2.0, 0.1));

    public HitSound() {
        super("Hit Sound", "Проигрывает уникальный приятный звук при попадании.", Category.UTILS);
        this.soundType.setChangeListener(() -> {
            if (!ConfigManager.isLoading()) {
                this.playSelectedSound();
            }
        });
    }

    @EventHandler
    private void onAttack(AttackEntityEvent attackEntityEvent) {
        EntityHitResult entityHitResult;
        if (attackEntityEvent.isSynthetic() || !(attackEntityEvent.getTarget() instanceof LivingEntity)) {
            return;
        }
        HitResult hitResult = this.mc.crosshairTarget;
        if (!(hitResult instanceof EntityHitResult) || (entityHitResult = (EntityHitResult)hitResult).getEntity() != attackEntityEvent.getTarget()) {
            return;
        }
        this.playSelectedSound();
    }

    private void playSelectedSound() {
        float f = this.volume.getFloat();
        float p = this.pitch.getFloat();

        if (this.soundType.is("NV · Тактический") || this.soundType.is("Client Тактический")) {
            SoundManager.playSound(SoundManager.NV_TACTICAL, f, p);
        } else if (this.soundType.is("NV · Стекло") || this.soundType.is("Client Стекло")) {
            SoundManager.playSound(SoundManager.NV_GLASS_BUTTON, f, p);
        } else if (this.soundType.is("Пузырь")) {
            SoundManager.playSound(SoundManager.BUBBLE, f, p);
        } else if (this.soundType.is("Поп")) {
            SoundManager.playSound(SoundManager.POP, f, p);
        } else if (this.soundType.is("Импульс")) {
            SoundManager.playSound(SoundManager.IMPULSE, f, p);
        } else if (this.soundType.is("Кристалл")) {
            SoundManager.playSound(SoundManager.CRYSTAL, f, p);
        } else if (this.soundType.is("Rust")) {
            SoundManager.playSound(SoundManager.RUST, f, p);
        } else if (this.soundType.is("Колокольчик")) {
            SoundManager.playSound(SoundManager.BELL, f, p);
        } else if (this.soundType.is("Щелчок")) {
            SoundManager.playSound(SoundManager.CLICK, f, p);
        } else if (this.soundType.is("CoD")) {
            SoundManager.playSound(SoundManager.COD, f, p);
        } else {
            SoundManager.playSound(SoundManager.BUBBLE, f, p);
        }
    }
}
