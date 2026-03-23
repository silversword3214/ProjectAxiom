package silversword.axiom.client.modules.moduleutils.SmartKillAura;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RotationManager {

    public static class Rotation {
        public float yaw, pitch;
        public Rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public Rotation calculateRotation(Player player, LivingEntity target, boolean predict) {
        Vec3 targetPos = target.position();
        if (predict) {
            targetPos = targetPos.add(target.getDeltaMovement());
        }

        Vec3 vec = targetPos.add(0, target.getBbHeight() / 2, 0).subtract(player.getEyePosition());
        double diffX = vec.x;
        double diffY = vec.y;
        double diffZ = vec.z;

        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, distance));

        return new Rotation(yaw, pitch);
    }

    public void rotateSmoothly(Player player, Rotation target, float maxTurnSpeed, float jitter) {
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        float yawDiff = Mth.wrapDegrees(target.yaw - currentYaw);
        float pitchDiff = target.pitch - currentPitch;

        // Rajoitetaan kääntymisnopeutta
        float yawChange = Mth.clamp(yawDiff, -maxTurnSpeed, maxTurnSpeed);
        float pitchChange = Mth.clamp(pitchDiff, -maxTurnSpeed, maxTurnSpeed);

        float newYaw = currentYaw + yawChange;
        float newPitch = currentPitch + pitchChange;

        // Lisätään pientä satunnaisuutta (jitter)
        if (jitter > 0) {
            newYaw += (float) ((Math.random() - 0.5) * jitter);
            newPitch += (float) ((Math.random() - 0.5) * jitter * 0.5);
        }

        // Normalisoidaan
        newYaw = Mth.wrapDegrees(newYaw);
        newPitch = Mth.clamp(newPitch, -90f, 90f);

        player.setYRot(newYaw);
        player.setXRot(newPitch);
    }

    public void reset() {
        // Ei tarvetta
    }
}