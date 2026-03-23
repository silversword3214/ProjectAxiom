package silversword.axiom.client.modules.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.event.render.Render3DEvent;

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
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.utils.render.TextUtils;

public final class WaypointModule extends AxiomMod {
    private final Minecraft mc = Minecraft.getInstance();
    private final Vector3d pos = new Vector3d();

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
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
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

    @Subscribe
    private void onRender2D(Render2DEvent event) {
        if (event.getGuiGraphics() == null) return;
        if (!isEnabled() || !showWaypoints.get() || mc.level == null || mc.player == null) return;

        // Haetaan renderöijä ja core
        RenderCore core = RenderAPI.getInstance().getCore();
        double maxDist = maxRenderDistance.getValue();
        boolean showDist = showDistance.get();

        // Kameran tiedot ruutumuunnokseen
        Camera camera = mc.gameRenderer.getMainCamera();
        Matrix4f proj = mc.gameRenderer.getProjectionMatrix(
                mc.gameRenderer.getFov(camera, event.getTickDelta(), true));
        Matrix4f view = new Matrix4f()
                .rotate(camera.rotation().conjugate())
                .translate(-(float) camera.position().x,
                        -(float) camera.position().y,
                        -(float) camera.position().z);

        for (Waypoint wp : WaypointManager.getInstance().getAll()) {
            if (!wp.enabled) continue;
            double dist = mc.player.position().distanceTo(new Vec3(wp.x, wp.y, wp.z));
            if (dist > maxDist) continue;

            // Muunnetaan maailmapiste ruutukoordinaateiksi
            Vec3 worldPos = new Vec3(wp.x, wp.y + 0.5, wp.z);
            Vec3 screenPos = worldToScreen(proj, view, worldPos);
            if (screenPos == null) continue;

            // Piirretään pin ja tekstit käyttäen corea
            renderWaypointPin(core, wp, screenPos.x, screenPos.y, dist, showDist, wp.scale);
        }
    }

    private Vec3 worldToScreen(Matrix4f proj, Matrix4f view, Vec3 worldPos) {
        Vector4f clip = new Vector4f((float) worldPos.x, (float) worldPos.y, (float) worldPos.z, 1.0f);
        clip.mul(view).mul(proj);
        if (clip.w <= 0.0f) return null;

        float invW = 1.0f / clip.w;
        float ndcX = clip.x * invW;
        float ndcY = clip.y * invW;

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        float screenX = (ndcX * 0.5f + 0.5f) * width;
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * height;

        return new Vec3(screenX, screenY, 0);
    }

    private void renderWaypointPin(RenderCore core, Waypoint wp, double screenX, double screenY,
                                   double dist, boolean showDist, double scale) {
        double charWidth = TextUtils.CHAR_UNIT * scale;
        double charHeight = TextUtils.FONT_HEIGHT * scale;
        double padding = 2.0 * scale;

        String letter = wp.name.isEmpty() ? "?" : wp.name.substring(0, 1).toUpperCase();
        double textWidth = charWidth;
        double textHeight = charHeight;

        double pinWidth, pinHeight;
        if (wp.shape.equals("Circle")) {
            double diameter = Math.max(textWidth, textHeight) + 2 * padding;
            pinWidth = pinHeight = diameter;
        } else {
            pinWidth = textWidth + 2 * padding;
            pinHeight = textHeight + 2 * padding;
        }

        double pinX = screenX - pinWidth / 2;
        double pinY = screenY - pinHeight / 2;

        // Haetaan värit (int ARGB)
        int bgColor = wp.bgColor;
        int outlineColor = wp.outlineColor;
        int textColor = wp.color;

        // Tausta
        if (wp.showBg) {
            if (wp.shape.equals("Circle")) {
                core.addCircle((float) screenX, (float) screenY, (float) (pinWidth / 2), bgColor);
            } else if (wp.shape.equals("Square")) {
                core.addRect2D((float) pinX, (float) pinY, (float) pinWidth, (float) pinHeight, bgColor);
            } else {
                double radius = 3.0 * scale;
                core.addRoundedRect((float) pinX, (float) pinY, (float) pinWidth, (float) pinHeight, (float) radius, bgColor);
            }
        }

        // Reunus
        if (wp.showOutline) {
            double thickness = Math.max(1.0, scale);
            if (wp.shape.equals("Circle")) {
                core.addCircleOutline((float) screenX, (float) screenY, (float) (pinWidth / 2), (float) thickness, outlineColor);
            } else if (wp.shape.equals("Square")) {
                core.addRectOutline2D((float) pinX, (float) pinY, (float) pinWidth, (float) pinHeight, (float) thickness, outlineColor);
            } else {
                double radius = 3.0 * scale;
                core.addRoundedRectOutline((float) pinX, (float) pinY, (float) pinWidth, (float) pinHeight, (float) radius, (float) thickness, outlineColor);
            }
        }

        // Pääkirjain
        TextRenderer text = TextRenderer.get();
        text.begin(scale, false, true);
        double letterX = screenX - textWidth / 2;
        double letterY = screenY - textHeight / 2;
        text.render(letter, letterX, letterY, new Color(textColor), true);
        text.end();

        // Etäisyysteksti
        if (showDist) {
            int d = (int) Math.round(dist);
            String distText = d + "m";
            double distScale = 0.7;
            double distCharWidth = TextUtils.CHAR_UNIT * distScale;
            double distWidth = distText.length() * distCharWidth;
            double distX = screenX - distWidth / 2;
            double distY = screenY + pinHeight / 2 + 2.0 * scale;

            text.begin(distScale, false, false);
            text.render(distText, distX, distY, new Color(0xFFFFFFFF), true);
            text.end();
        }
    }

    private void renderEdgeIndicator(Waypoint wp, double dist, boolean showDist, float scale) {
        // Compute direction from player to waypoint
        Vec3 player = mc.player.position();
        Vec3 to = new Vec3(wp.x, wp.y, wp.z).subtract(player).normalize();
        // Project onto screen edges? This is more complex. For simplicity, we can just not render edge indicators for now.
        // You can implement a screen edge arrow later.
    }
}