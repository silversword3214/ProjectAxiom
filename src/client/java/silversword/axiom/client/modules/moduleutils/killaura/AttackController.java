package silversword.axiom.client.modules.moduleutils.killaura;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import static silversword.axiom.client.main.AxiomInitialize.mc;

public class AttackController {
    private long lastAttackTime = 0;
    private long forcedDelay = 0;
    private Entity queuedTarget = null;

    public boolean canAttack(Player player) {
        if (player.getAttackStrengthScale(0.5f) < 1.0f) return false;

        long now = System.currentTimeMillis();
        long timeSinceLast = now - lastAttackTime;

        if (forcedDelay > 0 && timeSinceLast < forcedDelay) return false;

        return timeSinceLast >= 500;
    }

    public void recordAttack() {
        this.lastAttackTime = System.currentTimeMillis();
        // Lisätään inhimillistä vaihtelua seuraavaan lyöntiin (60-150ms)
        this.forcedDelay = 60 + (long)(Math.random() * 90);
    }

    public void queueAttack(Entity target) {
        this.queuedTarget = target;
    }

    public void onPreMotion() {
        // Jos jono on tyhjä, ei tehdä mitään
        if (queuedTarget == null) return;

        if (mc.player != null && mc.gameMode != null) {
            // Suoritetaan hyökkäys
            // gameMode.attack hoitaa swing-animaation sisäisesti
            mc.gameMode.attack(mc.player, queuedTarget);

            // Päivitetään viiveet
            recordAttack();
        }

        queuedTarget = null;
    }

    public void reset() {
        lastAttackTime = 0;
        forcedDelay = 0;
        queuedTarget = null;
    }
}