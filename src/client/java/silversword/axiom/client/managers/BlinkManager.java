package silversword.axiom.client.managers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.render.rendersystem.utils.render.ModelHelper;
import silversword.axiom.client.utils.render.CapturedModelState;

import java.util.concurrent.ConcurrentLinkedQueue;



public class BlinkManager {
    private static final BlinkManager INSTANCE = new BlinkManager();
    public static BlinkManager getInstance() { return INSTANCE; }
    private CapturedModelState capturedModel = null;

    private final ConcurrentLinkedQueue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private boolean blinking = false;
    private boolean flushing = false; // Lisätty isFlushing-tarkistus
    private Vec3 ghostPos = null;
    private float ghostYaw = 0f;

    // BlinkManager.java

    public void updateGhostPos() {
        if (AxiomInitialize.mc.player != null) {
            ghostPos = AxiomInitialize.mc.player.position();
        }
    }

    public void start(AbstractClientPlayer player, float tickDelta) {
        if (player != null) {
            this.ghostPos = player.position();

            // KORJAUS: Käytä yBodyRot (vartalon kierto), älä getYRot (pään/katseen kierto)
            this.ghostYaw = player.yBodyRot;

            PlayerModel model = ModelHelper.getUpdatedModel(player, tickDelta);
            if (model != null) {
                this.capturedModel = new CapturedModelState(model);
            }
        }
        packetQueue.clear();
        this.blinking = true;
    }

    public void stop() {
        blinking = false;
        flush();
    }

    public CapturedModelState getCapturedModel() {
        return capturedModel;
    }

    public void cancel() {
        blinking = false;
        packetQueue.clear();
    }


    public void flush() {
        if (flushing || AxiomInitialize.mc.getConnection() == null) return;

        flushing = true; // Asetetaan flushing päälle ennen jonon tyhjennystä
        try {
            while (!packetQueue.isEmpty()) {
                Packet<?> p = packetQueue.poll();
                if (p != null) {
                    AxiomInitialize.mc.getConnection().send(p);
                }
            }
        } finally {
            flushing = false; // Varmistetaan että flushing menee pois päältä
        }
    }

    public boolean handlePacket(Packet<?> packet) {
        if (flushing) return false;

        if (blinking) {
            // Liikkuminen
            if (packet instanceof ServerboundMovePlayerPacket) {
                packetQueue.add(packet);
                return true;
            }
            // Blokin asetus (tuki useille versioille)
            if (packet instanceof ServerboundUseItemOnPacket || packet instanceof ServerboundUseItemPacket) {
                packetQueue.add(packet);
                return true;
            }
        }
        return false;
    }



    public boolean isBlinking() {
        return blinking;
    }

    public boolean isFlushing() {
        return flushing;
    }

    public Vec3 getGhostPos() {
        return ghostPos;
    }

    public float getGhostYaw() {
        return ghostYaw;
    }

}