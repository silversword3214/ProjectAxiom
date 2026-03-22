package silversword.axiom.client.modules.moduleutils.SmartKillAura;

import net.minecraft.world.entity.player.Player;

public class AttackController {

    private long lastAttackTime = 0;
    private double currentDelay = 0;

    public boolean canAttack(Player player, double minCps, double maxCps) {
        // Lasketaan satunnainen viive annetulla CPS-välillä
        if (currentDelay == 0) {
            double cps = minCps + Math.random() * (maxCps - minCps);
            currentDelay = 1000.0 / cps; // millisekunteina
        }

        long now = System.currentTimeMillis();
        if (now - lastAttackTime >= currentDelay) {
            // Lisäksi tarkista 1.9+ cooldown
            float progress = player.getAttackStrengthScale(0.5f);
            return progress > 0.92f;
        }
        return false;
    }

    public void recordAttack() {
        lastAttackTime = System.currentTimeMillis();
        currentDelay = 0; // arvotaan uusi viive seuraavalle iskulle
    }

    public void reset() {
        lastAttackTime = 0;
        currentDelay = 0;
    }
}