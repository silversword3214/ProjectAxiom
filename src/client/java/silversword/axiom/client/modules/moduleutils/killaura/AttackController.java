package silversword.axiom.client.modules.moduleutils.killaura;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import static silversword.axiom.client.main.AxiomInitialize.mc;

public class AttackController {
    private long lastAttackTime = 0;
    private long forcedDelay = 0;
    private Entity queuedTarget = null; // Puskuri hyökkäykselle

    public boolean canAttack(Player player) {
        // 1. Tarkistetaan Vanillan hyökkäysnopeus (cooldown)
        if (player.getAttackStrengthScale(0.5f) < 1.0f) return false;

        long now = System.currentTimeMillis();
        long timeSinceLast = now - lastAttackTime;

        // 2. Tarkistetaan oma satunnainen lisäviive (anti-cheat)
        if (forcedDelay > 0 && timeSinceLast < forcedDelay) return false;

        // 3. Jos vähintään 500ms on kulunut (perusviive), sallitaan hyökkäys
        return timeSinceLast >= 500;
    }

    /**
     * Päivittää hyökkäysajat. Tätä kutsutaan KillAurasta heti lyönnin jälkeen.
     */
    public void recordAttack() {
        this.lastAttackTime = System.currentTimeMillis();
        // Lisätään inhimillistä vaihtelua seuraavaan lyöntiin (60-150ms)
        this.forcedDelay = 60 + (long)(Math.random() * 90);
    }

    /**
     * Laitetaan hyökkäys jonoon odottamaan PreMotion-vaihetta.
     */
    public void queueAttack(Entity target) {
        this.queuedTarget = target;
    }

    /**
     * Suoritetaan jonossa oleva hyökkäys.
     * Kutsutaan LocalPlayerMixinistä (sendPosition HEAD).
     */
    public void onPreMotion() {
        // Jos jono on tyhjä, ei tehdä mitään
        if (queuedTarget == null) return;

        if (mc.player != null && mc.gameMode != null) {
            // Suoritetaan hyökkäys
            mc.gameMode.attack(mc.player, queuedTarget);
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);

            // Päivitetään viiveet
            recordAttack();
        }

        // TÄRKEÄÄ: Tyhjennetään jono AINA metodin lopussa
        queuedTarget = null;
    }

    public void reset() {
        lastAttackTime = 0;
        forcedDelay = 0;
        queuedTarget = null;
    }
}