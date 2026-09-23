package silversword.axiom.client.modules.player;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.player.RemotePlayer;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

import java.util.UUID;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class FakePlayer extends AxiomMod implements KeybindConfigurable {
    private static int nextFakeEntityId = -1;
    // 26.3: GLFW_KEY_UNKNOWN → InputConstants.UNKNOWN.getValue() (-1)
    private final SettingKeybind toggleKey =
            new SettingKeybind("Toggle Key", InputConstants.UNKNOWN.getValue());
    private RemotePlayer fakePlayer;
    private int spawnDelay = 0;

    public FakePlayer() {
        super("FakePlayer", "Spawns a fake copy of you", ModuleCategory.PLAYER);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        spawnDelay = 0;
    }

    private void spawnFakePlayer() {
        if (mc.level == null || mc.player == null) {
            toggle();
            return;
        }

        fakePlayer = new RemotePlayer(mc.level, mc.player.getGameProfile());
        fakePlayer.setId(nextFakeEntityId--);
        fakePlayer.setUUID(UUID.randomUUID());
        fakePlayer.copyPosition(mc.player);
        fakePlayer.setHealth(mc.player.getHealth());
        fakePlayer.setPose(mc.player.getPose());
        fakePlayer.setYHeadRot(mc.player.yHeadRot);
        fakePlayer.setYBodyRot(mc.player.yBodyRot);
        fakePlayer.getInventory().replaceWith(mc.player.getInventory());
        fakePlayer.setPermanentlyInvulnerable(true);

        mc.level.addEntity(fakePlayer);
    }

    @Override
    protected void onDisable() {
        if (fakePlayer != null) {
            fakePlayer.discard();
            fakePlayer = null;
        }
        spawnDelay = 0;
    }

    @Override
    protected void onTick() {
        if (!isEnabled()) return;
        if (fakePlayer != null) return;

        if (spawnDelay > 0) {
            spawnDelay--;
            return;
        }

        spawnFakePlayer();
    }
}