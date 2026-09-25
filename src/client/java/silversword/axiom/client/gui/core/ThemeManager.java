package silversword.axiom.client.gui.core;

import silversword.axiom.client.config.ClickGuiConfigManager;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class ThemeManager {
    private static final Map<String, Theme> THEMES = new HashMap<>();

    static {
        try {

            THEMES.put("Default", new Theme());

            // Neon-theme
            Theme neon = new Theme();
            neon.panel = 0xCC0A0A1A;
            neon.header = 0xDD1A1A2A;
            neon.border = 0xFFFF00FF;
            neon.text = 0xFFFFFFFF;
            neon.textDim = 0xFFAAAAAA;
            neon.accent = 0xFF00FFFF;
            neon.button = 0x88222222;
            neon.buttonHover = 0xAA333333;
            neon.toggleOn = 0xFF00FF00;
            neon.toggleOff = 0xFF444444;
            neon.sliderFill = 0xFF00FFFF;
            neon.scrollbar = 0xAA2B2B2B;
            neon.scrollbarHover = 0xFF00FFFF;
            THEMES.put("Neon", neon);

            Theme neonBlue = new Theme();
            neonBlue.panel = 0xCC0A1A2A;
            neonBlue.header = 0xDD1A2A3A;
            neonBlue.border = 0xFF00AAFF;
            neonBlue.accent = 0xFF00AAFF;
            neonBlue.text = 0xFFFFFFFF;
            neonBlue.textDim = 0xFFAAAAAA;
            neonBlue.button = 0x88222222;
            neonBlue.buttonHover = 0xAA333333;
            neonBlue.toggleOn = 0xFF00AAFF;
            neonBlue.toggleOff = 0xFF444444;
            neonBlue.sliderFill = 0xFF00AAFF;
            neonBlue.scrollbar = 0xAA2B2B2B;
            neonBlue.scrollbarHover = 0xFF00AAFF;
            THEMES.put("Neon Blue", neonBlue);

            // --- Neon Green ---
            Theme neonGreen = new Theme();
            neonGreen.panel = 0xCC0A2A1A;
            neonGreen.header = 0xDD1A3A2A;
            neonGreen.border = 0xFF00FF88;
            neonGreen.accent = 0xFF00FF88;
            neonGreen.text = 0xFFFFFFFF;
            neonGreen.textDim = 0xFFAAAAAA;
            neonGreen.button = 0x88222222;
            neonGreen.buttonHover = 0xAA333333;
            neonGreen.toggleOn = 0xFF00FF88;
            neonGreen.toggleOff = 0xFF444444;
            neonGreen.sliderFill = 0xFF00FF88;
            neonGreen.scrollbar = 0xAA2B2B2B;
            neonGreen.scrollbarHover = 0xFF00FF88;
            THEMES.put("Neon Green", neonGreen);

            // --- Neon Pink ---
            Theme neonPink = new Theme();
            neonPink.panel = 0xCC2A0A2A;
            neonPink.header = 0xDD3A1A3A;
            neonPink.border = 0xFFFF66CC;
            neonPink.accent = 0xFFFF66CC;
            neonPink.text = 0xFFFFFFFF;
            neonPink.textDim = 0xFFAAAAAA;
            neonPink.button = 0x88222222;
            neonPink.buttonHover = 0xAA333333;
            neonPink.toggleOn = 0xFFFF66CC;
            neonPink.toggleOff = 0xFF444444;
            neonPink.sliderFill = 0xFFFF66CC;
            neonPink.scrollbar = 0xAA2B2B2B;
            neonPink.scrollbarHover = 0xFFFF66CC;
            THEMES.put("Neon Pink", neonPink);

            // --- Neon Orange ---
            Theme neonOrange = new Theme();
            neonOrange.panel = 0xCC2A1A0A;
            neonOrange.header = 0xDD3A2A1A;
            neonOrange.border = 0xFFFF9933;
            neonOrange.accent = 0xFFFF9933;
            neonOrange.text = 0xFFFFFFFF;
            neonOrange.textDim = 0xFFAAAAAA;
            neonOrange.button = 0x88222222;
            neonOrange.buttonHover = 0xAA333333;
            neonOrange.toggleOn = 0xFFFF9933;
            neonOrange.toggleOff = 0xFF444444;
            neonOrange.sliderFill = 0xFFFF9933;
            neonOrange.scrollbar = 0xAA2B2B2B;
            neonOrange.scrollbarHover = 0xFFFF9933;
            THEMES.put("Neon Orange", neonOrange);

            // --- Neon Purple ---
            Theme neonPurple = new Theme();
            neonPurple.panel = 0xCC1A0A3A;
            neonPurple.header = 0xDD2A1A4A;
            neonPurple.border = 0xFFAA55FF;
            neonPurple.accent = 0xFFAA55FF;
            neonPurple.text = 0xFFFFFFFF;
            neonPurple.textDim = 0xFFAAAAAA;
            neonPurple.button = 0x88222222;
            neonPurple.buttonHover = 0xAA333333;
            neonPurple.toggleOn = 0xFFAA55FF;
            neonPurple.toggleOff = 0xFF444444;
            neonPurple.sliderFill = 0xFFAA55FF;
            neonPurple.scrollbar = 0xAA2B2B2B;
            neonPurple.scrollbarHover = 0xFFAA55FF;
            THEMES.put("Neon Purple", neonPurple);

            // --- Gold ---
            Theme gold = new Theme();
            gold.panel = 0xCC332200;
            gold.header = 0xDD443311;
            gold.border = 0xFFFFD700;
            gold.accent = 0xFFFFD700;
            gold.text = 0xFFFFFFFF;
            gold.textDim = 0xFFDDDDDD;
            gold.button = 0xCC554422;
            gold.buttonHover = 0xDD665533;
            gold.toggleOn = 0xFFFFD700;
            gold.toggleOff = 0xFF444444;
            gold.sliderFill = 0xFFFFD700;
            gold.scrollbar = 0xAA2B2B2B;
            gold.scrollbarHover = 0xFFFFD700;
            THEMES.put("Gold", gold);

            // --- Cyberpunk ---
            Theme cyberpunk = new Theme();
            cyberpunk.panel = 0xCC1A0A2A;
            cyberpunk.header = 0xDD2A1A3A;
            cyberpunk.border = 0xFFFF00FF;
            cyberpunk.accent = 0xFF00FFFF;
            cyberpunk.text = 0xFFFFFFFF;
            cyberpunk.textDim = 0xFFAAAAAA;
            cyberpunk.button = 0x88222222;
            cyberpunk.buttonHover = 0xAA333333;
            cyberpunk.toggleOn = 0xFF00FFFF;
            cyberpunk.toggleOff = 0xFF444444;
            cyberpunk.sliderFill = 0xFF00FFFF;
            cyberpunk.scrollbar = 0xAA2B2B2B;
            cyberpunk.scrollbarHover = 0xFFFF00FF;
            THEMES.put("Cyberpunk", cyberpunk);

            // --- Midnight ---
            Theme midnight = new Theme();
            midnight.panel = 0xCC000022;
            midnight.header = 0xDD111133;
            midnight.border = 0xFFC0C0C0;     // hopea
            midnight.accent = 0xFFC0C0C0;
            midnight.text = 0xFFFFFFFF;
            midnight.textDim = 0xFFAAAAAA;
            midnight.button = 0x88222222;
            midnight.buttonHover = 0xAA333333;
            midnight.toggleOn = 0xFFC0C0C0;
            midnight.toggleOff = 0xFF444444;
            midnight.sliderFill = 0xFFC0C0C0;
            midnight.scrollbar = 0xAA2B2B2B;
            midnight.scrollbarHover = 0xFFC0C0C0;
            THEMES.put("Midnight", midnight);

            // --- Blood Red ---
            Theme bloodRed = new Theme();
            bloodRed.panel = 0xCC2A0A0A;
            bloodRed.header = 0xDD3A1A1A;
            bloodRed.border = 0xFFFF3333;     // punainen
            bloodRed.accent = 0xFFFF3333;
            bloodRed.text = 0xFFFFFFFF;
            bloodRed.textDim = 0xFFAAAAAA;
            bloodRed.button = 0x88222222;
            bloodRed.buttonHover = 0xAA333333;
            bloodRed.toggleOn = 0xFFFF3333;
            bloodRed.toggleOff = 0xFF444444;
            bloodRed.sliderFill = 0xFFFF3333;
            bloodRed.scrollbar = 0xAA2B2B2B;
            bloodRed.scrollbarHover = 0xFFFF3333;
            THEMES.put("Blood Red", bloodRed);

            // --- Aqua ---
            Theme aqua = new Theme();
            aqua.panel = 0xCC0A2A2A;
            aqua.header = 0xDD1A3A3A;
            aqua.border = 0xFF33CCCC;        // turkoosi
            aqua.accent = 0xFF33CCCC;
            aqua.text = 0xFFFFFFFF;
            aqua.textDim = 0xFFAAAAAA;
            aqua.button = 0x88222222;
            aqua.buttonHover = 0xAA333333;
            aqua.toggleOn = 0xFF33CCCC;
            aqua.toggleOff = 0xFF444444;
            aqua.sliderFill = 0xFF33CCCC;
            aqua.scrollbar = 0xAA2B2B2B;
            aqua.scrollbarHover = 0xFF33CCCC;
            THEMES.put("Aqua", aqua);


        } catch (Throwable t) {
            t.printStackTrace();
            if (!THEMES.containsKey("Default")) {
                // Varmistetaan että Default on olemassa
                Theme fallback = new Theme();
                fallback.panel = 0xCC000000;
                fallback.header = 0xDD111111;
                fallback.border = 0xFF222222;
                fallback.accent = 0xFF8A2BE2;
                fallback.text = 0xFFFFFFFF;
                fallback.textDim = 0xFFAAAAAA;
                fallback.button = 0x88222222;
                fallback.buttonHover = 0xAA333333;
                fallback.toggleOn = 0xFF2E7D32;
                fallback.toggleOff = 0xFF444444;
                fallback.sliderFill = 0xFF8A2BE2;
                fallback.scrollbar = 0xAA2B2B2B;
                fallback.scrollbarHover = 0xFF8A2BE2;
                THEMES.put("Default", fallback);
            }
        }


        // Editor/IDE -teemat
        register("Dracula",              0xCC282A36, 0xDD343746, 0xFF6272A4, 0xFFBD93F9, 0xFFF8F8F2, 0xFF6272A4);
        register("Nord",                 0xCC2E3440, 0xDD3B4252, 0xFF4C566A, 0xFF88C0D0, 0xFFECEFF4, 0xFF8FBCBB);
        register("Gruvbox Dark",         0xCC282828, 0xDD3C3836, 0xFF504945, 0xFFFABD2F, 0xFFEBDBB2, 0xFFA89984);
        register("Solarized Dark",       0xCC002B36, 0xDD073642, 0xFF586E75, 0xFF268BD2, 0xFFEEE8D5, 0xFF93A1A1);
        register("One Dark",             0xCC282C34, 0xDD2C313A, 0xFF3E4451, 0xFF61AFEF, 0xFFABB2BF, 0xFF5C6370);
        register("Tokyo Night",          0xCC1A1B26, 0xDD1F2335, 0xFF3B4261, 0xFF7AA2F7, 0xFFC0CAF5, 0xFF565F89);
        register("Monokai",              0xCC272822, 0xDD2D2E27, 0xFF3E3D32, 0xFFF92672, 0xFFF8F8F2, 0xFF75715E);
        register("Material Dark",        0xCC212121, 0xDD263238, 0xFF37474F, 0xFF82B1FF, 0xFFECEFF1, 0xFF78909C);
        register("Night Owl",            0xCC011627, 0xDD0B2942, 0xFF1D3B53, 0xFF82AAFF, 0xFFD6DEEB, 0xFF5F7E97);
        register("Palenight",            0xCC292D3E, 0xDD1B1E2B, 0xFF676E95, 0xFFC792EA, 0xFFA6ACCD, 0xFF676E95);
        register("Oceanic Next",         0xCC1B2B34, 0xDD343D46, 0xFF4F5B66, 0xFF6699CC, 0xFFC0C5CE, 0xFF65737E);
        register("Ayu Dark",             0xCC0A0E14, 0xDD0F1419, 0xFF1F2430, 0xFFFFCC66, 0xFFB3B1AD, 0xFF626A73);

        // Catppuccin-perhe
        register("Catppuccin Mocha",     0xCC1E1E2E, 0xDD181825, 0xFF313244, 0xFFCBA6F7, 0xFFCDD6F4, 0xFF6C7086);
        register("Catppuccin Macchiato", 0xCC24273A, 0xDD1E2030, 0xFF363A4F, 0xFFC6A0F6, 0xFFCAD3F5, 0xFF6E738D);
        register("Catppuccin Frappe",    0xCC303446, 0xDD292C3C, 0xFF414559, 0xFFCA9EE6, 0xFFC6D0F5, 0xFF737994);

        // Modernit / tyylikkäät
        register("Rosé Pine",            0xCC191724, 0xDD1F1D2E, 0xFF26233A, 0xFFEBBCBA, 0xFFE0DEF4, 0xFF6E6A86);
        register("Kanagawa",             0xCC1F1F28, 0xDD16161D, 0xFF2A2A37, 0xFF7E9CD8, 0xFFDCD7BA, 0xFF727169);
        register("Everforest",           0xCC2B3339, 0xDD323C41, 0xFF3A454A, 0xFFA7C080, 0xFFD3C6AA, 0xFF859289);
        register("Lavender",             0xCC1A1420, 0xDD241A2A, 0xFF3A2A45, 0xFFB19CD9, 0xFFE8DFF5, 0xFF8A7A9A);

        // Retrowave / neon
        register("Synthwave 84",         0xCC241B2F, 0xDD2A2139, 0xFF34294F, 0xFFFF7EDB, 0xFFF0EFF1, 0xFF848BBD);
        register("Matrix",               0xCC0D0208, 0xDD001107, 0xFF003B00, 0xFF00FF41, 0xFF00FF41, 0xFF008F11);
        register("Amber",                0xCC1C1400, 0xDD2A1F00, 0xFF3D2E00, 0xFFFFBF00, 0xFFFFD58A, 0xFF8A6F00);
        register("Cherry",               0xCC2A0A1A, 0xDD3D1025, 0xFF5A1A35, 0xFFFF4D6D, 0xFFFFE0E8, 0xFFA06070);

        // Luonto & pehmeät
        register("Ice Blue",             0xCC0B1A2A, 0xDD0F2540, 0xFF1E3A5F, 0xFF4DD0E1, 0xFFE0F7FA, 0xFF8BA6B9);
        register("Forest",               0xCC0E1E10, 0xDD152A17, 0xFF264D2A, 0xFF66BB6A, 0xFFD4E8D4, 0xFF6B8E6B);
        register("Sunset",               0xCC2A1A2A, 0xDD3A2530, 0xFF5A3A4A, 0xFFFF6B9D, 0xFFFFE8F0, 0xFFC090A0);
        register("Mint",                 0xCC0A1F1A, 0xDD0F2F28, 0xFF1A4A40, 0xFF4DFFB8, 0xFFE0FFF5, 0xFF6A9990);
        register("Coral",                0xCC2A1515, 0xDD3D1F1F, 0xFF5A2F2F, 0xFFFF7F50, 0xFFFFEDDF, 0xFFA06868);
    }

    private static boolean customsLoaded = false;

    private static int applyAlphaMultiplier(int color, int multiplierPercent) {
        int alpha = (color >> 24) & 0xFF;
        int newAlpha = (int) Math.min(255, Math.round(alpha * multiplierPercent / 100.0));
        return (newAlpha << 24) | (color & 0x00FFFFFF);
    }

    private static void ensureCustoms() {
        if (customsLoaded) return;
        customsLoaded = true;
        try { CustomThemeStore.ensureLoaded(); } catch (Throwable ignored) {}
    }

    public static boolean isBuiltIn(String name) {
        return THEMES.containsKey(name);   // koska custom-teemat eivät ole THEMES:ssa
    }

    // KORVAA getTheme-metodi:
    public static Theme getTheme(String name) {
        ensureCustoms();

        Theme base = null;
        if (THEMES.containsKey(name)) {
            base = THEMES.get(name);
        } else {
            CustomTheme custom = CustomThemeStore.get(name);
            if (custom != null) base = custom.toTheme();
        }
        if (base == null) base = THEMES.get("Default");
        if (base == null) base = new Theme();

        Theme copy = base.copy();
        int multiplier = ClickGuiConfigManager.getGlobalAlpha();
        copy.panel         = applyAlphaMultiplier(copy.panel,         multiplier);
        copy.header        = applyAlphaMultiplier(copy.header,        multiplier);
        copy.border        = applyAlphaMultiplier(copy.border,        multiplier);
        copy.knob          = applyAlphaMultiplier(copy.knob,          multiplier);
        copy.text          = applyAlphaMultiplier(copy.text,          multiplier);
        copy.textDim       = applyAlphaMultiplier(copy.textDim,       multiplier);
        copy.accent        = applyAlphaMultiplier(copy.accent,        multiplier);
        copy.button        = applyAlphaMultiplier(copy.button,        multiplier);
        copy.buttonHover   = applyAlphaMultiplier(copy.buttonHover,   multiplier);
        copy.toggleOff     = applyAlphaMultiplier(copy.toggleOff,     multiplier);
        copy.toggleOn      = applyAlphaMultiplier(copy.toggleOn,      multiplier);
        copy.sliderTrack   = applyAlphaMultiplier(copy.sliderTrack,   multiplier);
        copy.sliderFill    = applyAlphaMultiplier(copy.sliderFill,    multiplier);
        copy.scrollbar     = applyAlphaMultiplier(copy.scrollbar,     multiplier);
        copy.scrollbarHover= applyAlphaMultiplier(copy.scrollbarHover,multiplier);
        return copy;
    }

    /** Lyhyt rekisteröintimetodi teemoille jotka käyttävät vakiovärejä napeille. */
    private static void register(String name, int panel, int header, int border,
                                 int accent, int text, int textDim) {
        Theme t = new Theme();
        t.panel  = panel;
        t.header = header;
        t.border = border;
        t.accent = accent;
        t.text   = text;
        t.textDim = textDim;

        t.knob         = 0xFF141414;
        t.button       = 0x88222222;
        t.buttonHover  = 0xAA333333;
        t.toggleOff    = 0xFF444444;
        t.toggleOn     = accent;
        t.sliderTrack  = 0xFF333333;
        t.sliderFill   = accent;
        t.scrollbar    = 0xAA2B2B2B;
        t.scrollbarHover = accent;

        t.radius = 6;
        t.padding = 6;
        t.innerPadding = 4;
        t.headerHeight = 18;
        t.rowHeight = 16;

        THEMES.put(name, t);
    }

    // KORVAA getThemeNames-metodi:
    public static String[] getThemeNames() {
        ensureCustoms();
        LinkedHashSet<String> all = new LinkedHashSet<>(THEMES.keySet());
        for (CustomTheme c : CustomThemeStore.all()) all.add(c.name);
        return all.toArray(new String[0]);
    }

    public static Theme getCurrentTheme() {
        String name = ClickGuiConfigManager.getThemeName();
        if (name == null || name.isEmpty()) {
            name = "Default";
        }
        return getTheme(name);
    }

}