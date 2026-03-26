package silversword.axiom.client.modules.movement;

import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class BoatPhase extends AxiomMod {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    public final SettingNumber speed = new SettingNumber("Speed", 1.0, 0.1, 5.0, 0.1);

    public BoatPhase() {
        super("Boat Phase", "Makes boats phase through blocks (packet mode).", ModuleCategory.MOVEMENT);
        addHiddenSetting(toggleKey);
        addSetting(speed);
    }

    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    public void onTick() {}

    @Override
    public void onDisable() {
        // Make any boat the player is riding visible again
        if (mc.player != null && mc.player.getVehicle() instanceof AbstractBoat boat) {
            boat.setInvisible(false);
        }
    }
}