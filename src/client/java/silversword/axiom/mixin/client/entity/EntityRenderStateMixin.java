package silversword.axiom.mixin.client.entity;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import silversword.axiom.client.mixininterface.IEntityRenderState;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements IEntityRenderState {

    @Unique private Entity axiom$entity;

    @Override public void axiom$setEntity(Entity e) { this.axiom$entity = e; }
    @Override public Entity axiom$getEntity()       { return this.axiom$entity; }
}