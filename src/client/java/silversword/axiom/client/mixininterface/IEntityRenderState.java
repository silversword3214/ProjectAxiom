package silversword.axiom.client.mixininterface;

import net.minecraft.world.entity.Entity;

/**
 * Mahdollistaa EntityRenderState -> Entity -käänteismäppäyksen.
 * Täytetään EntityRenderDispatcherMixinissä.
 */
public interface IEntityRenderState {
    void axiom$setEntity(Entity entity);
    Entity axiom$getEntity();
}