package silversword.axiom.client.modules.moduleutils;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A dummy entity used as the camera target when Freecam is active.
 * It does nothing but hold a position and rotation, allowing the
 * game to load chunks around the freecam viewpoint.
 */
public class FreecamCameraEntity extends Entity {

    public FreecamCameraEntity(Level level) {
        super(EntityType.PLAYER, level); // Using PLAYER type ensures correct collision behavior (none)
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        // Do nothing – position is updated externally.
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float f) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    // Override to avoid being removed by the game
    @Override
    public void remove(RemovalReason removalReason) {}

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {

    }
}