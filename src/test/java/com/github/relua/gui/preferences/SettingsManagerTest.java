package com.github.relua.gui.preferences;

import com.github.relua.gui.theme.ThemeManager.Theme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import static org.junit.jupiter.api.Assertions.*;

class SettingsManagerTest {
    private Preferences preferences;

    @BeforeEach void createPreferences() {
        preferences = Preferences.userRoot().node("/com/github/relua/tests/settings-" + UUID.randomUUID());
    }

    @AfterEach void removePreferences() throws Exception {
        preferences.removeNode();
        Preferences.userRoot().flush();
    }

    @Test void persistsAllSettingsAndNotifiesOnce() throws Exception {
        SettingsManager manager = new SettingsManager(preferences);
        AtomicInteger changes = new AtomicInteger();
        Consumer<AppSettings> listener = value -> changes.incrementAndGet();
        manager.addListener(listener);
        AppSettings selected = new AppSettings("Courier New", 22, true, false, true, Theme.LIGHT, "WARNING");
        manager.apply(selected);
        manager.apply(selected);
        assertEquals(1, changes.get());
        assertEquals(selected, new SettingsManager(preferences).getSettings());
        manager.apply(AppSettings.defaults());
        assertEquals(AppSettings.defaults(), new SettingsManager(preferences).getSettings());
    }

    @Test void rejectsInvalidValuesAndFallsBackForCorruptStoredValues() {
        assertThrows(IllegalArgumentException.class, () -> new AppSettings("", 14, false, true, false, Theme.DARK, "INFO"));
        assertThrows(IllegalArgumentException.class, () -> new AppSettings("Consolas", 99, false, true, false, Theme.DARK, "INFO"));
        preferences.put("font_family", "\n");
        preferences.putInt("font_size", -4);
        preferences.put("theme", "INVALID");
        preferences.put("log_level", "INVALID");
        assertEquals(AppSettings.defaults(), new SettingsManager(preferences).getSettings());
    }

    @Test void themeControlsAndPreferencesUseTheSameState() throws Exception {
        SettingsManager manager = new SettingsManager(preferences);
        com.github.relua.gui.theme.ThemeManager themes = new com.github.relua.gui.theme.ThemeManager(manager);
        themes.setTheme(Theme.LIGHT, null);
        assertEquals(Theme.LIGHT, manager.getSettings().getTheme());
        assertEquals(Theme.LIGHT, new SettingsManager(preferences).getSettings().getTheme());
        manager.apply(new AppSettings("Courier New", 20, true, false, true, Theme.DARK, "ERROR"));
        assertEquals(Theme.DARK, themes.getCurrentTheme());
        themes.setTheme(Theme.LIGHT, null);
        assertEquals(20, manager.getSettings().getFontSize());
        assertEquals("Courier New", manager.getSettings().getFontFamily());
    }

    @Test void failedStorageDoesNotApplyOrLeaveDraftValues() {
        Preferences broken = new AbstractPreferences(null, "") {
            final Map<String, String> values = new HashMap<>();
            @Override protected void putSpi(String key, String value) { values.put(key, value); }
            @Override protected String getSpi(String key) { return values.get(key); }
            @Override protected void removeSpi(String key) { values.remove(key); }
            @Override protected void removeNodeSpi() {}
            @Override protected String[] keysSpi() { return values.keySet().toArray(new String[0]); }
            @Override protected String[] childrenNamesSpi() { return new String[0]; }
            @Override protected AbstractPreferences childSpi(String name) { throw new UnsupportedOperationException(); }
            @Override protected void syncSpi() {}
            @Override protected void flushSpi() throws BackingStoreException { throw new BackingStoreException("Unavailable storage"); }
        };
        SettingsManager manager = new SettingsManager(broken);
        AppSettings selected = new AppSettings("Arial", 24, true, false, true, Theme.LIGHT, "ERROR");
        assertThrows(BackingStoreException.class, () -> manager.apply(selected));
        assertEquals(AppSettings.defaults(), manager.getSettings());
        assertEquals(AppSettings.defaults(), new SettingsManager(broken).getSettings());
    }
}
