package silversword.axiom.client.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingSlider;

public final class Speed extends AxiomMod implements KeybindConfigurable {

    public final SettingSlider speed =
            new SettingSlider("Speed",
                    new double[]{0.2, 0.25, 0.3, 0.35, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1, 2, 3, 4, 5},
                    0.3);

    public final SettingBoolean groundOnly =
            new SettingBoolean("Ground Only", true);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public Speed() {
        super("Speed", "Increases horizontal movement speed", ModuleCategory.MOVEMENT);
        addSetting(speed);
        addSetting(groundOnly);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        PlayerEntity p = mc.player;

        if (p.forwardSpeed == 0 && p.sidewaysSpeed == 0) return;

        if (groundOnly.get() && !p.isOnGround()) return;

        double base = speed.getValue();

        float yaw = p.getYaw();

        // Minecraftin liikesuunta on yaw + 90°
        double rad = Math.toRadians(yaw + 90.0);

        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        double forward = p.forwardSpeed;
        double strafe = p.sidewaysSpeed;

        double motionX = forward * cos + strafe * sin;
        double motionZ = forward * sin - strafe * cos;

        motionX *= base;
        motionZ *= base;

        Vec3d current = p.getVelocity();
        p.setVelocity(motionX, current.y, motionZ);
    }
}