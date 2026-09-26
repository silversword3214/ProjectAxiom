package silversword.axiom.client.modules.moduleutils.killaura;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

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
        this.forcedDelay = 60 + (long)(Math.random() * 90);
    }

    public void queueAttack(Entity target) {
        this.queuedTarget = target;
    }

    /** Ei enää käytössä — hyökkäys tapahtuu KillAura:n kautta KeyMapping.click()illä. */
    public void onPreMotion() {
        this.queuedTarget = null;
    }

    public void reset() {
        lastAttackTime = 0;
        forcedDelay = 0;
        queuedTarget = null;
    }
}