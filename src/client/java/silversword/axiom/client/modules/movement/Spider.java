package silversword.axiom.client.modules.movement;

import silversword.axiom.client.event.player.PreMotionEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingSlider;
import net.minecraft.world.phys.Vec3;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class Spider extends AxiomMod {

    private final SettingSlider climbSpeed = new SettingSlider("Climb Speed", new double[]{0.1, 0.2, 0.3, 0.4, 0.5}, 0.2);
    private int wallTimeout = 0;

    public Spider() {
        super("Spider", "Allows climbing walls", ModuleCategory.MOVEMENT);
        addSetting(climbSpeed);
    }

    @Subscribe
    public void onPreMotion(PreMotionEvent event) {
        if (!isEnabled() || mc.player == null) return;

        if (mc.player.horizontalCollision) {
            wallTimeout = 2;
        } else if (wallTimeout > 0) {
            wallTimeout--;
        }

        if (wallTimeout > 0) {
            Vec3 motion = mc.player.getDeltaMovement();

            boolean isMoving = mc.player.input.keyPresses.forward() ||
                    mc.player.input.keyPresses.left() ||
                    mc.player.input.keyPresses.right();

            if (isMoving) {
                mc.player.setDeltaMovement(motion.x, climbSpeed.getValue(), motion.z);
                mc.player.fallDistance = 0;
            }
        }
    }

    @Override
    protected void onDisable() {
        wallTimeout = 0;
    }

    @Override
    protected void onTick() {

    }
}