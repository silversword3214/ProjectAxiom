package silversword.axiom.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;

public class AutoGapple extends AxiomMod implements KeybindConfigurable {

    private final SettingNumber triggerHealth =
            new SettingNumber("Trigger Health", 1.0, 20.0, 0.5, 18.0);

    private final SettingBoolean countAbsorption =
            new SettingBoolean("Count Absorption", true);

    private final SettingNumber noDamageSeconds =
            new SettingNumber("No Damage (s)", 0.0, 30.0, 0.5, 5.0);

    private final SettingNumber pearlWindowSeconds =
            new SettingNumber("Pearl Window (s)", 0.0, 5.0, 0.1, 1.0);

    private final SettingNumber maxHoldSeconds =
            new SettingNumber("Max Hold (s)", 1.0, 15.0, 0.5, 12.0);

    private final SettingNumber startCooldownTicks =
            new SettingNumber("Start Cooldown (t)", 0, 40, 1, 10);

    private final SettingMode appleType =
            new SettingMode("Apple Type", new String[]{"Any", "Normal Only", "Enchanted Only"}, "Any");

    private final SettingBoolean stopInInventory =
            new SettingBoolean("Stop In Inventory", true);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private int tick = 0;
    private int lastDamageTick = -999999;
    private int lastPearlTick = -999999;

    private boolean prevPearlCooling = false;

    private boolean eating = false;
    private int eatTicks = 0;
    private int cooldown = 0;

    private int prevSelectedSlot = -1;
    private boolean prevUsePressed = false;
    private int targetSlot = -1;

    private static final ItemStack PEARL_STACK = new ItemStack(Items.ENDER_PEARL);

    public AutoGapple() {
        super("Auto Gapple", "Eats gapples after no damage or right after pearl", ModuleCategory.COMBAT);
        addSetting(triggerHealth);
        addSetting(countAbsorption);
        addSetting(noDamageSeconds);
        addSetting(pearlWindowSeconds);
        addSetting(maxHoldSeconds);
        addSetting(startCooldownTicks);
        addSetting(appleType);
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
        if (mc == null || mc.player == null || mc.options == null) return;

        tick++;

        if (stopInInventory.get() && mc.screen != null) {
            stopEating(mc, true);
            return;
        }

        Player p = mc.player;

        if (p.hurtTime > 0) lastDamageTick = tick;

        boolean pearlCooling = p.getCooldowns().isOnCooldown(PEARL_STACK);
        if (!prevPearlCooling && pearlCooling) lastPearlTick = tick;
        prevPearlCooling = pearlCooling;

        if (cooldown > 0) cooldown--;

        if (eating) {
            continueEating(mc, p);
            return;
        }

        if (cooldown > 0) return;

        float hp = p.getHealth();
        if (countAbsorption.get()) hp += p.getAbsorptionAmount();
        if (hp >= (float) triggerHealth.getValue()) return;

        int noDamageTicksRequired = (int) Math.round(noDamageSeconds.getValue() * 20.0);
        int pearlTriggerWindowTicks = (int) Math.round(pearlWindowSeconds.getValue() * 20.0);

        boolean noDamageLongEnough = (tick - lastDamageTick) >= noDamageTicksRequired;
        boolean pearlJustUsed = (tick - lastPearlTick) <= pearlTriggerWindowTicks;

        if (!noDamageLongEnough && !pearlJustUsed) return;

        int slot = findGappleInHotbar(p);
        if (slot == -1) return;

        startEating(mc, p, slot);
    }

    private void startEating(Minecraft mc, Player p, int slot) {
        prevSelectedSlot = p.getInventory().getSelectedSlot();
        prevUsePressed = mc.options.keyUse.isDown();

        targetSlot = slot;
        p.getInventory().setSelectedSlot(targetSlot);

        eating = true;
        eatTicks = 0;

        mc.options.keyUse.setDown(true);
    }

    private void continueEating(Minecraft mc, Player p) {
        int currentSlot = p.getInventory().getSelectedSlot();
        if (currentSlot != targetSlot) {
            stopEating(mc, false);
            return;
        }

        float hp = p.getHealth();
        if (countAbsorption.get()) hp += p.getAbsorptionAmount();
        if (hp >= p.getMaxHealth()) {
            stopEating(mc, true);
            cooldown = (int) startCooldownTicks.getValue();
            return;
        }

        eatTicks++;
        int maxHoldTicksTotal = (int) Math.round(maxHoldSeconds.getValue() * 20.0);
        if (eatTicks >= maxHoldTicksTotal) {
            stopEating(mc, true);
            cooldown = (int) startCooldownTicks.getValue();
            return;
        }

        mc.options.keyUse.setDown(true);

        ItemStack stack = p.getInventory().getItem(currentSlot);
        if (!isGapple(stack)) {
            int newSlot = findGappleInHotbar(p);
            if (newSlot == -1) {
                stopEating(mc, true);
                cooldown = (int) startCooldownTicks.getValue();
            } else {
                targetSlot = newSlot;
                p.getInventory().setSelectedSlot(targetSlot);
            }
        }
    }

    private void stopEating(Minecraft mc, boolean restoreSlot) {
        if (mc == null || mc.player == null || mc.options == null) return;

        mc.options.keyUse.setDown(prevUsePressed);

        if (restoreSlot && prevSelectedSlot != -1) {
            mc.player.getInventory().setSelectedSlot(prevSelectedSlot);
        }

        eating = false;
        eatTicks = 0;
        prevSelectedSlot = -1;
        prevUsePressed = false;
        targetSlot = -1;
    }

    private int findGappleInHotbar(Player p) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (isGapple(s)) return i;
        }
        return -1;
    }

    private boolean isGapple(ItemStack s) {
        if (s == null || s.isEmpty()) return false;

        String t = appleType.getMode();
        boolean normal = s.getItem() == Items.GOLDEN_APPLE;
        boolean enchanted = s.getItem() == Items.ENCHANTED_GOLDEN_APPLE;

        if ("Normal Only".equals(t)) return normal;
        if ("Enchanted Only".equals(t)) return enchanted;
        return normal || enchanted;
    }

    @Override
    protected void onDisable() {
        stopEating(Minecraft.getInstance(), true);
        cooldown = 0;
    }
}
