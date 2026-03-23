package silversword.axiom.client.modules.movement;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.packets.PacketEvent;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.render.font.TextRenderer;

import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.utils.render.TextUtils;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class Blink extends AxiomMod implements KeybindConfigurable {
    private final RenderCore core = RenderAPI.getInstance().getCore();
    private final SettingKeybind toggleKey;
    private final SettingNumber textScale;
    private final SettingNumber limit;

    private List<Packet<?>> packetQueue = new ArrayList<>();
    private long startTime = 0;
    private boolean active = false;
    private Vec3 startPos;
    private float startYaw;
    private float startPitch;

    public Blink() {
        super("Blink", "Temporarily stops motion packets and undoes movement when disabled", ModuleCategory.MOVEMENT);

        limit = new SettingNumber("Limit", 0, 500, 1, 0);
        addSetting(limit);

        textScale = new SettingNumber("Text Scale", 0.5, 3.0, 0.1, 1.5);
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
        active = true;
        startTime = System.currentTimeMillis();
        packetQueue.clear();

        if (mc.player != null) {
            startPos = mc.player.position();
            startYaw = mc.player.getYRot();
            startPitch = mc.player.getXRot();
        }
    }

    @Override
    protected void onDisable() {
        active = false;

        // Undo: palautetaan alkuperäiseen paikkaan
        if (startPos != null && mc.player != null) {
            mc.player.snapTo(startPos.x, startPos.y, startPos.z, startYaw, startPitch);
            mc.player.displayClientMessage(Component.literal("§aBlink: Undone movement"), true);
        }

        packetQueue.clear();
        startPos = null;
    }

    @Override
    protected void onTick() {

    }

    @Subscribe
    private void onPacketSend(PacketEvent.Send event) {
        if (!active) return;

        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundMovePlayerPacket) {
            packetQueue.add(packet);
            event.setCancelled(true);

            int limitVal = (int) limit.getValue();
            if (limitVal > 0 && packetQueue.size() >= limitVal) {
                // Lähetetään ilmoitus, mutta ei tehdä mitään muuta (paketit jäävät jonoon)
                mc.player.displayClientMessage(Component.literal("§eBlink: Packet limit reached (" + limitVal + ")"), true);
            }
        }
    }

    @Subscribe
    private void onRender2D(Render2DEvent event) {
        if (!active || mc.player == null) return;

        long elapsed = System.currentTimeMillis() - startTime;
        int packetCount = packetQueue.size();
        int limitVal = (int) limit.getValue();

        String text;
        if (limitVal > 0) {
            text = String.format("Blink: %dms [%d/%d]", elapsed, packetCount, limitVal);
        } else {
            text = String.format("Blink: %dms [%d]", elapsed, packetCount);
        }

        TextRenderer tr = TextRenderer.get();
        double scale = textScale.getValue();

        double textWidth = text.length() * TextUtils.CHAR_UNIT * scale;
        double textHeight = TextUtils.FONT_HEIGHT * scale;

        // Haetaan ruudun koko eventistä
        int scaledWidth = event.getScreenWidth();
        int scaledHeight = event.getScreenHeight();

        // Hotbar on yleensä 22px korkea ja sijaitsee 3px pohjasta
        int hotbarY = scaledHeight - 22;
        int x = (int) ((scaledWidth - textWidth) / 2);
        int y = hotbarY - (int) textHeight - 10;

        double padding = 4 * scale;
        double bgX = x - padding;
        double bgY = y - padding;
        double bgWidth = textWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double radius = 3 * scale;
        double thickness = Math.max(1.0, scale);

        // Piirrä tausta coren kautta
        int bgArgb = new Color(0, 0, 0, 150).getARGB();
        core.addRoundedRect((float) bgX, (float) bgY, (float) bgWidth, (float) bgHeight, (float) radius, bgArgb);

        // Piirrä teksti
        tr.begin(scale, false, true);
        tr.render(text, x, y, Color.WHITE, false);
        tr.end();
    }
}