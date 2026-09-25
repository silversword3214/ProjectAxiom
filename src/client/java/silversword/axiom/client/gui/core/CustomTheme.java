package silversword.axiom.client.gui.core;

import java.util.Properties;

/** Käyttäjän luoma/muokattava teema. Kaikki kentät public, jotta editori voi käsitellä niitä suoraan. */
public final class CustomTheme {

    public String name = "Custom";

    public int panel, header, border, knob, text, textDim, accent,
            button, buttonHover, toggleOff, toggleOn,
            sliderTrack, sliderFill, scrollbar, scrollbarHover;

    public int radius = 6;
    public int padding = 6;
    public int innerPadding = 4;
    public int headerHeight = 18;
    public int rowHeight = 16;

    public static CustomTheme from(String name, Theme t) {
        CustomTheme c = new CustomTheme();
        c.name         = name;
        c.panel        = t.panel;
        c.header       = t.header;
        c.border       = t.border;
        c.knob         = t.knob;
        c.text         = t.text;
        c.textDim      = t.textDim;
        c.accent       = t.accent;
        c.button       = t.button;
        c.buttonHover  = t.buttonHover;
        c.toggleOff    = t.toggleOff;
        c.toggleOn     = t.toggleOn;
        c.sliderTrack  = t.sliderTrack;
        c.sliderFill   = t.sliderFill;
        c.scrollbar    = t.scrollbar;
        c.scrollbarHover = t.scrollbarHover;
        c.radius       = t.radius;
        c.padding      = t.padding;
        c.innerPadding = t.innerPadding;
        c.headerHeight = t.headerHeight;
        c.rowHeight    = t.rowHeight;
        return c;
    }

    public Theme toTheme() {
        Theme t = new Theme();
        t.panel        = panel;
        t.header       = header;
        t.border       = border;
        t.knob         = knob;
        t.text         = text;
        t.textDim      = textDim;
        t.accent       = accent;
        t.button       = button;
        t.buttonHover  = buttonHover;
        t.toggleOff    = toggleOff;
        t.toggleOn     = toggleOn;
        t.sliderTrack  = sliderTrack;
        t.sliderFill   = sliderFill;
        t.scrollbar    = scrollbar;
        t.scrollbarHover = scrollbarHover;
        t.radius       = radius;
        t.padding      = padding;
        t.innerPadding = innerPadding;
        t.headerHeight = headerHeight;
        t.rowHeight    = rowHeight;
        return t;
    }

    public void writeToProps(Properties p, String prefix) {
        p.setProperty(prefix + "name", name);
        p.setProperty(prefix + "panel", String.valueOf(panel));
        p.setProperty(prefix + "header", String.valueOf(header));
        p.setProperty(prefix + "border", String.valueOf(border));
        p.setProperty(prefix + "knob", String.valueOf(knob));
        p.setProperty(prefix + "text", String.valueOf(text));
        p.setProperty(prefix + "textDim", String.valueOf(textDim));
        p.setProperty(prefix + "accent", String.valueOf(accent));
        p.setProperty(prefix + "button", String.valueOf(button));
        p.setProperty(prefix + "buttonHover", String.valueOf(buttonHover));
        p.setProperty(prefix + "toggleOff", String.valueOf(toggleOff));
        p.setProperty(prefix + "toggleOn", String.valueOf(toggleOn));
        p.setProperty(prefix + "sliderTrack", String.valueOf(sliderTrack));
        p.setProperty(prefix + "sliderFill", String.valueOf(sliderFill));
        p.setProperty(prefix + "scrollbar", String.valueOf(scrollbar));
        p.setProperty(prefix + "scrollbarHover", String.valueOf(scrollbarHover));
        p.setProperty(prefix + "radius", String.valueOf(radius));
        p.setProperty(prefix + "padding", String.valueOf(padding));
        p.setProperty(prefix + "innerPadding", String.valueOf(innerPadding));
        p.setProperty(prefix + "headerHeight", String.valueOf(headerHeight));
        p.setProperty(prefix + "rowHeight", String.valueOf(rowHeight));
    }

    public static CustomTheme readFromProps(Properties p, String prefix) {
        CustomTheme c = new CustomTheme();
        c.name         = p.getProperty(prefix + "name", "Custom");
        c.panel        = parseInt(p, prefix + "panel",        0xCC000000);
        c.header       = parseInt(p, prefix + "header",       0xDD111111);
        c.border       = parseInt(p, prefix + "border",       0xFF222222);
        c.knob         = parseInt(p, prefix + "knob",         0xFF141414);
        c.text         = parseInt(p, prefix + "text",         0xFFFFFFFF);
        c.textDim      = parseInt(p, prefix + "textDim",      0xFFAAAAAA);
        c.accent       = parseInt(p, prefix + "accent",       0xFF8A2BE2);
        c.button       = parseInt(p, prefix + "button",       0x88222222);
        c.buttonHover  = parseInt(p, prefix + "buttonHover",  0xAA333333);
        c.toggleOff    = parseInt(p, prefix + "toggleOff",    0xFF444444);
        c.toggleOn     = parseInt(p, prefix + "toggleOn",     0xFF2E7D32);
        c.sliderTrack  = parseInt(p, prefix + "sliderTrack",  0xFF333333);
        c.sliderFill   = parseInt(p, prefix + "sliderFill",   0xFF8A2BE2);
        c.scrollbar    = parseInt(p, prefix + "scrollbar",    0xAA2B2B2B);
        c.scrollbarHover = parseInt(p, prefix + "scrollbarHover", 0xFF8A2BE2);
        c.radius       = parseInt(p, prefix + "radius",       6);
        c.padding      = parseInt(p, prefix + "padding",      6);
        c.innerPadding = parseInt(p, prefix + "innerPadding", 4);
        c.headerHeight = parseInt(p, prefix + "headerHeight", 18);
        c.rowHeight    = parseInt(p, prefix + "rowHeight",    16);
        return c;
    }

    private static int parseInt(Properties p, String key, int def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try { return (int) Long.parseLong(v.trim()); } catch (Exception e) { return def; }
    }
}