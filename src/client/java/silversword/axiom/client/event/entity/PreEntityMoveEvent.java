package silversword.axiom.client.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.eventbus.ICancellable;

public class PreEntityMoveEvent implements ICancellable {
    private boolean cancelled;
    private final Entity entity;
    private final MoverType moverType;
    private Vec3 movement;

    public PreEntityMoveEvent(Entity entity, MoverType moverType, Vec3 movement) {
        this.entity = entity;
        this.moverType = moverType;
        this.movement = movement;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    public Entity getEntity() {
        return entity;
    }

    public MoverType getMoverType() {
        return moverType;
    }

    public Vec3 getMovement() {
        return movement;
    }

    public void setMovement(Vec3 movement) {
        this.movement = movement;
    }
}