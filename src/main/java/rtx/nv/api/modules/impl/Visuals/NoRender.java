package rtx.nv.api.modules.impl.Visuals;

import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.BooleanSetting;

public class NoRender extends Module {
    public static final String FIRE = "Огонь";
    public static final String ENTITY_FIRE = "Огонь на сущностях";
    public static final String CAMERA_SHAKE = "Тряска камеры";
    public static final String VIEW_BOBBING = "Покачивание камеры";
    public static final String FOV_DYNAMIC = "Динамика поля зрения";
    public static final String SCOREBOARD = "Таблица счёта";
    public static final String BOSS_BAR = "Полоса босса";
    public static final String GLOW = "Свечение";

    private static NoRender instance;

    public final BooleanSetting fire = this.register(new BooleanSetting("Огонь на экране", "Скрывать текстуру огня на экране игрока.", true));
    public final BooleanSetting entityFire = this.register(new BooleanSetting("Огонь на существах", "Скрывать анимацию горения на моделях существ.", true));
    public final BooleanSetting cameraShake = this.register(new BooleanSetting("Тряска камеры", "Отключать тряску экрана от урона и взрывов.", true));
    public final BooleanSetting viewBobbing = this.register(new BooleanSetting("Покачивание камеры", "Отключать покачивание камеры при ходьбе.", false));
    public final BooleanSetting fovDynamic = this.register(new BooleanSetting("Динамика FOV", "Отключать динамическое изменение поля зрения.", false));
    public final BooleanSetting scoreboard = this.register(new BooleanSetting("Таблица счёта", "Скрывать боковую панель скорборда.", false));
    public final BooleanSetting bossBar = this.register(new BooleanSetting("Полоса босса", "Скрывать полосу здоровья босса сверху экрана.", false));
    public final BooleanSetting glow = this.register(new BooleanSetting("Свечение", "Отключать эффект свечения вокруг существ.", false));

    public NoRender() {
        super("No Render", "Скрывает выбранные визуальные эффекты.", Category.VISUALS);
        instance = this;
    }

    public static NoRender getInstance() {
        return instance;
    }

    public static boolean isActive(String string) {
        if (instance == null || !instance.isEnabled() || string == null) {
            return false;
        }
        return switch (string) {
            case FIRE -> instance.fire.getValue();
            case ENTITY_FIRE -> instance.entityFire.getValue();
            case CAMERA_SHAKE -> instance.cameraShake.getValue();
            case VIEW_BOBBING -> instance.viewBobbing.getValue();
            case FOV_DYNAMIC -> instance.fovDynamic.getValue();
            case SCOREBOARD -> instance.scoreboard.getValue();
            case BOSS_BAR -> instance.bossBar.getValue();
            case GLOW -> instance.glow.getValue();
            default -> false;
        };
    }
}
