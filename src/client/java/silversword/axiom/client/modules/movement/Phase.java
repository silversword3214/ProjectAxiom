package silversword.axiom.client.modules.movement;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.packets.PacketEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;

import java.util.ArrayList;
import java.util.List;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class Phase extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    public final SettingMode mode = new SettingMode("Mode", new String[]{"Single player", "Blink"}, "Single player");
    public final SettingNumber noClipSpeed = new SettingNumber("NoClip Speed", 0.1, 1.0, 0.05, 0.5);

    private static Phase instance;
    private static boolean moduleEnabled = false;

    private final List<ServerboundMovePlayerPacket> packetBuffer = new ArrayList<>();
    private boolean isBlinking = false;
    private int blinkTickCounter = 0;
    private static final int BLINK_INTERVAL = 3;
    private Vec3 serverPosition = null;
    private int blinkTimer = 0;
    private static final int BLINK_DELAY = 2;




    public Phase() {
        super("Phase", "Walk through walls", ModuleCategory.MOVEMENT);
        instance = this;
        addHiddenSetting(toggleKey);
        addSetting(mode);

    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        moduleEnabled = true;
        String currentMode = mode.getMode();
        if ("Blink".equals(currentMode)) {
            startBlink();
        }
    }

    @Override
    protected void onDisable() {
        moduleEnabled = false;
        if (isBlinking) {
            stopBlinkAndSendPackets();
        }
    }



    @Override
    protected void onTick() {
        if (!moduleEnabled) return;

        String currentMode = mode.getMode();
        if ("Blink".equals(currentMode)) {
            handleBlinkTick();
        }
    }

    private void startBlink() {
        isBlinking = true;
        packetBuffer.clear();
        blinkTickCounter = 0;
    }

    private void handleBlinkTick() {
        if (!isBlinking) return;
        blinkTimer++;
        if (blinkTimer >= BLINK_DELAY) {
            flushBlinkBuffer();
            blinkTimer = 0;
        }
    }

    private void flushBlinkBuffer() {
        if (packetBuffer.isEmpty()) return;
        // Lähetetään kaikki puskuroidut paketit yhdessä erässä
        for (ServerboundMovePlayerPacket packet : packetBuffer) {
            // Käytetään suoraa connectionia ilman eventtiä
            if (mc.getConnection() != null) {
                mc.getConnection().send(packet);
            }
        }
        packetBuffer.clear();
    }

    private void stopBlinkAndSendPackets() {
        if (!isBlinking) return;
        isBlinking = false;
        flushBlinkBuffer();
    }

    public static boolean isModuleEnabled() {
        return moduleEnabled;
    }

    public static boolean isNoClipMode() {
        return moduleEnabled && instance != null && "NoClip".equals(instance.mode.getMode());
    }

    public static boolean isBlinkMode() {
        return moduleEnabled && instance != null && "Blink".equals(instance.mode.getMode());
    }

    @Subscribe
    public void onPacketReceive(PacketEvent.Received event) {
        if (!moduleEnabled) return;
        if (!"NoClip".equals(mode.getMode())) return;

        if (event.getPacket() instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket) {
            event.setCancelled(true);
            // Päivitetään serverPosition vastaamaan clientin sijaintia
            if (mc.player != null) {
                serverPosition = mc.player.position();
            }
        }
    }

    @Subscribe
    public void onPacketSend(PacketEvent.Send event) {
        if (!moduleEnabled) return;
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket)) return;

        String currentMode = mode.getMode();

        if ("Blink".equals(currentMode)) {
            packetBuffer.add((ServerboundMovePlayerPacket) event.getPacket());
            event.setCancelled(true);
        }
    }
}