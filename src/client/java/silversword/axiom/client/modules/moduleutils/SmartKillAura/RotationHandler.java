package silversword.axiom.client.utils.player;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import silversword.axiom.client.utils.misc.Pool;

import java.util.ArrayList;
import java.util.List;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class RotationHandler {
    private static final Pool<Rotation> rotationPool = new Pool<>(Rotation::new);
    private static final List<Rotation> rotations = new ArrayList<>();
    private static Rotation lastRotation;
    private static int lastRotationTimer;
    private static boolean sentLastRotation;
    public static boolean rotating = false;

    private static float serverYaw, serverPitch;
    public static int rotationTimer;

    public static void rotate(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
        Rotation rotation = rotationPool.get();
        rotation.set(yaw, pitch, priority, clientSide, callback);

        int i = 0;
        for (; i < rotations.size(); i++) {
            if (priority > rotations.get(i).priority) break;
        }
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

    public static void rotateImmediate(double yaw, double pitch) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                (float) yaw,
                (float) pitch,
                mc.player.isOnGround(),
                mc.player.horizontalCollision
        ));
    }

    // Kutsutaan ennen liikepakettien lähetystä
    public static void onPreSendMovementPackets() {
        if (mc.getCameraEntity() != mc.player) return;
        sentLastRotation = false;

        if (!rotations.isEmpty()) {
            rotating = true;
            if (lastRotation != null) {
                rotationPool.free(lastRotation);
                lastRotation = null;
            }

            Rotation rotation = rotations.get(0);
            applyRotation(rotation);
            if (rotations.size() > 1) rotationPool.free(rotation);
        } else if (lastRotation != null) {
            // Pidetään viimeinen rotaatio vielä hetki
            lastRotationTimer++;
            if (lastRotationTimer > 5) { // 5 tickiä, säädettävissä
                rotationPool.free(lastRotation);
                lastRotation = null;
                rotating = false;
            } else {
                applyRotation(lastRotation);
                sentLastRotation = true;
            }
        }
    }

    // Kutsutaan liikepakettien lähetyksen jälkeen
    public static void onPostSendMovementPackets() {
        if (!rotations.isEmpty()) {
            // Poistetaan ensimmäinen (juuri käsitelty)
            Rotation first = rotations.remove(0);
            first.runCallback();

            if (rotations.isEmpty()) {
                lastRotation = first;
                lastRotationTimer = 0;
            } else {
                rotationPool.free(first);
            }

            // Jos on vielä rotaatioita, lähetetään niille paketit heti (ilman client-side vaikutusta)
            for (Rotation rotation : rotations) {
                sendRotationPacket(rotation);
                rotation.runCallback();
            }
            rotations.clear();
        } else if (sentLastRotation) {
            // Palautetaan clientin oma rotaatio (ei tarvetta, koska emme muuttaneet sitä)
            // Mutta jos clientSide oli true, meidän pitäisi palauttaa se.
        }
    }

    private static void applyRotation(Rotation rotation) {
        // Asetetaan serverin rotaatioarvot
        serverYaw = (float) rotation.yaw;
        serverPitch = (float) rotation.pitch;
        rotationTimer = 0;

        // Jos clientSide, muutetaan myös clientin omaa rotaatiota
        if (rotation.clientSide) {
            mc.player.setYaw((float) rotation.yaw);
            mc.player.setPitch((float) rotation.pitch);
        }

        // Lähetetään paketti
        sendRotationPacket(rotation);
    }

    private static void sendRotationPacket(Rotation rotation) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                (float) rotation.yaw,
                (float) rotation.pitch,
                mc.player.isOnGround(),
                mc.player.horizontalCollision
        ));
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

        public void runCallback() {
            if (callback != null) callback.run();
        }
    }
}