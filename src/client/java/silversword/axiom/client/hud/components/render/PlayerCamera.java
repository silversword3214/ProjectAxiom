package silversword.axiom.client.hud.components.render;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.lang.reflect.Field;

public class PlayerCamera extends Camera {
    private static final Field FOCUSED_ENTITY_FIELD;
    private static final Field AREA_FIELD;
    private static final Field READY_FIELD;

    static {
        try {
            FOCUSED_ENTITY_FIELD = Camera.class.getDeclaredField("focusedEntity");
            FOCUSED_ENTITY_FIELD.setAccessible(true);
            AREA_FIELD = Camera.class.getDeclaredField("area");
            AREA_FIELD.setAccessible(true);
            READY_FIELD = Camera.class.getDeclaredField("ready");
            READY_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to access Camera fields", e);
        }
    }

    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
    }

    public void setRotation(float yaw, float pitch) {
        super.setRotation(yaw, pitch);
    }

    public void moveBy(float surge, float heave, float sway) {
        super.moveBy(surge, heave, sway);
    }

    public void setFocusedEntity(Entity entity) {
        try {
            FOCUSED_ENTITY_FIELD.set(this, entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void setArea(World world) {
        try {
            AREA_FIELD.set(this, world);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void setReady(boolean ready) {
        try {
            READY_FIELD.setBoolean(this, ready);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}