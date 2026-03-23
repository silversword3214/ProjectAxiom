package silversword.axiom.mixin.client.accessors;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface ServerboundMovePlayerPacketAccessor {
    @Accessor("y")
    void axiom$setY(double y);

    @Accessor("onGround")
    void axiom$setOnGround(boolean onGround);
}