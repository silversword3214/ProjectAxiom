package silversword.axiom.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingSlider;

public class Reach extends AxiomMod implements KeybindConfigurable {

    private final SettingSlider reachSlider;

    // ✅ Aktiivinen reach-arvo, jota muu koodi / mixin voi lukea luotettavasti
    private static double ACTIVE_REACH = 3.0;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public Reach() {
        super("Reach", "Extends your interaction and attacking distance", ModuleCategory.COMBAT);

        // 3.0 → 6.0 step 0.1
        double[] presets = new double[31];
        for (int i = 0; i < presets.length; i++) {
            presets[i] = 3.0 + i * 0.1;
        }

        this.reachSlider = new SettingSlider("Reach Distance", presets, 3.0);
        addSetting(reachSlider);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    /**
     * ✅ Tätä käytä muualla: jos Reach on päällä, saat “aktiivisen” reachin,
     * muuten default 3.0.
     */
    public static double getActiveReach() {
        return ACTIVE_REACH;
    }

    /**
     * Sliderin arvo (aina), riippumatta onko moduuli päällä.
     */
    public double getReachDistance() {
        return reachSlider.getValue();
    }

    @Override
    protected void onEnable() {
        // Respawn/reapply: laitetaan arvo heti kun moduuli menee päälle
        ACTIVE_REACH = getReachDistance();
    }

    @Override
    protected void onDisable() {
        // Palauta vanilla
        ACTIVE_REACH = 3.0;
    }

    @Override
    public void onTick() {
        // ✅ Päivitä arvo tickissä jotta slider-muutokset ja respawnit eivät “irrota” reachia
        if (!isEnabled()) return;
        ACTIVE_REACH = getReachDistance();
    }

    // ---- Nämä on edelleen hyödyllisiä, mutta nyt ne käyttävät ACTIVE_REACH ----

    public boolean canReach(Entity target) {
        if (target == null) return false;

        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        Vec3 targetPos = new Vec3(
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ()
        );

        double distance = player.getEyePosition().distanceTo(targetPos);
        return distance <= ACTIVE_REACH;
    }

    public boolean canReach(HitResult hitResult) {
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) return false;

        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        double distance = player.getEyePosition().distanceTo(hitResult.getLocation());
        return distance <= ACTIVE_REACH;
    }

    public boolean canReachPos(Vec3 pos) {
        if (pos == null) return false;

        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        double distance = player.getEyePosition().distanceTo(pos);
        return distance <= ACTIVE_REACH;
    }
}
