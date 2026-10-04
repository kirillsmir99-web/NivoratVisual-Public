package rtx.nv.api.modules.impl.Visuals;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.utils.storage.friend.FriendUtils;

import java.util.HashMap;
import java.util.Map;

public class Wings extends Module {
    private static Wings instance;

    private static final String MODE_SELF = "Только себя";
    private static final String MODE_FRIENDS = "Себя и друзей";
    private static final String MODE_ALL = "Всех";

    private static final String DISPLAY_ALWAYS = "Всегда";
    private static final String DISPLAY_ELYTRA = "Только с элитрами";

    private static final String[] SKIN_NAMES = new String[]{
            "Ангельские",
            "Ангельские 2",
            "Ангельские на ногу",
            "Вишневые",
            "Демонические",
            "Демонические 2",
            "Зеленые",
            "Космические",
            "Космические 2",
            "Космические 3",
            "Костяные",
            "Красивые",
            "Божья коровка",
            "Варден",
            "Дракон",
            "Дракон 2",
            "Магма",
            "С ракетой",
            "Элементаль",
            "Ледяные",
            "Ледяные 2",
            "Ледяной дракон",
            "Ледяные с кольцом",
            "Любовные",
            "Огненные",
            "Песчаные",
            "Серые",
            "Снежные",
            "Стимпанк",
            "Фиолетовые",
            "Чернооранжевые",
            "Черный ангел"
    };

    private static final Map<String, String> NAME_TO_ID = new HashMap<>();
    private static final Map<String, Identifier> TEXTURE_CACHE = new HashMap<>();
    private static final Map<String, AssetInfo.TextureAsset> ASSET_CACHE = new HashMap<>();

    static {
        NAME_TO_ID.put("Ангельские", "angel");
        NAME_TO_ID.put("Ангельские 2", "angel_2");
        NAME_TO_ID.put("Ангельские на ногу", "angel_leg");
        NAME_TO_ID.put("Вишневые", "cherry");
        NAME_TO_ID.put("Демонические", "demon");
        NAME_TO_ID.put("Демонические 2", "demon_2");
        NAME_TO_ID.put("Зеленые", "green");
        NAME_TO_ID.put("Космические", "cosmic");
        NAME_TO_ID.put("Космические 2", "cosmic_2");
        NAME_TO_ID.put("Космические 3", "cosmic_3");
        NAME_TO_ID.put("Костяные", "bone");
        NAME_TO_ID.put("Красивые", "gray_crystal");
        NAME_TO_ID.put("Божья коровка", "ladybug");
        NAME_TO_ID.put("Варден", "warden");
        NAME_TO_ID.put("Дракон", "dragon");
        NAME_TO_ID.put("Дракон 2", "dragon_china");
        NAME_TO_ID.put("Магма", "magma");
        NAME_TO_ID.put("С ракетой", "rocket");
        NAME_TO_ID.put("Элементаль", "elemental");
        NAME_TO_ID.put("Ледяные", "ice");
        NAME_TO_ID.put("Ледяные 2", "ice_2");
        NAME_TO_ID.put("Ледяной дракон", "ice_dragon");
        NAME_TO_ID.put("Ледяные с кольцом", "ice_ring");
        NAME_TO_ID.put("Любовные", "love");
        NAME_TO_ID.put("Огненные", "fire");
        NAME_TO_ID.put("Песчаные", "sand");
        NAME_TO_ID.put("Серые", "gray");
        NAME_TO_ID.put("Снежные", "snow");
        NAME_TO_ID.put("Стимпанк", "steampunk");
        NAME_TO_ID.put("Фиолетовые", "crystal");
        NAME_TO_ID.put("Чернооранжевые", "orange");
        NAME_TO_ID.put("Черный ангел", "dark_angel");
    }

    private final ModeSetting wingSkin = this.register(new ModeSetting("Скин крыльев", "Выбор внешнего вида крыльев.", rtx.nv.ClientEdition.isTrial() ? "Фиолетовые" : "Ангельские", rtx.nv.ClientEdition.isTrial() ? new String[]{"Фиолетовые"} : SKIN_NAMES));
    private final ModeSetting displayMode = this.register(new ModeSetting("Режим", "Условия отображения косметических крыльев.", DISPLAY_ALWAYS, DISPLAY_ALWAYS, DISPLAY_ELYTRA));
    private final ModeSetting targetMode = this.register(new ModeSetting("Отображать для", "Выбор игроков для отрисовки крыльев.", MODE_SELF, MODE_SELF, MODE_FRIENDS, MODE_ALL));
    private final NumberSetting wingScale = this.register(new NumberSetting("Масштаб", "Размер модели крыльев.", 1.0, 0.7, 1.4, 0.05));
    private final NumberSetting animSpeed = this.register(new NumberSetting("Скорость анимации", "Скорость плавного взмаха крыльев.", 1.0, 0.2, 2.0, 0.1));
    private final BooleanSetting emissive = this.register(new BooleanSetting("Свечение", "Яркое свечение крыльев без затенения.", false));
    private final BooleanSetting hideVanillaElytra = this.register(new BooleanSetting("Скрывать обычные элитры", "Скрывать стандартную плоскую модель элитр.", true));

    public Wings() {
        super("Wings", "Косметические крылья и кастомные скины элитр с плавной анимацией.", Category.VISUALS);
        instance = this;
    }

    public static Wings getInstance() {
        Wings mod = ModuleManager.get().get(Wings.class);
        return mod != null ? mod : instance;
    }

    public boolean shouldRender(AbstractClientPlayerEntity player) {
        if (!this.isEnabled() || player == null || this.mc.player == null) {
            return false;
        }
        if (this.targetMode.is(MODE_ALL)) {
            return true;
        }
        if (this.targetMode.is(MODE_FRIENDS)) {
            return player == this.mc.player || FriendUtils.isFriend(player.getName().getString());
        }
        return player == this.mc.player;
    }

    public boolean isOnlyWithElytra() {
        return this.displayMode.is(DISPLAY_ELYTRA);
    }

    public boolean isHideVanillaElytra() {
        return this.hideVanillaElytra.getValue();
    }

    public boolean isEmissive() {
        return this.emissive.getValue();
    }

    public float getScale() {
        return this.wingScale.getFloat();
    }

    public float getAnimSpeed() {
        return this.animSpeed.getFloat();
    }

    public String getSelectedSkinId() {
        if (rtx.nv.ClientEdition.isTrial()) return "crystal";
        String skinName = this.wingSkin.getValue();
        return NAME_TO_ID.getOrDefault(skinName, "angel");
    }

    public Identifier getTexture(String skinId) {
        return TEXTURE_CACHE.computeIfAbsent(skinId, id -> Identifier.of("nv", "textures/wings/" + id + ".png"));
    }

    public AssetInfo.TextureAsset getActiveElytraAsset() {
        String skinId = getSelectedSkinId();
        return ASSET_CACHE.computeIfAbsent(skinId, id -> {
            Identifier assetId = Identifier.of("nv", "wings/" + id);
            Identifier texPath = getTexture(id);
            return new AssetInfo.TextureAssetInfo(assetId, texPath);
        });
    }
}
