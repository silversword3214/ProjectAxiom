package silversword.axiom.client.mixininterface;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;

public interface IOrderedRenderCommandQueueImplAccessor {
    Int2ObjectMap<BatchingRenderCommandQueue> axiom$getBatchingQueues();
}