package silversword.axiom.client.hud.util;

public final class ComboCounter {
    private static int currentCombo = 0;
    private static long lastHitTime = 0;
    private static final long TIMEOUT_MS = 2000; // 2 sekuntia

    private ComboCounter() {}

    /**
     * Kutsu tätä aina kun pelaaja osuu entiteettiin.
     */
    public static void onHit() {
        long now = System.currentTimeMillis();
        if (now - lastHitTime > TIMEOUT_MS) {
            currentCombo = 1;
        } else {
            currentCombo++;
        }
        lastHitTime = now;
    }

    /**
     * Palauttaa nykyisen combon. Nollautuu automaattisesti aikakatkaisun jälkeen.
     */
    public static int getCombo() {
        if (System.currentTimeMillis() - lastHitTime > TIMEOUT_MS) {
            currentCombo = 0;
        }
        return currentCombo;
    }

    /**
     * Nollaa combon manuaalisesti (esim. kuoleman tai maailmanvaihdon yhteydessä).
     */
    public static void resetCombo() {
        currentCombo = 0;
        lastHitTime = 0;
    }
}