package silversword.axiom.client.modules;

public enum ModuleCategory {
    ALL("All"),
    HIDDEN("Hidden"),
    MOVEMENT("Movement"),
    COMBAT("Combat"),
    RENDER("Render"),
    PLAYER("Player"),
    WORLD("World"),
    MISC("Misc");



    public final String displayName;

    ModuleCategory(String displayName) {
        this.displayName = displayName;
    }

    public static ModuleCategory fromDisplay(String s) {
        if (s == null) return ALL;
        for (ModuleCategory c : values()) {
            if (c.displayName.equalsIgnoreCase(s)) return c;
            if (c.name().equalsIgnoreCase(s)) return c;
        }
        return ALL;
    }
}
