package silversword.axiom.mixin.client.accessors;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {

    @Accessor("blockBreakingCooldown")
    void setBlockBreakingCooldown(int value);

    @Accessor("blockBreakingCooldown")
    int getBlockBreakingCooldown();

    @Accessor("currentBreakingProgress")
        float axiom$getBreakingProgress();

    @Accessor("currentBreakingProgress")
        void axiom$setCurrentBreakingProgress(float progress);

    @Accessor("currentBreakingPos")
    BlockPos axiom$getCurrentBreakingBlockPos();
}
