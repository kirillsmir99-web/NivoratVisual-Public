package rtx.nv.api.drags.components;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.drags.components.ListHudComp;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.PotionsModule;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.render.render2d.Render2D;

public final class PotionsComp extends ListHudComp {
    private static final RegistryEntry<StatusEffect>[] PREVIEW_EFFECTS = new RegistryEntry[]{
        StatusEffects.SPEED, StatusEffects.JUMP_BOOST, StatusEffects.REGENERATION, StatusEffects.FIRE_RESISTANCE
    };
    private static final int NEGATIVE_COLOR = ColorUtil.lerpColor(-3355444, -53714, 0.32f);
    private static final float EFFECT_ICON_SIZE = 8.0f;
    private static final int PULSE_THRESHOLD_TICKS = 200;
    private static final long PULSE_PERIOD_MS = 900L;

    private long previewSwitchMs;
    private int previewIndex;

    public PotionsComp() {
        super("potions", "Зелья", PotionsModule.class, 104.0f, 33.0f);
    }

    private static boolean isNegative(RegistryEntry<StatusEffect> registryEntry) {
        if (registryEntry.value().getCategory() == StatusEffectCategory.HARMFUL) {
            return true;
        }
        return registryEntry.value() == StatusEffects.SLOW_FALLING.value();
    }

    private static float smooth(float f) {
        f = Math.max(0.0f, Math.min(1.0f, f));
        return f * f * (3.0f - 2.0f * f);
    }

    @Override
    protected boolean hasHeader() {
        return false;
    }

    @Override
    protected String headerIconGlyph() {
        return null;
    }

    private static String formatLevel(int amplifier) {
        return switch (amplifier) {
            case 0 -> "I";
            case 1 -> "II";
            case 2 -> "III";
            case 3 -> "IV";
            case 4 -> "V";
            case 5 -> "VI";
            default -> String.valueOf(amplifier + 1);
        };
    }

    private static float getRemainingRatio(StatusEffectInstance inst) {
        if (inst.isInfinite()) {
            return 1.0f;
        }
        int dur = Math.max(0, inst.getDuration());
        // Approximate baseline of standard potion duration (e.g. 3600 ticks = 3 mins)
        float maxExpected = Math.max(dur, 2400.0f);
        return Math.max(0.05f, Math.min(1.0f, (float) dur / maxExpected));
    }

    @Override
    protected List<ListHudComp.Row> collectRows() {
        List<ListHudComp.Row> list = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            if (DragSystem.get().isDragModeActive()) {
                list.add(this.previewRow());
            }
            return list;
        }

        PotionsModule module = ModuleManager.get().get(PotionsModule.class);
        boolean isSorted = module == null || module.isSorted();

        List<StatusEffectInstance> effects = new ArrayList<>(mc.player.getStatusEffects());
        if (isSorted) {
            effects.sort(Comparator.comparingInt(inst -> inst.isInfinite() ? Integer.MAX_VALUE : inst.getDuration()));
        }

        for (StatusEffectInstance inst : effects) {
            if (inst == null) continue;
            RegistryEntry<StatusEffect> entry = inst.getEffectType();
            String rawName = Formatting.strip(entry.value().getName().getString());
            int amp = inst.getAmplifier();
            String level = formatLevel(amp);

            String displayName = rawName + " " + level;
            int color = isNegative(entry) ? NEGATIVE_COLOR : 0;
            float ratio = getRemainingRatio(inst);

            list.add(new ListHudComp.Row(
                entry,
                displayName,
                formatDuration(inst),
                (drawContext, x, y, size, alpha) -> drawEffectIcon(entry, x, y, size, alpha, ratio),
                expiryPulse(inst),
                color
            ));
        }

        if (list.isEmpty() && DragSystem.get().isDragModeActive()) {
            list.add(this.previewRow());
        }
        return list;
    }

    private static String formatDuration(StatusEffectInstance statusEffectInstance) {
        if (statusEffectInstance.isInfinite()) {
            return "**:**";
        }
        int totalSec = Math.max(0, statusEffectInstance.getDuration()) / 20;
        int mins = totalSec / 60;
        int secs = totalSec % 60;
        return mins + ":" + String.format(Locale.ROOT, "%02d", secs);
    }

    private static void drawEffectIcon(RegistryEntry<StatusEffect> registryEntry, float x, float y, float size, float alpha, float ratio) {
        float offset = (size - EFFECT_ICON_SIZE) * 0.5f;
        float iconX = x + offset;
        float iconY = y + offset - 1.0f;
        Render2D.effectIcon(registryEntry, iconX, iconY, EFFECT_ICON_SIZE, ColorUtil.multAlpha(-1, alpha));

        // Thin remaining duration indicator bar under the icon
        if (ratio >= 0.0f) {
            float barW = EFFECT_ICON_SIZE;
            float barH = 1.0f;
            float barX = iconX;
            float barY = iconY + EFFECT_ICON_SIZE + 1.0f;
            int barBg = ColorUtil.multAlpha(0xFF202020, alpha * 0.6f);
            Render2D.rect(barX, barY, barW, barH, 0.5f, barBg);
            int barFg = ColorUtil.multAlpha(ClientPalette.loopColor(0.0f) | 0xFF000000, alpha * 0.9f);
            Render2D.rect(barX, barY, barW * Math.max(0.08f, ratio), barH, 0.5f, barFg);
        }
    }

    private static ListHudComp.AlphaPulse expiryPulse(StatusEffectInstance statusEffectInstance) {
        if (statusEffectInstance.isInfinite() || statusEffectInstance.getDuration() > PULSE_THRESHOLD_TICKS) {
            return null;
        }
        return () -> {
            int dur = Math.max(0, statusEffectInstance.getDuration());
            if (dur > PULSE_THRESHOLD_TICKS) {
                return 1.0f;
            }
            float progress = 1.0f - (float) dur / (float) PULSE_THRESHOLD_TICKS;
            float factor = smooth(progress) * 0.8f;
            float time = (float) (System.currentTimeMillis() % PULSE_PERIOD_MS) / (float) PULSE_PERIOD_MS;
            float wave = 0.5f - 0.5f * (float) Math.cos(time * 2.0 * Math.PI);
            return 1.0f - factor * wave;
        };
    }

    private ListHudComp.Row previewRow() {
        long now = System.currentTimeMillis();
        if (now - this.previewSwitchMs >= 1000L) {
            this.previewIndex = (this.previewIndex + 1) % PREVIEW_EFFECTS.length;
            this.previewSwitchMs = now;
        }
        RegistryEntry<StatusEffect> entry = PREVIEW_EFFECTS[this.previewIndex];
        return new ListHudComp.Row("preview", "Скорость II", "1:45", (drawContext, x, y, size, alpha) -> drawEffectIcon(entry, x, y, size, alpha, 0.75f));
    }
}
