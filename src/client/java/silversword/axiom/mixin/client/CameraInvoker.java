package silversword.axiom.mixin.client;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraInvoker {
    @Invoker("setPos")
    void invokeSetPos(double x, double y, double z);

    @Invoker("setRotation")
    void invokeSetRotation(float yaw, float pitch);

    @Invoker("moveBy")
    void invokeMoveBy(float x, float y, float z);
}