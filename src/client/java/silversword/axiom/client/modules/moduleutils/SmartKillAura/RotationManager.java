package silversword.axiom.client.modules.moduleutils.SmartKillAura;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationManager {

    public static class Rotation {
        public float yaw, pitch;
        public Rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public Rotation calculateRotation(PlayerEntity player, LivingEntity target, boolean predict) {
        Vec3d targetPos = target.getEntityPos();
        if (predict) {
            targetPos = targetPos.add(target.getVelocity());
        }

        Vec3d vec = targetPos.add(0, target.getHeight() / 2, 0).subtract(player.getEyePos());
        double diffX = vec.x;
        double diffY = vec.y;
        double diffZ = vec.z;

        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, distance));

        return new Rotation(yaw, pitch);
    }

    public void rotateSmoothly(PlayerEntity player, Rotation target, float maxTurnSpeed, float jitter) {
        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();

        float yawDiff = MathHelper.wrapDegrees(target.yaw - currentYaw);
        float pitchDiff = target.pitch - currentPitch;

        // Rajoitetaan kääntymisnopeutta
        float yawChange = MathHelper.clamp(yawDiff, -maxTurnSpeed, maxTurnSpeed);
        float pitchChange = MathHelper.clamp(pitchDiff, -maxTurnSpeed, maxTurnSpeed);

        float newYaw = currentYaw + yawChange;
        float newPitch = currentPitch + pitchChange;

        // Lisätään pientä satunnaisuutta (jitter)
        if (jitter > 0) {
            newYaw += (float) ((Math.random() - 0.5) * jitter);
            newPitch += (float) ((Math.random() - 0.5) * jitter * 0.5);
        }

        // Normalisoidaan
        newYaw = MathHelper.wrapDegrees(newYaw);
        newPitch = MathHelper.clamp(newPitch, -90f, 90f);

        player.setYaw(newYaw);
        player.setPitch(newPitch);
    }

    public void reset() {
        // Ei tarvetta
    }
}