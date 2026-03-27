package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.WaypointManager;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.waypoints.Waypoint;
import silversword.axiom.client.modules.waypoints.WaypointManagerWindow;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.render.NametagUtils;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.utils.render.TextUtils;
import org.joml.Matrix4f;

public final class WaypointModule extends AxiomMod {
    private final Minecraft mc = Minecraft.getInstance();

    public final SettingBoolean showWaypoints = new SettingBoolean("Show Waypoints", true);
    public final SettingBoolean showDistance = new SettingBoolean("Show Distance", true);
    public final SettingNumber maxRenderDistance = new SettingNumber("Max Distance", 100, 5000, 10, 1000);
    public final SettingNumber scale = new SettingNumber("Scale", 0.5, 3.0, 0.1, 1.0);
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    public final SettingBoolean showBackground = new SettingBoolean("Show Background", true);
    public final SettingColor backgroundColor = new SettingColor("Background Color", new Color(0,0,0,100));
    public final SettingBoolean showOutline = new SettingBoolean("Show Outline", true);
    public final SettingColor outlineColor = new SettingColor("Outline Color", new Color(255,255,255,255));
    public final SettingMode shape = new SettingMode("Shape", new String[]{"Circle", "Square", "Rounded"}, "Rounded");

    public WaypointModule() {
        super("Waypoints", "Manage and display custom waypoints", ModuleCategory.RENDER);
        addHiddenSetting(toggleKey);
        addSetting(showWaypoints);
        addSetting(showDistance);
        addSetting(maxRenderDistance);
        addSetting(scale);
        addSetting(showBackground);
        addHiddenSetting(backgroundColor.getSetting());
        addSetting(showOutline);
        addHiddenSetting(outlineColor.getSetting());
        addSetting(shape);
    }

    public void openManager() {
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        factory.openPopupWindow("waypoint_manager", "Waypoint Manager",
                (sw - 600) / 2, (sh - 400) / 2, 600, 400,
                new WaypointManagerWindow(this));
    }

    @Subscribe
    private void onRender2D(Render2DEvent event) {
        if (event.getGuiGraphics() == null) return;
        if (!isEnabled() || !showWaypoints.get() || mc.level == null || mc.player == null) return;

        RenderCore core = RenderAPI.getInstance().getCore();
        double maxDist = maxRenderDistance.getValue();

        for (Waypoint wp : WaypointManager.getInstance().getAll()) {
            if (!wp.enabled) continue;

            double dist = mc.player.position().distanceTo(new Vec3(wp.x, wp.y, wp.z));
            if (dist > maxDist) continue;

            // Käytetään NametagUtilsia maailmapisteen muuntamiseen (huomioi North-bugin ja FOV:n)
            Vec3 worldPos = new Vec3(wp.x, wp.y + 0.5, wp.z);
            Vec3 screenPos = NametagUtils.worldToScreen(worldPos);

            if (screenPos == null) continue; // Piste on kameran takana

            // screenPos.z sisältää w-arvon (etäisyyden), jota voidaan käyttää lisäskaalaukseen jos halutaan
            renderWaypointPin(core, wp, screenPos.x, screenPos.y, dist, wp.scale * scale.getValue());
        }
    }

    private void renderWaypointPin(RenderCore core, Waypoint wp, double screenX, double screenY, double dist, double finalScale) {
        // 1. Lasketaan mitat skaalaamattomana
        double textWidth = TextUtils.CHAR_UNIT;
        double textHeight = TextUtils.FONT_HEIGHT;
        double padding = 2.0;

        double pinWidth, pinHeight;
        if (wp.shape.equals("Circle")) {
            pinWidth = pinHeight = (Math.max(textWidth, textHeight) + 2 * padding) * finalScale;
        } else {
            pinWidth = (textWidth + 2 * padding) * finalScale;
            pinHeight = (textHeight + 2 * padding) * finalScale;
        }

        double pinX = screenX - pinWidth / 2;
        double pinY = screenY - pinHeight / 2;

        // 2. Tausta ja reunus (käyttäen skaalattuja mittoja)
        if (wp.showBg) {
            if (wp.shape.equals("Circle")) {
                core.addCircle((float) screenX, (float) screenY, (float) (pinWidth / 2), wp.bgColor);
            } else if (wp.shape.equals("Square")) {
                core.addRect2D((float) pinX, (float) pinY, (float) pinWidth, (float) pinHeight, wp.bgColor);
            } else {
                float radius = (float) (3.0 * finalScale);
                core.addRoundedRect((float) pinX, (float) pinY, (float) pinWidth, (float) pinHeight, radius, wp.bgColor);
            }
        }

        if (wp.showOutline) {
            float thickness = (float) Math.max(1.0, finalScale);
            if (wp.shape.equals("Circle")) {
                core.addCircleOutline((float) screenX, (float) screenY, (float) (pinWidth / 2), thickness, wp.outlineColor);
            } else if (wp.shape.equals("Square")) {
                core.addRectOutline2D((float) pinX, (float) pinY, (float) pinWidth, (float) pinHeight, thickness, wp.outlineColor);
            } else {
                float radius = (float) (3.0 * finalScale);
                core.addRoundedRectOutline((float) pinX, (float) pinY, (float) pinWidth, (float) pinHeight, radius, thickness, wp.outlineColor);
            }
        }

        // 3. Pääkirjain (skaalattuna oikeaan paikkaan)
        String letter = wp.name.isEmpty() ? "?" : wp.name.substring(0, 1).toUpperCase();
        TextRenderer text = TextRenderer.get();
        text.begin(finalScale, false, true);

        // Jaetaan koordinaatit scalella, koska text.begin skaalaa ne takaisin
        float letterX = (float) ((screenX - (textWidth * finalScale) / 2) / finalScale);
        float letterY = (float) ((screenY - (textHeight * finalScale) / 2) / finalScale);

        text.render(letter, letterX, letterY, new Color(wp.color), true);
        text.end();

        // 4. Etäisyysteksti
        if (showDistance.get()) {
            String distText = (int) Math.round(dist) + "m";
            double distScale = finalScale * 0.7; // Suhteellinen skaalaus waypointin kokoon

            text.begin(distScale, false, false);
            // Lasketaan leveys ja paikka
            float distW = (float) (distText.length() * TextUtils.CHAR_WIDTH);
            float dX = (float) ((screenX - (distW * distScale) / 2) / distScale);
            float dY = (float) ((screenY + pinHeight / 2 + 2 * finalScale) / distScale);

            text.render(distText, dX, dY, new Color(0xFFFFFFFF), true);
            text.end();
        }
    }

    @Override
    protected void onTick() {}
}