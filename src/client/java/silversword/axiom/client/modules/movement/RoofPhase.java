package silversword.axiom.client.modules.movement;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec2;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;
import java.util.Random;
public class RoofPhase extends AxiomMod implements KeybindConfigurable {
    private final SettingMode flyMode = new SettingMode("Mode", new String[]{"Vanilla", "Packet"}, "Packet");
    private final SettingSlider speed = new SettingSlider("Speed", new double[]{0.3, 0.5, 0.8, 1.0, 1.2, 1.5, 1.8, 2.0, 2.5}, 1.2);
    private final SettingSlider vSpeed = new SettingSlider("Vertical Speed", new double[]{0.4, 0.6, 0.8, 1.0, 1.2, 1.5, 2.0}, 1.0);
    private final SettingSlider hoverOffset = new SettingSlider("Hover Offset", new double[]{1e-10, 3e-10, 5e-10, 1e-9, 3e-9}, 5e-10);
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private int packetCounter = 0;
    private Random random = new Random();
    private Double oldFovEffectScale = null;
    public RoofPhase() {
        super("Roof Phase", "Phase through roof 1 block thick (experimental)", ModuleCategory.MOVEMENT);
        addHiddenSetting(toggleKey);

    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        String mode = flyMode.getMode();
        mc.player.setNoGravity(true);
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        Vec2 movementInput = mc.player.input.getMoveVector();
        double forward = movementInput.y;
        double strafe = movementInput.x;
        float yaw = mc.player.getYRot();
        double motionX = 0, motionZ = 0, motionY = 0;
        double speedVal = speed.getValue();
        motionX += -Math.sin(Math.toRadians(yaw)) * forward * speedVal + Math.cos(Math.toRadians(yaw)) * strafe * speedVal;
        motionZ += Math.cos(Math.toRadians(yaw)) * forward * speedVal + Math.sin(Math.toRadians(yaw)) * strafe * speedVal;
        // Jump / sneak - nyt selvä vaikutus
        if (mc.options.keyJump.isDown()) {
            motionY += vSpeed.getValue() * 1.5; // iso kerroin → nousee ripeästi
        }
        if (mc.options.keyShift.isDown()) {
            motionY -= vSpeed.getValue() * 1.2;
        }
        x += motionX;
        y += motionY;
        z += motionZ;
        // Pieni varianssi kaikissa suunnissa
        double randX = (random.nextDouble() - 0.5) * 0.00025;
        double randZ = (random.nextDouble() - 0.5) * 0.00025;
        double randY = (random.nextDouble() - 0.5) * 0.00012;
        x += randX;
        z += randZ;
        y += randY;
        if (mode.equals("Vanilla")) {
            mc.player.getAbilities().mayfly = true;
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlyingSpeed((float) (speedVal * 0.05f));
            mc.player.getAbilities().setWalkingSpeed((float) (speedVal * 0.1f));
            mc.player.onUpdateAbilities();
        } else {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().mayfly = false;
            // RATKAISU: ENEMMÄN PAKETTEJA PER TICK → paljon nopeampi tuntuma
            int packetsPerTick = 6 + (int)(speedVal * 5); // 6–18 packetia/tick riippuen speedistä
            for (int i = 0; i < packetsPerTick; i++) {
                double yOffset = (packetCounter + i) % 2 == 0 ? hoverOffset.getValue() : -hoverOffset.getValue();
                // MotionY lisätään jokaiseen pakettiin → jump toimii kunnolla
                sendPos(
                        x + randX * (i * 0.03),
                        y + motionY + yOffset + randY,
                        z + randZ * (i * 0.03),
                        false
                );
            }
            // Ground-spoof useammin (vähentää kick-riskiä)
            boolean groundSpoof = packetCounter % 2 == 0 || random.nextInt(100) < 60;
            sendPos(x, y, z, groundSpoof);
            packetCounter++;
        }
        if (!mode.equals("Vanilla")) {
            mc.player.setDeltaMovement(0, 0, 0);
        }
    }
    private void sendPos(double x, double y, double z, boolean onGround) {
        Minecraft mc = Minecraft.getInstance();
        mc.getConnection().send(
                new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, mc.player.horizontalCollision)
        );
    }
    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) {
            oldFovEffectScale = mc.options.fovEffectScale().get();
            mc.options.fovEffectScale().set(0.0);
        }
        packetCounter = 0;
    }
    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null && oldFovEffectScale != null) {
            mc.options.fovEffectScale().set(oldFovEffectScale);
            oldFovEffectScale = null;
        }
        if (mc != null && mc.player != null) {
            mc.player.setNoGravity(false);
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().mayfly = false;
            mc.player.getAbilities().setFlyingSpeed(0.05F);
            mc.player.getAbilities().setWalkingSpeed(0.1F);
            mc.player.onUpdateAbilities();
            mc.player.stopFallFlying();
        }

        packetCounter = 0;
    }
}
