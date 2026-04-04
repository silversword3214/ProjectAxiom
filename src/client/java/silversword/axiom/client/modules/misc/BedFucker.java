package silversword.axiom.client.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.misc.ShapeModeEnum;
import silversword.axiom.client.setting.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class BedFucker extends AxiomMod implements KeybindConfigurable {

    private final Minecraft mc = Minecraft.getInstance();
    private final Random random = new Random();

    // Keybind
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // Main settings
    private final SettingNumber range;
    private final SettingBoolean ignoreWalls;
    private final SettingBoolean breakRespawnAnchor;
    private final SettingNumber breakDelayTicks;       // delay between breaks (ticks)
    private final SettingBoolean randomDelay;
    private final SettingBoolean silentRotate;
    private final SettingNumber breakProgress;         // 0-1, 1 = instant break

    // ESP
    private final SettingBoolean renderESP;
    private final SettingColor espColor;

    // Cooldown
    private int breakCooldown = 0;
    private BlockPos currentTarget = null;

    public BedFucker() {
        super("BedFucker", "Automatically breaks beds (and anchors) through walls", ModuleCategory.PLAYER);

        range = new SettingNumber("Range", 2.0, 10.0, 0.5, 5.0);
        ignoreWalls = new SettingBoolean("Ignore Walls", true);
        breakRespawnAnchor = new SettingBoolean("Break Respawn Anchor", false);
        breakDelayTicks = new SettingNumber("Delay (ticks)", 1, 40, 1, 4);
        randomDelay = new SettingBoolean("Random Delay", true);
        silentRotate = new SettingBoolean("Silent Rotate", true);
        breakProgress = new SettingNumber("Break Progress", 0.1, 1.0, 0.05, 1.0);

        renderESP = new SettingBoolean("Render ESP", true);
        espColor = new SettingColor("ESP Color", new Color(255, 80, 80, 100));

        addSetting(range);
        addSetting(ignoreWalls);
        addSetting(breakRespawnAnchor);
        addSetting(breakDelayTicks);
        addSetting(randomDelay);
        addSetting(silentRotate);
        addSetting(breakProgress);
        addSetting(renderESP);
        addHiddenSetting(espColor.getSetting());
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        if (!isEnabled() || mc.player == null || mc.level == null) return;

        if (breakCooldown > 0) {
            breakCooldown--;
            return;
        }

        // Find the nearest bed (or anchor)
        BlockPos target = findClosestTarget();
        if (target == null) return;

        // Check line of sight if not ignoring walls
        if (!ignoreWalls.get() && !hasLineOfSight(target)) return;

        // Break the block
        breakBlock(target);

        // Apply cooldown
        int delay = (int) breakDelayTicks.getValue();
        if (randomDelay.get()) {
            delay += random.nextInt(Math.max(1, delay / 2));
        }
        breakCooldown = delay;
    }

    @Subscribe
    private void onRender3D(Render3DEvent event) {
        if (!isEnabled() || !renderESP.get() || mc.player == null || mc.level == null) return;

        double maxDist = range.getValue();
        Vec3 eyePos = mc.player.getEyePosition();

        for (BlockPos pos : getTargetsInRange()) {
            if (eyePos.distanceToSqr(Vec3.atCenterOf(pos)) > maxDist * maxDist) continue;

            // For beds, draw the combined bounding box of both parts
            BlockState state = mc.level.getBlockState(pos);
            if (state.getBlock() instanceof BedBlock) {
                drawBedESP(event, pos, state);
            } else {
                drawBlockESP(event, pos);
            }
        }
    }

    // -------------------------------------------------------------------------
    //  Target detection
    // -------------------------------------------------------------------------

    private BlockPos findClosestTarget() {
        BlockPos closest = null;
        double closestDistSq = Double.MAX_VALUE;
        Vec3 eyePos = mc.player.getEyePosition();
        double maxDist = range.getValue();

        for (BlockPos pos : getTargetsInRange()) {
            double distSq = eyePos.distanceToSqr(Vec3.atCenterOf(pos));
            if (distSq <= maxDist * maxDist && distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = pos;
            }
        }
        return closest;
    }

    private List<BlockPos> getTargetsInRange() {
        List<BlockPos> targets = new ArrayList<>();
        if (mc.player == null || mc.level == null) return targets;

        int r = (int) Math.ceil(range.getValue());
        BlockPos center = mc.player.blockPosition();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (isBreakable(state)) {
                        // For beds, add both parts only once (avoid duplicates)
                        if (state.getBlock() instanceof BedBlock) {
                            if (state.getValue(BedBlock.PART) == BedPart.HEAD) {
                                targets.add(pos);
                            }
                        } else {
                            targets.add(pos);
                        }
                    }
                }
            }
        }
        return targets;
    }

    private boolean isBreakable(BlockState state) {
        if (state.getBlock() instanceof BedBlock) return true;
        if (breakRespawnAnchor.get() && state.getBlock() == Blocks.RESPAWN_ANCHOR) return true;
        return false;
    }

    private boolean hasLineOfSight(BlockPos pos) {
        if (mc.player == null || mc.level == null) return false;
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 targetPos = Vec3.atCenterOf(pos);
        BlockHitResult hit = mc.level.clip(new ClipContext(
                eyePos,
                targetPos,
                ClipContext.Block.COLLIDER,  // <- oikea vakio
                ClipContext.Fluid.NONE,      // <- oikea vakio
                mc.player
        ));
        return hit.getBlockPos().equals(pos);
    }


    private void breakBlock(BlockPos pos) {
        if (mc.player == null || mc.getConnection() == null) return;

        // Silent rotation: temporarily change player's yaw/pitch to face the block
        float[] originalRot = null;
        if (silentRotate.get()) {
            originalRot = new float[]{mc.player.getYRot(), mc.player.getXRot()};
            Vec3 eyePos = mc.player.getEyePosition();
            Vec3 targetVec = Vec3.atCenterOf(pos).subtract(eyePos).normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(targetVec.z, targetVec.x)) - 90;
            float pitch = (float) -Math.toDegrees(Math.asin(targetVec.y));
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        }


        mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pos, Direction.UP, 0));

        if (breakProgress.getValue() >= 0.99) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    pos, Direction.UP, 0));
        } else {

            mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    pos, Direction.UP, 0));
        }


        if (silentRotate.get() && originalRot != null) {
            mc.player.setYRot(originalRot[0]);
            mc.player.setXRot(originalRot[1]);
        }
    }


    private void drawBlockESP(Render3DEvent event, BlockPos pos) {
        event.getRenderer().drawBox(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                espColor.getCurrentColor().getARGB(),  // filled color
                0,                                     // no outline
                ShapeModeEnum.SIDES,                  // only filled
                0
        );
    }

    private void drawBedESP(Render3DEvent event, BlockPos pos, BlockState state) {
        // Find the other part of the bed
        BedPart part = state.getValue(BedBlock.PART);
        BlockPos otherPos = part == BedPart.HEAD ? pos.relative(state.getValue(BedBlock.FACING).getOpposite())
                : pos.relative(state.getValue(BedBlock.FACING));
        BlockPos min = new BlockPos(Math.min(pos.getX(), otherPos.getX()),
                Math.min(pos.getY(), otherPos.getY()),
                Math.min(pos.getZ(), otherPos.getZ()));
        BlockPos max = new BlockPos(Math.max(pos.getX(), otherPos.getX()) + 1,
                Math.max(pos.getY(), otherPos.getY()) + 1,
                Math.max(pos.getZ(), otherPos.getZ()) + 1);

        event.getRenderer().drawBox(
                min.getX(), min.getY(), min.getZ(),
                max.getX(), max.getY(), max.getZ(),
                espColor.getCurrentColor().getARGB(),
                0, ShapeModeEnum.SIDES, 0
        );
    }
}