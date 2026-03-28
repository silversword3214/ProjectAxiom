package silversword.axiom.client.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.render.rendersystem.utils.misc.ShapeModeEnum;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingSlider;
import silversword.axiom.mixin.client.accessors.MultiPlayerGameModeAccessor;
import silversword.axiom.client.eventbus.Subscribe;

import java.util.*;

public class FastBreak extends AxiomMod implements KeybindConfigurable {

    private final Minecraft mc = Minecraft.getInstance();

    // ---- ASETUKSET ----
    private final SettingSlider speed = new SettingSlider(
            "Speed",
            new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10},
            2
    );

    private final SettingBoolean legitMode = new SettingBoolean(
            "Legit Mode",
            false
    );

    private final SettingBoolean fastMine = new SettingBoolean(
            "Fast Mine",
            false
    );

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // ---- LEGIT-TILA (yksi blokki kerrallaan, vanha) ----
    private BlockPos lastBlock;
    private int tickCounter = 0;

    // ---- MONI-BLOKKI -TILA (uusi, PacketMine-tyylinen) ----
    private final List<BlockEntry> blocks = new ArrayList<>();
    private boolean shouldUpdateSlot = false; // ei käytetä vielä, mutta varataan
    private BlockPos lastAttackedBlock = null;

    public FastBreak() {
        super("Fast Break", "Speeds up block breaking and removes the delay.", ModuleCategory.WORLD);

        addSetting(speed);
        addSetting(legitMode);
        addSetting(fastMine); // uusi asetus
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (!(mc.hitResult instanceof BlockHitResult hit)) return;

        MultiPlayerGameMode manager = mc.gameMode;
        if (manager == null) return;

        // Poistetaan Minecraftin oma viive (vaikuttaa molempiin moodeihin)
        ((MultiPlayerGameModeAccessor) manager).setDestroyDelay(0);

        // --- LEGIT-TILA (vanha) ---
        if (legitMode.get()) {
            BlockPos pos = hit.getBlockPos();
            Direction dir = hit.getDirection();

            int speedLevel = (int) speed.getValue();

            tickCounter++;
            if (tickCounter < (6 - speedLevel)) return;
            tickCounter = 0;

            if (pos.equals(lastBlock) && speedLevel <= 2) return;

            lastBlock = pos;

            mc.getConnection().send(
                    new ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                            pos,
                            dir
                    )
            );
            return;
        }

        // --- MONI-BLOKKI -TILA (PacketMine-tyylinen) ---
        // 1) Poistetaan vanhat tai tuhoutuneet blokit
        blocks.removeIf(BlockEntry::shouldRemove);

        // 2) Lisätään uusi blokki, jos klikattu uutta
        if (mc.options.keyAttack.isDown()) {
            BlockPos targetPos = hit.getBlockPos();
            if (!targetPos.equals(lastAttackedBlock)) {
                // Tarkistetaan, ettei blokki ole jo listassa
                boolean alreadyMining = false;
                for (BlockEntry entry : blocks) {
                    if (entry.blockPos.equals(targetPos)) {
                        alreadyMining = true;
                        break;
                    }
                }
                if (!alreadyMining && BlockUtils.canBreak(targetPos)) {
                    blocks.add(new BlockEntry(targetPos, hit.getDirection()));
                }
                lastAttackedBlock = targetPos;
            }
        } else {
            lastAttackedBlock = null;
        }

        // 3) Käsitellään ensimmäistä blokkia listassa (jos on)
        if (!blocks.isEmpty()) {
            BlockEntry block = blocks.get(0);
            block.mine();
        }
    }

    // Renderöidään liila laatikko jokaisen listassa olevan blokin ympärille
    @Subscribe
    private void onRender3D(Render3DEvent event) {
        if (!isEnabled() || legitMode.get()) return;
        if (blocks.isEmpty()) return;

        int fillColor = 0x20FF00FF; // läpinäkyvä liila
        int lineColor = 0xFFAA00FF; // kirkas liila reunaviiva
        ShapeModeEnum mode = ShapeModeEnum.BOTH;

        for (BlockEntry block : blocks) {
            BlockState state = mc.level.getBlockState(block.blockPos);
            if (state.isAir()) continue;

            AABB box = state.getShape(mc.level, block.blockPos).bounds();
            double x1 = block.blockPos.getX() + box.minX;
            double y1 = block.blockPos.getY() + box.minY;
            double z1 = block.blockPos.getZ() + box.minZ;
            double x2 = block.blockPos.getX() + box.maxX;
            double y2 = block.blockPos.getY() + box.maxY;
            double z2 = block.blockPos.getZ() + box.maxZ;

            event.getRenderer().drawBox(x1, y1, z1, x2, y2, z2, fillColor, lineColor, mode, 0);
        }
    }

    @Override
    protected void onDisable() {
        lastBlock = null;
        tickCounter = 0;
        blocks.clear();
        lastAttackedBlock = null;
        shouldUpdateSlot = false;
    }

    // Sisäinen luokka yhdelle rikottavalle blokille (kuten PacketMinen MyBlock)
    private class BlockEntry {
        public final BlockPos blockPos;
        public final Direction direction;
        public final Block block;
        public final BlockState blockState;
        public int timer;       // viive ennen ensimmäistä pakettia
        public int startTime;   // milloin aloitettiin rikkominen
        public boolean mining;  // onko START+STOP jo lähetetty
        public int lastPacketTick; // viimeisen paketin ticki (vain fastMine)

        public BlockEntry(BlockPos pos, Direction dir) {
            this.blockPos = pos;
            this.direction = dir;
            this.blockState = mc.level.getBlockState(pos);
            this.block = blockState.getBlock();
            int speedLevel = (int) speed.getValue();
            // speed 1..5 -> delay 5..1 (nopeampi speed -> pienempi delay)
            this.timer = Math.max(0, 6 - speedLevel);
            this.mining = false;
            this.lastPacketTick = 0;
        }

        public boolean shouldRemove() {
            boolean broken = mc.level.getBlockState(blockPos).getBlock() != block;
            boolean timeout = (mining && (mc.player.tickCount - startTime > 120)); // 5 sekuntia timeout
            boolean distance = mc.player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > (mc.player.blockInteractionRange() * mc.player.blockInteractionRange());
            return broken || timeout || distance;
        }

        public void mine() {
            // Jos Fast Mine on päällä, ohitetaan alkuviive ja lähetetään paketteja toistuvasti
            if (fastMine.get()) {
                int speedLevel = (int) speed.getValue();
                int delay = Math.max(1, 6 - speedLevel); // nopeus 5 -> 1 tick, nopeus 1 -> 5 tickiä
                int currentTick = mc.player.tickCount;

                if (!mining) {
                    // Lähetetään START kerran
                    mc.getConnection().send(
                            new ServerboundPlayerActionPacket(
                                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                                    blockPos,
                                    direction
                            )
                    );
                    mining = true;
                    startTime = currentTick;
                    lastPacketTick = currentTick - delay; // sallitaan välitön paketti
                }

                // Lähetetään STOP-paketti, jos viive on kulunut
                if (currentTick - lastPacketTick >= delay) {
                    mc.getConnection().send(
                            new ServerboundPlayerActionPacket(
                                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                                    blockPos,
                                    direction
                            )
                    );
                    lastPacketTick = currentTick;
                }
                return;
            }

            // Alkuperäinen toiminta (Fast Mine pois)
            if (timer > 0) {
                timer--;
                return;
            }

            if (!mining) {
                // Lähetetään START ja STOP samassa tickissä (kuten PacketMine)
                mc.getConnection().send(
                        new ServerboundPlayerActionPacket(
                                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                                blockPos,
                                direction
                        )
                );
                mc.getConnection().send(
                        new ServerboundPlayerActionPacket(
                                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                                blockPos,
                                direction
                        )
                );
                mining = true;
                startTime = mc.player.tickCount;
            } else {
            }
        }
    }
    private static class BlockUtils {
        public static boolean canBreak(BlockPos pos) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return false;
            BlockState state = mc.level.getBlockState(pos);
            return !state.isAir() && state.getDestroySpeed(mc.level, pos) != -1.0f;
        }
    }
}