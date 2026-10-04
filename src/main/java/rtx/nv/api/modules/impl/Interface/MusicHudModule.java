package rtx.nv.api.modules.impl.Interface;

import org.lwjgl.glfw.GLFW;
import rtx.nv.api.modules.settings.impl.BindSetting;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.utils.key.KeyBind;

public final class MusicHudModule extends InterfaceComponentModule {
    private static MusicHudModule instance;

    public final ModeSetting source = this.register(new ModeSetting(
        "Источник музыки", "Автоматически находит играющий плеер. Если сайт не распознаётся, выберите «Браузер».",
        "Автоматически", "Автоматически", "Яндекс Музыка", "VK Музыка", "Spotify", "Браузер"
    )).visibleWhen(() -> System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win"));

    public final ModeSetting style = this.register(new ModeSetting(
        "Стиль",
        "Стиль визуального оформления карточки виджета.",
        "Стеклянный",
        "Стеклянный", "Квадратный"
    ));

    public final BooleanSetting controls = this.register(new BooleanSetting(
        "Кнопки",
        "Отображать кнопки переключения треков прямо на виджете.",
        true
    ));

    public final BooleanSetting showCover = this.register(new BooleanSetting(
        "Обложка",
        "Отображать обложку воспроизводимого трека.",
        true
    ));

    public final BooleanSetting timeline = this.register(new BooleanSetting(
        "Таймлайн",
        "Отображать полосу воспроизведения и время звучания.",
        true
    ));

    public final BooleanSetting equalizer = this.register(new BooleanSetting(
        "Эквалайзер",
        "Полосы реагируют на громкость и частоты общего звука компьютера в Windows.",
        true
    ));

    public final BooleanSetting glow = this.register(new BooleanSetting(
        "Свечение",
        "Мягкое неоновое свечение в тон обложки или акцента.",
        true
    ));

    public final NumberSetting glowIntensity = this.register(new NumberSetting(
        "Сила свечения",
        "Интенсивность внешнего неонового ореола.",
        1.2, 0.5, 3.0, 0.1
    )).visibleWhen(this.glow::getValue);

    public final BooleanSetting autoHide = this.register(new BooleanSetting(
        "Автоскрытие",
        "Скрывать карточку после заданной задержки. На паузе она остаётся доступной.",
        false
    ));

    public final ModeSetting autoHideMode = this.register(new ModeSetting(
        "Режим скрытия",
        "Условие срабатывания автоскрытия виджета.",
        "После смены трека",
        "После смены трека", "При остановке"
    )).visibleWhen(this.autoHide::getValue);

    public final NumberSetting hideDelay = this.register(new NumberSetting(
        "Задержка скрытия",
        "Секунды ожидания перед скрытием виджета.",
        5.0, 1.0, 30.0, 1.0
    )).visibleWhen(this.autoHide::getValue);

    public final NumberSetting scale = this.register(new NumberSetting(
        "Масштаб",
        "Масштаб отображения музыкального виджета.",
        1.0, 0.7, 1.5, 0.05
    ));

    public final SeparatorSetting visualSeparator = this.register(new SeparatorSetting("Всплывающие визуалы"));

    public final BooleanSetting screenPopup = this.register(new BooleanSetting(
        "Баннер на экране",
        "Всплывающая плашка перед экраном при переключении трека.",
        true
    ));

    public final ModeSetting popupPosition = this.register(new ModeSetting(
        "Позиция баннера",
        "Расположение всплывающего уведомления о треке на экране.",
        "Сверху",
        "Сверху", "Над хотбаром"
    )).visibleWhen(this.screenPopup::getValue);

    public final NumberSetting popupDuration = this.register(new NumberSetting(
        "Длительность баннера",
        "Время показа всплывающего баннера на экране в секундах.",
        4.5, 2.0, 10.0, 0.5
    )).visibleWhen(this.screenPopup::getValue);

    public final BooleanSetting skyWords = this.register(new BooleanSetting(
        "Слова в небе",
        "Парящие строки и слова трека в небе перед игроком.",
        true
    ));

    public final BooleanSetting wordsBackground = this.register(new BooleanSetting(
        "Фон слов", "Плашка позади текста песни. Выключите для текста без фона.", true
    )).visibleWhen(this.skyWords::getValue);

    public final ModeSetting wordsMode = this.register(new ModeSetting(
        "Где показывать слова", "Выберите положение текста текущей песни.",
        "На блоке · 3D", "На блоке · 3D", "Перед игроком · 3D", "На экране · 2D"
    )).visibleWhen(this.skyWords::getValue);

    public final ModeSetting wordsAnimation = this.register(new ModeSetting(
        "Появление слов", "Плавный подъём букв с подсветкой по времени песни.",
        "Плавный подъём", "Плавный подъём", "Караоке", "Без движения"
    )).visibleWhen(this.skyWords::getValue);

    public final NumberSetting wordsScale = this.register(new NumberSetting(
        "Размер слов", "Размер текста песни.", 1.0, 0.5, 2.0, 0.05
    )).visibleWhen(this.skyWords::getValue);

    public final NumberSetting wordsOffset = this.register(new NumberSetting(
        "Синхронизация · мс", "Поправка времени, если слова опережают музыку или отстают.", 0, -3000, 3000, 50
    )).visibleWhen(this.skyWords::getValue);

    public final NumberSetting wordsDistance = this.register(new NumberSetting(
        "Дистанция в мире",
        "Минимальная дистанция появления. Длинная строка отодвигается, чтобы не закрывать обзор.",
        6.5, 3.0, 16.0, 0.5
    )).visibleWhen(() -> this.skyWords.getValue() && !this.wordsMode.is("На экране · 2D"));

    public final NumberSetting wordsSpread = this.register(new NumberSetting(
        "Разброс слов",
        "Разброс слов по сторонам от направления движения. Центр обзора остаётся свободным.",
        0.35, 0.1, 0.8, 0.05
    )).visibleWhen(() -> this.skyWords.getValue() && this.wordsMode.is("Перед игроком · 3D"));

    public final SeparatorSetting keysSeparator = this.register(new SeparatorSetting("Клавиши управления"));

    public final BindSetting dialKey = this.register(new BindSetting(
        "Кнопка меню",
        "Клавиша открытия радиального переключателя треков.",
        KeyBind.keyboard(GLFW.GLFW_KEY_R)
    ));

    public final BindSetting playPauseKey = this.register(new BindSetting(
        "Пауза / Плей",
        "Быстрая клавиша приостановки и возобновления."
    ));

    public final BindSetting nextKey = this.register(new BindSetting(
        "Следующий трек",
        "Быстрая клавиша перехода к следующему треку."
    ));

    public final BindSetting prevKey = this.register(new BindSetting(
        "Предыдущий трек",
        "Быстрая клавиша перехода к предыдущему треку."
    ));

    public MusicHudModule() {
        super("Music HUD", "Виджет плеера с обложкой и управлением треками.", rtx.nv.api.modules.Category.MEDIA);
        instance = this;
    }

    public static MusicHudModule getInstance() {
        return instance;
    }

    public float scale() {
        return this.scale.getFloat();
    }
}
