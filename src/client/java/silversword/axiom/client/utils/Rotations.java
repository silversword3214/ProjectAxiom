package silversword.axiom.client.utils;

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

    // Smooth silent rotation (Scaffold)
    private static boolean smoothActive = false;
    private static float startServerYaw, startServerPitch;
    private static int smoothSteps;
    private static int stepIndex;
    private static float stepYaw, stepPitch;
    private static Runnable smoothCallback;

    // FIX: Maksimi muutos per tick (Vulcanille ystävällinen)
    private static final float MAX_YAW_CHANGE_PER_TICK = 10.0f;
    private static final float MAX_PITCH_CHANGE_PER_TICK = 5.0f;

    public static void onPreSendMovementPackets() {
        if (mc.player == null) return;

        if (smoothActive) {
            if (stepIndex < smoothSteps) {
                stepIndex++;
                float currentYaw = startServerYaw + stepYaw * stepIndex;
                float currentPitch = startServerPitch + stepPitch * stepIndex;
                serverYaw = normalizeYaw(currentYaw);
                serverPitch = clampPitch(currentPitch);
                rotationTimer = 0;
                if (stepIndex == smoothSteps && smoothCallback != null) {
                    smoothCallback.run();
                    smoothCallback = null;
                }
                return;
            } else {
                smoothActive = false;
            }
        }

        if (!rotations.isEmpty()) {
            Rotation rotation = rotations.remove(0);
            serverYaw = normalizeYaw((float) rotation.yaw);
            serverPitch = clampPitch((float) rotation.pitch);
            rotationTimer = 0;
            if (rotation.callback != null) rotation.callback.run();
            rotations.clear();
        } else {
            if (rotationTimer < Integer.MAX_VALUE) rotationTimer++;
        }
    }

    public static void onPostSendMovementPackets() {
        // Tyhjä
    }

    public static void rotateSmooth(double yaw, double pitch, int steps, Runnable callback) {
        if (mc.player == null) return;
        smoothActive = false; // keskeytä edellinen

        startServerYaw = serverYaw;
        startServerPitch = serverPitch;
        float targetYaw = normalizeYaw((float) yaw);
        float targetPitch = clampPitch((float) pitch);

        // Lasketaan lyhin ero yaw:ssa (vältetään ympäripyörähdys)
        float deltaYaw = targetYaw - startServerYaw;
        deltaYaw = (deltaYaw % 360 + 360) % 360;
        if (deltaYaw > 180) deltaYaw -= 360;

        float deltaPitch = targetPitch - startServerYaw; // FIX: oikea deltaPitch
        deltaPitch = targetPitch - startServerPitch;

        // FIX: Pakotetaan maksimimuutos per tick
        int neededStepsYaw = (int) Math.ceil(Math.abs(deltaYaw) / MAX_YAW_CHANGE_PER_TICK);
        int neededStepsPitch = (int) Math.ceil(Math.abs(deltaPitch) / MAX_PITCH_CHANGE_PER_TICK);
        int actualSteps = Math.max(steps, Math.max(neededStepsYaw, neededStepsPitch));
        actualSteps = Math.max(actualSteps, 1);

        stepYaw = deltaYaw / actualSteps;
        stepPitch = deltaPitch / actualSteps;
        smoothSteps = actualSteps;
        stepIndex = 0;
        smoothActive = true;
        smoothCallback = callback;
    }

    public static void rotateSmooth(double yaw, double pitch, Runnable callback) {
        rotateSmooth(yaw, pitch, 5, callback);
    }

    public static void rotate(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
        if (smoothActive) return;
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

    private static float normalizeYaw(float yaw) {
        yaw = yaw % 360;
        if (yaw < 0) yaw += 360;
        return yaw;
    }

    private static float clampPitch(float pitch) {
        if (pitch > 90) return 90;
        if (pitch < -90) return -90;
        return pitch;
    }

    public static float getServerYaw() { return serverYaw; }
    public static float getServerPitch() { return serverPitch; }
    public static int getRotationTimer() { return rotationTimer; }
    public static boolean isRotating() { return smoothActive || !rotations.isEmpty(); }

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