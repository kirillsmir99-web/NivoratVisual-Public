package rtx.nv.utils.render.fonts;

import rtx.nv.api.modules.Category;

/**
 * Standardized Centralized Icon Registry for Nivorat Visual.
 * Replaces scattered legacy character codes with standardized vector glyph constants.
 * Mapped 1-to-1 to nv MSDF font atlas (U+E001 .. U+E02B).
 */
public final class NvIcons {
    private NvIcons() {}

    // Navigation (10)
    public static final String MODULES = "\uE001";
    public static final String PINNED = "\uE002";
    public static final String VISUALS = "\uE003";
    public static final String INTERFACE = "\uE004";
    public static final String DISPLAY = INTERFACE;
    public static final String UTILS = "\uE005";
    public static final String THEMES = "\uE006";
    public static final String SEARCH = "\uE007";
    public static final String SETTINGS = "\uE008";
    public static final String LANGUAGE = "\uE009";
    public static final String PROFILE = "\uE02B";

    // Visuals (8)
    public static final String SCALE = "\uE00A";
    public static final String ASPECT_RATIO = "\uE00B";
    public static final String COLOR = "\uE00C";
    public static final String GLOW = "\uE00D";
    public static final String GLASS = "\uE00E";
    public static final String SHARDS = "\uE00F";
    public static final String EDGE_FRINGE = "\uE02C";
    public static final String EDGE_WAVE = "\uE03D";

    // Audio (3)
    public static final String SOUND = "\uE010";
    public static final String VOLUME = "\uE011";
    public static final String PITCH = "\uE012";

    // Controls (3)
    public static final String BIND = "\uE013";
    public static final String SPEED = "\uE014";
    public static final String ANIMATION = "\uE015";

    // HUD (9)
    public static final String JUMP_CIRCLE = "\uE016";
    public static final String MUSIC = "\uE017";
    public static final String COOLDOWNS = "\uE018";
    public static final String HOTKEYS = "\uE019";
    public static final String POTIONS = "\uE01A";
    public static final String ARRAYLIST = "\uE01B";
    public static final String CROWN = "\uE02D";
    public static final String PET = "\uE02E";
    public static final String PICKAXE = "\uE02F";

    // Media (5)
    public static final String HEART = "\uE026";
    public static final String PLAY = "\uE027";
    public static final String PAUSE = "\uE028";
    public static final String PREVIOUS = "\uE029";
    public static final String NEXT = "\uE02A";

    // Themes (9)
    public static final String ADD = "\uE01C";
    public static final String DELETE = "\uE01D";
    public static final String DUPLICATE = "\uE01E";
    public static final String IMPORT = "\uE01F";
    public static final String EXPORT = "\uE020";
    public static final String REFRESH = "\uE037";
    public static final String RESET = "\uE038";
    public static final String RESTORE = "\uE039";
    public static final String SAVE = "\uE03A";

    // Window & Interface (14)
    public static final String CLOSE = "\uE021";
    public static final String BACK = "\uE022";
    public static final String CHEVRON_DOWN = "\uE023";
    public static final String CHECK = "\uE024";
    public static final String MORE = "\uE025";
    public static final String CHEVRON_RIGHT = "\uE030";
    public static final String COPY = "\uE031";
    public static final String FONT = "\uE032";
    public static final String INFO = "\uE033";
    public static final String KEYBIND = "\uE034";
    public static final String MAXIMIZE = "\uE035";
    public static final String PIN = "\uE036";
    public static final String TRASH = "\uE03B";
    public static final String WARNING = "\uE03C";

    // Minecraft (20)
    public static final String ARMOR = "\uE03E";
    public static final String CAPE = "\uE03F";
    public static final String COMBAT = "\uE040";
    public static final String COORDINATES = "\uE041";
    public static final String COSMETICS = "\uE042";
    public static final String CROSSHAIR = "\uE043";
    public static final String ENTITIES = "\uE044";
    public static final String FPS = "\uE045";
    public static final String FRIENDS = "\uE046";
    public static final String INVENTORY = "\uE047";
    public static final String KEYSTROKES = "\uE048";
    public static final String MODULES_GRID = "\uE049";
    public static final String MOVEMENT = "\uE04A";
    public static final String PING = "\uE04B";
    public static final String PLAYER = "\uE04C";
    public static final String RENDER = "\uE04D";
    public static final String SERVERS = "\uE04E";
    public static final String SHADER = "\uE04F";
    public static final String WAYPOINTS = "\uE050";
    public static final String WORLD = "\uE051";

    // Legacy Aliases for backwards compatibility
    public static final String WAVE_EDGE = EDGE_WAVE;
    public static final String LIVE_EDGE = EDGE_WAVE;
    public static final String GEAR = SETTINGS;
    public static final String ARROW_DOWN = CHEVRON_DOWN;
    public static final String KEYBOARD = BIND;

    /**
     * Resolves the primary icon glyph string for a given category.
     */
    public static String forCategory(Category category) {
        if (category == null) return MODULES;
        return switch (category) {
            case PINNED -> PINNED;
            case VISUALS -> VISUALS;
            case DISPLAY -> INTERFACE;
            case UTILS -> UTILS;
            case THEMES -> THEMES;
            case MEDIA -> MUSIC;
            case ABOUT -> INFO;
            default -> MODULES;
        };
    }

    /**
     * Resolves the primary icon character for a given category.
     */
    public static char charForCategory(Category category) {
        String s = forCategory(category);
        return s.isEmpty() ? '\uE001' : s.charAt(0);
    }

    /**
     * Fallback resolver ensuring an icon is valid and present in Fonts.NV.
     */
    public static String safe(String icon) {
        if (icon == null || icon.isEmpty()) {
            return SETTINGS;
        }
        if (!Fonts.NV.hasGlyph(icon)) {
            rtx.nv.NV.LOGGER.warn("[NV Icons] Missing glyph: {}, using SETTINGS fallback", icon);
            return SETTINGS;
        }
        return icon;
    }
}
