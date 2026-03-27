package silversword.axiom.client.modules.movement;

import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class BoatPhase extends AxiomMod {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    public final SettingNumber speed = new SettingNumber("Speed", 0.1, 5, 0.1, 1);
    public final SettingBoolean keepGroundCollision = new SettingBoolean("Keep Ground Collision", true);

    private boolean disableNextTick = false;

    public BoatPhase() {
        super("Boat Phase", "Makes boats phase through blocks (packet mode).", ModuleCategory.HIDDEN);
        addHiddenSetting(toggleKey);
        addSetting(speed);
        addSetting(keepGroundCollision);
    }

    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    public void onTick() {
        if (disableNextTick) {
            setEnabled(false);
            disableNextTick = false;
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null && mc.player.getVehicle() instanceof AbstractBoat boat) {
            boat.setInvisible(false);
        }
    }

    public void scheduleDisableAfterOneTick() {
        disableNextTick = true;
    }
}