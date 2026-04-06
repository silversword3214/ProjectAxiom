package silversword.axiom.client.modules.movement;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.packets.PacketEvent;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.BlinkManager;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.misc.ShapeModeEnum;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.utils.render.TextUtils;
import org.lwjgl.glfw.GLFW;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class Blink extends AxiomMod implements KeybindConfigurable {
    private final RenderCore core = RenderAPI.getInstance().getCore();
    private final SettingKeybind toggleKey;
    private final SettingMode mode; // Käytetään sinun SettingMode-luokkaasi
    private final SettingNumber textScale;

    private Vec3 startPos;
    private float startYaw, startPitch;
    private long startTime = 0;

    public Blink() {
        super("Blink", "Holds movement packets to simulate lag", ModuleCategory.MOVEMENT);

        // Luodaan moodit: "Post" (lähetä) ja "Cancel" (peruuta)
        mode = new SettingMode("Mode", new String[]{"Post", "Cancel"}, "Post");
        addSetting(mode);

        textScale = new SettingNumber("Text Scale", 0.5, 3.0, 0.1, 1);
        addSetting(textScale);

        toggleKey = new SettingKeybind("Toggle Key", GLFW.GLFW_KEY_UNKNOWN);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        if (mc.player == null) return;

        startTime = System.currentTimeMillis();
        startPos = mc.player.position();
        startYaw = mc.player.getYRot();
        startPitch = mc.player.getXRot();

        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        BlinkManager.getInstance().start(mc.player, tickDelta);
    }

    @Override
    protected void onDisable() {
        if (mc.player == null) return;

        String currentMode = mode.getMode();

        if (currentMode.equals("Cancel")) {
            // MOODI: Cancel - Tyhjennä paketit ja palauta sijainti
            BlinkManager.getInstance().cancel();
            mc.player.snapTo(startPos.x, startPos.y, startPos.z, startYaw, startPitch);
            mc.player.displayClientMessage(Component.literal("§cBlink: Movement Cancelled"), true);
        } else {
            // MOODI: Post - Lähetä paketit palvelimelle (pysyt uudessa paikassa)
            BlinkManager.getInstance().stop();
            mc.player.displayClientMessage(Component.literal("§aBlink: Movement Posted"), true);
        }

        startPos = null;
    }

    @Override
    protected void onTick() {

    }

    @Subscribe
    private void onPacketSend(PacketEvent.Send event) {
        // BlinkManager hoitaa pakettien sieppaamisen ja jonoituksen
        if (BlinkManager.getInstance().handlePacket(event.getPacket())) {
            event.setCancelled(true);
        }
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        BlinkManager blink = BlinkManager.getInstance();
        if (blink.isBlinking() && blink.getGhostPos() != null) {
            Vec3 pos = blink.getGhostPos();
            Renderer3D renderer = event.getRenderer();

            double minX = pos.x - 0.3;
            double minY = pos.y;
            double minZ = pos.z - 0.3;
            double maxX = pos.x + 0.3;
            double maxY = pos.y + 1.8;
            double maxZ = pos.z + 0.3;

            renderer.drawBox(minX, minY, minZ, maxX, maxY, maxZ,
                    0x44FFFFFF,
                    0xFFFFFFFF,
                    ShapeModeEnum.BOTH, 0);
        }
    }

    @Subscribe
    private void onRender2D(Render2DEvent event) {
        if (!BlinkManager.getInstance().isBlinking()) return;

        // Lasketaan aika sekunteina (esim. 1250ms -> 1.3s)
        double seconds = (System.currentTimeMillis() - startTime) / 1000.0;
        String text = String.format("Blink: %.1fs (%s)", seconds, mode.getMode());

        double scale = textScale.getValue();

        // Haetaan ruudun keskipiste
        int centerX = event.getScreenWidth() / 2;
        int centerY = event.getScreenHeight() / 2;

        // Sijoitetaan crosshairin oikealle puolelle
        // Lisätään pieni offset (esim. 10 pikseliä), jotta teksti ei ole crosshairin päällä
        int x = centerX + 10;
        int y = centerY - (int)(TextUtils.getHeight() * scale / 2); // Keskitetään pystysuunnassa tekstiin nähden

        TextRenderer tr = TextRenderer.get();
        tr.begin(scale, false, true);
        // Piirretään teksti (false = ei keskitystä, koska haluamme sen alkavan x-pisteestä)
        tr.render(text, x, y, Color.WHITE, true);
        tr.end();
    }
}