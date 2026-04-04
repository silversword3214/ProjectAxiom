package silversword.axiom.client.render.font;

import silversword.axiom.client.utils.render.FontUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Fonts {
    public static final String[] BUILTIN_FONTS = { "jetbrains_mono", "freedom" };

    public static String currentFontName = BUILTIN_FONTS[0]; // "jetbrains_mono"

    public static String DEFAULT_FONT_FAMILY;
    public static FontFace DEFAULT_FONT;

    public static final List<FontFamily> FONT_FAMILIES = new ArrayList<>();
    private static TextRenderer renderer;

    private Fonts() {}

    public static List<String> getAvailableFonts() {
        List<String> list = new ArrayList<>();
        // Lisätään kaikki fontit FONT_FAMILIES:sta
        for (FontFamily f : FONT_FAMILIES) {
            list.add(f.getName());
        }
        return list;
    }

    public static void refresh() {
        FONT_FAMILIES.clear();

        for (String builtin : BUILTIN_FONTS) {
            FontUtils.loadBuiltin(FONT_FAMILIES, builtin);
        }

        for (String path : FontUtils.getSearchPaths()) {
            FontUtils.loadSystem(FONT_FAMILIES, new java.io.File(path));
        }

        FONT_FAMILIES.sort(Comparator.comparing(FontFamily::getName));

        if (!FONT_FAMILIES.isEmpty()) {
            DEFAULT_FONT_FAMILY = FONT_FAMILIES.get(0).getName();
            DEFAULT_FONT = FONT_FAMILIES.get(0).get(FontInfo.Type.Regular);
            // Tarkista, onko currentFontName olemassa
            boolean found = false;
            for (FontFamily f : FONT_FAMILIES) {
                if (f.getName().equalsIgnoreCase(currentFontName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                currentFontName = DEFAULT_FONT_FAMILY;
            }
        } else {
            throw new RuntimeException("Ei yhtään fonttia ladattu!");
        }

        try {
            setFont(currentFontName);
        } catch (Exception e) {
            System.err.println("Fonts.refresh: unable to load custom font, using default");
            load(DEFAULT_FONT);
            e.printStackTrace();
        }
    }

    public static void setFont(String familyName) {
        currentFontName = familyName;
        FontFamily family = getFamily(familyName);
        if (family != null && family.hasType(FontInfo.Type.Regular)) {
            load(family.get(FontInfo.Type.Regular));
        } else {
            System.err.println("Fonttia " + familyName + " ei löytynyt. Palautetaan oletusfonttiin.");
            if (DEFAULT_FONT != null) {
                load(DEFAULT_FONT);
                currentFontName = DEFAULT_FONT.info.family();
            } else {
                throw new IllegalStateException("Ei oletusfonttia!");
            }
        }
    }

    public static void load(FontFace fontFace) {
        if (renderer != null) {
            if (renderer instanceof CustomTextRenderer ctr) {
                if (ctr.fontFace.equals(fontFace)) return;
                ctr.destroy();
            }
            renderer = null;
        }
        try {
            renderer = new CustomTextRenderer(fontFace);
        } catch (Exception e) {
            e.printStackTrace();
            if (fontFace.equals(DEFAULT_FONT)) {
                // default failed; keep renderer null so callers can fallback manually
                renderer = null;
                return;
            }
            load(DEFAULT_FONT);
        }
    }

    public static TextRenderer getRenderer() {
        if (renderer == null) throw new IllegalStateException("Fonts not initialised");
        return renderer;
    }

    public static FontFamily getFamily(String name) {
        for (FontFamily f : FONT_FAMILIES) {
            if (f.getName().equalsIgnoreCase(name)) return f;
        }
        return null;
    }
}