package silversword.axiom.client.modules.movement;

import silversword.axiom.client.event.player.PreMotionEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class VulcanHighJump extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingNumber jumpCooldown = new SettingNumber("Cooldown (ticks)", 1, 20, 1, 5);
    private final SettingBoolean onlyWhenSprinting = new SettingBoolean("Only When Sprinting", false);
    private final SettingBoolean resetFallDistance = new SettingBoolean("Reset Fall Distance", true);

    private int cooldown = 0;
    private boolean hasJumpedInAir = true;

    public VulcanHighJump() {
        super("VulcanHighJump", "Jumps 1.5 blocks", ModuleCategory.MOVEMENT);
        addSetting(jumpCooldown);
        addSetting(onlyWhenSprinting);
        addSetting(resetFallDistance);
        addHiddenSetting(toggleKey);
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

        if (onlyWhenSprinting.get() && !mc.player.isSprinting()) return;

        if (!hasJumpedInAir) {
            mc.player.jumpFromGround();
            hasJumpedInAir = true;

            if (resetFallDistance.get()) {
                mc.player.fallDistance = 0;
            }

            cooldown = (int) jumpCooldown.getValue();
        }
    }
}