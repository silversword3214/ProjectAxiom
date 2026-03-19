package silversword.axiom.client.modules.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.WaypointManager;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.waypoints.Waypoint;
import silversword.axiom.client.modules.waypoints.WaypointManagerWindow;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.render.NametagUtils;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.utils.render.TextUtils;

public final class WaypointModule extends AxiomMod {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Global settings that affect rendering
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
    private final Vector3d pos = new Vector3d();

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

    // Called when the gear button in ModuleRow is clicked
    public void openManager() {
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        // Avataan leveämpi ikkuna (600x400)
        factory.openPopupWindow("waypoint_manager", "Waypoint Manager",
                (sw - 600) / 2, (sh - 400) / 2, 600, 400,
                new WaypointManagerWindow(this));
    }

    @Override
    protected void onTick() {

    }

    @AxiomEvent
    private void onRender3D(Render3DEvent event) {
        NametagUtils.onRender(RenderUtils.view);
    }

    // WaypointModule.java (korjattu renderöinti)

    @AxiomEvent
    private void onRender2D(Render2DEvent event) {
        if (event.drawContext == null) return;
        if (!isEnabled() || !showWaypoints.get() || mc.world == null || mc.player == null) return;

        double maxDist = maxRenderDistance.getValue();
        boolean showDist = showDistance.get();

        for (Waypoint wp : WaypointManager.getInstance().getAll()) {
            if (!wp.enabled) continue;
            double dist = mc.player.getEntityPos().distanceTo(new Vec3d(wp.x, wp.y, wp.z));
            if (dist > maxDist) continue;

            pos.set(wp.x, wp.y + 0.5, wp.z);
            if (!NametagUtils.worldToScreen(pos, (float) wp.scale)) continue;
            NametagUtils.scale = (float) wp.scale;
            NametagUtils.begin(pos, event.drawContext);

            // Piirretään pin ja tekstit
            renderWaypointPin(wp, dist, showDist);

            NametagUtils.end(event.drawContext);
        }
    }

    private void renderWaypointPin(Waypoint wp, double dist, boolean showDist) {
        double scale = wp.scale;
        double charWidth = TextUtils.CHAR_UNIT * scale;
        double charHeight = TextUtils.FONT_HEIGHT * scale;
        double padding = 2.0 * scale;

        String letter = wp.name.isEmpty() ? "?" : wp.name.substring(0, 1).toUpperCase();
        double textWidth = charWidth;
        double textHeight = charHeight;

        // Pinin koko muodon mukaan
        double pinWidth, pinHeight;
        if (wp.shape.equals("Circle")) {
            double diameter = Math.max(textWidth, textHeight) + 2 * padding;
            pinWidth = pinHeight = diameter;
        } else {
            pinWidth = textWidth + 2 * padding;
            pinHeight = textHeight + 2 * padding;
        }

        double pinX = -pinWidth / 2;
        double pinY = -pinHeight / 2;

        Renderer2D.COLOR.begin();
        TextRenderer textRenderer = TextRenderer.get();

        // Tausta
        if (wp.showBg) {
            Color bg = new Color(wp.bgColor);
            if (wp.shape.equals("Circle")) {
                Renderer2D.COLOR.drawCircle(0, 0, pinWidth / 2, bg);
            } else if (wp.shape.equals("Square")) {
                Renderer2D.COLOR.quad(pinX, pinY, pinWidth, pinHeight, bg);
            } else {
                double radius = 3.0 * scale;
                Renderer2D.COLOR.drawRoundedRect(pinX, pinY, pinWidth, pinHeight, radius, bg);
            }
        }

        // Reunus
        if (wp.showOutline) {
            Color outline = new Color(wp.outlineColor);
            if (wp.shape.equals("Circle")) {
                Renderer2D.COLOR.drawCircleOutline(0, 0, pinWidth / 2, outline, 1.0);
            } else if (wp.shape.equals("Square")) {
                Renderer2D.COLOR.boxLines(pinX, pinY, pinWidth, pinHeight, outline);
            } else {
                double radius = 3.0 * scale;
                Renderer2D.COLOR.drawRoundedRectOutline(pinX, pinY, pinWidth, pinHeight, radius, outline, 1.0);
            }
        }

        // Pääkirjain (iso ja tarkka)
        textRenderer.beginBig();
        double letterX = -textWidth / 2;
        double letterY = -textHeight / 2;
        textRenderer.render(letter, letterX, letterY, new Color(wp.color), true);
        textRenderer.end();

        // Etäisyysteksti (pienempi, varjolla)
        if (showDist) {
            int d = (int) Math.round(dist);
            String distText = d + "m";
            double distScale = 0.7; // pienempi skaala
            double distCharWidth = TextUtils.CHAR_UNIT * distScale;
            double distWidth = distText.length() * distCharWidth;
            double distX = -distWidth / 2;
            double distY = pinHeight / 2 + 2.0 * scale; // hieman pinin alapuolelle

            textRenderer.begin(distScale, false, false);
            textRenderer.render(distText, distX, distY, new Color(0xFFFFFFFF), true);
            textRenderer.end();
        }

        Renderer2D.COLOR.render();
    }

    private void renderEdgeIndicator(Waypoint wp, double dist, boolean showDist, float scale) {
        // Compute direction from player to waypoint
        Vec3d player = mc.player.getEntityPos();
        Vec3d to = new Vec3d(wp.x, wp.y, wp.z).subtract(player).normalize();
        // Project onto screen edges? This is more complex. For simplicity, we can just not render edge indicators for now.
        // You can implement a screen edge arrow later.
    }
}