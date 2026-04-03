package silversword.axiom.client.modules.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingSlider;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class AutoFarm extends AxiomMod implements KeybindConfigurable {

    private final SettingSlider range = new SettingSlider("Range", new double[]{2, 3, 4, 5, 6}, 4.5);
    private final SettingBoolean autoHoe = new SettingBoolean("Auto Hoe", true);
    private final SettingBoolean autoPlant = new SettingBoolean("Auto Plant", true);
    private final SettingBoolean packetMine = new SettingBoolean("Packet Break", true);
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private int timer = 0;

    public AutoFarm() {
        super("Auto Farm", "Automates harvesting, hoeing and planting", ModuleCategory.MISC);

        addSetting(range);
        addSetting(autoHoe);
        addSetting(autoPlant);
        addSetting(packetMine);
        addSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        if (!isEnabled() || mc.player == null || mc.level == null) return;

        // Pieni viive (1 action / 2 tickiä) ettei serveri potki ulos
        if (timer > 0) {
            timer--;
            return;
        }

        BlockPos playerPos = mc.player.blockPosition();
        double r = range.getValue();

        for (double x = -r; x <= r; x++) {
            for (double y = -r; y <= r; y++) {
                for (double z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.offset((int)x, (int)y, (int)z);
                    BlockState state = mc.level.getBlockState(pos);
                    Block block = state.getBlock();

                    // 1. HARVEST (Täysikasvuiset kasvit)
                    if (block instanceof CropBlock crops && crops.isMaxAge(state)) {
                        harvest(pos);
                        timer = 2;
                        return;
                    }

                    // 2. HOE (Mullan muokkaus pelloksi)
                    if (autoHoe.get() && (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK)) {
                        if (mc.level.getBlockState(pos.above()).isAir()) {
                            useHoe(pos);
                            timer = 2;
                            return;
                        }
                    }

                    // 3. PLANT (Istutus tyhjälle pellolle)
                    if (autoPlant.get() && block == Blocks.FARMLAND) {
                        if (mc.level.getBlockState(pos.above()).isAir()) {
                            plant(pos);
                            timer = 2;
                            return;
                        }
                    }
                }
            }
        }
    }

    private void harvest(BlockPos pos) {
        if (packetMine.get()) {
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
        } else {
            mc.gameMode.destroyBlock(pos);
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void useHoe(BlockPos pos) {
        int hoeSlot = findHoe();
        if (hoeSlot != -1) {
            int oldSlot = mc.player.getInventory().selected;
            switchToSlot(hoeSlot);
            sendInteract(pos);
            // Jos haluat palauttaa slotin heti: switchToSlot(oldSlot);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private void plant(BlockPos pos) {
        int seedSlot = findSeeds();
        if (seedSlot != -1) {
            switchToSlot(seedSlot);
            sendInteract(pos);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private void sendInteract(BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(
                new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5),
                Direction.UP, pos, false);

        mc.player.connection.send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, 0));
    }

    private void switchToSlot(int slot) {
        if (mc.player.getInventory().selected != slot) {
            mc.player.getInventory().selected = slot;
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        }
    }

    private int findHoe() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof HoeItem) return i;
        }
        return -1;
    }

    private int findSeeds() {
        Item[] seeds = {Items.WHEAT_SEEDS, Items.POTATO, Items.CARROT, Items.BEETROOT_SEEDS, Items.PUMPKIN_SEEDS, Items.MELON_SEEDS, Items.NETHER_WART};
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            for (Item seed : seeds) {
                if (stack.is(seed)) return i;
            }
        }
        return -1;
    }

    private void reset() {
        timer = 0;
    }
}