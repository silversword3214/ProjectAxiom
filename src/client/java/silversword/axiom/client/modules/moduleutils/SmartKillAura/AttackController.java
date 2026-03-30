package silversword.axiom.client.modules.moduleutils.SmartKillAura;

import net.minecraft.world.entity.player.Player;

public class AttackController {

    private long lastAttackTime = 0;
    private double currentDelay = 0;
    private boolean waitingForRotation = false;
    private long rotationReadyTime = 0;

    public boolean canAttack(Player player, double minCps, double maxCps) {
        // Odota että attack bar on täynnä (1.0)
        if (player.getAttackStrengthScale(0.5f) < 1.0f) return false;

        // CPS-pohjainen lisäviive (humanisoi)
        if (currentDelay == 0) {
            double cps = minCps + Math.random() * (maxCps - minCps);
            currentDelay = 1000.0 / cps;
        }

        long now = System.currentTimeMillis();
        return now - lastAttackTime >= currentDelay;
    }

    public void recordAttack() {
        lastAttackTime = System.currentTimeMillis();
        currentDelay = 0;
        waitingForRotation = false;
    }

    public void reset() {
        lastAttackTime = 0;
        currentDelay = 0;
        waitingForRotation = false;
        rotationReadyTime = 0;
    }

    public void startWaitingForRotation() {
        waitingForRotation = true;
        rotationReadyTime = System.currentTimeMillis() + (long)(Math.random() * 20);
    }

    public boolean isRotationReady() {
        if (!waitingForRotation) return true;
        if (System.currentTimeMillis() >= rotationReadyTime) {
            waitingForRotation = false;
            return true;
        }
        return false;
    }
}