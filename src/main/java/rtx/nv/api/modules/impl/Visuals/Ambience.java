package rtx.nv.api.modules.impl.Visuals;
import rtx.nv.api.events.EventHandler;
import java.awt.Color;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.world.biome.Biome;
import rtx.nv.api.events.impl.network.PacketEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;

public final class Ambience
extends Module {
    private final SeparatorSetting timeSeparator = this.register(new SeparatorSetting("\u0412\u0440\u0435\u043c\u044f"));
    private final ModeSetting mode = this.register(new ModeSetting("\u0420\u0435\u0436\u0438\u043c", "\u0420\u0435\u0436\u0438\u043c \u0432\u0440\u0435\u043c\u0435\u043d\u0438 \u0432 \u043c\u0438\u0440\u0435.", "\u0414\u0435\u043d\u044c", "\u0414\u0435\u043d\u044c", "\u041f\u043e\u043b\u0434\u0435\u043d\u044c", "\u041d\u043e\u0447\u044c", "\u041f\u043e\u043b\u043d\u043e\u0447\u044c", "\u0421\u0432\u043e\u0439"));
    private final NumberSetting customTime = this.register(new NumberSetting("\u0412\u0440\u0435\u043c\u044f", "\u0421\u0432\u043e\u0451 \u0432\u0440\u0435\u043c\u044f \u0441\u0443\u0442\u043e\u043a.", 1000.0, 0.0, 24000.0, 100.0).visibleWhen(() -> this.mode.is("\u0421\u0432\u043e\u0439")));
    private final SeparatorSetting weatherSeparator = this.register(new SeparatorSetting("\u041f\u043e\u0433\u043e\u0434\u0430"));
    private final ModeSetting weather = this.register(new ModeSetting("\u041f\u043e\u0433\u043e\u0434\u0430", "\u041f\u043e\u0434\u043c\u0435\u043d\u0430 \u043f\u043e\u0433\u043e\u0434\u044b \u043d\u0430 \u043a\u043b\u0438\u0435\u043d\u0442\u0435.", "\u042f\u0441\u043d\u043e", "\u042f\u0441\u043d\u043e", "\u0414\u043e\u0436\u0434\u044c", "\u0413\u0440\u043e\u0437\u0430", "\u0421\u043d\u0435\u0433"));
    private final SeparatorSetting lightSeparator = this.register(new SeparatorSetting("\u041e\u0441\u0432\u0435\u0449\u0435\u043d\u0438\u0435"));
    private final NumberSetting saturation = this.register(new NumberSetting("\u041d\u0430\u0441\u044b\u0449\u0435\u043d\u043d\u043e\u0441\u0442\u044c", "\u0421\u043c\u0435\u0449\u0435\u043d\u0438\u0435 \u043c\u043d\u043e\u0436\u0438\u0442\u0435\u043b\u044f \u043d\u0430\u0441\u044b\u0449\u0435\u043d\u043d\u043e\u0441\u0442\u0438 \u043c\u0438\u0440\u0430.", 0.5, -1.0, 1.0, 0.05));
    private final NumberSetting brightness = this.register(new NumberSetting("\u042f\u0440\u043a\u043e\u0441\u0442\u044c", "\u0421\u043c\u0435\u0449\u0435\u043d\u0438\u0435 \u044f\u0440\u043a\u043e\u0441\u0442\u0438 \u043c\u0438\u0440\u0430.", -0.1, -1.0, 1.0, 0.05));
        private final SeparatorSetting skySeparator = this.register(new SeparatorSetting("Своё небо"));
    private final BooleanSetting customSky = this.register(new BooleanSetting("Своё небо", "Полная замена неба кастомным шейдером.", false));
    private final ModeSetting skyType = this.register(new ModeSetting("Тип неба", "Какое небо рисовать.", "Северное сияние", "Северное сияние", "Чёрная дыра", "Туманность", "Звездопад", "Бездна").visibleWhen(this.customSky::getValue));
    private final ModeSetting skyQuality = this.register(new ModeSetting("Качество неба", "Детализация и нагрузка на видеокарту.", "Сбалансированный", "Экономный", "Сбалансированный", "Высокий").visibleWhen(this.customSky::getValue));
    private final SliderSetting skyDensity = this.register(new SliderSetting("Плотность", "Плотность космических облаков и звезд.").range(0.5f, 2.5f).increment(0.1f).setValue(1.0f).visible(this.customSky::getValue));
    private final SliderSetting skyLayers = this.register(new SliderSetting("Слои", "Количество слоев процедурного шума.").range(1.0f, 6.0f).increment(1.0f).setValue(4.0f).visible(this.customSky::getValue));
    private final SliderSetting skyBrightnessLevel = this.register(new SliderSetting("Яркость неба", "Насколько ярко светится кастомное небо.").range(10, 200).increment(1).setValue(100.0f).visible(this.customSky::getValue));
    private final ModeSetting skyColorMode = this.register(new ModeSetting("Цвет неба", "Откуда брать цвет неба.", "Тема", "Тема", "Свой").visibleWhen(this.customSky::getValue));
    private final BooleanSetting skySecondColor = this.register(new BooleanSetting("Второй цвет неба", "Градиент из двух цветов.", false).visibleWhen(() -> this.customSky.getValue() && !this.skyColorMode.is("Тема")));
    private final ColorSetting skyColor = this.register(new ColorSetting("Цвет неба 1", "Основной цвет неба.", new Color(90, 255, 150, 255)).visibleWhen(() -> this.customSky.getValue() && this.skyColorMode.is("Свой")));
    private final ColorSetting skyColor2 = this.register(new ColorSetting("Цвет неба 2", "Второй цвет неба.", new Color(120, 90, 255, 255)).visibleWhen(() -> this.customSky.getValue() && this.skyColorMode.is("Свой") && this.skySecondColor.getValue()));

    private final SeparatorSetting windSeparator = this.register(new SeparatorSetting("Ветер"));
    private final BooleanSetting wind = this.register(new BooleanSetting("Покачивание растений", "Плавно качает траву и листву ветром, как в шейдерах.", false) {
        @Override
        public String getDescription() {
            if (rtx.nv.utils.render.post.handsflame.IrisShaderCompat.isShaderPackInUse()) {
                return "Плавно качает траву и листву ветром. Недоступно при активном шейдер паке Iris (используйте встроенное покачивание шейдера).";
            }
            return super.getDescription();
        }
    });
    private final SliderSetting windGrassStrength = this.register(new SliderSetting("Сила травы", "Амплитуда покачивания травы и растений.").range(0, 100).increment(1).setValue(60.0f).visible(this.wind::getValue));
    private final SliderSetting windLeavesStrength = this.register(new SliderSetting("Сила листвы", "Амплитуда покачивания листвы и лиан.").range(0, 100).increment(1).setValue(45.0f).visible(this.wind::getValue));
    private final SliderSetting windSpeed = this.register(new SliderSetting("Скорость ветра", "Скорость покачивания растительности.").range(0, 100).increment(1).setValue(50.0f).visible(this.wind::getValue));
    private final BooleanSetting windGusts = this.register(new BooleanSetting("Порывы ветра", "Волны порывов периодически усиливают покачивание.", true).visibleWhen(this.wind::getValue));
    private boolean weatherOverrideActive;

    public int skyResolutionScale(int type) {
        if (skyQuality.is("Экономный")) return 4;
        if (skyQuality.is("Высокий")) return type == 2 ? 1 : 2;
        return type == 2 ? 2 : 3;
    }
    private boolean cachedServerRaining;
    private float cachedServerRainLevel;
    private float cachedServerThunderLevel;
    private ClientWorld weatherSnapshotLevel;

    public Ambience() {
        super("Ambience", "\u0418\u0437\u043c\u0435\u043d\u044f\u0435\u0442 \u0432\u0440\u0435\u043c\u044f, \u043f\u043e\u0433\u043e\u0434\u0443 \u0438 \u0430\u0442\u043c\u043e\u0441\u0444\u0435\u0440\u0443 \u043c\u0438\u0440\u0430.", Category.VISUALS);
        this.wind.setChangeListener(() -> {
            if (this.wind.getValue() && rtx.nv.utils.render.post.handsflame.IrisShaderCompat.isShaderPackInUse()) {
                rtx.nv.api.modules.impl.Interface.NotificationsModule.notify("Ветер NV отключен: активен шейдер пак Iris. Настройте волны в опциях шейдера", 3500L);
            }
        });
    }

    public static Ambience getInstance() {
        return ModuleManager.get().get(Ambience.class);
    }

    @Override
    protected void onDisable() {
        ClientWorld clientWorld = this.mc.world;
        if (clientWorld instanceof ClientWorld) {
            ClientWorld clientWorld2 = clientWorld;
            this.restoreWeather(clientWorld2, clientWorld2.getLevelProperties());
        } else {
            this.clearWeatherSnapshot();
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (!packetEvent.isReceive()) {
            return;
        }
        if (packetEvent.getPacket() instanceof WorldTimeUpdateS2CPacket && this.isEnabled()) {
            packetEvent.cancel();
            return;
        }
        if (!(packetEvent.getPacket() instanceof GameStateChangeS2CPacket gameStateChangeS2CPacket)) {
            return;
        }
        GameStateChangeS2CPacket.Reason reason = gameStateChangeS2CPacket.getReason();
        if (!Ambience.isWeatherPacket(reason)) {
            return;
        }
        this.updateCachedWeather(gameStateChangeS2CPacket);
        if (this.isEnabled()) {
            packetEvent.cancel();
        }
    }

    public Biome.Precipitation precipitation() {
        if (!this.isEnabled()) {
            return null;
        }
        if (this.weather.is("\u0421\u043d\u0435\u0433")) {
            return Biome.Precipitation.SNOW;
        }
        if (this.weather.is("\u0414\u043e\u0436\u0434\u044c") || this.weather.is("\u0413\u0440\u043e\u0437\u0430")) {
            return Biome.Precipitation.RAIN;
        }
        return Biome.Precipitation.NONE;
    }

    public long getInternalTime() {
        if (this.mode.is("\u0414\u0435\u043d\u044c")) {
            return 1000L;
        }
        if (this.mode.is("\u041f\u043e\u043b\u0434\u0435\u043d\u044c")) {
            return 6000L;
        }
        if (this.mode.is("\u041d\u043e\u0447\u044c")) {
            return 13000L;
        }
        if (this.mode.is("\u041f\u043e\u043b\u043d\u043e\u0447\u044c")) {
            return 18000L;
        }
        return (long)this.customTime.getValue();
    }

    public void syncWeather(ClientWorld clientWorld, ClientWorld.Properties properties) {
        if (clientWorld == null || properties == null) {
            this.clearWeatherSnapshot();
            return;
        }
        if (this.weatherSnapshotLevel != clientWorld) {
            this.clearWeatherSnapshot();
        }
        if (!this.weatherOverrideActive) {
            this.cachedServerRaining = properties.isRaining();
            this.cachedServerRainLevel = clientWorld.getRainGradient(1.0f);
            this.cachedServerThunderLevel = clientWorld.getThunderGradient(1.0f);
            this.weatherOverrideActive = true;
            this.weatherSnapshotLevel = clientWorld;
        }
        boolean bl = this.shouldForcePrecipitation();
        properties.setRaining(bl);
        clientWorld.setRainGradient(bl ? 1.0f : 0.0f);
        clientWorld.setThunderGradient(this.weather.is("\u0413\u0440\u043e\u0437\u0430") ? 1.0f : 0.0f);
    }

    public float getSaturationFactor() {
        return Math.clamp(1.0f + this.saturation.getFloat(), 0.0f, 2.0f);
    }

    private void updateCachedWeather(GameStateChangeS2CPacket gameStateChangeS2CPacket) {
        GameStateChangeS2CPacket.Reason reason = gameStateChangeS2CPacket.getReason();
        if (reason == GameStateChangeS2CPacket.RAIN_STARTED) {
            this.cachedServerRaining = true;
            this.cachedServerRainLevel = Math.max(this.cachedServerRainLevel, 1.0f);
            return;
        }
        if (reason == GameStateChangeS2CPacket.RAIN_STOPPED) {
            this.cachedServerRaining = false;
            this.cachedServerRainLevel = 0.0f;
            this.cachedServerThunderLevel = 0.0f;
            return;
        }
        if (reason == GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED) {
            this.cachedServerRainLevel = Math.clamp(gameStateChangeS2CPacket.getValue(), 0.0f, 1.0f);
            this.cachedServerRaining = this.cachedServerRainLevel > 1.0E-4f;
            return;
        }
        if (reason == GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED) {
            this.cachedServerThunderLevel = Math.clamp(gameStateChangeS2CPacket.getValue(), 0.0f, 1.0f);
        }
    }

    public float getBrightnessValue() {
        return this.brightness.getFloat();
    }

    public boolean isCustomSkyActive() {
        return this.isEnabled() && this.customSky.getValue();
    }

    public int skyGradientMode() {
        if (!this.skySecondColor.getValue()) {
            return 0;
        }
        if (this.skyColorMode.is("\u0422\u0435\u043c\u0430")) {
            try {
                InterfaceModule interfaceModule = InterfaceModule.getInstance();
                if (interfaceModule != null && !interfaceModule.usesSecondClientColor()) {
                    return 0;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return 1;
    }

    public float skyBrightness() {
        return this.skyBrightnessLevel.getFloat() / 100.0f;
    }

    public float getWindSpeed() {
        return this.windSpeed.getFloat() / 100.0f;
    }

    public boolean isWindActive() {
        if (rtx.nv.utils.render.post.handsflame.IrisShaderCompat.isShaderPackInUse()) {
            return false;
        }
        return this.isEnabled() && this.wind.getValue() && (this.windGrassStrength.getFloat() > 0.0f || this.windLeavesStrength.getFloat() > 0.0f);
    }

    public int skyColor2RGB() {
        if (this.skyColorMode.is("\u0421\u0432\u043e\u0439")) {
            return this.skyColor2.getColorOpaque();
        }
        try {
            InterfaceModule interfaceModule = InterfaceModule.getInstance();
            if (interfaceModule != null) {
                return interfaceModule.usesSecondClientColor() ? interfaceModule.clientSecondaryColorOpaque() : interfaceModule.clientPrimaryColorOpaque();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return -8889601;
    }

    public float getWindGrassStrength() {
        return this.windGrassStrength.getFloat() / 100.0f;
    }

    public boolean hasWindGusts() {
        return this.windGusts.getValue();
    }

    public int skyTypeIndex() {
        if (this.skyType.is("Чёрная дыра") || this.skyType.is("\u0427\u0451\u0440\u043d\u0430\u044f \u0434\u044b\u0440\u0430")) {
            return 1;
        }
        if (this.skyType.is("Туманность") || this.skyType.is("\u0422\u0443\u043c\u0430\u043d\u043d\u043e\u0441\u0442\u044c")) {
            return 2;
        }
        if (this.skyType.is("Звездопад") || this.skyType.is("\u0417\u0432\u0435\u0437\u0434\u043e\u043f\u0430\u0434")) {
            return 3;
        }
        if (this.skyType.is("Бездна") || this.skyType.is("\u0411\u0435\u0437\u0434\u043d\u0430")) {
            return 4;
        }
        return 0;
    }

    public boolean isThemeTint() {
        return this.skyColorMode.is("Тема") || this.skyColorMode.is("\u0422\u0435\u043c\u0430");
    }

    public float skyDensity() {
        return this.skyDensity.getFloat();
    }

    public int skyLayers() {
        return this.skyLayers.getInt();
    }

    private void restoreWeather(ClientWorld clientWorld, ClientWorld.Properties properties) {
        if (!this.weatherOverrideActive) {
            return;
        }
        properties.setRaining(this.cachedServerRaining);
        clientWorld.setRainGradient(this.cachedServerRainLevel);
        clientWorld.setThunderGradient(this.cachedServerThunderLevel);
        this.clearWeatherSnapshot();
    }

    private boolean shouldForcePrecipitation() {
        return this.weather.is("\u0414\u043e\u0436\u0434\u044c") || this.weather.is("\u0413\u0440\u043e\u0437\u0430") || this.weather.is("\u0421\u043d\u0435\u0433");
    }

    private static boolean isWeatherPacket(GameStateChangeS2CPacket.Reason reason) {
        return reason == GameStateChangeS2CPacket.RAIN_STARTED || reason == GameStateChangeS2CPacket.RAIN_STOPPED || reason == GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED || reason == GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED;
    }

    public float getWindLeavesStrength() {
        return this.windLeavesStrength.getFloat() / 100.0f;
    }

    public int skyColorRGB() {
        if (this.skyColorMode.is("\u0421\u0432\u043e\u0439")) {
            return this.skyColor.getColorOpaque();
        }
        try {
            InterfaceModule interfaceModule = InterfaceModule.getInstance();
            if (interfaceModule != null) {
                return interfaceModule.clientPrimaryColorOpaque();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return -10813546;
    }

    private void clearWeatherSnapshot() {
        this.weatherOverrideActive = false;
        this.weatherSnapshotLevel = null;
    }
}

