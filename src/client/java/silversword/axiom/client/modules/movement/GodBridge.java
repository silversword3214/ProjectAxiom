package silversword.axiom.client.modules.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.player.PreMotionEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.utils.Rotations;

import java.util.Arrays;
import java.util.List;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class GodBridge extends AxiomMod {

    private final SettingBoolean tower = new SettingBoolean("Tower", true);
    private final SettingBoolean legitMode = new SettingBoolean("Legit (Eagle)", true);
    private final SettingBoolean raytrace = new SettingBoolean("Raytrace Check", true);

    private final List<Class<? extends Block>> blacklistedBlocks = Arrays.asList(
            FallingBlock.class, ChestBlock.class, EnderChestBlock.class, TrapDoorBlock.class,
            ButtonBlock.class, LeverBlock.class, SaplingBlock.class, FlowerBlock.class,
            TallGrassBlock.class, WebBlock.class, TorchBlock.class, ScaffoldingBlock.class,
            StairBlock.class, SlabBlock.class, FenceBlock.class
    );

    private boolean isPlacing = false;
    private int placeDelay = 0;
    private int legitCooldown = 0;

    public GodBridge() {
        super("God Bridge", "Auto God bridge, not scaffold", ModuleCategory.MOVEMENT);
        addSetting(tower);
        addSetting(legitMode);
        addSetting(raytrace);
    }

    @Subscribe
    public void onPreMotion(PreMotionEvent event) {
        if (!isEnabled() || mc.player == null || mc.level == null) return;


        if (legitMode.get()) {
            if (legitCooldown > 0) {
                legitCooldown--;
                return;
            }
            if (placeDelay > 0) {
                placeDelay--;
                return;
            }
            placeDelay = 0;
        }

        BlockPos below = mc.player.blockPosition().below();
        boolean isOverAir = mc.level.getBlockState(below).isAir();

        if (legitMode.get()) {
            if (isOverAir) {
                mc.options.keyShift.setDown(true);
            } else if (mc.options.keyShift.isDown() && !mc.options.keyJump.isDown()) {
                mc.options.keyShift.setDown(false);
            }
        }

        if (!isOverAir) return;
        if (isPlacing || Rotations.isRotating()) return;

        double hSpeed = Math.hypot(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().z);
        if (hSpeed > 0.3 && !mc.player.onGround) return;

        BlockData data = getBlockData(below);
        if (data == null) return;

        int slot = getBestBlockSlot();
        if (slot == -1) return;
        final int finalSlot = slot;
        final BlockPos targetPos = data.pos;
        final Direction targetSide = data.side;

        Vec3 hitVec = getAccurateHitVec(targetPos, targetSide);
        float[] rots = calculateRotations(hitVec);

        float currentYaw = Rotations.getServerYaw();
        float currentPitch = Rotations.getServerPitch();
        float yawDiff = Math.abs(normalizeYawDiff(rots[0] - currentYaw));
        float pitchDiff = Math.abs(rots[1] - currentPitch);
        if (yawDiff < 1.5f && pitchDiff < 1.5f) {

            performPlacement(targetPos, targetSide, finalSlot);

            if (legitMode.get()) {
                legitCooldown = 1 + (int)(Math.random() * 2);
            }
            return;
        }

        isPlacing = true;
        int dynamicSteps = Math.max(3, (int)Math.ceil(Math.max(Math.abs(yawDiff)/10.0, Math.abs(pitchDiff)/5.0)));
        Rotations.rotateSmooth(rots[0], rots[1], dynamicSteps, () -> {
            try {
                if (!isEnabled() || mc.player == null || mc.level == null) return;
                BlockPos currentBelow = mc.player.blockPosition().below();
                if (!mc.level.getBlockState(currentBelow).isAir()) return;
                var neighborState = mc.level.getBlockState(targetPos);
                if (neighborState.isAir() || !neighborState.getFluidState().isEmpty()) return;
                int currentSlot = getBestBlockSlot();
                if (currentSlot == -1) return;
                performPlacement(targetPos, targetSide, currentSlot);
                if (legitMode.get()) {
                    legitCooldown = 1 + (int)(Math.random() * 2);
                }
            } finally {
                isPlacing = false;
            }
        });
    }

    private void performPlacement(BlockPos targetPos, Direction targetSide, int slot) {
        int oldSlot = mc.player.getInventory().selected;
        mc.player.getInventory().selected = slot;
        Vec3 hitVec = getAccurateHitVec(targetPos, targetSide);
        BlockHitResult hitResult = new BlockHitResult(hitVec, targetSide, targetPos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.player.getInventory().selected = oldSlot;
    }

    private Vec3 getAccurateHitVec(BlockPos pos, Direction side) {
        double x = pos.getX() + 0.5 + side.getStepX() * 0.5;
        double y = pos.getY() + 0.5 + side.getStepY() * 0.5;
        double z = pos.getZ() + 0.5 + side.getStepZ() * 0.5;
        return new Vec3(x, y, z);
    }

    private int getBestBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                if (stack.getItem() == Items.STRING) continue;
                boolean blacklisted = blacklistedBlocks.stream().anyMatch(c -> c.isInstance(blockItem.getBlock()));
                if (!blacklisted) return i;
            }
        }
        return -1;
    }

    private BlockData getBlockData(BlockPos pos) {
        for (Direction d : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP}) {
            BlockPos neighbor = pos.relative(d);
            var state = mc.level.getBlockState(neighbor);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return new BlockData(neighbor, d.getOpposite());
            }
        }
        return null;
    }

    private float[] calculateRotations(Vec3 hitVec) {
        double x = hitVec.x - mc.player.getX();
        double y = hitVec.y - (mc.player.getY() + mc.player.getEyeHeight());
        double z = hitVec.z - mc.player.getZ();
        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, dist));
        return new float[]{yaw, pitch};
    }

    private float normalizeYawDiff(float diff) {
        diff = diff % 360;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        return diff;
    }

    @Override
    protected void onDisable() {
        if (mc.options != null) mc.options.keyShift.setDown(false);
        isPlacing = false;
        placeDelay = 0;
        legitCooldown = 0;
    }

    @Override
    protected void onTick() {}

    private record BlockData(BlockPos pos, Direction side) {}
}