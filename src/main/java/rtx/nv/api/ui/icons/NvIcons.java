package rtx.nv.api.ui.icons;

import rtx.nv.api.modules.Category;

/**
 * Mirror of NvIcons in rtx.nv.api.ui.icons package.
 * Points to the canonical implementation in rtx.nv.utils.render.fonts.NvIcons.
 */
public final class NvIcons {
    private NvIcons() {}

    public static final String MODULES = rtx.nv.utils.render.fonts.NvIcons.MODULES;
    public static final String PINNED = rtx.nv.utils.render.fonts.NvIcons.PINNED;
    public static final String VISUALS = rtx.nv.utils.render.fonts.NvIcons.VISUALS;
    public static final String INTERFACE = rtx.nv.utils.render.fonts.NvIcons.INTERFACE;
    public static final String UTILS = rtx.nv.utils.render.fonts.NvIcons.UTILS;
    public static final String THEMES = rtx.nv.utils.render.fonts.NvIcons.THEMES;
    public static final String SEARCH = rtx.nv.utils.render.fonts.NvIcons.SEARCH;
    public static final String SETTINGS = rtx.nv.utils.render.fonts.NvIcons.SETTINGS;
    public static final String LANGUAGE = rtx.nv.utils.render.fonts.NvIcons.LANGUAGE;
    public static final String SCALE = rtx.nv.utils.render.fonts.NvIcons.SCALE;
    public static final String ASPECT_RATIO = rtx.nv.utils.render.fonts.NvIcons.ASPECT_RATIO;
    public static final String COLOR = rtx.nv.utils.render.fonts.NvIcons.COLOR;
    public static final String GLOW = rtx.nv.utils.render.fonts.NvIcons.GLOW;
    public static final String GLASS = rtx.nv.utils.render.fonts.NvIcons.GLASS;
    public static final String SHARDS = rtx.nv.utils.render.fonts.NvIcons.SHARDS;
    public static final String SOUND = rtx.nv.utils.render.fonts.NvIcons.SOUND;
    public static final String VOLUME = rtx.nv.utils.render.fonts.NvIcons.VOLUME;
    public static final String PITCH = rtx.nv.utils.render.fonts.NvIcons.PITCH;
    public static final String BIND = rtx.nv.utils.render.fonts.NvIcons.BIND;
    public static final String SPEED = rtx.nv.utils.render.fonts.NvIcons.SPEED;
    public static final String ANIMATION = rtx.nv.utils.render.fonts.NvIcons.ANIMATION;
    public static final String JUMP_CIRCLE = rtx.nv.utils.render.fonts.NvIcons.JUMP_CIRCLE;
    public static final String MUSIC = rtx.nv.utils.render.fonts.NvIcons.MUSIC;
    public static final String COOLDOWNS = rtx.nv.utils.render.fonts.NvIcons.COOLDOWNS;
    public static final String HOTKEYS = rtx.nv.utils.render.fonts.NvIcons.HOTKEYS;
    public static final String POTIONS = rtx.nv.utils.render.fonts.NvIcons.POTIONS;
    public static final String ARRAYLIST = rtx.nv.utils.render.fonts.NvIcons.ARRAYLIST;
    public static final String ADD = rtx.nv.utils.render.fonts.NvIcons.ADD;
    public static final String DELETE = rtx.nv.utils.render.fonts.NvIcons.DELETE;
    public static final String DUPLICATE = rtx.nv.utils.render.fonts.NvIcons.DUPLICATE;
    public static final String IMPORT = rtx.nv.utils.render.fonts.NvIcons.IMPORT;
    public static final String EXPORT = rtx.nv.utils.render.fonts.NvIcons.EXPORT;
    public static final String CLOSE = rtx.nv.utils.render.fonts.NvIcons.CLOSE;
    public static final String BACK = rtx.nv.utils.render.fonts.NvIcons.BACK;
    public static final String CHEVRON_DOWN = rtx.nv.utils.render.fonts.NvIcons.CHEVRON_DOWN;
    public static final String CHECK = rtx.nv.utils.render.fonts.NvIcons.CHECK;
    public static final String MORE = rtx.nv.utils.render.fonts.NvIcons.MORE;
    public static final String HEART = rtx.nv.utils.render.fonts.NvIcons.HEART;
    public static final String PLAY = rtx.nv.utils.render.fonts.NvIcons.PLAY;
    public static final String PAUSE = rtx.nv.utils.render.fonts.NvIcons.PAUSE;
    public static final String PREVIOUS = rtx.nv.utils.render.fonts.NvIcons.PREVIOUS;
    public static final String NEXT = rtx.nv.utils.render.fonts.NvIcons.NEXT;
    public static final String PROFILE = rtx.nv.utils.render.fonts.NvIcons.PROFILE;
    public static final String EDGE_FRINGE = rtx.nv.utils.render.fonts.NvIcons.EDGE_FRINGE;
    public static final String EDGE_WAVE = rtx.nv.utils.render.fonts.NvIcons.EDGE_WAVE;

    public static final String REFRESH = rtx.nv.utils.render.fonts.NvIcons.REFRESH;
    public static final String RESET = rtx.nv.utils.render.fonts.NvIcons.RESET;
    public static final String RESTORE = rtx.nv.utils.render.fonts.NvIcons.RESTORE;
    public static final String SAVE = rtx.nv.utils.render.fonts.NvIcons.SAVE;
    public static final String CHEVRON_RIGHT = rtx.nv.utils.render.fonts.NvIcons.CHEVRON_RIGHT;
    public static final String COPY = rtx.nv.utils.render.fonts.NvIcons.COPY;
    public static final String FONT = rtx.nv.utils.render.fonts.NvIcons.FONT;
    public static final String INFO = rtx.nv.utils.render.fonts.NvIcons.INFO;
    public static final String KEYBIND = rtx.nv.utils.render.fonts.NvIcons.KEYBIND;
    public static final String MAXIMIZE = rtx.nv.utils.render.fonts.NvIcons.MAXIMIZE;
    public static final String PIN = rtx.nv.utils.render.fonts.NvIcons.PIN;
    public static final String TRASH = rtx.nv.utils.render.fonts.NvIcons.TRASH;
    public static final String WARNING = rtx.nv.utils.render.fonts.NvIcons.WARNING;

    // Legacy Aliases
    public static final String WAVE_EDGE = rtx.nv.utils.render.fonts.NvIcons.WAVE_EDGE;
    public static final String LIVE_EDGE = EDGE_WAVE;
    public static final String GEAR = SETTINGS;
    public static final String ARROW_DOWN = CHEVRON_DOWN;
    public static final String KEYBOARD = BIND;

    public static String forCategory(Category category) {
        return rtx.nv.utils.render.fonts.NvIcons.forCategory(category);
    }

    public static char charForCategory(Category category) {
        return rtx.nv.utils.render.fonts.NvIcons.charForCategory(category);
    }

    public static String safe(String icon) {
        return rtx.nv.utils.render.fonts.NvIcons.safe(icon);
    }
}
