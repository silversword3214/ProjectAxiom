// TODO(Ravel): Failed to fully resolve file: null cannot be cast to non-null type com.intellij.psi.PsiJavaCodeReferenceElement
package silversword.axiom.client.modules.movement;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.render.rendersystem.ShapeMode;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class Scaffold extends AxiomMod implements KeybindConfigurable {

    public static Scaffold INSTANCE;

    // --- Asetukset ---
    private final SettingBoolean render = new SettingBoolean("Render", true);
    private final SettingMode shapeMode = new SettingMode("Shape Mode", new String[]{"Lines", "Sides", "Both"}, "Both");
    private final SettingColor sideColor = new SettingColor("Side Color", new Color(0, 204, 0, 30));
    private final SettingColor lineColor = new SettingColor("Line Color", new Color(0, 204, 0, 255));
    private final SettingBoolean customRange = new SettingBoolean("Custom Range", false);
    private final SettingNumber range = new SettingNumber("Range", 1, 6, 1, 4);
    private final SettingBoolean towerMode = new SettingBoolean("Tower Mode", false);
    private final SettingNumber blocksPerSecond = new SettingNumber("Blocks/s", 1, 40, 1, 20);
    private final SettingNumber placeDelay = new SettingNumber("Place Delay (ms)", 0, 1000, 10, 50);
    private final SettingBoolean randomDelay = new SettingBoolean("Random Delay", true);
    private final SettingNumber delayRandomness = new SettingNumber("Delay Randomness", 0, 200, 10, 100);
    private final SettingBoolean godBridge = new SettingBoolean("GodBridge Mode", true);
    private final SettingMode ledgeAction = new SettingMode("Ledge Action", new String[]{"Jump", "Sneak", "StopInput", "Backwards"}, "Jump");
    private final SettingNumber forceSneakBelow = new SettingNumber("Force Sneak Below", 0, 10, 1, 3);
    private final SettingNumber sneakTicks = new SettingNumber("Sneak Ticks", 1, 10, 1, 2);
    private final SettingBoolean eagle = new SettingBoolean("Eagle", true);
    private final SettingNumber blocksToEagle = new SettingNumber("Blocks to Eagle", 0, 5, 1, 3);
    private final SettingNumber eagleEdgeDistance = new SettingNumber("Eagle Edge Distance", 0, 3, 0.1, 1.0);
    private final SettingBoolean silentRotation = new SettingBoolean("Silent Rotation", true);
    private final SettingMode rotationMode = new SettingMode("Rotation Mode", new String[]{"Center", "Random", "Stabilized", "Close"}, "Center");
    private final SettingNumber minDist = new SettingNumber("Min Distance", 0, 1, 0.05, 0.2);
    private final SettingBoolean zitter = new SettingBoolean("Zitter", false);
    private final SettingMode zitterMode = new SettingMode("Zitter Mode", new String[]{"Smooth", "Teleport"}, "Smooth");
    private final SettingNumber zitterAmount = new SettingNumber("Zitter Amount", 0, 10, 0.5, 2.0);
    private final SettingNumber keepRotationTicks = new SettingNumber("Keep Rotation Ticks", 0, 10, 1, 3);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // --- Tila ---
    private BlockPos targetPos;
    private long lastPlaceTime = 0;
    private long placeInterval;
    private boolean isOnRightSide = false;
    private boolean ledgeActive = false;
    private int sneakTimer = 0;
    private boolean waitingForSneak = false;
    private int blockCount = 0;
    private int rotationTicksLeft = 0;
    private float lastYaw, lastPitch; // viimeisimmät lähetetyt rotaatiot

    public Scaffold() {
        super("Scaffold", "Places automatically block in front of you", ModuleCategory.MOVEMENT);
        INSTANCE = this;

        addSetting(render);
        addSetting(shapeMode);
        addHiddenSetting(sideColor.getSetting());
        addHiddenSetting(lineColor.getSetting());
        addHiddenSetting(toggleKey);
        addSetting(customRange);
        addSetting(range);
        addSetting(towerMode);
        addSetting(blocksPerSecond);
        addSetting(placeDelay);
        addSetting(randomDelay);
        addSetting(delayRandomness);
        addSetting(godBridge);
        addSetting(ledgeAction);
        addSetting(forceSneakBelow);
        addSetting(sneakTicks);
        addSetting(eagle);
        addSetting(blocksToEagle);
        addSetting(eagleEdgeDistance);
        addSetting(silentRotation);
        addSetting(rotationMode);
        addSetting(minDist);
        addSetting(zitter);
        addSetting(zitterMode);
        addSetting(zitterAmount);
        addSetting(keepRotationTicks);

        AxiomInitialize.EVENT_BUS.subscribe(this);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        targetPos = null;
        lastPlaceTime = 0;
        isOnRightSide = false;
        ledgeActive = false;
        sneakTimer = 0;
        waitingForSneak = false;
        blockCount = 0;
        rotationTicksLeft = 0;
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;

        // Päivitetään asetusnopeus
        placeInterval = (long) (1000.0 / blocksPerSecond.getValue());

        // Tarkista onko kädessä blokkia
        if (!isHoldingBlock()) {
            targetPos = null;
            return;
        }

        // Laske kohdeblokki (jalan alla)
        BlockPos playerPos = mc.player.blockPosition();
        BlockPos belowPos = playerPos.below();

        if (mc.level.getBlockState(belowPos).isAir()) {
            targetPos = belowPos;
        } else {
            targetPos = null;
            return;
        }

        // Eagle-tarkistus (reunalla hiiviskely)
        if (eagle.get()) {
            Vec3 playerFeet = mc.player.position();
            double edgeDistance = getEdgeDistance(playerFeet, targetPos);
            if (edgeDistance > eagleEdgeDistance.getValue() && blockCount >= blocksToEagle.getValue()) {
                waitingForSneak = true;
                sneakTimer = (int) sneakTicks.getValue();
            }
        }

        // Simuloi pelaajan sijainti seuraavalla tickillä
        Vec3 predictedPos = mc.player.position().add(mc.player.getDeltaMovement());
        BlockPos predictedBelow = BlockPos.containing(predictedPos.x, predictedPos.y - 0.5, predictedPos.z).below();
        boolean willBeOnLedge = mc.level.getBlockState(predictedBelow).isAir();

        // Reunalta putoamisen esto (GodBridge)
        if (godBridge.get() && willBeOnLedge) {
            handleLedge();
        }

        // Aseta blokki (viive huomioiden)
        long now = System.currentTimeMillis();
        long effectiveDelay = getEffectiveDelay();
        if (now - lastPlaceTime >= effectiveDelay) {
            if (placeBlock(belowPos)) {
                lastPlaceTime = now;
                blockCount++;
            }
        }

        // Tower mode (automaattinen hyppy)
        if (towerMode.get() && mc.options.keyJump.isDown()) {
            mc.player.jumpFromGround();
        }

        // Vähennä rotaation säilytystikkien määrää
        if (rotationTicksLeft > 0) {
            rotationTicksLeft--;
        }
    }

    private long getEffectiveDelay() {
        long base = (long) placeDelay.getValue() + placeInterval;
        if (randomDelay.get()) {
            int randomness = (int) delayRandomness.getValue();
            base += (long) (Math.random() * randomness) - randomness / 2;
        }
        return Math.max(0, base);
    }

    private double getEdgeDistance(Vec3 feet, BlockPos target) {
        // Laske etäisyys blokin reunasta (yksinkertainen versio)
        double x = feet.x - target.getX() - 0.5;
        double z = feet.z - target.getZ() - 0.5;
        return Math.max(Math.abs(x), Math.abs(z));
    }

    private void handleLedge() {
        if (ledgeActive) return;

        if (forceSneakBelow.getValue() > 0 && blockCount < forceSneakBelow.getValue()) {
            waitingForSneak = true;
            sneakTimer = (int) sneakTicks.getValue();
            ledgeActive = true;
            return;
        }

        String action = ledgeAction.getMode();
        switch (action) {
            case "Jump":
                mc.player.jumpFromGround();
                break;
            case "Sneak":
                waitingForSneak = true;
                sneakTimer = (int) sneakTicks.getValue();
                break;
            case "StopInput":
                // TODO: pysäytä liike väliaikaisesti
                break;
            case "Backwards":
                // TODO: simuloi taaksepäin liikettä
                break;
        }
        ledgeActive = true;
    }

    @AxiomEvent
    public void onRender(Render3DEvent event) {
        if (!isEnabled() || !render.get() || targetPos == null) return;
        if (!mc.level.getBlockState(targetPos).isAir()) return;
        if (!isHoldingBlock()) return;

        double x1 = targetPos.getX();
        double y1 = targetPos.getY();
        double z1 = targetPos.getZ();
        double x2 = x1 + 1;
        double y2 = y1 + 1;
        double z2 = z1 + 1;

        ShapeMode mode = ShapeMode.valueOf(shapeMode.getMode());
        Color side = sideColor.getCurrentColor();
        Color line = lineColor.getCurrentColor();

        event.render.drawBox(x1, y1, z1, x2, y2, z2, side, line, mode, 0);
    }

    private boolean isHoldingBlock() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandItem();
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
    }

    private boolean placeBlock(BlockPos pos) {
        if (mc.player == null || mc.level == null) return false;
        if (!mc.level.getBlockState(pos).isAir()) {
            return false;
        }

        BlockPos neighbor = findNeighbor(pos);
        if (neighbor == null) {
            return false;
        }

        Direction direction = getDirection(neighbor, pos);
        if (direction == null) {
            return false;
        }

        Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
                direction.getStepX() * 0.5,
                direction.getStepY() * 0.5,
                direction.getStepZ() * 0.5
        );

        BlockHitResult hitResult = new BlockHitResult(hitVec, direction, neighbor, false);

        // Tarkista minDist
        if (minDist.getValue() > 0 && !checkMinDist(pos, hitVec, direction)) {
            return false;
        }

        // Laske halutut rotaatiot
        float[] targetRotations = getRotations(pos, hitVec);

        // Tallenna vanhat rotaatiot
        float oldYaw = mc.player.getYRot();
        float oldPitch = mc.player.getXRot();

        // Aseta väliaikaiset rotaatiot
        mc.player.setYRot(targetRotations[0]);
        mc.player.setXRot(targetRotations[1]);

        // Yritä asettaa blokki
        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);

        // Palauta vanhat rotaatiot
        mc.player.setYRot(oldYaw);
        mc.player.setXRot(oldPitch);

        if (result.consumesAction()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
            return true;
        } else {
            System.out.println("[Scaffold] interactBlock failed: " + result);
            return false;
        }
    }

    private void sendRotation(float yaw, float pitch) {
        // Lähetä serverille paketti, joka ilmoittaa rotaation
        ServerboundMovePlayerPacket.Rot packet = new ServerboundMovePlayerPacket.Rot(
                yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision
        );
        mc.player.connection.send(packet);
    }

    private boolean checkMinDist(BlockPos targetPos, Vec3 hitVec, Direction side) {
        double min = minDist.getValue();
        if (min <= 0) return true; // ei tarkistusta

        // Muunnetaan hitVec suhteessa targetPos:iin (0..1 välillä)
        double x = hitVec.x - targetPos.getX();
        double y = hitVec.y - targetPos.getY();
        double z = hitVec.z - targetPos.getZ();

        // Lasketaan etäisyys lähimpään reunaan (tai kulmaan)
        double dist;
        if (side.getAxis() == Direction.Axis.X) {
            // osuma x-suuntaisella sivulla – etäisyys y- ja z-akseleilla
            dist = Math.min(Math.min(y, 1 - y), Math.min(z, 1 - z));
        } else if (side.getAxis() == Direction.Axis.Y) {
            dist = Math.min(Math.min(x, 1 - x), Math.min(z, 1 - z));
        } else {
            dist = Math.min(Math.min(x, 1 - x), Math.min(y, 1 - y));
        }

        // dist on nyt etäisyys lähimpään reunaan/kulmaan (0..0.5)
        return dist >= min;
    }

    private BlockPos findNeighbor(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (!mc.world.getBlockState(neighbor).isAir()) {
                return neighbor;
            }
        }
        return null;
    }

    private Direction getDirection(BlockPos neighbor, BlockPos target) {
        int dx = target.getX() - neighbor.getX();
        int dy = target.getY() - neighbor.getY();
        int dz = target.getZ() - neighbor.getZ();
        if (dx != 0) return dx > 0 ? Direction.EAST : Direction.WEST;
        if (dy != 0) return dy > 0 ? Direction.UP : Direction.DOWN;
        if (dz != 0) return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        return null;
    }

    private float[] getRotations(BlockPos targetPos, Vec3d hitVec) {
        float yaw = 0, pitch = 0;

        if (!godBridge.get()) {
            // Käytetään tavallisia rotaatioita (katso kohti blokkia)
            Vec3d playerPos = mc.player.getEyePos();
            double dx = hitVec.x - playerPos.x;
            double dy = hitVec.y - playerPos.y;
            double dz = hitVec.z - playerPos.z;

            double distance = Math.sqrt(dx * dx + dz * dz);
            yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
            pitch = (float) -Math.toDegrees(Math.atan2(dy, distance));
        } else {
            // GodBridge-tila: käytetään liikesuuntaa ja zitteriä
            PlayerInput playerInput = mc.player.input.playerInput;
            float forward = 0;
            float sideways = 0;

            if (playerInput.forward()) forward += 1;
            if (playerInput.backward()) forward -= 1;
            if (playerInput.left()) sideways += 1;
            if (playerInput.right()) sideways -= 1;

            if (forward == 0 && sideways == 0) {
                yaw = getYawForPosition(targetPos);
            } else {
                double movementYaw = Math.toDegrees(Math.atan2(-sideways, forward));
                yaw = (float) movementYaw + mc.player.getYaw();

                float rounded = Math.round(yaw / 45) * 45;
                if (rounded % 90 == 0) {
                    yaw = rounded + (isOnRightSide ? 45 : -45);
                } else {
                    yaw = rounded;
                }
            }
            pitch = 75.0f;
        }

        // Sovelletaan rotationMode
        switch (rotationMode.getMode()) {
            case "Random":
                yaw += (float) ((Math.random() - 0.5) * 10);
                pitch += (float) ((Math.random() - 0.5) * 5);
                break;
            case "Stabilized":
                // Pidä yaw lähellä edellistä (jos mahdollista)
                if (rotationTicksLeft > 0) {
                    yaw = lastYaw;
                    pitch = lastPitch;
                }
                break;
            case "Close":
                // Pieni offset
                yaw += 0.5f;
                pitch -= 0.5f;
                break;
            // Center: ei muutosta
        }

        // Zitter (tähtäyksen heilunta)
        if (zitter.get()) {
            if (zitterMode.getMode().equals("Smooth")) {
                // Pehmeä heilunta (siniaalto)
                float amount = (float) zitterAmount.getValue();
                yaw += (float) (Math.sin(System.currentTimeMillis() / 200.0) * amount);
                pitch += (float) (Math.cos(System.currentTimeMillis() / 200.0) * amount);
            } else if (zitterMode.getMode().equals("Teleport")) {
                // Pieniä loikkia
                if (Math.random() < 0.05) {
                    float amount = (float) zitterAmount.getValue();
                    yaw += (float) ((Math.random() - 0.5) * amount);
                    pitch += (float) ((Math.random() - 0.5) * amount);
                }
            }
        }

        lastYaw = yaw;
        lastPitch = pitch;
        return new float[]{yaw, pitch};
    }

    private float getYawForPosition(BlockPos pos) {
        Vec3d playerPos = mc.player.getEyePos();
        double dx = pos.getX() + 0.5 - playerPos.x;
        double dz = pos.getZ() + 0.5 - playerPos.z;
        return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
    }

    public boolean shouldSneak() {
        if (waitingForSneak && sneakTimer > 0) {
            sneakTimer--;
            if (sneakTimer <= 0) {
                waitingForSneak = false;
                ledgeActive = false;
            }
            return true;
        }
        return false;
    }
}