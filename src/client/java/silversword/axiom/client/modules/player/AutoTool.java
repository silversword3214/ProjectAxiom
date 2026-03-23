package silversword.axiom.client.modules.player;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

public class AutoTool extends AxiomMod implements KeybindConfigurable {

    private final Minecraft mc = Minecraft.getInstance();
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
        if (!isEnabled() || mc.player == null || mc.hitResult == null) return;

        // Varmistetaan, että kohde on block
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;

        BlockPos blockPos = blockHit.getBlockPos();

        // Vaihtaa vain kun aloitat uuden lohkon hakkaamisen
        if (lastBlockPos == null || !lastBlockPos.equals(blockPos)) {
            lastBlockPos = blockPos;
            switchToOptimalTool(blockPos);
        }
    }

    private void switchToOptimalTool(BlockPos blockPos) {
        BlockState blockState = mc.level.getBlockState(blockPos);

        int bestSlot = -1;
        float bestSpeed = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            // Laskee kuinka nopea työkalu on louhimiseen
            float speed = stack.getDestroySpeed(blockState);

            // Tarkista onko työkalu sopiva tälle lohkolle
            if (!stack.isCorrectToolForDrops(blockState)) {
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
        return itemStack.is(ItemTags.AXES) ||
                itemStack.is(ItemTags.HOES) ||
                itemStack.is(ItemTags.PICKAXES) ||
                itemStack.is(ItemTags.SHOVELS);
    }
}