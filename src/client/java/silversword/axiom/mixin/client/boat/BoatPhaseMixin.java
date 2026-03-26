package silversword.axiom.mixin.client.boat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.movement.BoatPhase;

@Mixin(AbstractBoat.class)
public abstract class BoatPhaseMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void axiom$boatPhase(CallbackInfo ci) {
        AbstractBoat boat = (AbstractBoat)(Object)this;
        Minecraft mc = Minecraft.getInstance();
        BoatPhase mod = ModuleManager.getInstance().getModule(BoatPhase.class);
        if (mod == null || !mod.isEnabled()) return;

        if (mc.player == null) return;

        LivingEntity controller = boat.getControllingPassenger();
        if (controller != mc.player) return;

        // Make the boat invisible (client‑side)
        boat.setInvisible(true);

        // Disable gravity and reset motion
        boat.setNoGravity(true);
        boat.setDeltaMovement(Vec3.ZERO);
        boat.fallDistance = 0.0F;

        // Read input
        float forward = 0, strafe = 0, up = 0;
        if (mc.options.keyUp.isDown()) forward = 1;
        if (mc.options.keyDown.isDown()) forward = -1;
        if (mc.options.keyLeft.isDown()) strafe = -1;
        if (mc.options.keyRight.isDown()) strafe = 1;
        if (mc.options.keyJump.isDown()) up = 1;
        if (mc.options.keyShift.isDown()) up = -1;

        if (forward != 0 || strafe != 0) {
            double len = Math.hypot(forward, strafe);
            forward /= len;
            strafe /= len;
        }

        // Get player's look direction
        Vec3 look = mc.player.getLookAngle();
        Vec3 forwardDir = look;
        Vec3 rightDir = new Vec3(-look.z, 0, look.x).normalize();

        // Calculate movement
        Vec3 move = forwardDir.scale(forward).add(rightDir.scale(strafe));
        double speed = mod.speed.getValue();
        double motionX = move.x * speed;
        double motionZ = move.z * speed;
        double motionY = up * speed;

        // New position for the player (the boat will follow)
        Vec3 newPos = mc.player.position().add(motionX, motionY, motionZ);

        // Send position packet to the server
        ClientPacketListener connection = mc.getConnection();
        if (connection != null) {
            ServerboundMovePlayerPacket.PosRot packet = new ServerboundMovePlayerPacket.PosRot(
                    newPos.x, newPos.y, newPos.z,
                    mc.player.getYRot(), mc.player.getXRot(),
                    false, false  // onGround = false, horizontalCollision = false
            );
            connection.send(packet);
        }

        // Update positions locally to prevent visual lag
        boat.setPos(newPos.x, newPos.y, newPos.z);
        mc.player.setPos(newPos.x, newPos.y, newPos.z);

        // Cancel the original tick to prevent vanilla physics
        ci.cancel();
    }
}