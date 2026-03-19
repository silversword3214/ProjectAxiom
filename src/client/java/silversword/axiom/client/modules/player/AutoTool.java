package silversword.axiom.client.modules.player;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

public class AutoTool extends AxiomMod implements KeybindConfigurable {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private BlockPos lastBlockPos = null;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public AutoTool() {
        super("Auto Tool", "Automatically switches to the best tool for the block you're breaking", ModuleCategory.PLAYER);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.crosshairTarget == null) return;

        // Varmistetaan, että kohde on block
        if (!(mc.crosshairTarget instanceof BlockHitResult blockHit)) return;

        BlockPos blockPos = blockHit.getBlockPos();

        // Vaihtaa vain kun aloitat uuden lohkon hakkaamisen
        if (lastBlockPos == null || !lastBlockPos.equals(blockPos)) {
            lastBlockPos = blockPos;
            switchToOptimalTool(blockPos);
        }
    }

    private void switchToOptimalTool(BlockPos blockPos) {
        BlockState blockState = mc.world.getBlockState(blockPos);

        int bestSlot = -1;
        float bestSpeed = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            // Laskee kuinka nopea työkalu on louhimiseen
            float speed = stack.getMiningSpeedMultiplier(blockState);

            // Tarkista onko työkalu sopiva tälle lohkolle
            if (!stack.isSuitableFor(blockState)) {
                speed = -1;
            }

            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        // Vaihda slot, jos löytyi parempi työkalu
        if (bestSlot != -1) {
            mc.player.getInventory().setSelectedSlot(bestSlot);
        }
    }

    public static boolean isTool(ItemStack itemStack) {
        return itemStack.isIn(ItemTags.AXES) ||
                itemStack.isIn(ItemTags.HOES) ||
                itemStack.isIn(ItemTags.PICKAXES) ||
                itemStack.isIn(ItemTags.SHOVELS);
    }
}