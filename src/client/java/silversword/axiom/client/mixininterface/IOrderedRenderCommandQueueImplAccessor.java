package silversword.axiom.client.mixininterface;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.SubmitNodeCollection;

public interface IOrderedRenderCommandQueueImplAccessor {
    Int2ObjectMap<SubmitNodeCollection> axiom$getBatchingQueues();
}