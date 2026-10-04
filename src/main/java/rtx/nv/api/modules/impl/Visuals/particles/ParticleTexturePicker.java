package rtx.nv.api.modules.impl.Visuals.particles;

import java.util.Random;
import net.minecraft.util.Identifier;
import rtx.nv.api.modules.settings.impl.SelectSetting;

public final class ParticleTexturePicker {
    public static final String SHOW_ALL = "Отображать всё";

    public static final String[] WORLD_MODE_OPTIONS = new String[]{
        SHOW_ALL, "Точка", "Звезда", "Сердце", "Треугольник", "Искры", "Световые штрихи", "Орбитальные точки"
    };

    public static final String[] HIT_MODE_OPTIONS = new String[]{
        SHOW_ALL, "Точка", "Звезда", "Сердце", "Искры", "Дуги", "Лепестки света"
    };

    private ParticleTexturePicker() {}

    public static Identifier pickWorld(SelectSetting selectSetting, Random random) {
        return pick(selectSetting, random, ParticleConstants.WORLD_TEXTURES);
    }

    public static Identifier pickHit(SelectSetting selectSetting, Random random) {
        return pick(selectSetting, random, ParticleConstants.HIT_TEXTURES);
    }

    public static Identifier pick(SelectSetting selectSetting, Random random, ParticleTexture[] pool) {
        for (ParticleTexture pt : pool) {
            if (selectSetting.is(pt.mode())) {
                return pt.id();
            }
        }
        return pool[random.nextInt(pool.length)].id();
    }

    public static Identifier pick(SelectSetting selectSetting, Random random) {
        return pickWorld(selectSetting, random);
    }

    public static String[] modeOptions() {
        return WORLD_MODE_OPTIONS.clone();
    }

    public static String[] worldModeOptions() {
        return WORLD_MODE_OPTIONS.clone();
    }

    public static String[] hitModeOptions() {
        return HIT_MODE_OPTIONS.clone();
    }
}
