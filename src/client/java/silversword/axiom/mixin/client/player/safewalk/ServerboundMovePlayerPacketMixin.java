package silversword.axiom.mixin.client.player.safewalk;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import silversword.axiom.client.modules.movement.SafeWalk;


@Mixin(ServerboundMovePlayerPacket.class)
public class ServerboundMovePlayerPacketMixin {

    @Mixin(ServerboundMovePlayerPacket.Pos.class)
    public static class PosMixin {
        @ModifyArg(
                method = "<init>",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;<init>(DDDFFZZZZ)V"),
                index = 6
        )
        private static boolean modifyOnGround(boolean onGround) {
            if (SafeWalk.shouldPreventFall) {
                return true;
            }
            return onGround;
        }
    }

    @Mixin(ServerboundMovePlayerPacket.PosRot.class)
    public static class PosRotMixin {
        @ModifyArg(
                method = "<init>",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;<init>(DDDFFZZZZ)V"),
                index = 6
        )
        private static boolean modifyOnGround(boolean onGround) {
            if (SafeWalk.shouldPreventFall) {
                return true;
            }
            return onGround;
        }
    }

    @Mixin(ServerboundMovePlayerPacket.Rot.class)
    public static class RotMixin {
        @ModifyArg(
                method = "<init>",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;<init>(DDDFFZZZZ)V"),
                index = 6
        )
        private static boolean modifyOnGround(boolean onGround) {
            if (SafeWalk.shouldPreventFall) {
                return true;
            }
            return onGround;
        }
    }

    @Mixin(ServerboundMovePlayerPacket.StatusOnly.class)
    public static class StatusOnlyMixin {
        @ModifyArg(
                method = "<init>",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;<init>(DDDFFZZZZ)V"),
                index = 6
        )
        private static boolean modifyOnGround(boolean onGround) {
            if (SafeWalk.shouldPreventFall) {
                return true;
            }
            return onGround;
        }
    }
}