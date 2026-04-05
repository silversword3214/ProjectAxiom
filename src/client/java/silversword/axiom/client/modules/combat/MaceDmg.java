package silversword.axiom.client.modules.combat;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import silversword.axiom.client.event.player.AttackEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class MaceDmg extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    public final SettingBoolean onlyWhenHoldingMace = new SettingBoolean("Only When Holding Mace", true);
    public final SettingNumber fakeFallBlocks = new SettingNumber("Fake Fall Blocks", 5, 100, 1, 30);

    public MaceDmg() {
        super("Mace Dmg", "Overwhelming mace damage", ModuleCategory.COMBAT);
        addSetting(onlyWhenHoldingMace);
        addSetting(fakeFallBlocks);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Subscribe
    private void onPlayerAttack(AttackEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.player.connection == null) return;

        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity)) return;

        if (onlyWhenHoldingMace.get() && !mc.player.getMainHandItem().is(Items.MACE)) return;

        double fakeY = fakeFallBlocks.getValue();
        double sqrtY = Math.sqrt(fakeY);

        // Lähetä väärennetyt liikepaketit (kuten Wurstissa)
        for (int i = 0; i < 4; i++) {
            sendFakeY(0);
        }
        sendFakeY(sqrtY);
        sendFakeY(0);
    }

    private void sendFakeY(double offset) {
        if (mc.player == null || mc.player.connection == null) return;
        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                mc.player.getX(),
                mc.player.getY() + offset,
                mc.player.getZ(),
                false,
                mc.player.horizontalCollision
        ));
    }

    @Override
    protected void onTick() {

    }
}