package silversword.axiom.mixin.client.boat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.movement.BoatFly;

@Mixin(AbstractBoat.class)
public abstract class BoatFlyMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void axiom$boatFly(CallbackInfo ci) {
        AbstractBoat boat = (AbstractBoat)(Object)this;
        Minecraft mc = Minecraft.getInstance();
        BoatFly mod = ModuleManager.getInstance().getModule(BoatFly.class);
        if (mod == null || !mod.isEnabled()) return;
        if (mc.player == null) return;


        LivingEntity controller = boat.getControllingPassenger();
        if (controller != mc.player) return;

        boat.setNoGravity(true);

        assert controller != null;
        boat.setYRot(controller.getYRot());

        // Read movement input
        float forward = 0, strafe = 0, up = 0;
        if (mc.options.keyUp.isDown()) strafe = -1;
        if (mc.options.keyDown.isDown()) strafe = 1;
        if (mc.options.keyLeft.isDown()) forward = 1;
        if (mc.options.keyRight.isDown()) forward = -1;
        if (mc.options.keyJump.isDown()) up = 1;
        if (mc.options.keyShift.isDown()) up = -1;

        if (forward != 0 || strafe != 0) {
            double len = Math.hypot(forward, strafe);
            forward /= len;
            strafe /= len;
        }

        float yaw = controller.getYRot();
        float speed = (float) mod.speed.getValue();
        
        double motionX = (forward * Math.cos(Math.toRadians(yaw)) + strafe * Math.sin(Math.toRadians(yaw))) * speed;
        double motionZ = (forward * Math.sin(Math.toRadians(yaw)) - strafe * Math.cos(Math.toRadians(yaw))) * speed;
        double motionY = up * speed;

        boat.setDeltaMovement(motionX, motionY, motionZ);
    }
}