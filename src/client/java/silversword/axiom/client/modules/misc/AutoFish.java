package silversword.axiom.client.modules.misc;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.setting.SettingKeybind;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class AutoFish extends AxiomMod implements KeybindConfigurable {

    private final SettingNumber delay = new SettingNumber("Delay (ticks)", 0, 40, 1, 5);
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private int castCooldown = 0;
    private boolean shouldReelFlag = false;

    public AutoFish() {
        super("Auto Fish", "Automatically catches fish", ModuleCategory.MISC);
        addSetting(delay);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        castCooldown = 0;
        shouldReelFlag = false;
    }

    @Override
    protected void onDisable() {
        castCooldown = 0;
        shouldReelFlag = false;
    }

    @Override
    protected void onTick() {
        if (mc.player == null) return;

        if (castCooldown > 0) {
            castCooldown--;
            return;
        }

        if (shouldReelFlag) {
            reelIn();
            shouldReelFlag = false;
            castCooldown = (int) delay.getValue();
            return;
        }

        // Jos onkea ei ole heitetty, heitä
        if (mc.player.fishing == null) {
            castRod();
        }
    }

    public void onFishBite() {
        if (!isEnabled()) return;
        if (castCooldown > 0) return;
        shouldReelFlag = true;
    }

    private void reelIn() {
        if (mc.gameMode != null) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
    }

    private void castRod() {
        if (mc.gameMode != null) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
    }
}