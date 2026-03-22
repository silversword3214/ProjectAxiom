package silversword.axiom.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

public class ShieldBreaker extends AxiomMod implements KeybindConfigurable {

    private final Minecraft mc = Minecraft.getInstance();

    public final SettingNumber range = new SettingNumber("Range", 1, 6, 0.5, 4.5);
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0, true);
    public final SettingKeybind triggerKey = new SettingKeybind("Trigger Key", 0, false);

    private int originalSlot = -1;
    private boolean actionInProgress = false;
    private int step = 0; // 0 = idle, 1 = vaihda kirves, 2 = lyö, 3 = vaihda takaisin
    private long lastActionTime = 0;
    private boolean wasKeyPressed = false;

    public ShieldBreaker() {
        super("ShieldBreaker", "Press key to instantly axe-switch-hit and switch back", ModuleCategory.COMBAT);
        addSetting(range);
        addHiddenSetting(triggerKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return triggerKey;
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;

        // Keybindin painallus käynnistää toiminnon (ei togglaa moduulia)
        int key = triggerKey.get();
        if (key != 0) {
            long handle = mc.getWindow().handle();
            boolean pressed = org.lwjgl.glfw.GLFW.glfwGetKey(handle, key) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            if (pressed && !wasKeyPressed) {
                trigger();
            }
            wasKeyPressed = pressed;
        }

        // Jos toiminto on käynnissä, suorita vaiheet
        if (actionInProgress) {
            long now = System.currentTimeMillis();
            if (now - lastActionTime < 50) return; // pieni viive vaiheiden välillä (50 ms)

            switch (step) {
                case 1: // Vaihda kirveeseen
                    int axeSlot = findAxeInHotbar();
                    if (axeSlot == -1) {
                        actionInProgress = false; // ei kirvestä, lopeta
                        break;
                    }
                    originalSlot = mc.player.getInventory().getSelectedSlot();
                    mc.player.getInventory().setSelectedSlot(axeSlot);
                    step = 2;
                    lastActionTime = now;
                    break;
                case 2: // Lyö
                    Player target = findTarget();
                    if (target != null) {
                        mc.gameMode.attack(mc.player, target);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                    step = 3;
                    lastActionTime = now;
                    break;
                case 3: // Vaihda takaisin
                    if (originalSlot != -1) {
                        mc.player.getInventory().setSelectedSlot(originalSlot);
                    }
                    actionInProgress = false;
                    step = 0;
                    break;
            }
        }
    }

    private void trigger() {
        if (!actionInProgress && mc.player != null) {
            originalSlot = mc.player.getInventory().getSelectedSlot();
            actionInProgress = true;
            step = 1;
            lastActionTime = System.currentTimeMillis();
        }
    }

    private Player findTarget() {
        double maxDist = range.getValue();
        Player closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Player player : mc.level.players()) {
            if (player == mc.player || player.isDeadOrDying()) continue;
            double dist = mc.player.distanceTo(player);
            if (dist < maxDist && dist < closestDist) {
                closest = player;
                closestDist = dist;
            }
        }
        return closest;
    }

    private int findAxeInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }
}