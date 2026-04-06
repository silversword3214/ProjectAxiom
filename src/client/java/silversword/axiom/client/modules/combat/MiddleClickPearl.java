package silversword.axiom.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;

public final class MiddleClickPearl extends AxiomMod {

    private boolean wasPressed = false;
    private int oldSlot = -1;
    private boolean needsRestore = false;

    public MiddleClickPearl() {
        super("Middle Click Pearl", "Throws a pearl with scroll-wheel button", ModuleCategory.COMBAT);
    }

    @Override
    protected void onTick() {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null || mc.screen != null) return;

        boolean isPressed = mc.mouseHandler.isMiddlePressed();

        if (isPressed && !wasPressed) {
            int pearlSlot = findPearlInHotbar(mc.player.getInventory());

            if (pearlSlot != -1) {
                oldSlot = mc.player.getInventory().selected;

                mc.player.getInventory().selected = pearlSlot;

                mc.options.keyUse.setDown(true);
                needsRestore = true;
            }
        }

        if (needsRestore && (!isPressed || mc.player.getInventory().getItem(mc.player.getInventory().selected).isEmpty())) {
            mc.options.keyUse.setDown(false);

            if (oldSlot != -1) {
                mc.player.getInventory().selected = oldSlot;
            }

            needsRestore = false;
            oldSlot = -1;
        }

        wasPressed = isPressed;
    }

    private int findPearlInHotbar(Inventory inv) {
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(Items.ENDER_PEARL)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null && needsRestore) {
            mc.options.keyUse.setDown(false);
            if (oldSlot != -1 && mc.player != null) {
                mc.player.getInventory().selected = oldSlot;
            }
        }
        needsRestore = false;
    }
}