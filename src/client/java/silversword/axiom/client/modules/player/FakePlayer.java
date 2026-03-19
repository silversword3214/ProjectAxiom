package silversword.axiom.client.modules.player;

import net.minecraft.client.network.OtherClientPlayerEntity;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import org.lwjgl.glfw.GLFW;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class FakePlayer extends AxiomMod implements KeybindConfigurable {
    private final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", GLFW.GLFW_KEY_UNKNOWN);
    private OtherClientPlayerEntity fakePlayer;
    private int spawnDelay = 0;

    public FakePlayer() {
        super("FakePlayer", "Spawns a fake copy of you (client-side)", ModuleCategory.PLAYER);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        spawnDelay = 0; // Odotetaan 5 tickiä ennen spawnausta
    }

    private void spawnFakePlayer() {
        if (mc.world == null || mc.player == null) {
            toggle(); // Jos maailma ei ole valmis, sammutetaan
            return;
        }

        fakePlayer = new OtherClientPlayerEntity(mc.world, mc.player.getGameProfile());
        fakePlayer.copyPositionAndRotation(mc.player);
        fakePlayer.setHealth(mc.player.getHealth());
        fakePlayer.setPose(mc.player.getPose());
        fakePlayer.setHeadYaw(mc.player.headYaw);
        fakePlayer.setBodyYaw(mc.player.bodyYaw);
        fakePlayer.getInventory().clone(mc.player.getInventory());
        fakePlayer.setInvulnerable(true);

        mc.world.addEntity(fakePlayer);
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