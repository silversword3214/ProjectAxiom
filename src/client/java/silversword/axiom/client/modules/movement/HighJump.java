package silversword.axiom.client.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.player.PreMotionEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;

public final class HighJump extends AxiomMod implements KeybindConfigurable {

    private final Minecraft mc = Minecraft.getInstance();

    // -- Asetukset --
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingMode mode = new SettingMode("Mode", new String[]{"Vanilla", "Smooth", "Curve", "Multi"}, "Vanilla");
    private final SettingSlider height = new SettingSlider("Height Multiplier", new double[]{1.0, 1.5, 2.0, 3.0, 5.0, 10.0}, 2.0);

    // -- Tilamuuttujat --
    private boolean previousGround = false;
    private boolean wasJumpKeyDown = false;
    private int smoothTicks = 0;
    private int curveTicks = 0;

    // Minecraftin vakiohypyn nopeus ilman potion-efektejä
    private static final double BASE_JUMP_MOTION = 0.42;

    public HighJump() {
        super("High Jump", "Modifies your jump height", ModuleCategory.MOVEMENT);
        addSetting(mode);
        addSetting(height);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        previousGround = mc.player != null && mc.player.onGround();
        wasJumpKeyDown = mc.options.keyJump.isDown();
        smoothTicks = 0;
    }

    @Override
    protected void onDisable() {
        smoothTicks = 0;
        curveTicks = 0;
    }

    @Override
    protected void onTick() {
        // Logiikka ajetaan PreMotionEventissä, jotta se on täydellisessä synkassa pelaajan pakettien ja fysiikan kanssa.
    }

    @Subscribe
    private void onPreMotion(PreMotionEvent event) {
        if (!isEnabled() || mc.player == null || mc.level == null) return;

        boolean currentGround = mc.player.onGround();
        boolean jumpDown = mc.options.keyJump.isDown();
        boolean jumpPressed = jumpDown && !wasJumpKeyDown;

        // KUNNOLLINEN TUNNISTUS: Pelaaja on oikeasti irronnut maasta ylöspäin pelin oman logiikan sallimana.
        // Tämä estää bugaamisen esim. veden alla, cobwebissä tai kattoa vasten hypätessä.
        boolean justJumped = !currentGround && previousGround && mc.player.getDeltaMovement().y > 0;

        String currentMode = mode.getMode();
        double multiplier = height.getValue();

        if (currentMode.equals("Vanilla")) {
            // Vanilla: Raaka motionin kertolasku, kun hyppy tapahtuu. Helpoin tapa, mutta flaggaa heti.
            if (justJumped) {
                Vec3 motion = mc.player.getDeltaMovement();
                mc.player.setDeltaMovement(motion.x, BASE_JUMP_MOTION * multiplier, motion.z);
            }

        } else if (currentMode.equals("Smooth")) {
            // Smooth: Jakaa motion-lisäyksen useammalle tickille. Ei näytä AC:lle massiiviselta piikiltä.
            if (justJumped) {
                smoothTicks = (int) Math.max(3, multiplier * 2); // Levitetään tickejä sitä enemmän, mitä korkeampi hyppy
            }

            if (smoothTicks > 0) {
                Vec3 motion = mc.player.getDeltaMovement();
                // Lasketaan kuinka paljon ylimääräistä vauhtia pitää antaa per tick (vakiohypyn ylittävä osuus)
                double extraMotionTotal = (BASE_JUMP_MOTION * multiplier) - BASE_JUMP_MOTION;
                double boostPerTick = extraMotionTotal / (multiplier * 2);

                mc.player.setDeltaMovement(motion.x, motion.y + boostPerTick, motion.z);
                smoothTicks--;
            }

        } else if (currentMode.equals("Curve")) {
            // Curve: Manipuloidaan painovoimaa hypyn alussa, mutta pakotetaan se loppumaan.
            if (justJumped) {
                curveTicks = (int) (10 * multiplier); // Kuinka kauan antigravity vaikuttaa
            }

            if (curveTicks > 0 && !currentGround && mc.player.getDeltaMovement().y > 0) {
                Vec3 motion = mc.player.getDeltaMovement();

                // Minecraftin painovoima on -0.08 per tick.
                // Rajoitetaan nosto maksimissaan 0.07:ään, jotta motion.y vähenee AINA
                // vähintään 0.01 per tick. Näin loputon leijuminen on fyysisesti mahdotonta.
                double antiGravity = Math.min(0.07, 0.02 * multiplier);

                mc.player.setDeltaMovement(motion.x, motion.y + antiGravity, motion.z);
                curveTicks--;
            } else {
                curveTicks = 0;
            }
        } else if (currentMode.equals("Multi")) {
            // Multi: Normaali boostattu hyppy + mahdollistaa hyppäämisen ilmassa loputtomasti.
            if (justJumped) {
                Vec3 motion = mc.player.getDeltaMovement();
                mc.player.setDeltaMovement(motion.x, BASE_JUMP_MOTION * multiplier, motion.z);
            } else if (jumpPressed && !currentGround) {
                // Mid-air hyppy. Nollataan Y-motion ensin, jotta tippumisvauhti ei heikennä hyppyä.
                Vec3 motion = mc.player.getDeltaMovement();
                mc.player.setDeltaMovement(motion.x, BASE_JUMP_MOTION * multiplier, motion.z);

                // Nollataan fall distance manuaalisesti (client-side), ettei peli tapa pelaajaa ilmahypystä
                mc.player.fallDistance = 0;
            }
        }

        // Päivitetään tilamuuttujat seuraavaa tickiä varten
        previousGround = currentGround;
        wasJumpKeyDown = jumpDown;
    }
}