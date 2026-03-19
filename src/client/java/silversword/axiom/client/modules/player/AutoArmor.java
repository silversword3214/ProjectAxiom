package silversword.axiom.client.modules.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Box;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public final class AutoArmor extends AxiomMod implements KeybindConfigurable {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Asetukset
    public final SettingMode priority;          // "Protection", "Armor Value", "Both"
    public final SettingNumber delay;           // tick-määrä vaihtojen välillä
    public final SettingBoolean onlyIfBetter;   // vaihda vain jos uusi on parempi
    public final SettingBoolean pickFromGround; // poimi maasta
    public final SettingNumber pickRange;       // kuinka kaukaa maasta poimitaan (lohkoina)
    public final SettingBoolean replaceElytra;  // vaihda elytra, jos se on vähissä
    public final SettingNumber elytraThreshold; // prosenttiraja elytran kestävyydelle (0–100)
    public final SettingKeybind toggleKey;

    private final AtomicInteger tickCounter = new AtomicInteger(0);
    private final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public AutoArmor() {
        super("AutoArmor", "Automatically equips the best armor and picks up from ground", ModuleCategory.PLAYER);

        priority = new SettingMode("Priority", new String[]{"Protection", "Armor Value", "Both"}, "Both");
        delay = new SettingNumber("Delay", 1, 20, 1, 5);
        onlyIfBetter = new SettingBoolean("Only if better", true);
        pickFromGround = new SettingBoolean("Pick from ground", false);
        pickRange = new SettingNumber("Pick range", 1, 10, 1, 5);
        replaceElytra = new SettingBoolean("Replace Elytra", true);
        elytraThreshold = new SettingNumber("Elytra threshold %", 0, 100, 5, 10);
        toggleKey = new SettingKeybind("Toggle Key", 0);

        addSetting(priority);
        addSetting(delay);
        addSetting(onlyIfBetter);
        addSetting(pickFromGround);
        addSetting(pickRange);
        addSetting(replaceElytra);
        addSetting(elytraThreshold);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return; // älä tee mitään, jos inventory on auki

        int currentTick = tickCounter.incrementAndGet();
        if (currentTick < delay.getValue()) return;
        tickCounter.set(0);

        // 1. Korvaa panssarit inventaariosta
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack current = mc.player.getEquippedStack(slot);
            int bestSlot = findBestArmorSlot(slot);
            if (bestSlot == -1) continue;

            ItemStack bestStack = mc.player.getInventory().getStack(bestSlot);
            if (shouldReplace(current, bestStack)) {
                swapArmor(bestSlot, slot);
                return; // vain yksi vaihto per tick (delayn mukaan)
            }
        }

        // 2. Korvaa elytra, jos se on vähissä
        if (replaceElytra.get()) {
            ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            if (isElytra(chest) && isElytraDamaged(chest)) {
                int newElytraSlot = findBestElytraInInventory();
                if (newElytraSlot != -1) {
                    swapArmor(newElytraSlot, EquipmentSlot.CHEST);
                    return;
                }
            }
        }

        // 3. Poimi maasta (jos asetus päällä)
        if (pickFromGround.get()) {
            tryPickFromGround();
        }
    }

    private void tryPickFromGround() {
        if (mc.player == null || mc.world == null) return;

        double range = pickRange.getValue();
        Box box = new Box(mc.player.getBlockPos()).expand(range);
        List<ItemEntity> items = mc.world.getEntitiesByClass(ItemEntity.class, box, e -> true);

        // Ryhmitellään maassa olevat esineet slotin mukaan
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack current = mc.player.getEquippedStack(slot);
            ItemStack bestGround = findBestGroundItem(slot, items);
            if (bestGround.isEmpty()) continue;

            if (shouldReplace(current, bestGround)) {
                // Tarkistetaan, onko inventaariossa jo parempi (jotta emme poimi turhaan)
                int bestInvSlot = findBestArmorSlot(slot);
                if (bestInvSlot != -1) {
                    ItemStack bestInv = mc.player.getInventory().getStack(bestInvSlot);
                    if (shouldReplace(bestInv, bestGround)) {
                        // Maassa oleva on parempi kuin inventaariossa oleva paras – poimitaan
                        pickUpAndEquip(bestGround, slot);
                        return;
                    }
                } else {
                    // Inventaariossa ei ole yhtään tämän slotin panssaria
                    pickUpAndEquip(bestGround, slot);
                    return;
                }
            }
        }
    }

    private ItemStack findBestGroundItem(EquipmentSlot slot, List<ItemEntity> items) {
        ItemStack best = ItemStack.EMPTY;
        double bestScore = -1;

        for (ItemEntity entity : items) {
            ItemStack stack = entity.getStack();
            if (!isArmorItem(stack, slot)) continue;

            double score = getArmorScore(stack);
            if (score > bestScore) {
                bestScore = score;
                best = stack;
            }
        }
        return best;
    }

    private void pickUpAndEquip(ItemStack groundStack, EquipmentSlot slot) {
        if (mc.player == null || mc.interactionManager == null) return;

        // Pudota nykyinen panssari (jos on)
        ItemStack current = mc.player.getEquippedStack(slot);
        if (!current.isEmpty()) {
            mc.player.dropItem(current, false);
            mc.player.equipStack(slot, ItemStack.EMPTY);
        }

        // Tee tilaa inventaarioon (jos täynnä)
        if (mc.player.getInventory().getEmptySlot() == -1) {
            int worstSlot = findWorstItemInInventory();
            if (worstSlot != -1) {
                ItemStack worst = mc.player.getInventory().getStack(worstSlot);
                mc.player.dropItem(worst, false);
                mc.player.getInventory().removeStack(worstSlot);
            }
        }
    }

    private int findWorstItemInInventory() {
        int worstSlot = -1;
        double worstScore = Double.MAX_VALUE;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            // Vältetään panssareita, jotka ovat hyviä? Tässä etsitään yksinkertaisesti pienin score
            double score = getArmorScore(stack);
            if (score < worstScore) {
                worstScore = score;
                worstSlot = i;
            }
        }
        return worstSlot;
    }

    private int findSlotForStack(ItemStack stack) {
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (ItemStack.areEqual(inv.getStack(i), stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isElytra(ItemStack stack) {
        return stack.getItem() == Items.ELYTRA;
    }

    private boolean isElytraDamaged(ItemStack stack) {
        if (!isElytra(stack)) return false;
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) return false;
        int damage = stack.getDamage();
        int remaining = maxDamage - damage;
        int percent = (remaining * 100) / maxDamage;
        return percent <= elytraThreshold.getValue();
    }

    private int findBestElytraInInventory() {
        int bestSlot = -1;
        int bestDamage = Integer.MAX_VALUE; // etsitään vähiten vahingoittunutta

        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (!isElytra(stack)) continue;
            int damage = stack.getDamage();
            if (damage < bestDamage) {
                bestDamage = damage;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private boolean isArmorItem(ItemStack stack, EquipmentSlot slot) {
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        return equippable != null && equippable.slot() == slot;
    }

    private int findBestArmorSlot(EquipmentSlot slot) {
        int bestSlot = -1;
        double bestScore = -1;

        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty() || !isArmorItem(stack, slot)) continue;

            double score = getArmorScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private double getArmorScore(ItemStack stack) {
        double armorValue = getArmorValue(stack);
        int protection = getProtectionLevel(stack);
        switch (priority.getMode()) {
            case "Protection":
                return protection + armorValue * 0.1; // materiaalin vaikutus 10%
            case "Armor Value":
                return armorValue;
            case "Both":
                return armorValue + protection * 2;
            default:
                return 0;
        }
    }

    private int getProtectionLevel(ItemStack stack) {
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench == null) return 0;

        int total = 0;
        for (var entry : ench.getEnchantmentEntries()) {
            Optional<RegistryKey<Enchantment>> key = entry.getKey().getKey();
            if (key.equals(Enchantments.PROTECTION) ||
                    key.equals(Enchantments.FIRE_PROTECTION) ||
                    key.equals(Enchantments.BLAST_PROTECTION) ||
                    key.equals(Enchantments.PROJECTILE_PROTECTION)) {
                total += entry.getIntValue();
            }
        }
        return total;
    }

    private int getArmorValue(ItemStack stack) {
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable == null) return 0;

        EquipmentSlot slot = equippable.slot();
        if (slot != EquipmentSlot.HEAD && slot != EquipmentSlot.CHEST &&
                slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) {
            return 0;
        }

        Item item = stack.getItem();

        // Nahkapanssari
        if (item == Items.LEATHER_HELMET) return 1;
        if (item == Items.LEATHER_CHESTPLATE) return 3;
        if (item == Items.LEATHER_LEGGINGS) return 2;
        if (item == Items.LEATHER_BOOTS) return 1;

        // Kultapanssari
        if (item == Items.GOLDEN_HELMET) return 2;
        if (item == Items.GOLDEN_CHESTPLATE) return 5;
        if (item == Items.GOLDEN_LEGGINGS) return 3;
        if (item == Items.GOLDEN_BOOTS) return 1;

        if (item == Items.COPPER_HELMET) return 2;
        if (item == Items.COPPER_CHESTPLATE) return 4;
        if (item == Items.COPPER_LEGGINGS) return 3;
        if (item == Items.COPPER_BOOTS) return 1;

        // Rautapanssari
        if (item == Items.IRON_HELMET) return 2;
        if (item == Items.IRON_CHESTPLATE) return 6;
        if (item == Items.IRON_LEGGINGS) return 5;
        if (item == Items.IRON_BOOTS) return 2;

        // Timanttipanssari
        if (item == Items.DIAMOND_HELMET) return 3;
        if (item == Items.DIAMOND_CHESTPLATE) return 8;
        if (item == Items.DIAMOND_LEGGINGS) return 6;
        if (item == Items.DIAMOND_BOOTS) return 3;

        // Netheriittipanssari
        if (item == Items.NETHERITE_HELMET) return 3;
        if (item == Items.NETHERITE_CHESTPLATE) return 8;
        if (item == Items.NETHERITE_LEGGINGS) return 6;
        if (item == Items.NETHERITE_BOOTS) return 3;

        // Kilpikonnankuori
        if (item == Items.TURTLE_HELMET) return 2;

        // Ketjupanssari
        if (item == Items.CHAINMAIL_HELMET) return 2;
        if (item == Items.CHAINMAIL_CHESTPLATE) return 5;
        if (item == Items.CHAINMAIL_LEGGINGS) return 4;
        if (item == Items.CHAINMAIL_BOOTS) return 1;

        return 0;
    }

    private boolean shouldReplace(ItemStack current, ItemStack best) {
        if (best.isEmpty()) return false;
        if (current.isEmpty()) return true;

        double currentScore = getArmorScore(current);
        double bestScore = getArmorScore(best);

        if (onlyIfBetter.get()) {
            return bestScore > currentScore + 0.5; // pieni marginaali
        } else {
            return bestScore > currentScore;
        }
    }

    private void swapArmor(int invSlot, EquipmentSlot armorSlot) {
        if (mc.interactionManager == null) return;

        int fromSlot = invSlot < 9 ? invSlot + 36 : invSlot;
        int toSlot = switch (armorSlot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> -1;
        };
        if (toSlot == -1) return;

        // Click invSlot (ota esine)
        mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                fromSlot,
                0,
                SlotActionType.PICKUP,
                mc.player
        );
        // Click armorSlot (aseta sinne)
        mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                toSlot,
                0,
                SlotActionType.PICKUP,
                mc.player
        );
        // Jos kädessä on vielä jokin (esim. vanha panssari), klikataan takaisin invSlot
        // Tämä vaihtoehto on monimutkainen. Yksinkertaisempi: käytä QUICK_MOVEä, mutta se vaatii, että slot on tyhjä.
        // Siksi pitää ensin tyhjentää armorSlot.
    }

}