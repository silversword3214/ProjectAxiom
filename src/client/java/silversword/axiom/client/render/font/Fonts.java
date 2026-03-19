package silversword.axiom.client.render.font;

import silversword.axiom.client.utils.render.FontUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public final class Fonts {
    public static final String[] BUILTIN_FONTS = { "jetbrains_mono" };

    public static String DEFAULT_FONT_FAMILY;
    public static FontFace DEFAULT_FONT;

    public static final List<FontFamily> FONT_FAMILIES = new ArrayList<>();
    private static TextRenderer renderer;

    private Fonts() {}

    public static void refresh() {
        FONT_FAMILIES.clear();

        for (String builtin : BUILTIN_FONTS) {
            FontUtils.loadBuiltin(FONT_FAMILIES, builtin);
        }

        for (String path : FontUtils.getSearchPaths()) {
            FontUtils.loadSystem(FONT_FAMILIES, new java.io.File(path));
        }

        FONT_FAMILIES.sort(Comparator.comparing(FontFamily::getName));

        DEFAULT_FONT_FAMILY = FontUtils.getBuiltinFontInfo(BUILTIN_FONTS[0]).family();
        DEFAULT_FONT = getFamily(DEFAULT_FONT_FAMILY).get(FontInfo.Type.Regular);

        // attempt to load our custom renderer; if it fails, fall back to vanilla
        try {
            load(DEFAULT_FONT);
        } catch (Exception e) {
            System.err.println("Fonts.refresh: unable to load custom font, using vanilla renderer");
            e.printStackTrace();
            renderer = VanillaTextRenderer.INSTANCE;
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
