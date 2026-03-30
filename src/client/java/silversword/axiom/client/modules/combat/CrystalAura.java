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
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.SmartKillAura.RotationHandler;
import silversword.axiom.client.setting.*;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class CrystalAura extends AxiomMod implements KeybindConfigurable {

    private static CrystalAura instance;

    // ---------- Asetukset ----------
    private final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private final SettingMode targetMode = new SettingMode(
            "Target Mode", new String[]{"Players", "Mobs", "Both"}, "Players"
    );

    private final SettingMode priority = new SettingMode(
            "Priority", new String[]{"Distance", "Health", "Armor", "Hybrid"}, "Distance"
    );

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

    private final SettingSlider returnDelay = new SettingSlider(
            "Return Delay (sec)", new double[]{0.5, 1.0, 1.5, 2.0, 3.0, 5.0}, 1.0
    );

    private final SettingBoolean returnToPrevious = new SettingBoolean("Return to Previous Slot", true);

    // ---------- State ----------
    private long lastPlaceTime = 0;
    private long lastBreakTime = 0;
    private LivingEntity currentTarget = null;

    private int previousSlot = -1;
    private int noTargetTicks = 0;

    public CrystalAura() {
        super("Crystal Aura", "Automatically places and detonates end crystals", ModuleCategory.COMBAT);
        addSetting(toggleKey);
        addSetting(targetMode);
        addSetting(priority);
        addSetting(returnToPrevious);
        addSetting(returnDelay);
        addSetting(placeRange);
        addSetting(breakRange);
        addSetting(placeDelay);
        addSetting(breakDelay);
        addSetting(autoSwitch);
        addSetting(checkWalls);
        addSetting(silentRotations);
        addSetting(turnSpeed);
        addSetting(addJitter);
        addSetting(jitterAmount);
        instance = this;
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        currentTarget = null;
        lastPlaceTime = 0;
        lastBreakTime = 0;
    }

    @Override
    protected void onDisable() {
        if (returnToPrevious.get() && previousSlot != -1) {
            assert mc.player != null;
            mc.player.getInventory().selected = previousSlot;
            previousSlot = -1;
        }
        currentTarget = null;
        noTargetTicks = 0;
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;

        // 1. Valitse kohde
        currentTarget = selectTarget();
        if (currentTarget == null) {
            // Ei kohdetta -> kasvatetaan laskuria
            if (returnToPrevious.get() && previousSlot != -1) {
                noTargetTicks++;
                int maxTicks = (int)(returnDelay.getValue() * 20); // 20 ticks per second
                if (noTargetTicks >= maxTicks) {
                    // Palauta alkuperäinen slot
                    mc.player.getInventory().selected = previousSlot;
                    previousSlot = -1;
                    noTargetTicks = 0;
                }
            }
            return;
        } else {
            // Kohde löytyi -> nollaa laskuri ja varmista että kristalli on kädessä
            noTargetTicks = 0;
            if (autoSwitch.get() && mc.player.getMainHandItem().getItem() != Items.END_CRYSTAL) {
                // Tallenna nykyinen slot jos ei vielä tallennettu
                if (previousSlot == -1) {
                    previousSlot = mc.player.getInventory().selected;
                }
                int crystalSlot = findCrystalSlot();
                if (crystalSlot != -1) {
                    mc.player.getInventory().selected = crystalSlot;
                }
            }
        }

        // 2. Laske rotaatio kohteeseen
        float yaw = (float) getYawTo(currentTarget);
        float pitch = (float) getPitchTo(currentTarget);
        yaw = net.minecraft.util.Mth.wrapDegrees(yaw);
        pitch = net.minecraft.util.Mth.clamp(pitch, -90, 90);

        // 3. Käsittele rotaatio (silent / smooth)
        if (silentRotations.get()) {
            // Silent: lähetä suoraan paketti
            RotationHandler.rotateImmediate(yaw, pitch);
        } else {
            // Smooth: käytä RotationHandlerin smooth-järjestelmää
            RotationHandler.rotate(yaw, pitch, (int) turnSpeed.getValue(), null);
        }

        // 4. Yritä asettaa kristalli
        if (System.currentTimeMillis() - lastPlaceTime >= placeDelay.getValue()) {
            BlockPos placePos = findBestCrystalSpot(currentTarget);
            if (placePos != null && isInRange(placePos, placeRange.getValue())) {
                if (placeCrystal(placePos)) {
                    lastPlaceTime = System.currentTimeMillis();
                }
            }
        }

        // 5. Yritä räjäyttää kristalli
        if (System.currentTimeMillis() - lastBreakTime >= breakDelay.getValue()) {
            EndCrystal crystal = findClosestCrystal(currentTarget);
            if (crystal != null && isInRange(crystal, breakRange.getValue())) {
                breakCrystal(crystal);
                lastBreakTime = System.currentTimeMillis();
            }
        }
    }

    // ---------- Target Selection ----------
    private LivingEntity selectTarget() {
        // Käytä samaa logiikkaa kuin KillAuran TargetManager
        // Voit joko kopioida TargetManagerin tänne tai tehdä siitä yhteisen.
        // Yksinkertainen versio:
        return mc.level.getEntitiesOfClass(LivingEntity.class,
                        mc.player.getBoundingBox().inflate(8),
                        e -> isValidTarget(e)).stream()
                .min((a, b) -> {
                    if (priority.getMode().equals("Distance"))
                        return Double.compare(a.distanceToSqr(mc.player), b.distanceToSqr(mc.player));
                    else if (priority.getMode().equals("Health"))
                        return Float.compare(a.getHealth(), b.getHealth());
                    // ... muut
                    return 0;
                }).orElse(null);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == mc.player) return false;
        if (!entity.isAlive()) return false;
        if (targetMode.getMode().equals("Players") && !(entity instanceof Player)) return false;
        if (targetMode.getMode().equals("Mobs") && entity instanceof Player) return false;
        if (checkWalls.get() && !hasLineOfSight(entity)) return false;
        return true;
    }

    private boolean hasLineOfSight(Entity entity) {
        Vec3 start = mc.player.getEyePosition();
        Vec3 end = entity.getBoundingBox().getCenter();
        return mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player))
                .getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    // ---------- Crystal Placement Logic ----------
    private BlockPos findBestCrystalSpot(LivingEntity target) {
        // Etsi obsidian/bedrock -lohkoja targetin ympäriltä
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int radius = 3; // etäisyys kohteesta
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -1; y <= 2; y++) { // targetin korkeudelta
                    pos.set(target.getBlockX() + x, target.getBlockY() + y, target.getBlockZ() + z);
                    if (isValidCrystalSpot(pos)) {
                        // Tarkista, että kristalli osuu kohteeseen (räjähdysalue)
                        // Laske lohkon keskipiste (x+0.5, y+0.5, z+0.5)
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
        // 1. Alustan on oltava obsidian tai bedrock
        var blockState = mc.level.getBlockState(pos);
        if (!blockState.is(Blocks.OBSIDIAN) && !blockState.is(Blocks.BEDROCK)) return false;

        // 2. Yläpuolella on oltava ilmaa (ja mahdollisesti muita korvaavia blokkeja)
        BlockPos above = pos.above();
        if (!mc.level.getBlockState(above).isAir() && !mc.level.getBlockState(above).canBeReplaced()) return false;

        // 3. Toinen yläpuolella myös ilmaa (kristalli on 1 blokki korkea, mutta tarvitsee tilaa)
        BlockPos above2 = above.above();
        if (!mc.level.getBlockState(above2).isAir() && !mc.level.getBlockState(above2).canBeReplaced()) return false;

        // 4. Ei muita kristalleja tai entiteettejä tässä paikassa
        return mc.level.getEntitiesOfClass(EndCrystal.class, new net.minecraft.world.phys.AABB(above)).isEmpty();
    }

    private boolean placeCrystal(BlockPos pos) {
        // Varmista, että end crystal on kädessä
        if (autoSwitch.get() && mc.player.getMainHandItem().getItem() != Items.END_CRYSTAL) {
            int slot = findCrystalSlot();
            if (slot == -1) return false;
            mc.player.getInventory().selected = slot;
        }

        // Tarkista, että pelaaja on tarpeeksi lähellä
        if (mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > placeRange.getValue() * placeRange.getValue())
            return false;

        // Lähetä rotaatio ennen placementia (jos silent rotations)
        if (silentRotations.get()) {
            // Laske rotaatio tarkasti lohkon keskikohtaan
            float[] rotations = getRotationsTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            RotationHandler.rotateImmediate(rotations[0], rotations[1]);
        }

        // Lähetä place-paketti
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

    private int findCrystalSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.END_CRYSTAL) {
                return i;
            }
        }
        return -1;
    }

    // ---------- Crystal Detonation ----------
    private EndCrystal findClosestCrystal(LivingEntity target) {
        return mc.level.getEntitiesOfClass(EndCrystal.class,
                        target.getBoundingBox().inflate(6),
                        e -> e.isAlive() && e.distanceTo(target) <= 6 && e.distanceTo(mc.player) <= breakRange.getValue())
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(target), b.distanceToSqr(target)))
                .orElse(null);
    }

    private void breakCrystal(Entity crystal) {
        // Lähetä rotaatio ennen rikkomista (silent)
        if (silentRotations.get()) {
            float[] rotations = getRotationsTo(crystal.getX(), crystal.getY() + crystal.getBbHeight() / 2, crystal.getZ());
            RotationHandler.rotateImmediate(rotations[0], rotations[1]);
        }

        ServerboundInteractPacket packet = ServerboundInteractPacket.createAttackPacket(crystal, mc.player.isShiftKeyDown());
        mc.getConnection().send(packet);
    }

    // ---------- Apumetodit ----------
    private float[] getRotationsTo(double x, double y, double z) {
        double dx = x - mc.player.getX();
        double dy = y - (mc.player.getY() + mc.player.getEyeHeight());
        double dz = z - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        if (addJitter.get()) {
            float jit = (float) jitterAmount.getValue();
            yaw += (float) ((Math.random() - 0.5) * jit);
            pitch += (float) ((Math.random() - 0.5) * jit * 0.5);
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