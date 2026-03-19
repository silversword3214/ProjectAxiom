package silversword.axiom.mixin.client.accessors;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {

    @Accessor("pos")
    Vec3d axiom$getPos();

    @Accessor("pos")
    void axiom$setPos(Vec3d pos);

    @Invoker("setRotation")
    void axiom$setRotation(float yaw, float pitch);

    @Accessor("focusedEntity")
    void setFocusedEntity(Entity entity);

}
