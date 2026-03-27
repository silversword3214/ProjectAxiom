package silversword.axiom.client.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.*;

public class Flight extends AxiomMod implements KeybindConfigurable {

    public static Flight INSTANCE;

    private final SettingMode flyMode = new SettingMode("Mode", new String[]{"Vanilla", "Velocity", "Packet", "Glide"}, "Packet");
    private final SettingSlider speed = new SettingSlider("Horizontal Speed", new double[]{0.5, 1.0, 1.5, 2.0, 3.0, 5.0}, 1.0);
    private final SettingSlider vSpeed = new SettingSlider("Vertical Speed", new double[]{0.2, 0.5, 0.8, 1.0, 1.5}, 0.5);
    private final SettingMode antiKick = new SettingMode("Anti-Kick", new String[]{"Dolphin", "Drop", "None"}, "Dolphin");

    private int tickCounter = 0;
    private Double oldFovEffectScale = null;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public Flight() {
        super("Flight", "Advanced bypass flight with oscillation logic", ModuleCategory.MOVEMENT);
        INSTANCE = this;

        addSetting(flyMode);
        addSetting(speed);
        addSetting(vSpeed);
        addSetting(antiKick);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;


        mc.player.setNoGravity(true);
        mc.player.fallDistance = 0.0f;
        tickCounter++;

        String mode = flyMode.getMode();

        // --- LIIKKEEN LASKENTA ---
        Vec2 input = mc.player.input.getMoveVector();
        float yaw = mc.player.getYRot();
        double s = speed.getValue();

        double motionX = 0, motionY = 0, motionZ = 0;

        if (input.y != 0 || input.x != 0) {
            double rad = Math.toRadians(yaw);
            motionX = (-Math.sin(rad) * input.y + Math.cos(rad) * input.x) * s;
            motionZ = (Math.cos(rad) * input.y + Math.sin(rad) * input.x) * s;
        }

        if (mc.options.keyJump.isDown()) motionY += vSpeed.getValue();
        if (mc.options.keyShift.isDown()) motionY -= vSpeed.getValue();

        // --- MODET ---
        switch (mode) {
            case "Vanilla":
                handleVanilla(mc, s);
                break;

            case "Velocity":
                mc.player.setDeltaMovement(motionX, motionY, motionZ);
                applyAntiKick(mc, false);
                break;

            case "Packet":
                mc.player.setDeltaMovement(0, 0, 0);
                double nextX = mc.player.getX() + motionX;
                double nextY = mc.player.getY() + motionY;
                double nextZ = mc.player.getZ() + motionZ;

                // Anti-Kick muokkaa suoraan paketin Y-arvoa
                nextY = applyAntiKick(mc, nextY);

                mc.player.setPos(nextX, nextY, nextZ);
                mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(nextX, nextY, nextZ, false, false));
                break;

            case "Glide":
                // Hidas "liitotila", joka laskee pelaajaa 0.01 per tick (luonnollinen bypass)
                mc.player.setDeltaMovement(motionX, motionY - 0.01, motionZ);
                break;
        }
    }

    /**
     * Palauttaa muokatun Y-koordinaatin Anti-Kickin perusteella
     */
    private double applyAntiKick(Minecraft mc, double currentY) {
        if (!isOffGround(mc) || antiKick.getMode().equals("None")) return currentY;

        if (antiKick.getMode().equals("Dolphin")) {
            // Värähtely: nostaa ja laskee pelaajaa vuorotellen 0.04 yksikköä.
            // Tämä resetoi Vanillan 'floating' laskurin joka toisella tickillä.
            return (tickCounter % 2 == 0) ? currentY + 0.04 : currentY - 0.04;
        }
        else if (antiKick.getMode().equals("Drop")) {
            // Perinteinen dippi 40 tickin välein
            if (tickCounter % 40 == 0) return currentY - 0.08;
        }

        return currentY;
    }

    // Ylikuormitettu versio Velocity-modelle
    private void applyAntiKick(Minecraft mc, boolean unused) {
        if (tickCounter % 40 == 0 && isOffGround(mc) && antiKick.getMode().equals("Drop")) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -0.08, mc.player.getDeltaMovement().z);
        }
    }

    private void handleVanilla(Minecraft mc, double s) {
        mc.player.getAbilities().mayfly = true;
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlyingSpeed((float) (s * 0.05));
        mc.player.onUpdateAbilities();
    }

    private boolean isOffGround(Minecraft mc) {
        return !mc.player.onGround() && !mc.player.isInWater() && !mc.player.onClimbable();
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            oldFovEffectScale = mc.options.fovEffectScale().get();
            mc.options.fovEffectScale().set(0.0);
        }
        tickCounter = 0;
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (oldFovEffectScale != null) mc.options.fovEffectScale().set(oldFovEffectScale);

        mc.player.setNoGravity(false);
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().mayfly = false;
        mc.player.getAbilities().setFlyingSpeed(0.05f);
        mc.player.onUpdateAbilities();
    }
}