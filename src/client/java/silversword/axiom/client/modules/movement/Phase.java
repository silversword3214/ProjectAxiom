package silversword.axiom.client.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class Phase extends AxiomMod {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    public final SettingNumber speed = new SettingNumber("Speed", 1.0, 0.1, 5.0, 0.1);


    public Phase() {
        super("Phase", "Allows you to phase through blocks.", ModuleCategory.MOVEMENT);
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
        // Reset physics for the local player
        Player player = mc.player;
        if (player != null) {
            player.noPhysics = false;
            player.setNoGravity(false);
            player.setDeltaMovement(player.getDeltaMovement());
            // Let the game handle fall from this point
            if (!player.onGround()) {
                // Force a small downward velocity to help land
                player.setDeltaMovement(player.getDeltaMovement().x, -0.5, player.getDeltaMovement().z);
            }
        }

        // Also reset for the integrated server player if in singleplayer
        if (mc.isSingleplayer()) {
            MinecraftServer server = mc.getSingleplayerServer();
            if (server != null) {
                ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
                if (serverPlayer != null) {
                    serverPlayer.noPhysics = false;
                    serverPlayer.setNoGravity(false);
                    serverPlayer.setDeltaMovement(serverPlayer.getDeltaMovement());
                }
            }
        }
    }
}