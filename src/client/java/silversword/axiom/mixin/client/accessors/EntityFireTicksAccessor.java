package silversword.axiom.mixin.client.accessors;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityFireTicksAccessor {

    @Accessor("remainingFireTicks")
    int axiom$getRemainingFireTicks();

    @Accessor("remainingFireTicks")
    void axiom$setRemainingFireTicks(int ticks);
}
