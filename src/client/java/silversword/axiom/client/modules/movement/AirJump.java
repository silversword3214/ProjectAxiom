package silversword.axiom.client.modules.movement;

import silversword.axiom.client.event.player.PreMotionEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class AirJump extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private int jumpCooldown = 4;
    private int cooldown = 0;
    private boolean hasJumpedInAir = true;

    public AirJump() {
        super("Air Jump", "Jump in the air", ModuleCategory.MOVEMENT);

    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        cooldown = 0;
        hasJumpedInAir = false;
    }

    @Override
    protected void onTick() {

    }

    @Subscribe
    private void onPreMotion(PreMotionEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (mc.player.onGround()) {
            hasJumpedInAir = false;
            return;
        }

        if (mc.player.isInWater() || mc.player.isInLava()) return;

        if (!hasJumpedInAir && mc.options.keyJump.isDown()) {
            mc.player.jumpFromGround();
            hasJumpedInAir = false;

            cooldown = jumpCooldown;
        }
    }
}