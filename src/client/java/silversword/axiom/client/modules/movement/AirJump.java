package silversword.axiom.client.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;


public class AirJump extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public AirJump() {
        super("Air Jump", "Allows player to jump when in air", ModuleCategory.MOVEMENT);
        addHiddenSetting(toggleKey);

    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        // Disabled in Creative mode
        if (mc.player.getAbilities().instabuild) {
            return;
        }

        // If falling
        if (mc.player.getDeltaMovement().y < -0.1 || mc.player.fallDistance > 2.0f) {
            // Lähetä packet
            mc.getConnection().send(
                    new ServerboundMovePlayerPacket.StatusOnly(
                            true,
                            mc.player.horizontalCollision
                    )
            );

            // Set on client-side
            mc.player.setOnGround(true);
            mc.player.fallDistance = 0.0f;
        }

    }
}
