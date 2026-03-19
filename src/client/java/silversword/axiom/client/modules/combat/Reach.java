package silversword.axiom.client.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
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

        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;

        Vec3d targetPos = new Vec3d(
                target.getX(),
                target.getY() + target.getHeight() * 0.5,
                target.getZ()
        );

        double distance = player.getEyePos().distanceTo(targetPos);
        return distance <= ACTIVE_REACH;
    }

    public boolean canReach(HitResult hitResult) {
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) return false;

        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;

        double distance = player.getEyePos().distanceTo(hitResult.getPos());
        return distance <= ACTIVE_REACH;
    }

    public boolean canReachPos(Vec3d pos) {
        if (pos == null) return false;

        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;

        double distance = player.getEyePos().distanceTo(pos);
        return distance <= ACTIVE_REACH;
    }
}
