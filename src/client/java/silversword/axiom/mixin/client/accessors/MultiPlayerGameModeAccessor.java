package silversword.axiom.mixin.client.accessors;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {

    @Accessor("destroyDelay")
    void setDestroyDelay(int value);

    @Accessor("destroyDelay")
    int getDestroyDelay();

    @Accessor("destroyProgress")
        float axiom$getBreakingProgress();

    @Accessor("destroyProgress")
        void axiom$setDestroyProgress(float progress);

    @Accessor("destroyBlockPos")
    BlockPos axiom$getCurrentBreakingBlockPos();
}
