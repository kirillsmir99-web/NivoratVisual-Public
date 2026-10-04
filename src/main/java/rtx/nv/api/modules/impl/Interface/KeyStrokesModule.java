package rtx.nv.api.modules.impl.Interface;

import java.util.ArrayDeque;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.input.MouseButtonEvent;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;

public final class KeyStrokesModule extends InterfaceComponentModule {
    private static final long CPS_WINDOW_MS = 1000L;

    public final ModeSetting preset = this.register(new ModeSetting(
        "Пресет",
        "Готовый шаблон оформления клавиш.",
        "Квадратный",
        "Квадратный", "Стеклянный", "Скругленный", "Компактный", "Кастомный"
    ));

    public final ModeSetting style = this.register(new ModeSetting(
        "Стиль",
        "Стиль оформления клавиш: Нестеклянный или Стеклянный.",
        "Нестеклянный",
        "Нестеклянный", "Стеклянный"
    ));

    public final NumberSetting cornerRadius = this.register(new NumberSetting(
        "Скругление",
        "Радиус скругления углов кнопок.",
        0.0, 0.0, 12.0, 0.5
    ));

    public final BooleanSetting glow = this.register(new BooleanSetting(
        "Свечение",
        "Неоновое свечение клавиш при нажатии.",
        true
    ));

    public final NumberSetting glowIntensity = this.register(new NumberSetting(
        "Сила свечения",
        "Яркость и радиус свечения нажатых клавиш.",
        1.0, 0.2, 2.5, 0.1
    )).visibleWhen(this.glow::getValue);

    public final NumberSetting keySize = this.register(new NumberSetting(
        "Размер кнопок",
        "Ширина и высота основных кнопок движения.",
        23.0, 18.0, 30.0, 1.0
    ));

    public final BooleanSetting showMouse = this.register(new BooleanSetting(
        "Кнопки мыши",
        "Отображать кнопки ЛКМ и ПКМ.",
        true
    ));

    public final BooleanSetting showCps = this.register(new BooleanSetting(
        "CPS",
        "Отображать количество кликов в секунду.",
        true
    )).visibleWhen(this.showMouse::getValue);

    public final BooleanSetting showSpace = this.register(new BooleanSetting(
        "Пробел",
        "Отображать полоску клавиши прыжка.",
        true
    ));

    private final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
    private final ArrayDeque<Long> rightClicks = new ArrayDeque<>();

    public KeyStrokesModule() {
        super("KeyStrokes", "Нажатия клавиш движения и кликов: перетаскиваемый блок с кастомным оформлением.");
    }

    public int leftCps() {
        return prune(this.leftClicks);
    }

    public int rightCps() {
        return prune(this.rightClicks);
    }

    private static int prune(ArrayDeque<Long> deque) {
        long now = System.currentTimeMillis();
        long window = now - CPS_WINDOW_MS;
        while (!deque.isEmpty() && deque.peekFirst() < window) {
            deque.pollFirst();
        }
        return deque.size();
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != MouseButtonEvent.Action.PRESS || this.mc.currentScreen != null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (event.button == 0) {
            this.leftClicks.addLast(now);
        } else if (event.button == 1) {
            this.rightClicks.addLast(now);
        }
    }
}
