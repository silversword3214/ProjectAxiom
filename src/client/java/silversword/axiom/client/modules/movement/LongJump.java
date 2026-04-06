package silversword.axiom.client.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.player.PreMotionEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingSlider;

public final class LongJump extends AxiomMod implements KeybindConfigurable {

    private final Minecraft mc = Minecraft.getInstance();

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingSlider boost = new SettingSlider("Boost", new double[]{1.0, 1.5, 2.0, 3.0, 5.0, 10.0}, 2.0);

    private boolean wasOnGround = true;

    public LongJump() {
        super("LongJump", "Gives a velocity boost when jumping", ModuleCategory.MOVEMENT);
        addSetting(boost);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        if (mc.player != null) {
            wasOnGround = mc.player.onGround();
        }
    }

    @Override
    protected void onTick() {
        // Logiikka ajetaan PreMotionissa
    }

    @Subscribe
    private void onPreMotion(PreMotionEvent event) {
        if (!isEnabled() || mc.player == null) return;

        boolean currentGround = mc.player.onGround();

        // Tunnistetaan hyppyhetki: pelaaja oli maassa, mutta nyt ilmassa ja nousemassa ylös
        if (!currentGround && wasOnGround && mc.player.getDeltaMovement().y > 0) {

            // Tarkistetaan liikkuuko pelaaja (ettei boostaa paikallaan hypätessä)
            if (mc.player.input.getMoveVector().lengthSquared() > 0) {
                applyBoost();
            }
        }

        wasOnGround = currentGround;
    }

    private void applyBoost() {
        // Haetaan pelaajan katsomissuunta (Yaw)
        float yaw = mc.player.getYRot() * (float) (Math.PI / 180.0);
        double multiplier = boost.getValue();

        // Lasketaan uusi vauhti katsomissuunnan perusteella
        // Minecraftissa -sin(yaw) on X ja cos(yaw) on Z
        double motionX = -Mth.sin(yaw) * (0.2 * multiplier);
        double motionZ = Mth.cos(yaw) * (0.2 * multiplier);

        Vec3 current = mc.player.getDeltaMovement();

        // Asetetaan uusi liikevektori (säilytetään nykyinen Y-nousu)
        mc.player.setDeltaMovement(motionX, current.y, motionZ);
    }
}