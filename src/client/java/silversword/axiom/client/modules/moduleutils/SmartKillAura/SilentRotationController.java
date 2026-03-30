package silversword.axiom.client.modules.moduleutils.SmartKillAura;

import net.minecraft.util.Mth;

public class SilentRotationController {
    private float currentYaw, currentPitch;
    private float targetYaw, targetPitch;
    private float turnSpeed;
    private float jitter;
    private boolean active = false;
    private boolean hasTarget = false;

    public void init(float startYaw, float startPitch) {
        this.currentYaw = startYaw;
        this.currentPitch = startPitch;
        this.active = false;
        this.hasTarget = false;
    }

    public void setTarget(float yaw, float pitch, float turnSpeed, float jitter) {
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.turnSpeed = turnSpeed;
        this.jitter = jitter;
        this.hasTarget = true;
        this.active = true;
    }

    public void update() {
        if (!hasTarget || !active) return;

        float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float yawStep = Mth.clamp(yawDiff, -turnSpeed, turnSpeed);
        float pitchStep = Mth.clamp(pitchDiff, -turnSpeed, turnSpeed);

        float newYaw = currentYaw + yawStep;
        float newPitch = currentPitch + pitchStep;

        if (jitter > 0) {
            newYaw += (float) ((Math.random() - 0.5) * jitter);
            newPitch += (float) ((Math.random() - 0.5) * jitter * 0.5);
        }

        newYaw = Mth.wrapDegrees(newYaw);
        newPitch = Mth.clamp(newPitch, -90f, 90f);

        currentYaw = newYaw;
        currentPitch = newPitch;

        // Jos tarpeeksi lähellä, deaktivoidaan
        if (Math.abs(yawDiff) < turnSpeed * 0.5f && Math.abs(pitchDiff) < turnSpeed * 0.5f) {
            active = false;
        }
    }

    public void reset() {
        active = false;
        hasTarget = false;
    }

    public float getCurrentYaw() {
        return currentYaw;
    }

    public float getCurrentPitch() {
        return currentPitch;
    }

    public boolean hasTarget() {
        return hasTarget;
    }

    public boolean isActive() {
        return active;
    }
}