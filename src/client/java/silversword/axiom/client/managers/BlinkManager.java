package silversword.axiom.client.managers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.render.rendersystem.utils.render.ModelHelper;
import silversword.axiom.client.utils.render.CapturedModelState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BlinkManager {
    private static final BlinkManager INSTANCE = new BlinkManager();
    public static BlinkManager getInstance() { return INSTANCE; }
    private CapturedModelState capturedModel = null;

    private final ConcurrentLinkedQueue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private boolean blinking = false;
    private boolean flushing = false;
    private Vec3 ghostPos = null;
    private float ghostYaw = 0f;

    // Suodatusflagit
    private boolean filterMovement = true;
    private boolean filterAttack = true;
    private boolean filterPlacement = true;
    private final Map<BlockPos, BlockState> pendingBlockPlacements = new ConcurrentHashMap<>();

    public void updateGhostPos() {
        if (AxiomInitialize.mc.player != null) {
            ghostPos = AxiomInitialize.mc.player.position();
        }
    }

    public void start(AbstractClientPlayer player, float tickDelta) {
        if (player != null) {
            this.ghostPos = player.position();
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
        flush(Integer.MAX_VALUE);  // lähetä kaikki loput (tai voit haluta kutsua flushAll)
    }

    public CapturedModelState getCapturedModel() {
        return capturedModel;
    }

    public void cancel() {
        blinking = false;
        packetQueue.clear();
    }

    // Vanha flush kutsutaan nyt uuden version kautta
    public void flush() {
        flush(Integer.MAX_VALUE);
    }

    /**
     * Lähettää korkeintaan maxPackets määrän paketteja jonosta.
     * @return jäljellä olevien pakettien määrä
     */
    public int flush(int maxPackets) {
        if (flushing || AxiomInitialize.mc.getConnection() == null) return packetQueue.size();
        flushing = true;
        int sent = 0;
        try {
            // Tyhjennetään block ghostit ennen pakettien lähetystä
            BlockGhostManager.getInstance().clearAll();

            while (sent < maxPackets && !packetQueue.isEmpty()) {
                Packet<?> p = packetQueue.poll();
                if (p != null) {
                    AxiomInitialize.mc.getConnection().send(p);
                    sent++;
                }
            }
        } finally {
            flushing = false;
        }
        return packetQueue.size();
    }

    public boolean hasQueuedPackets() {
        return !packetQueue.isEmpty();
    }

    public int getQueuedPacketCount() {
        return packetQueue.size();
    }

    public void setFilters(boolean movement, boolean attack, boolean placement) {
        this.filterMovement = movement;
        this.filterAttack = attack;
        this.filterPlacement = placement;
    }

    public boolean handlePacket(Packet<?> packet) {
        if (flushing) return false;

        if (blinking) {
            // Liikepaketit
            if (filterMovement && packet instanceof ServerboundMovePlayerPacket) {
                packetQueue.add(packet);
                return true;
            }
            // Hyökkäyspaketit
            if (filterAttack && packet instanceof ServerboundInteractPacket) {
                packetQueue.add(packet);
                return true;
            }
            // Blokin asetuspaketit (placement)
            if (filterPlacement && packet instanceof ServerboundUseItemOnPacket useItemOn) {
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