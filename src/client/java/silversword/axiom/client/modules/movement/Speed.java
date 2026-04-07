package silversword.axiom.client.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.*;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class Speed extends AxiomMod implements KeybindConfigurable {

    // ===================== MOODI =====================
    public final SettingMode mode = new SettingMode("Mode",
            new String[]{"Vanilla", "Custom", "Vulcan"},
            "Vanilla");

    // ===================== VANILLA-ASETUKSET =====================
    public final SettingSlider vanillaSpeed = new SettingSlider("Vanilla Speed",
            new double[]{0.2, 0.25, 0.3, 0.35, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.2, 1.5, 2.0},
            0.3);

    // ===================== CUSTOM-ASETUKSET (täysi konfigurointi) =====================
    public final SettingString customSpeedGround = new SettingString("Speed Ground", "0.4");
    public final SettingString customSpeedAir = new SettingString("Speed Air", "0.3");
    public final SettingBoolean customAirStrafe = new SettingBoolean("Air Strafe", true);
    public final SettingString customAirStrafeBoost = new SettingString("Air Strafe Boost", "0.02");
    public final SettingBoolean customGroundOnly = new SettingBoolean("Ground Only", false);
    public final SettingString customJumpHeight = new SettingString("Jump Height", "0.42");
    public final SettingString customLowHop = new SettingString("Low Hop", "0.0");
    public final SettingBoolean customAutoJump = new SettingBoolean("Auto Jump", false);
    public final SettingBoolean customJumpResetMotion = new SettingBoolean("Jump Reset Motion", true);
    public final SettingBoolean customTimerEnabled = new SettingBoolean("Timer Enabled", false);
    public final SettingString customTimerSpeed = new SettingString("Timer Speed", "1.08");
    public final SettingBoolean customTimerOnlyWhenMoving = new SettingBoolean("Timer Only When Moving", true);
    public final SettingString customHorizontalMultiplier = new SettingString("Horizontal Multiplier", "1.0");
    public final SettingString customVerticalMultiplier = new SettingString("Vertical Multiplier", "1.0");
    public final SettingString customGlideMultiplier = new SettingString("Glide Multiplier", "1.0");
    public final SettingString customGroundFriction = new SettingString("Ground Friction", "0.91");
    public final SettingString customAirFriction = new SettingString("Air Friction", "0.98");
    public final SettingBoolean customFrictionOverride = new SettingBoolean("Override Friction", false);
    public final SettingBoolean customFakeOnGround = new SettingBoolean("Fake OnGround Packet", false);
    public final SettingBoolean customIgnoreGroundOnly = new SettingBoolean("Ignore Ground Only", false);
    public final SettingBoolean customSneakBoost = new SettingBoolean("Sneak Boost", false);
    public final SettingString customSneakBoostAmount = new SettingString("Sneak Boost Amount", "0.3");
    public final SettingBoolean customBlinkWhileSpeeding = new SettingBoolean("Blink While Speeding", false);
    public final SettingString customBlinkDelayMs = new SettingString("Blink Delay (ms)", "200");
    public final SettingString customBlinkBurstPackets = new SettingString("Blink Burst Packets/Tick", "5");
    public final SettingRangeSlider customSpeedRange = new SettingRangeSlider("Speed Range", 0.2, 0.8, 0.1, 2.0, 0.01);
    public final SettingRangeSlider customTimerRange = new SettingRangeSlider("Timer Range", 0.9, 1.1, 0.5, 2.0, 0.01);
    public final SettingMode customSubMode = new SettingMode("SubMode",
            new String[]{"Normal", "StrafeBHop", "TimerBypass", "MotionBoost", "GrimEdge"},
            "Normal");

    // ===================== VULCAN-ASETUKSET (optimoitu antamallasi konffilla) =====================
    public final SettingBoolean vulcanShowAdvanced = new SettingBoolean("Vulcan Show Advanced", false);
    public final SettingString vulcanSpeedGround = new SettingString("Vulcan Speed Ground", "0.2");
    public final SettingString vulcanSpeedAir = new SettingString("Vulcan Speed Air", "0.19");
    public final SettingString vulcanLowHop = new SettingString("Vulcan Low Hop", "0.1");
    public final SettingString vulcanJumpHeight = new SettingString("Vulcan Jump Height", "0.11");
    public final SettingString vulcanTimerSpeed = new SettingString("Vulcan Timer Speed", "0.0000005");
    public final SettingString vulcanHorizontalMultiplier = new SettingString("Vulcan Horizontal Multiplier", "1.8");
    public final SettingString vulcanAirStrafeBoost = new SettingString("Vulcan Air Strafe Boost", "0.1");
    public final SettingString vulcanBlinkDelay = new SettingString("Vulcan Blink Delay (ms)", "200");
    public final SettingString vulcanBlinkBurst = new SettingString("Vulcan Blink Burst Packets/Tick", "5");
    public final SettingBoolean vulcanTimerOnlyWhenMoving = new SettingBoolean("Vulcan Timer Only When Moving", true);
    public final SettingBoolean vulcanFakeOnGround = new SettingBoolean("Vulcan Fake OnGround Packet", true);
    public final SettingBoolean vulcanAutoJump = new SettingBoolean("Vulcan Auto Jump", false);
    public final SettingBoolean vulcanAirStrafe = new SettingBoolean("Vulcan Air Strafe", false);
    public final SettingBoolean vulcanBlinkWhileSpeeding = new SettingBoolean("Vulcan Blink While Speeding", true);
    public final SettingMode vulcanSubMode = new SettingMode("Vulcan SubMode",
            new String[]{"TimerBypass", "Normal"}, "TimerBypass");

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // ===================== TILAMUUTTUJAT =====================
    private boolean wasOnGround = true;
    private int jumpTicks = 0;
    private boolean isTimerActive = false;
    private float originalTimerSpeed = 1.0f;
    private int strafeBHopStage = 0;

    // ===================== KONSTRUKTORI =====================
    public Speed() {
        super("Speed", "Speed hack with customizability", ModuleCategory.MOVEMENT);
        addSetting(mode);
        addSetting(vanillaSpeed);

        // Custom asetukset
        addSetting(customSpeedGround);
        addSetting(customSpeedAir);
        addSetting(customAirStrafe);
        addSetting(customAirStrafeBoost);
        addSetting(customGroundOnly);
        addSetting(customJumpHeight);
        addSetting(customLowHop);
        addSetting(customAutoJump);
        addSetting(customJumpResetMotion);
        addSetting(customTimerEnabled);
        addSetting(customTimerSpeed);
        addSetting(customTimerOnlyWhenMoving);
        addSetting(customHorizontalMultiplier);
        addSetting(customVerticalMultiplier);
        addSetting(customGlideMultiplier);
        addSetting(customGroundFriction);
        addSetting(customAirFriction);
        addSetting(customFrictionOverride);
        addSetting(customFakeOnGround);
        addSetting(customIgnoreGroundOnly);
        addSetting(customSneakBoost);
        addSetting(customSneakBoostAmount);
        addSetting(customBlinkWhileSpeeding);
        addSetting(customBlinkDelayMs);
        addSetting(customBlinkBurstPackets);
        addSetting(customSpeedRange);
        addSetting(customTimerRange);
        addSetting(customSubMode);

        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    // ===================== APUMETODIT =====================
    private double parseDoubleSafe(SettingString setting, double defaultValue) {
        try {
            return Double.parseDouble(setting.getString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void setStrafeMotion(double moveSpeed) {
        Player p = mc.player;
        if (p == null) return;
        float yaw = p.getYRot();
        double rad = Math.toRadians(yaw + 90.0);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        double forward = p.zza;
        double strafe = p.xxa;
        double motionX = forward * cos + strafe * sin;
        double motionZ = forward * sin - strafe * cos;
        double len = Math.hypot(motionX, motionZ);
        if (len != 0) {
            motionX /= len;
            motionZ /= len;
        }
        Vec3 current = p.getDeltaMovement();
        p.setDeltaMovement(motionX * moveSpeed, current.y, motionZ * moveSpeed);
    }

    private void setStrafeMotionWithVertical(double moveSpeed, double yMotion) {
        Player p = mc.player;
        if (p == null) return;
        float yaw = p.getYRot();
        double rad = Math.toRadians(yaw + 90.0);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        double forward = p.zza;
        double strafe = p.xxa;
        double motionX = forward * cos + strafe * sin;
        double motionZ = forward * sin - strafe * cos;
        double len = Math.hypot(motionX, motionZ);
        if (len != 0) {
            motionX /= len;
            motionZ /= len;
        }
        p.setDeltaMovement(motionX * moveSpeed, yMotion, motionZ * moveSpeed);
    }

    private void applyCustomFriction() {
        if (!customFrictionOverride.get()) return;
        Player p = mc.player;
        if (p == null) return;
        double friction = p.onGround() ? parseDoubleSafe(customGroundFriction, 0.91) : parseDoubleSafe(customAirFriction, 0.98);
        Vec3 motion = p.getDeltaMovement();
        p.setDeltaMovement(motion.x * friction, motion.y, motion.z * friction);
    }

    private void applyCustomMotionMultipliers() {
        Player p = mc.player;
        if (p == null) return;
        double hor = parseDoubleSafe(customHorizontalMultiplier, 1.0);
        double ver = parseDoubleSafe(customVerticalMultiplier, 1.0);
        Vec3 motion = p.getDeltaMovement();
        p.setDeltaMovement(motion.x * hor, motion.y * ver, motion.z * hor);
    }

    private void handleTimer(boolean timerEnabled, SettingString timerSpeedSetting, boolean onlyWhenMoving) {
        if (!timerEnabled) {
            if (isTimerActive) deactivateTimer();
            return;
        }
        if (mc.level == null) return;
        boolean moving = mc.player.zza != 0 || mc.player.xxa != 0;
        if (onlyWhenMoving && !moving) {
            if (isTimerActive) deactivateTimer();
            return;
        }
        if (!isTimerActive) {
            originalTimerSpeed = mc.level.tickRateManager().tickrate();
            double speed = parseDoubleSafe(timerSpeedSetting, 1.08);
            mc.level.tickRateManager().setTickRate(originalTimerSpeed / (float) speed);
            isTimerActive = true;
        }
    }

    private void deactivateTimer() {
        if (mc.level != null && isTimerActive) {
            mc.level.tickRateManager().setTickRate(originalTimerSpeed);
            isTimerActive = false;
        }
    }

    // ===================== VANILLA-MOODI =====================
    private void handleVanillaMode() {
        setStrafeMotion(vanillaSpeed.getValue());
    }

    // ===================== CUSTOM-MOODI =====================
    private void handleCustomMode() {
        Player p = mc.player;
        if (p == null) return;
        boolean moving = p.zza != 0 || p.xxa != 0;
        if (customGroundOnly.get() && !p.onGround() && !moving) return;

        // Timer
        handleTimer(customTimerEnabled.get(), customTimerSpeed, customTimerOnlyWhenMoving.get());
        applyCustomFriction();

        double speedGround = parseDoubleSafe(customSpeedGround, 0.4);
        double speedAir = parseDoubleSafe(customSpeedAir, 0.3);
        double jumpHeight = parseDoubleSafe(customJumpHeight, 0.42);
        double lowHopVal = parseDoubleSafe(customLowHop, 0.0);
        double airStrafeBoostVal = parseDoubleSafe(customAirStrafeBoost, 0.02);

        String sub = customSubMode.getMode();
        switch (sub) {
            case "StrafeBHop" -> {
                if (p.onGround()) {
                    p.jumpFromGround();
                    setStrafeMotionWithVertical(speedGround, jumpHeight);
                    strafeBHopStage = 0;
                } else {
                    if (strafeBHopStage < 5) {
                        setStrafeMotion(speedAir * 0.85);
                        strafeBHopStage++;
                    } else {
                        setStrafeMotion(speedAir);
                    }
                }
            }
            case "TimerBypass" -> {
                if (p.onGround()) {
                    if (!wasOnGround && jumpTicks > 2) {
                        double hop = lowHopVal > 0 ? lowHopVal : jumpHeight;
                        p.setDeltaMovement(p.getDeltaMovement().x, hop, p.getDeltaMovement().z);
                    }
                    setStrafeMotion(speedGround * 0.9);
                    wasOnGround = true;
                } else {
                    wasOnGround = false;
                    setStrafeMotion(speedAir * 0.7);
                }
            }
            case "MotionBoost" -> setStrafeMotion(speedGround);
            case "GrimEdge" -> {
                if (p.onGround()) {
                    p.jumpFromGround();
                    setStrafeMotionWithVertical(speedGround, jumpHeight);
                } else {
                    setStrafeMotion(speedAir);
                    if (p.fallDistance > 0) {
                        Vec3 motion = p.getDeltaMovement();
                        p.setDeltaMovement(motion.x * 1.055, motion.y, motion.z * 1.055);
                    }
                }
                if (customFakeOnGround.get() && p.getDeltaMovement().y < 0) {
                    p.setOnGround(true);
                }
            }
            default -> { // Normal
                if (p.onGround()) {
                    if (customAutoJump.get()) {
                        p.jumpFromGround();
                        if (customJumpResetMotion.get()) {
                            setStrafeMotionWithVertical(speedGround, jumpHeight);
                        } else {
                            setStrafeMotion(speedGround);
                        }
                    } else {
                        setStrafeMotion(speedGround);
                    }
                } else {
                    setStrafeMotion(speedAir);
                }
            }
        }

        if (customAirStrafe.get() && !p.onGround() && (p.xxa != 0 || p.zza != 0)) {
            Vec3 motion = p.getDeltaMovement();
            p.setDeltaMovement(motion.x * (1 + airStrafeBoostVal), motion.y, motion.z * (1 + airStrafeBoostVal));
        }

        if (customSneakBoost.get() && p.isCrouching()) {
            double boost = parseDoubleSafe(customSneakBoostAmount, 0.3);
            Vec3 motion = p.getDeltaMovement();
            p.setDeltaMovement(motion.x * (1 + boost), motion.y, motion.z * (1 + boost));
        }

        applyCustomMotionMultipliers();

        if (p.isFallFlying()) {
            double glide = parseDoubleSafe(customGlideMultiplier, 1.0);
            Vec3 motion = p.getDeltaMovement();
            p.setDeltaMovement(motion.x * glide, motion.y, motion.z * glide);
        }

        // BlinkManager integraatio (esimerkki, toteuta tarvittaessa)
        if (customBlinkWhileSpeeding.get()) {
            double delay = parseDoubleSafe(customBlinkDelayMs, 200);
            int burst = (int) parseDoubleSafe(customBlinkBurstPackets, 5);
            // scheduleBlink(delay, burst); // oma toteutus
        }

        jumpTicks++;
    }

    // ===================== VULCAN-MOODI =====================
    private void handleVulcanMode() {
        Player p = mc.player;
        if (p == null) return;

        double speedGround = parseDoubleSafe(vulcanSpeedGround, 0.2);
        double speedAir = parseDoubleSafe(vulcanSpeedAir, 0.19);
        double lowHopVal = parseDoubleSafe(vulcanLowHop, 0.1);
        double jumpHeight = parseDoubleSafe(vulcanJumpHeight, 0.11);
        double timerSpeedVal = parseDoubleSafe(vulcanTimerSpeed, 0.0000005);
        double horizontalMultiplier = parseDoubleSafe(vulcanHorizontalMultiplier, 1.8);
        double airStrafeBoostVal = parseDoubleSafe(vulcanAirStrafeBoost, 0.1);
        double blinkDelay = parseDoubleSafe(vulcanBlinkDelay, 200);
        int blinkBurst = (int) parseDoubleSafe(vulcanBlinkBurst, 5);

        // Timer (äärimmäisen pieni)
        boolean moving = p.zza != 0 || p.xxa != 0;
        if (vulcanTimerOnlyWhenMoving.get() && !moving) {
            if (isTimerActive) deactivateTimer();
        } else {
            if (!isTimerActive && mc.level != null) {
                originalTimerSpeed = mc.level.tickRateManager().tickrate();
                mc.level.tickRateManager().setTickRate(originalTimerSpeed / (float) timerSpeedVal);
                isTimerActive = true;
            }
        }

        String sub = vulcanSubMode.getMode();
        if (sub.equals("TimerBypass")) {
            if (p.onGround()) {
                if (!wasOnGround && jumpTicks > 2) {
                    double hop = lowHopVal > 0 ? lowHopVal : jumpHeight;
                    p.setDeltaMovement(p.getDeltaMovement().x, hop, p.getDeltaMovement().z);
                }
                setStrafeMotion(speedGround * 0.9);
                wasOnGround = true;
            } else {
                wasOnGround = false;
                setStrafeMotion(speedAir * 0.7);
            }
        } else {
            // Normal submode
            if (p.onGround()) {
                if (vulcanAutoJump.get()) {
                    p.jumpFromGround();
                    setStrafeMotionWithVertical(speedGround, jumpHeight);
                } else {
                    setStrafeMotion(speedGround);
                }
            } else {
                setStrafeMotion(speedAir);
            }
        }

        // Horizontal multiplier (tärkeä Vulcanin bypassille)
        Vec3 motion = p.getDeltaMovement();
        p.setDeltaMovement(motion.x * horizontalMultiplier, motion.y, motion.z * horizontalMultiplier);

        if (vulcanAirStrafe.get() && !p.onGround() && (p.xxa != 0 || p.zza != 0)) {
            motion = p.getDeltaMovement();
            p.setDeltaMovement(motion.x * (1 + airStrafeBoostVal), motion.y, motion.z * (1 + airStrafeBoostVal));
        }

        if (vulcanFakeOnGround.get() && p.getDeltaMovement().y < 0) {
            p.setOnGround(true);
        }

        if (vulcanBlinkWhileSpeeding.get()) {
            // scheduleBlink(blinkDelay, blinkBurst); // oma toteutus
        }

        jumpTicks++;
    }

    // ===================== PÄÄLOOPPI =====================
    @Override
    protected void onTick() {
        if (mc.player == null) return;
        Player p = mc.player;

        if (mode.getMode().equals("Vanilla")) {
            handleVanillaMode();
        } else if (mode.getMode().equals("Custom")) {
            // Custom-moodi kunnioittaa ignoreGroundOnly -asetusta
            if (customIgnoreGroundOnly.get() || !customGroundOnly.get() || p.onGround() || (p.zza != 0 || p.xxa != 0)) {
                handleCustomMode();
            }
        } else if (mode.getMode().equals("Vulcan")) {
            handleVulcanMode();
        }
    }

    @Override
    public void onDisable() {
        deactivateTimer();
        super.onDisable();
    }
}