package silversword.axiom.client.gui.core;

public final class Theme {

    // Spacing / layout
    public int padding = 6;
    public int innerPadding = 4;
    public int headerHeight = 18;
    public int rowHeight = 16;

    // Rounded corners
    public int radius = 6;

    // Colors (ARGB)
    public int panel  = 0xCC000000;
    public int header = 0xDD111111;
    public int border = 0xFF222222;
    public int knob = 0xFF141414;
    public int text = 0xFFFFFFFF;
    public int textDim = 0xFFAAAAAA;
    public int accent = 0xFF8A2BE2;
    public int button = 0x88222222;
    public int buttonHover = 0xAA333333;
    public int toggleOff = 0xFF444444;
    public int toggleOn = 0xFF2E7D32;
    public int sliderTrack = 0xFF333333;
    public int sliderFill = 0xFF8A2BE2;
    public int scrollbar = 0xAA2B2B2B;
    public int scrollbarHover = 0xFF8A2BE2;


    public Theme() {}

    public int accentColor() {
        return accent;
    }

    public Theme copy() {
        Theme t = new Theme();
        t.padding = padding;
        t.innerPadding = innerPadding;
        t.headerHeight = headerHeight;
        t.rowHeight = rowHeight;
        t.radius = radius;
        t.panel = panel;
        t.header = header;
        t.border = border;
        t.knob = knob;
        t.text = text;
        t.textDim = textDim;
        t.accent = accent;
        t.button = button;
        t.buttonHover = buttonHover;
        t.toggleOff = toggleOff;
        t.toggleOn = toggleOn;
        t.sliderTrack = sliderTrack;
        t.sliderFill = sliderFill;
        t.scrollbar = scrollbar;
        t.scrollbarHover = scrollbarHover;
        return t;
    }
}