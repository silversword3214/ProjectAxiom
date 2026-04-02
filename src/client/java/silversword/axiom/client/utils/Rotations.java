package silversword.axiom.client.utils;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import silversword.axiom.client.utils.misc.Pool;
import java.util.ArrayList;
import java.util.List;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class Rotations {
    private static final Pool<Rotation> rotationPool = new Pool<>(Rotation::new);
    private static final List<Rotation> rotations = new ArrayList<>();

    private static float serverYaw;
    private static float serverPitch;
    private static int rotationTimer = 0;

    private static float preYaw, prePitch;
    private static Rotation lastRotation;
    private static int lastRotationTimer;
    private static boolean sentLastRotation;
    public static boolean rotating = false;

    /**
     * Kutsutaan LocalPlayerMixinistä (sendPosition HEAD).
     * Tämä valmistelee rotaation ja suorittaa moduulin (esim. KillAura) lyöntilogiikan
     * ENNEN kuin Minecraft ehtii lähettää liikkumispaketin.
     */
    public static void onPreSendMovementPackets() {
        if (mc.player == null || mc.getCameraEntity() != mc.player) return;
        sentLastRotation = false;

        if (!rotations.isEmpty()) {
            rotating = true;
            if (lastRotation != null) {
                rotationPool.free(lastRotation);
                lastRotation = null;
            }

            Rotation rotation = rotations.remove(0);

            applyRotation(rotation);

            if (rotation.callback != null) rotation.callback.run();

            rotations.clear();

        } else {
            rotating = false;
            lastRotation = null;

        }
    }

    public static void onPostSendMovementPackets() {
        if (mc.player == null) return;
        if (sentLastRotation || rotating) {
            mc.player.setYRot(preYaw);
            mc.player.setXRot(prePitch);
        }
    }

    private static void applyRotation(Rotation rotation) {
        preYaw = mc.player.getYRot();
        prePitch = mc.player.getXRot();

        if (rotation.clientSide) {
            mc.player.setYRot((float) rotation.yaw);
            mc.player.setXRot((float) rotation.pitch);
        }

        serverYaw = (float) rotation.yaw;
        serverPitch = (float) rotation.pitch;
        rotationTimer = 0;

    }

    public static void rotate(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
        Rotation rotation = rotationPool.get();
        rotation.set(yaw, pitch, priority, clientSide, callback);

        int i = 0;
        while (i < rotations.size() && priority <= rotations.get(i).priority) i++;
        rotations.add(i, rotation);
    }

    public static void rotate(double yaw, double pitch, int priority, Runnable callback) {
        rotate(yaw, pitch, priority, false, callback);
    }

    public static void rotate(double yaw, double pitch, Runnable callback) {
        rotate(yaw, pitch, 0, callback);
    }

    public static void rotate(double yaw, double pitch, int priority) {
        rotate(yaw, pitch, priority, null);
    }

    public static void rotate(double yaw, double pitch) {
        rotate(yaw, pitch, 0, null);
    }

    public static float getServerYaw() { return serverYaw; }
    public static float getServerPitch() { return serverPitch; }
    public static int getRotationTimer() { return rotationTimer; }

    private static class Rotation {
        public double yaw, pitch;
        public int priority;
        public boolean clientSide;
        public Runnable callback;

        public void set(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.priority = priority;
            this.clientSide = clientSide;
            this.callback = callback;
        }
    }
}