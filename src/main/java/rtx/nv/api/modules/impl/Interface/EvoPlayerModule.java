package rtx.nv.api.modules.impl.Interface;

import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.music.lyrics.MusicSubtitlesConfig;

public final class EvoPlayerModule extends Module {

    public final ModeSetting cardStyle = this.register(new ModeSetting(
        "Вид карточки",
        "Компактный остров или развернутый плеер управления.",
        "Остров",
        "Остров", "Плеер"
    ));

    public final BooleanSetting watermarkLyrics = this.register(new BooleanSetting(
        "Караоке в карточке",
        "Синхронизированные строки караоке с плавной подсветкой слов внутри виджета.",
        true
    ));

    public final BooleanSetting accentWash = this.register(new BooleanSetting(
        "Акцентный ореол",
        "Мягкая неоновая подсветка слева в тон играющего трека.",
        true
    ));

    public final NumberSetting scale = this.register(new NumberSetting(
        "Масштаб",
        "Размер карточки плеера на экране.",
        1.0, 0.7, 1.5, 0.05
    ));

    public final SeparatorSetting worldSeparator = this.register(new SeparatorSetting("Субтитры в мире · WorldLyrics"));

    public final BooleanSetting worldLyrics = this.register(new BooleanSetting(
        "Слова в мире",
        "Парящий 3D текст играющей песни прямо в игровом мире.",
        true
    ));

    public final NumberSetting distance = this.register(new NumberSetting(
        "Дистанция",
        "Дальность от игрока, на которой возникают строки.",
        7.0, 3.0, 14.0, 0.5
    )).visibleWhen(this.worldLyrics::getValue);

    public final NumberSetting size = this.register(new NumberSetting(
        "Размер",
        "Масштаб букв парящих строк песни.",
        1.0, 0.5, 2.0, 0.05
    )).visibleWhen(this.worldLyrics::getValue);

    public final NumberSetting scatter = this.register(new NumberSetting(
        "Разброс",
        "Ширина распределения строк вокруг направления взгляда.",
        0.7, 0.0, 1.0, 0.05
    )).visibleWhen(this.worldLyrics::getValue);

    public final NumberSetting offset = this.register(new NumberSetting(
        "Sync Смещение · мс",
        "Смещение времени, если слова спешат или отстают от музыки.",
        0, -1500, 1500, 50
    )).visibleWhen(this.worldLyrics::getValue);

    public final BooleanSetting roadside = this.register(new BooleanSetting(
        "Вдоль дороги",
        "При беге слова появляются впереди по сторонам от пути движения.",
        true
    )).visibleWhen(this.worldLyrics::getValue);

    public final BooleanSetting glow = this.register(new BooleanSetting(
        "Свечение",
        "Мягкое неоновое свечение вокруг букв парящего текста.",
        true
    )).visibleWhen(this.worldLyrics::getValue);

    public final NumberSetting glowStrength = this.register(new NumberSetting(
        "Свечение Сила",
        "Яркость ореола вокруг парящих слов.",
        0.85, 0.1, 1.0, 0.05
    )).visibleWhen(() -> this.worldLyrics.getValue() && this.glow.getValue());

    public final BooleanSetting shadow = this.register(new BooleanSetting(
        "Тень",
        "Мягкая тёмная подложка для надёжной читаемости на светлом небе.",
        true
    )).visibleWhen(this.worldLyrics::getValue);

    public final BooleanSetting recognize = this.register(new BooleanSetting(
        "Распознавание речи",
        "Определение текста трека через локальный движок речи при отсутствии онлайн слов.",
        false
    )).visibleWhen(this.worldLyrics::getValue);

    public final ModeSetting model = this.register(new ModeSetting(
        "Модель речи",
        "Точность распознавания речи Whisper.",
        "Base",
        "Base", "Small"
    )).visibleWhen(() -> this.worldLyrics.getValue() && this.recognize.getValue());

    public final ModeSetting language = this.register(new ModeSetting(
        "Язык речи",
        "Язык для распознавания голоса в песне.",
        "Auto",
        "Auto", "Russian", "English"
    )).visibleWhen(() -> this.worldLyrics.getValue() && this.recognize.getValue());

    public EvoPlayerModule() {
        super("Evo Player", "Медиаплеер в стиле динамического острова с караоке и поддержкой WorldLyrics.", Category.MEDIA);
    }

    public float getScale() {
        return this.scale.getFloat();
    }

    @Override
    public void onEnable() {
        this.syncWorldLyricsConfig();
    }

    @Override
    public void onDisable() {
        MusicSubtitlesConfig.INSTANCE.sync();
    }

    public void syncWorldLyricsConfig() {
        if (!this.isEnabled() || !this.worldLyrics.getValue()) {
            return;
        }
        MusicSubtitlesConfig cfg = MusicSubtitlesConfig.INSTANCE;
        cfg.subtitlesEnabled = true;
        cfg.subtitlesMode = "WORLD_3D";
        cfg.subtitlesScale = this.size.getFloat();
        cfg.subtitlesDistance = this.distance.getFloat();
        cfg.subtitlesOffsetMs = (int) this.offset.getFloat();
        cfg.sprintWordsEnabled = this.roadside.getValue();
        cfg.sprintWordsSpread = this.scatter.getFloat();
        cfg.sprintWordsDistance = this.distance.getFloat();
        cfg.subtitlesGlow = this.glow.getValue();
        cfg.subtitlesGlowStrength = this.glowStrength.getFloat();
        cfg.subtitlesTextShadow = this.shadow.getValue();
        cfg.subtitlesBgEnabled = this.shadow.getValue();
        cfg.speechRecognitionEnabled = this.recognize.getValue();
        cfg.subtitlesAnimation = "KARAOKE_BOUNCE";
    }
}
