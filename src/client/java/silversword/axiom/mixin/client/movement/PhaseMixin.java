package silversword.axiom.mixin.client.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.movement.Phase;

@Mixin(LocalPlayer.class)
public abstract class PhaseMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void axiom$phaseTick(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer)(Object)this;
        Minecraft mc = Minecraft.getInstance();
        Phase mod = ModuleManager.getInstance().getModule(Phase.class);
        if (mod == null || !mod.isEnabled()) return;

        // Disable collision and gravity on client
        player.noPhysics = true;
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;

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

        Vec3 look = player.getLookAngle();
        Vec3 forwardDir = look;
        Vec3 rightDir = new Vec3(-look.z, 0, look.x).normalize();

        Vec3 move = forwardDir.scale(forward).add(rightDir.scale(strafe));
        double speed = mod.speed.getValue();

        double motionX = move.x * speed;
        double motionZ = move.z * speed;
        double motionY = up * speed;

        // Determine if the player should be considered on ground (for packet)
        boolean isOnGround = motionY == 0 && player.onGround();

        // Store old position for interpolation
        Vec3 oldPos = player.position();
        player.xo = oldPos.x;
        player.yo = oldPos.y;
        player.zo = oldPos.z;

        Vec3 newPos = oldPos.add(motionX, motionY, motionZ);

        // Update client player position
        player.setPos(newPos.x, newPos.y, newPos.z);

        // Handle server sync
        MinecraftServer server = mc.getSingleplayerServer();
        if (server != null) {
            // Singleplayer: update the server player directly
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
            if (serverPlayer != null) {
                // Store server player's current position for interpolation
                Vec3 serverOldPos = serverPlayer.position();
                serverPlayer.xo = serverOldPos.x;
                serverPlayer.yo = serverOldPos.y;
                serverPlayer.zo = serverOldPos.z;

                serverPlayer.setPos(newPos.x, newPos.y, newPos.z);
                serverPlayer.onGround = isOnGround;
                serverPlayer.noPhysics = true;
                serverPlayer.setNoGravity(true);
                serverPlayer.setDeltaMovement(Vec3.ZERO);
                serverPlayer.fallDistance = 0.0F;
                serverPlayer.connection.teleport(newPos.x, newPos.y, newPos.z, player.getYRot(), player.getXRot());
            }
        } else {
            // Multiplayer: send a position packet
            ClientPacketListener connection = mc.getConnection();
            if (connection != null) {
                ServerboundMovePlayerPacket.PosRot packet = new ServerboundMovePlayerPacket.PosRot(
                        newPos.x, newPos.y, newPos.z,
                        player.getYRot(), player.getXRot(),
                        isOnGround, false
                );
                connection.send(packet);
            }
        }

        // Cancel the original tick to prevent vanilla movement
        ci.cancel();
    }
}