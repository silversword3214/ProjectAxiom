package silversword.axiom.mixin.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SubmitNodeStorage.class)
public interface SubmitNodeStorageAccessor {
    @Accessor("submitsPerOrder")
    Int2ObjectAVLTreeMap<SubmitNodeCollection> axiom$getSubmitsPerOrder();
}