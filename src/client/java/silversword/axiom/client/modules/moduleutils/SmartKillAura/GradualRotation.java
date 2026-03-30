package silversword.axiom.client.modules.moduleutils.SmartKillAura;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class GradualRotation {
    private float targetYaw, targetPitch;
    private float currentYaw, currentPitch;
    private float turnSpeed;       // astetta/tick
    private float jitterAmount;    // satunnaisuuden määrä
    private boolean active = false;
    private boolean hasTarget = false;

    public void setTarget(float yaw, float pitch, float turnSpeed, float jitter) {
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.turnSpeed = turnSpeed;
        this.jitterAmount = jitter;
        this.hasTarget = true;

        if (!active) {
            // Alustetaan nykyiset arvot serverin rotaatiosta (tai clientin, jos ei ole tallessa)
            currentYaw = RotationHandler.getServerYaw();
            currentPitch = RotationHandler.getServerPitch();
            active = true;
        }
    }

    /**
     * Päivitetään nykyistä rotaatiota kohti tavoitetta.
     * Palauttaa true, jos rotaatiota pitää lähettää tällä tickillä.
     */
    public boolean updateAndSend() {
        if (!hasTarget || !active) return false;

        // Laske erot (wrap)
        float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        // Rajoitetaan kääntymisnopeutta
        float yawStep = Mth.clamp(yawDiff, -turnSpeed, turnSpeed);
        float pitchStep = Mth.clamp(pitchDiff, -turnSpeed, turnSpeed);

        float newYaw = currentYaw + yawStep;
        float newPitch = currentPitch + pitchStep;

        // Lisää jitter (satunnaisuus)
        if (jitterAmount > 0) {
            newYaw += (float) ((Math.random() - 0.5) * jitterAmount);
            newPitch += (float) ((Math.random() - 0.5) * jitterAmount * 0.5);
        }

        // Normalisoi
        newYaw = Mth.wrapDegrees(newYaw);
        newPitch = Mth.clamp(newPitch, -90f, 90f);

        // Jos muutos on hyvin pieni, emme lähetä pakettia (säästää liikennettä)
        boolean changed = Math.abs(newYaw - currentYaw) > 0.01f || Math.abs(newPitch - currentPitch) > 0.01f;

        if (changed) {
            currentYaw = newYaw;
            currentPitch = newPitch;
            // Lähetetään rotaatiopaketti (yhdistetään position kanssa, jos mahdollista)
            sendRotationPacket();
        }

        // Jos olemme jo tarpeeksi lähellä tavoitetta, deaktivoidaan tilapäisesti
        if (Math.abs(yawDiff) < turnSpeed * 0.5f && Math.abs(pitchDiff) < turnSpeed * 0.5f) {
            active = false;
        }

        return changed;
    }

    private void sendRotationPacket() {
        // Tässä yhdistetään nykyinen positio ja rotaatio samaan pakettiin, jos positio on muuttunut tällä tickillä.
        // Koska emme tiedä, onko positio muuttunut, turvallisin tapa on lähettää pelkkä rotaatiopaketti.
        // Mutta voidaan tarkistaa, onko pelaajan positio muuttunut edellisestä tickistä.
        // Yksinkertainen tapa: lähetä aina Rot-paketti, mutta se on erillinen.
        // Anti-cheatin kannalta parempi olisi yhdistää position kanssa, mutta vaatii enemmän kontekstia.
        // Tässä käytetään RotationHandlerin olemassa olevaa metodia, joka lähettää vain rotaation.
        RotationHandler.rotateImmediate(currentYaw, currentPitch);
    }

    public void reset() {
        active = false;
        hasTarget = false;
    }

    public boolean isActive() {
        return active;
    }
}