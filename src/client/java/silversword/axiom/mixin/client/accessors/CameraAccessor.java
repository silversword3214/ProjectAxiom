package silversword.axiom.mixin.client.accessors;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {

    @Accessor("position")
    Vec3 axiom$getPosition();

    @Accessor("position")
    void axiom$setPosition(Vec3 pos);

    @Invoker("setRotation")
    void axiom$setRotation(float yaw, float pitch);

    @Accessor("entity")
    void setEntity(Entity entity);

}
