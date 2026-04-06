package silversword.axiom.client.modules.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.*;
import silversword.axiom.client.utils.Rotations;

import java.util.Comparator;
import java.util.Random;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class CrystalAura extends AxiomMod implements KeybindConfigurable {

    private static CrystalAura instance;
    private final Random random = new Random();

    // ---------- ASETUKSET ----------
    private final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // Toimintatapa
    private final SettingMode crystalMode = new SettingMode(
            "Crystal Mode", new String[]{"Auto", "Hold"}, "Auto"
    );

    // Kohdevalinta (vain Auto-moodissa)
    private final SettingMode targetMode = new SettingMode(
            "Target Mode", new String[]{"Players", "Mobs", "Both"}, "Players"
    );
    private final SettingMode priority = new SettingMode(
            "Priority", new String[]{"Distance", "Health", "Armor", "Hybrid"}, "Distance"
    );

    // Etäisyydet ja viiveet
    private final SettingSlider placeRange = new SettingSlider(
            "Place Range", new double[]{3.5, 4.0, 4.5, 5.0, 5.5}, 4.5
    );
    private final SettingSlider breakRange = new SettingSlider(
            "Break Range", new double[]{3.5, 4.0, 4.5, 5.0, 5.5}, 4.5
    );
    private final SettingSlider placeDelay = new SettingSlider(
            "Place Delay (ms)", new double[]{150, 200, 250, 300, 350, 400}, 200
    );
    private final SettingSlider breakDelay = new SettingSlider(
            "Break Delay (ms)", new double[]{100, 150, 200, 250, 300}, 150
    );
    private final SettingSlider holdPlaceDelay = new SettingSlider(
            "Hold Place Delay (ms)", new double[]{100, 150, 200, 250, 300}, 150
    );
    private final SettingSlider holdBreakDelay = new SettingSlider(
            "Hold Break Delay (ms)", new double[]{50, 100, 150, 200, 250}, 100
    );

    // Automaattinen slotin vaihto (Auto-moodissa)
    private final SettingBoolean autoSwitch = new SettingBoolean("Auto Switch", true);
    private final SettingBoolean checkWalls = new SettingBoolean("Check Walls", true);
    private final SettingBoolean silentRotations = new SettingBoolean("Silent Rotations", true);
    private final SettingSlider turnSpeed = new SettingSlider(
            "Turn Speed (deg/tick)", new double[]{10, 15, 20, 25, 30, 35, 40}, 20
    );
    private final SettingBoolean addJitter = new SettingBoolean("Add Jitter", true);
    private final SettingSlider jitterAmount = new SettingSlider(
            "Jitter Amount", new double[]{0.5, 1.0, 1.5, 2.0, 2.5, 3.0}, 1.5
    );

    // Slot palautus (Auto-moodi)
    private final SettingSlider returnDelay = new SettingSlider(
            "Return Delay (sec)", new double[]{0.5, 1.0, 1.5, 2.0, 3.0, 5.0}, 1.0
    );
    private final SettingBoolean returnToPrevious = new SettingBoolean("Return to Previous Slot", true);

    // ---------- TILAMUUTTUJAT ----------
    private long lastPlaceTime = 0;
    private long lastBreakTime = 0;
    private LivingEntity currentTarget = null;
    private int previousSlot = -1;
    private int noTargetTicks = 0;

    // Hold-moodin tilat
    private long lastHoldPlaceTime = 0;
    private long lastHoldBreakTime = 0;
    private BlockPos lastHoldPlacedPos = null;
    private long lastHoldPlacedTime = 0;

    public CrystalAura() {
        super("Crystal Aura", "Automatically places crystals on obsidian and detonates them", ModuleCategory.COMBAT);
        addSetting(toggleKey);
        addSetting(crystalMode);
        addSetting(targetMode);
        addSetting(priority);
        addSetting(placeRange);
        addSetting(breakRange);
        addSetting(placeDelay);
        addSetting(breakDelay);
        addSetting(holdPlaceDelay);
        addSetting(holdBreakDelay);
        addSetting(autoSwitch);
        addSetting(checkWalls);
        addSetting(silentRotations);
        addSetting(turnSpeed);
        addSetting(addJitter);
        addSetting(jitterAmount);
        addSetting(returnToPrevious);
        addSetting(returnDelay);
        instance = this;
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        resetState();
    }

    @Override
    protected void onDisable() {
        if (returnToPrevious.get() && previousSlot != -1 && mc.player != null) {
            mc.player.getInventory().selected = previousSlot;
        }
        resetState();
    }

    private void resetState() {
        currentTarget = null;
        lastPlaceTime = 0;
        lastBreakTime = 0;
        previousSlot = -1;
        noTargetTicks = 0;
        lastHoldPlaceTime = 0;
        lastHoldBreakTime = 0;
        lastHoldPlacedPos = null;
        lastHoldPlacedTime = 0;
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;

        if (crystalMode.getMode().equals("Auto")) {
            tickAuto();
        } else {
            tickHold();
        }
    }

    // ==================== AUTO-MOODI ====================
    private void tickAuto() {
        currentTarget = selectTarget();
        if (currentTarget == null) {
            handleNoTarget();
            return;
        } else {
            noTargetTicks = 0;
            ensureCrystalInHand();
        }

        // Rotaatiot
        applyRotations(currentTarget);

        // Kristallin asetus
        if (System.currentTimeMillis() - lastPlaceTime >= placeDelay.getValue()) {
            BlockPos placePos = findBestCrystalSpot(currentTarget);
            if (placePos != null && isInRange(placePos, placeRange.getValue())) {
                if (placeCrystal(placePos, true)) {
                    lastPlaceTime = System.currentTimeMillis();
                }
            }
        }

        // Kristallin räjäytys
        if (System.currentTimeMillis() - lastBreakTime >= breakDelay.getValue()) {
            EndCrystal crystal = findClosestCrystal(currentTarget);
            if (crystal != null && isInRange(crystal, breakRange.getValue())) {
                breakCrystal(crystal);
                lastBreakTime = System.currentTimeMillis();
            }
        }
    }

    private void handleNoTarget() {
        if (returnToPrevious.get() && previousSlot != -1) {
            noTargetTicks++;
            int maxTicks = (int)(returnDelay.getValue() * 20);
            if (noTargetTicks >= maxTicks) {
                mc.player.getInventory().selected = previousSlot;
                previousSlot = -1;
                noTargetTicks = 0;
            }
        }
    }

    private void ensureCrystalInHand() {
        if (autoSwitch.get() && mc.player.getMainHandItem().getItem() != Items.END_CRYSTAL) {
            if (previousSlot == -1) previousSlot = mc.player.getInventory().selected;
            int crystalSlot = findCrystalSlot();
            if (crystalSlot != -1) mc.player.getInventory().selected = crystalSlot;
        }
    }

    // ==================== HOLD-MOODI ====================
    private void tickHold() {
        // Tarkistetaan, pidetäänkö oikeaa nappia pohjassa
        if (!mc.mouseHandler.isRightPressed()) {
            lastHoldPlacedPos = null; // nollataan kun nappi vapautetaan
            return;
        }

        // 1. Etsi lohko johon katsoo
        HitResult hit = mc.player.pick(placeRange.getValue(), 0.0f, false);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();

        // 2. Aseta kristalli viiveen mukaan (EI vaihdeta slotia)
        if (System.currentTimeMillis() - lastHoldPlaceTime >= holdPlaceDelay.getValue()) {
            if (isValidCrystalSpot(pos)) {
                if (placeCrystalManual(pos)) {
                    lastHoldPlaceTime = System.currentTimeMillis();
                    lastHoldPlacedPos = pos;
                    lastHoldPlacedTime = System.currentTimeMillis();
                }
            }
        }

        // 3. Räjäytä automaattisesti lähin kristalli (tai juuri asetettu)
        if (System.currentTimeMillis() - lastHoldBreakTime >= holdBreakDelay.getValue()) {
            EndCrystal crystal = findClosestCrystalToPlayer();
            if (crystal != null && mc.player.distanceToSqr(crystal) <= breakRange.getValue() * breakRange.getValue()) {
                breakCrystal(crystal);
                lastHoldBreakTime = System.currentTimeMillis();
            }
        }
    }

    private EndCrystal findClosestCrystalToPlayer() {
        return mc.level.getEntitiesOfClass(EndCrystal.class,
                        mc.player.getBoundingBox().inflate(breakRange.getValue()),
                        e -> e.isAlive() && mc.player.distanceToSqr(e) <= breakRange.getValue() * breakRange.getValue())
                .stream()
                .min(Comparator.comparingDouble(e -> mc.player.distanceToSqr(e)))
                .orElse(null);
    }

    // ==================== YHTEISET TOIMINNOT ====================
    private void applyRotations(LivingEntity target) {
        float yaw = (float) getYawTo(target);
        float pitch = (float) getPitchTo(target);
        yaw = net.minecraft.util.Mth.wrapDegrees(yaw);
        pitch = net.minecraft.util.Mth.clamp(pitch, -90, 90);

        if (silentRotations.get()) {
            Rotations.rotate(yaw, pitch);
        } else {
            Rotations.rotate(yaw, pitch, (int) turnSpeed.getValue(), null);
        }
    }

    /**
     * Asettaa kristallin Auto-moodissa (vaihtaa slotia tarvittaessa)
     */
    private boolean placeCrystal(BlockPos pos, boolean checkSlot) {
        if (checkSlot && autoSwitch.get() && mc.player.getMainHandItem().getItem() != Items.END_CRYSTAL) {
            int slot = findCrystalSlot();
            if (slot == -1) return false;
            mc.player.getInventory().selected = slot;
        }

        if (mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > placeRange.getValue() * placeRange.getValue())
            return false;

        return sendPlacePacket(pos);
    }

    /**
     * Asettaa kristallin Hold-moodissa – EI vaihda slotia, luottaa siihen että kristalli on kädessä.
     */
    private boolean placeCrystalManual(BlockPos pos) {
        if (mc.player.getMainHandItem().getItem() != Items.END_CRYSTAL) {
            return false; // Ei kristallia kädessä
        }
        return sendPlacePacket(pos);
    }

    private boolean sendPlacePacket(BlockPos pos) {
        if (silentRotations.get()) {
            float[] rotations = getRotationsTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            Rotations.rotate(rotations[0], rotations[1]);
        }

        Direction direction = Direction.UP;
        Vec3 hitVec = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, direction, pos, false);
        ServerboundUseItemOnPacket packet = new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                hitResult,
                mc.player.getInventory().selected
        );
        mc.getConnection().send(packet);
        return true;
    }

    private void breakCrystal(Entity crystal) {
        if (silentRotations.get()) {
            float[] rotations = getRotationsTo(crystal.getX(), crystal.getY() + crystal.getBbHeight() / 2, crystal.getZ());
            Rotations.rotate(rotations[0], rotations[1]);
        }

        ServerboundInteractPacket packet = ServerboundInteractPacket.createAttackPacket(crystal, mc.player.isShiftKeyDown());
        mc.getConnection().send(packet);
    }

    // ==================== APUMETODIT ====================
    private LivingEntity selectTarget() {
        return mc.level.getEntitiesOfClass(LivingEntity.class,
                        mc.player.getBoundingBox().inflate(8),
                        this::isValidTarget).stream()
                .min((a, b) -> {
                    switch (priority.getMode()) {
                        case "Distance": return Double.compare(a.distanceToSqr(mc.player), b.distanceToSqr(mc.player));
                        case "Health": return Float.compare(a.getHealth(), b.getHealth());
                        default: return 0;
                    }
                }).orElse(null);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == mc.player || !entity.isAlive()) return false;
        String mode = targetMode.getMode();
        if (mode.equals("Players") && !(entity instanceof Player)) return false;
        if (mode.equals("Mobs") && entity instanceof Player) return false;
        if (checkWalls.get() && !hasLineOfSight(entity)) return false;
        return true;
    }

    private boolean hasLineOfSight(Entity entity) {
        Vec3 start = mc.player.getEyePosition();
        Vec3 end = entity.getBoundingBox().getCenter();
        return mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player))
                .getType() == HitResult.Type.MISS;
    }

    private BlockPos findBestCrystalSpot(LivingEntity target) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int radius = 3;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -1; y <= 2; y++) {
                    pos.set(target.getBlockX() + x, target.getBlockY() + y, target.getBlockZ() + z);
                    if (isValidCrystalSpot(pos)) {
                        Vec3 blockCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        if (blockCenter.distanceToSqr(target.position()) <= 4.0 * 4.0) {
                            return pos.immutable();
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isValidCrystalSpot(BlockPos pos) {
        var blockState = mc.level.getBlockState(pos);
        if (!blockState.is(Blocks.OBSIDIAN) && !blockState.is(Blocks.BEDROCK)) return false;

        BlockPos above = pos.above();
        if (!mc.level.getBlockState(above).isAir() && !mc.level.getBlockState(above).canBeReplaced()) return false;

        BlockPos above2 = above.above();
        if (!mc.level.getBlockState(above2).isAir() && !mc.level.getBlockState(above2).canBeReplaced()) return false;

        return mc.level.getEntitiesOfClass(EndCrystal.class, new net.minecraft.world.phys.AABB(above)).isEmpty();
    }

    private EndCrystal findClosestCrystal(LivingEntity target) {
        return mc.level.getEntitiesOfClass(EndCrystal.class,
                        target.getBoundingBox().inflate(6),
                        e -> e.isAlive() && e.distanceTo(target) <= 6 && e.distanceTo(mc.player) <= breakRange.getValue())
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(target)))
                .orElse(null);
    }

    private int findCrystalSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.END_CRYSTAL) return i;
        }
        return -1;
    }

    private float[] getRotationsTo(double x, double y, double z) {
        double dx = x - mc.player.getX();
        double dy = y - (mc.player.getY() + mc.player.getEyeHeight());
        double dz = z - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        if (addJitter.get()) {
            float jit = (float) jitterAmount.getValue();
            yaw += (float) ((random.nextDouble() - 0.5) * jit);
            pitch += (float) ((random.nextDouble() - 0.5) * jit * 0.5);
        }
        return new float[]{yaw, pitch};
    }

    private double getYawTo(LivingEntity target) {
        return getRotationsTo(target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ())[0];
    }

    private double getPitchTo(LivingEntity target) {
        return getRotationsTo(target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ())[1];
    }

    private boolean isInRange(BlockPos pos, double range) {
        return mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= range * range;
    }

    private boolean isInRange(Entity entity, double range) {
        return mc.player.distanceToSqr(entity) <= range * range;
    }

    public static CrystalAura getInstance() {
        return instance;
    }
}