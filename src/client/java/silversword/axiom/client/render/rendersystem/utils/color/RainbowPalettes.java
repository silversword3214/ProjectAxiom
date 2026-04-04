package silversword.axiom.client.render.rendersystem.utils.color;

public final class RainbowPalettes {
    public static final RainbowPalette NEON = new RainbowPalette("Neon", new int[]{
            0xFFFF00FF, // pinkki
            0xFF00FFFF, // syaani
            0xFF00FF00, // vihreä
            0xFFFFFF00, // keltainen
            0xFFFF6600, // oranssi
    });

    public static final RainbowPalette PASTEL = new RainbowPalette("Pastel", new int[]{
            0xFFFFB6C1, // vaaleanpunainen
            0xFFB0E0E6, // jäänsininen
            0xFFC0F0C0, // minttu
            0xFFFFDAB9, // persikka
            0xFFE6E6FA, // laventeli
    });

    public static final RainbowPalette DARK = new RainbowPalette("Dark", new int[]{
            0xFF880000, // tummanpunainen
            0xFF008800, // tummanvihreä
            0xFF000088, // tummansininen
            0xFF884400, // ruskea
            0xFF440088, // violetti
    });

    public static final RainbowPalette LIGHT = new RainbowPalette("Light", new int[]{
            0xFFFFFFFF, // valkoinen
            0xFFEEEEEE,
            0xFFDDDDDD,
            0xFFCCCCCC,
            0xFFBBBBBB,
    });

    public static final RainbowPalette WARM = new RainbowPalette("Warm", new int[]{
            0xFFFF4500, // oranssinpunainen
            0xFFFF8C00, // oranssi
            0xFFFFD700, // kultainen
            0xFFFFA07A, // vaalea lohi
            0xFFFF6347, // tomaatti
    });

    public static final RainbowPalette COOL = new RainbowPalette("Cool", new int[]{
            0xFF1E90FF, // dodger blue
            0xFF00CED1, // turkoosi
            0xFF3CB371, // merenvihreä
            0xFF9370DB, // keskivioletti
            0xFF87CEEB, // taivaansininen
    });

    public static final RainbowPalette MONOCHROME = new RainbowPalette("Monochrome", new int[]{
            0xFF404040,
            0xFF606060,
            0xFF808080,
            0xFFA0A0A0,
            0xFFC0C0C0,
    });

    public static final RainbowPalette[] ALL = {
            NEON, PASTEL, DARK, LIGHT, WARM, COOL, MONOCHROME
    };

    public static RainbowPalette getByName(String name) {
        for (RainbowPalette p : ALL) {
            if (p.getName().equalsIgnoreCase(name)) return p;
        }
        return NEON;
    }
}