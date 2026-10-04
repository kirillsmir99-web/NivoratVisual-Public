package rtx.nv.api.modules.impl.Interface;

import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;

public final class WatermarkModule extends InterfaceComponentModule {
    private static WatermarkModule instance;

    public final ModeSetting style = this.register(new ModeSetting(
        "Стиль",
        "Стиль оформления ватермарка.",
        "Капсула",
        "Капсула", "Минимализм"
    ));

    public final BooleanSetting showCombatTag = this.register(new BooleanSetting(
        "Кулдаун КТ",
        "Отображать таймер режима боя в ватермарке.",
        true
    ));

    public final BooleanSetting combatGlow = this.register(new BooleanSetting(
        "Подсветка в бою",
        "Боевая пульсирующая подсветка ватермарка при активном КТ.",
        true
    ));

    public final BooleanSetting showServer = this.register(new BooleanSetting(
        "Сервер",
        "Отображать текущий сервер или режим игры.",
        true
    ));

    public final BooleanSetting showFps = this.register(new BooleanSetting(
        "FPS",
        "Отображать показатели FPS и пинга.",
        false
    ));

    public final BooleanSetting showNick = this.register(new BooleanSetting(
        "Ник",
        "Отображать ник игрока в Watermark.",
        true
    ));

    public final BooleanSetting showTime = this.register(new BooleanSetting(
        "Время",
        "Отображать время в Watermark.",
        true
    ));

    public final ModeSetting timeFormat = this.register(new ModeSetting(
        "Формат времени",
        "Формат отображения часов: 24-часовой или 12-часовой.",
        "24 часа",
        "24 часа", "12 часов"
    )).visibleWhen(this.showTime::getValue);

    public final BooleanSetting chromaWave = this.register(new BooleanSetting(
        "Волна градиента",
        "Плавный бегущий перелив цвета по тексту.",
        true
    ));

    public final NumberSetting waveSpeed = this.register(new NumberSetting(
        "Скорость волны",
        "Скорость анимации бегущего перелива цвета.",
        1.2, 0.2, 4.0, 0.1
    )).visibleWhen(this.chromaWave::getValue);

    public final BooleanSetting hideInF3 = this.register(new BooleanSetting(
        "Скрывать в F3",
        "Автоматически скрывать ватермарк при открытии экрана отладки F3.",
        true
    ));

    public final NumberSetting scale = this.register(new NumberSetting(
        "Масштаб",
        "Масштаб отображения ватермарка.",
        1.0, 0.7, 1.5, 0.05
    ));

    public WatermarkModule() {
        super("Watermark", "Перемещаемый вотермарк клиента с fps/tps.");
        instance = this;
    }

    public static WatermarkModule getInstance() {
        return instance;
    }

    public float scale() {
        return this.scale.getFloat();
    }
}

