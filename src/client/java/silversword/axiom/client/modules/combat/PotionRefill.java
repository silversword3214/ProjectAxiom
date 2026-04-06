package silversword.axiom.client.modules.combat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.autopot.AutoPotWindow;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.setting.SettingString;

import java.util.*;

public final class PotionRefill extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    public final SettingNumber refillDelayTicks = new SettingNumber("Refill Delay (t)", 0, 40, 1, 5);

    private final SettingString selectedEffectsJson = new SettingString("SelectedEffects", "[]");
    private final SettingString selectedAmountsJson = new SettingString("SelectedAmounts", "{}");

    private Set<String> selectedEffects = new HashSet<>();
    private Map<String, Integer> selectedAmounts = new HashMap<>();
    private boolean loaded = false;

    private int delayTimer = 0;

    public PotionRefill() {
        super("Potion Refill", "Automatically refills selected potions to hotbar", ModuleCategory.COMBAT);
        addHiddenSetting(toggleKey);
        addSetting(refillDelayTicks);
        addHiddenSetting(selectedEffectsJson);
        addHiddenSetting(selectedAmountsJson);
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    // --- LATAUS JA TALLENNUS ---

    private void loadSettings() {
        if (loaded) return;
        loaded = true;

        Gson gson = new Gson();

        // Ladataan mitkä efektit on päällä
        String effectsJson = selectedEffectsJson.getString();
        if (effectsJson != null && !effectsJson.isEmpty()) {
            try {
                selectedEffects = gson.fromJson(effectsJson, new TypeToken<Set<String>>(){}.getType());
            } catch (Exception e) { selectedEffects = new HashSet<>(); }
        }
        if (selectedEffects == null) selectedEffects = new HashSet<>();

        // Ladataan kuinka monta kutakin efektiä halutaan
        String amountsJson = selectedAmountsJson.getString();
        if (amountsJson != null && !amountsJson.isEmpty()) {
            try {
                selectedAmounts = gson.fromJson(amountsJson, new TypeToken<Map<String, Integer>>(){}.getType());
            } catch (Exception e) { selectedAmounts = new HashMap<>(); }
        }
        if (selectedAmounts == null) selectedAmounts = new HashMap<>();
    }

    private void saveSettings() {
        Gson gson = new Gson();
        selectedEffectsJson.setValue(gson.toJson(selectedEffects));
        selectedAmountsJson.setValue(gson.toJson(selectedAmounts));
    }

    // --- METODIT IKKUNALLE (Window/Entry) ---

    public boolean isEffectSelected(Identifier id) {
        loadSettings();
        return selectedEffects.contains(id.toString());
    }

    public void setEffectSelected(Identifier id, boolean selected) {
        loadSettings();
        if (selected) selectedEffects.add(id.toString());
        else selectedEffects.remove(id.toString());
        saveSettings();
    }

    public int getTargetAmount(Identifier id) {
        loadSettings();
        return selectedAmounts.getOrDefault(id.toString(), 1);
    }

    public void setTargetAmount(Identifier id, int amount) {
        loadSettings();
        selectedAmounts.put(id.toString(), Math.max(1, Math.min(64, amount)));
        saveSettings();
    }

    public void openManager() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        factory.openPopupWindow("auto_pot_manager", "Select potions",
                (sw - 400)/2, (sh - 450)/2, 400, 450, new AutoPotWindow(this));
    }

    // --- REFILLER LOGIIKKA ---

    @Override
    protected void onTick() {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        Player player = mc.player;

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        loadSettings();
        if (selectedEffects.isEmpty()) return;

        boolean isInvOpen = mc.screen instanceof InventoryScreen;
        boolean isStill = player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() < 0.001;

        if (isInvOpen || isStill) {
            if (refillLogic(mc, player)) {
                delayTimer = (int) refillDelayTicks.getValue();
            }
        }
    }

    private boolean refillLogic(Minecraft mc, Player player) {
        for (String targetId : selectedEffects) {
            int targetCount = selectedAmounts.getOrDefault(targetId, 1);
            int currentCount = countPotionsInHotbar(player, targetId);

            if (currentCount < targetCount) {
                int backpackSlot = findPotionInBackpack(player, targetId);
                if (backpackSlot != -1) {
                    int hotbarSlot = getBestHotbarSlot(player);
                    if (hotbarSlot != -1) {
                        moveItem(mc, player, backpackSlot, hotbarSlot);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int countPotionsInHotbar(Player player, String targetId) {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            if (matchPotion(player.getInventory().getItem(i), targetId)) {
                count += player.getInventory().getItem(i).getCount();
            }
        }
        return count;
    }

    private int findPotionInBackpack(Player player, String targetId) {
        for (int i = 9; i < 36; i++) {
            if (matchPotion(player.getInventory().getItem(i), targetId)) return i;
        }
        return -1;
    }

    private boolean matchPotion(ItemStack stack, String targetId) {
        if (stack.isEmpty() || stack.getItem() != Items.SPLASH_POTION || !stack.has(DataComponents.POTION_CONTENTS)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;

        for (net.minecraft.world.effect.MobEffectInstance inst : contents.getAllEffects()) {
            Identifier effectId = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(inst.getEffect().value());
            if (effectId != null && effectId.toString().equals(targetId)) return true;
        }
        return false;
    }

    private int getBestHotbarSlot(Player player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).isEmpty()) return i;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            boolean isImportant = false;
            for (String id : selectedEffects) {
                if (matchPotion(stack, id)) { isImportant = true; break; }
            }
            if (!isImportant) return i;
        }
        return -1;
    }

    private void moveItem(Minecraft mc, Player player, int sourceSlot, int targetHotbarIndex) {
        int containerId = player.inventoryMenu.containerId;
        mc.gameMode.handleInventoryMouseClick(containerId, sourceSlot, targetHotbarIndex, ClickType.SWAP, player);
    }

    @Override
    protected void onDisable() {
        delayTimer = 0;
    }
}