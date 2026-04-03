package silversword.axiom.client.utils.render;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class InterpolationUtil {

    public static Vec3 interpolateEntity(Entity entity, float tickDelta) {
        double x = entity.xOld + (entity.getX() - entity.xOld) * tickDelta;
        double y = entity.yOld + (entity.getY() - entity.yOld) * tickDelta;
        double z = entity.zOld + (entity.getZ() - entity.zOld) * tickDelta;
        return new Vec3(x, y, z);
    }

    public static double interpolateX(Entity entity, float tickDelta) {
        return entity.xOld + (entity.getX() - entity.xOld) * tickDelta;
    }

    public static double interpolateY(Entity entity, float tickDelta) {
        return entity.yOld + (entity.getY() - entity.yOld) * tickDelta;
    }

    public static double interpolateZ(Entity entity, float tickDelta) {
        return entity.zOld + (entity.getZ() - entity.zOld) * tickDelta;
    }
}