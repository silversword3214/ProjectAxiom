package silversword.axiom.client.modules.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

public class NoFall extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);


    public  NoFall () {
        super("NoFall", "No fall damage", ModuleCategory.PLAYER);

        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // Disabled in Creative mode
        if (mc.player.getAbilities().creativeMode) {
            return;
        }

        // If falling
        if (mc.player.getVelocity().y < -0.1 || mc.player.fallDistance > 2.0f) {
            // Lähetä packet
            mc.getNetworkHandler().sendPacket(
                    new PlayerMoveC2SPacket.OnGroundOnly(
                            true,
                            mc.player.horizontalCollision
                    )
            );

            // Set on client-side
            mc.player.fallDistance = 0.0f;
        }

    }
}
