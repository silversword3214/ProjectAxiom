package silversword.axiom.client.gui.core;

import silversword.axiom.client.config.ClickGuiConfigManager;

import java.util.HashMap;
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
    }

    private static int applyAlphaMultiplier(int color, int multiplierPercent) {
        int alpha = (color >> 24) & 0xFF;
        int newAlpha = (int) Math.min(255, Math.round(alpha * multiplierPercent / 100.0));
        return (newAlpha << 24) | (color & 0x00FFFFFF);
    }

    public static Theme getTheme(String name) {
        Theme theme = THEMES.get(name);
        if (theme == null) {
            theme = THEMES.get("Default");
        }
        if (theme == null) {
            theme = new Theme();
        }
        Theme copy = theme.copy();
        int multiplier = ClickGuiConfigManager.getGlobalAlpha();
        // Apply multiplier to all color fields
        copy.panel = applyAlphaMultiplier(copy.panel, multiplier);
        copy.header = applyAlphaMultiplier(copy.header, multiplier);
        copy.border = applyAlphaMultiplier(copy.border, multiplier);
        copy.knob = applyAlphaMultiplier(copy.knob, multiplier);
        copy.text = applyAlphaMultiplier(copy.text, multiplier); // text alpha might be 0xFF, so multiplier doesn't change
        copy.textDim = applyAlphaMultiplier(copy.textDim, multiplier);
        copy.accent = applyAlphaMultiplier(copy.accent, multiplier);
        copy.button = applyAlphaMultiplier(copy.button, multiplier);
        copy.buttonHover = applyAlphaMultiplier(copy.buttonHover, multiplier);
        copy.toggleOff = applyAlphaMultiplier(copy.toggleOff, multiplier);
        copy.toggleOn = applyAlphaMultiplier(copy.toggleOn, multiplier);
        copy.sliderTrack = applyAlphaMultiplier(copy.sliderTrack, multiplier);
        copy.sliderFill = applyAlphaMultiplier(copy.sliderFill, multiplier);
        copy.scrollbar = applyAlphaMultiplier(copy.scrollbar, multiplier);
        copy.scrollbarHover = applyAlphaMultiplier(copy.scrollbarHover, multiplier);
        return copy;
    }

    public static Theme getCurrentTheme() {
        String name = ClickGuiConfigManager.getThemeName();
        if (name == null || name.isEmpty()) {
            name = "Default";
        }
        return getTheme(name);
    }

    public static String[] getThemeNames() {
        return THEMES.keySet().toArray(new String[0]);
    }
}