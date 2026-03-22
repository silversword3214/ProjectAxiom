package silversword.axiom.mixin.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import silversword.axiom.client.mixininterface.IEntityRenderState;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements IEntityRenderState {
    @Unique
    private Entity entity;

    @Override
    public Entity axiom$getEntity() {
        return entity;
    }

    @Override
    public void axiom$setEntity(Entity entity) {
        this.entity = entity;
    }
}