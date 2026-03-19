package silversword.axiom.mixin.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OrderedRenderCommandQueueImpl.class)
public interface OrderedRenderCommandQueueImplAccessor {
    @Accessor("batchingQueues") // Yarn name – Mixin will remap to field_62244 in production
    Int2ObjectAVLTreeMap<BatchingRenderCommandQueue> axiom$getBatchingQueues();
}