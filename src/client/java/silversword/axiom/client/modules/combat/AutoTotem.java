package silversword.axiom.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

public class AutoTotem extends AxiomMod implements KeybindConfigurable {

    private final SettingNumber healthThreshold =
            new SettingNumber("Health Threshold", 1.0, 20.0, 0.5, 16.0);

    private final SettingBoolean countAbsorption =
            new SettingBoolean("Count Absorption", true);

    private final SettingNumber cooldownTicks =
            new SettingNumber("Cooldown Ticks", 0, 40, 1, 8);

    private final SettingBoolean stopInInventory =
            new SettingBoolean("Stop In Inventory", true);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);


    private int cooldown = 0;

    public AutoTotem() {
        super("Auto Totem", "Moves a Totem to offhand when health is low", ModuleCategory.COMBAT);
        addSetting(healthThreshold);
        addSetting(countAbsorption);
        addSetting(cooldownTicks);
        addSetting(stopInInventory);

        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.gameMode == null) return;

        if (stopInInventory.get() && mc.screen != null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        Player player = mc.player;

        if (player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) return;

        float hp = player.getHealth();
        if (countAbsorption.get()) hp += player.getAbsorptionAmount();

        if (hp > (float) healthThreshold.getValue()) return;

        int invIndex = findTotemInInventory(player);
        if (invIndex == -1) return;

        int totemSlotId = invIndexToPlayerScreenSlotId(invIndex);
        int offhandSlotId = 45;

        int syncId = player.inventoryMenu.containerId;

        mc.gameMode.handleInventoryMouseClick(syncId, totemSlotId, 0, ClickType.PICKUP, player);
        mc.gameMode.handleInventoryMouseClick(syncId, offhandSlotId, 0, ClickType.PICKUP, player);
        mc.gameMode.handleInventoryMouseClick(syncId, totemSlotId, 0, ClickType.PICKUP, player);

        cooldown = (int) cooldownTicks.getValue();
    }

    private int findTotemInInventory(Player player) {
        for (int i = 0; i < 36; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() == Items.TOTEM_OF_UNDYING) return i;
        }
        return -1;
    }

    private int invIndexToPlayerScreenSlotId(int invIndex) {
        if (invIndex < 0 || invIndex > 35) return -1;
        if (invIndex < 9) return 36 + invIndex;
        return invIndex;
    }

    @Override
    protected void onDisable() {
        cooldown = 0;
    }
}
