package silversword.axiom.client.gui.window;

public final class WindowIds {
    private WindowIds() {}

    public static final String MAIN = "main";

    public static String settings(String moduleId) {
        return "settings:" + moduleId;
    }

    public static final String HUD_EDITOR = "hud_editor";
    public static final String HUD_PALETTE = "hud_palette";
}
