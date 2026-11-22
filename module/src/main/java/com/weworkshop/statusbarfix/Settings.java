package com.weworkshop.statusbarfix;

import de.robv.android.xposed.XSharedPreferences;

/**
 * User-configurable colors, written by {@link SettingsActivity} in this app's own process and
 * read from every hooked process (launcher, System UI) through Xposed's shared-preferences
 * bridge. Each read reloads the file, since LSPosed runs this module in several long-lived
 * processes that never restart when the user changes a setting.
 */
final class Settings {
    static final String PACKAGE = "com.weworkshop.statusbarfix";
    static final String PREFS_NAME = "settings";

    static final int DEFAULT_DARK = 0xFF000000;
    static final int DEFAULT_LIGHT = 0xFFFFFFFF;
    /** ZTE's dark lock screen content color (see ColorUtils.computeForeColor). */
    static final int DEFAULT_LOCK_CONTENT_DARK = 0xA6000000;

    /** Follow the live wallpaper's own dark/light hint (the default). */
    static final int HOME_STATUS_AUTO = 0;
    static final int HOME_STATUS_DARK = 1;
    static final int HOME_STATUS_LIGHT = 2;

    private static final XSharedPreferences prefs = new XSharedPreferences(PACKAGE, PREFS_NAME);

    private Settings() {
    }

    private static int get(String key, int def) {
        try {
            prefs.reload();
            return prefs.getInt(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    /** Home screen status bar icons: {@link #HOME_STATUS_AUTO}, {@code _DARK} or {@code _LIGHT}. */
    static int homeStatusMode() {
        return get("home_status_mode", HOME_STATUS_AUTO);
    }

    // Lock screen status bar icons (always auto, only the two colors are configurable).
    static int lockStatusDark() {
        return get("lock_status_dark", DEFAULT_DARK);
    }

    static int lockStatusLight() {
        return get("lock_status_light", DEFAULT_LIGHT);
    }

    // Lock screen content: clock, date, widgets and shortcuts.
    static int lockContentDark() {
        return get("lock_content_dark", DEFAULT_LOCK_CONTENT_DARK);
    }

    static int lockContentLight() {
        return get("lock_content_light", DEFAULT_LIGHT);
    }

    // Home screen app label text (generic Launcher3 "BubbleTextView").
    static int labelDark() {
        return get("label_dark", DEFAULT_DARK);
    }

    static int labelLight() {
        return get("label_light", DEFAULT_LIGHT);
    }
}
