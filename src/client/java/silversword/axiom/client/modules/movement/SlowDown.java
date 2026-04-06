package silversword.axiom.client.modules.movement;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.setting.SettingKeybind;

import static silversword.axiom.client.main.AxiomInitialize.mc;

import net.minecraft.world.TickRateManager;

public final class SlowDown extends AxiomMod implements KeybindConfigurable {

    private final SettingNumber speed = new SettingNumber("Speed", 0.05, 1, 0.01, 0.5);
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private float originalTickRate = 20.0f;
    private boolean originalStored = false;

    public SlowDown() {
        super("Slow Motion", "Slows down the games tick rate", ModuleCategory.MOVEMENT);
        addSetting(speed);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        storeOriginalTickRate();
        applyTickRate();
    }

    @Override
    protected void onDisable() {
        restoreTickRate();
    }

    @Override
    protected void onTick() {
        if (mc.level == null) return;

        if (!originalStored) {
            storeOriginalTickRate();
        }

        applyTickRate();
    }

    private void storeOriginalTickRate() {
        if (mc.level != null) {
            TickRateManager tickRateManager = mc.level.tickRateManager();
            originalTickRate = tickRateManager.tickrate();
            originalStored = true;
        }
    }

    private void applyTickRate() {
        if (mc.level == null) return;

        TickRateManager tickRateManager = mc.level.tickRateManager();
        float multiplier = (float) speed.getValue();
        float newRate = originalTickRate * multiplier;

        newRate = Math.max(0.1f, Math.min(100.0f, newRate));

        tickRateManager.setTickRate(newRate);
    }

    private void restoreTickRate() {
        if (mc.level != null && originalStored) {
            TickRateManager tickRateManager = mc.level.tickRateManager();
            tickRateManager.setTickRate(originalTickRate);
        }
        originalStored = false;
    }
}