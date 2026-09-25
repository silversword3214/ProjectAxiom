package silversword.axiom.client.gui.core;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

/** Tallentaa ja lataa käyttäjän custom-teemat. Käyttää Properties-tiedostoa. */
public final class CustomThemeStore {

    private static final Map<String, CustomTheme> CUSTOMS = new LinkedHashMap<>();
    private static boolean loaded = false;

    private CustomThemeStore() {}

    private static File file() {
        try {
            File gameDir = Minecraft.getInstance().gameDirectory;
            File cfg = new File(gameDir, "config");
            if (!cfg.exists()) cfg.mkdirs();
            return new File(cfg, "axiom-custom-themes.properties");
        } catch (Throwable t) {
            File home = new File(System.getProperty("user.home", "."), ".axiom");
            home.mkdirs();
            return new File(home, "custom-themes.properties");
        }
    }

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        load();
    }

    public static Collection<CustomTheme> all() {
        ensureLoaded();
        return CUSTOMS.values();
    }

    public static CustomTheme get(String name) {
        ensureLoaded();
        return CUSTOMS.get(name);
    }

    public static boolean isCustom(String name) {
        ensureLoaded();
        return CUSTOMS.containsKey(name);
    }

    public static void put(CustomTheme theme) {
        ensureLoaded();
        if (theme == null || theme.name == null || theme.name.isEmpty()) return;
        CUSTOMS.put(theme.name, theme);
        save();
    }

    public static void remove(String name) {
        ensureLoaded();
        if (CUSTOMS.remove(name) != null) save();
    }

    // ── IO ─────────────────────────────────────────────────────────
    private static void load() {
        File f = file();
        if (!f.isFile()) return;
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(f)) {
            p.load(in);
        } catch (Throwable t) { return; }

        // Etsi kaikki themeNames
        String list = p.getProperty("theme.names", "");
        if (list.isEmpty()) return;
        for (String name : list.split(",")) {
            name = name.trim();
            if (name.isEmpty()) continue;
            try {
                CustomTheme c = CustomTheme.readFromProps(p, "theme." + name + ".");
                c.name = name;
                CUSTOMS.put(name, c);
            } catch (Throwable ignored) {}
        }
    }

    private static void save() {
        File f = file();
        Properties p = new Properties();

        StringBuilder names = new StringBuilder();
        for (String n : CUSTOMS.keySet()) {
            if (names.length() > 0) names.append(',');
            names.append(n);
        }
        p.setProperty("theme.names", names.toString());

        for (Map.Entry<String, CustomTheme> e : CUSTOMS.entrySet()) {
            e.getValue().writeToProps(p, "theme." + e.getKey() + ".");
        }

        try (FileOutputStream out = new FileOutputStream(f)) {
            p.store(out, "Axiom custom themes");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}