package silversword.axiom.mixin.client.accessors;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityFireTicksAccessor {

    @Accessor("fireTicks")
    int axiom$getFireTicks();

    @Accessor("fireTicks")
    void axiom$setFireTicks(int ticks);
}
