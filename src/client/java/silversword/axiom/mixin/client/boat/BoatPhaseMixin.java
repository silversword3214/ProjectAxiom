package silversword.axiom.mixin.client.boat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.mixininterface.BoatPhaseMixinAccessor;
import silversword.axiom.client.modules.movement.BoatPhase;

@Mixin(AbstractBoat.class)
public abstract class BoatPhaseMixin implements BoatPhaseMixinAccessor {

    private int tickCounter = 0;
    private Vec3 lastTargetPos = null; // store the last position we set

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void axiom$boatPhase(CallbackInfo ci) {
        AbstractBoat boat = (AbstractBoat)(Object)this;
        Minecraft mc = Minecraft.getInstance();
        BoatPhase mod = ModuleManager.getInstance().getModule(BoatPhase.class);
        if (mod == null || !mod.isEnabled()) return;

        if (mc.player == null) return;

        LivingEntity controller = boat.getControllingPassenger();
        if (controller != mc.player) return;

        // Hide the boat
        boat.setInvisible(true);

        // Disable gravity and reset motion
        boat.setNoGravity(true);
        boat.setDeltaMovement(Vec3.ZERO);
        boat.fallDistance = 0.0F;

        // Input handling
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

        // Direction from player look
        Vec3 look = mc.player.getLookAngle();
        Vec3 forwardDir = look;
        Vec3 rightDir = new Vec3(-look.z, 0, look.x).normalize();

        Vec3 move = forwardDir.scale(forward).add(rightDir.scale(strafe));
        double speed = mod.speed.getValue();

        double motionX = move.x * speed;
        double motionZ = move.z * speed;
        double motionY = up * speed;

        Vec3 newPos = mc.player.position().add(motionX, motionY, motionZ);

        // --- Ground collision prevention ---
        if (mod.keepGroundCollision.get()) {
            int x = (int) Math.floor(newPos.x);
            int z = (int) Math.floor(newPos.z);
            int groundY = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (groundY > mc.level.getMinY()) {
                double minY = groundY + 0.001;
                if (newPos.y < minY && motionY < 0) {
                    newPos = new Vec3(newPos.x, minY, newPos.z);
                    motionY = 0;
                }
            }
        }

        // --- Dolphin oscillation for packet Y (anti‑kick) ---
        double packetY = newPos.y;
        if (!mc.player.onGround() && !mc.player.isInWater() && !mc.player.isInLava()) {
            tickCounter++;
            packetY += (tickCounter % 2 == 0) ? 0.04 : -0.04;
        } else {
            tickCounter = 0;
        }

        // Send packet to server
        ClientPacketListener connection = mc.getConnection();
        if (connection != null) {
            ServerboundMovePlayerPacket.PosRot packet = new ServerboundMovePlayerPacket.PosRot(
                    newPos.x, packetY, newPos.z,
                    mc.player.getYRot(), mc.player.getXRot(),
                    false, false
            );
            connection.send(packet);
        }

        // Update local positions
        boat.setPos(newPos.x, newPos.y, newPos.z);
        mc.player.setPos(newPos.x, newPos.y, newPos.z);
        lastTargetPos = newPos; // remember the position we set

        ci.cancel();
    }

    // This method is called to reset the target when the player dismounts
    public void resetBoatPhaseTarget() {
        lastTargetPos = null;
    }

    // Tail injection to force position back if it drifted
    @Inject(method = "tick", at = @At("TAIL"))
    private void axiom$boatPhaseTail(CallbackInfo ci) {
        AbstractBoat boat = (AbstractBoat)(Object)this;
        Minecraft mc = Minecraft.getInstance();
        BoatPhase mod = ModuleManager.getInstance().getModule(BoatPhase.class);
        if (mod == null || !mod.isEnabled()) return;
        if (lastTargetPos == null) return;

        // If the boat's position differs from our target, force it back
        if (boat.position().distanceToSqr(lastTargetPos) > 0.01) {
            boat.setPos(lastTargetPos.x, lastTargetPos.y, lastTargetPos.z);
        }
    }
}