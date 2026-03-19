package silversword.axiom.mixin.client.accessors;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerMoveC2SPacket.class)
public interface PlayerMoveC2SPacketAccessor {
    @Accessor("y")
    void axiom$setY(double y);

    @Accessor("onGround")
    void axiom$setOnGround(boolean onGround);
}