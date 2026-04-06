package silversword.axiom.client.modules.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import silversword.axiom.client.event.player.PreMotionEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class SafeWalk extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingMode mode = new SettingMode("Mode", new String[]{"Legit", "Silent"}, "Legit");
    private final SettingSlider edgeDistance = new SettingSlider("Edge Distance", new double[]{0.01, 0.05, 0.07, 0.1, 0.15, 0.2, 0.25}, 0.07);

    private boolean wasSneaking = false;
    public static boolean shouldPreventFall = false;

    public SafeWalk() {
        super("SafeWalk", "Prevents falling off edges", ModuleCategory.MOVEMENT);
        addSetting(mode);
        addSetting(edgeDistance);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onDisable() {
        if (mc.player != null && wasSneaking) {
            mc.options.keyShift.setDown(false);
            wasSneaking = false;
        }
        shouldPreventFall = false;
    }

    @Override
    protected void onTick() {

    }

    @Subscribe
    private void onPreMotion(PreMotionEvent event) {
        if (!isEnabled() || mc.player == null || mc.level == null) return;

        // Varmistetaan, että SafeWalk toimii vain kun pelaaja on maassa
        if (!mc.player.onGround()) {
            resetSafeWalk();
            return;
        }

        // Tarkistetaan onko pelaaja reunalla (eli alla ei ole kiinteää blokkia)
        if (isAtEdge()) {
            String currentMode = mode.getMode();
            if (currentMode.equals("Legit")) {
                if (!wasSneaking) {
                    mc.options.keyShift.setDown(true);
                    wasSneaking = true;
                }
                // Pakotetaan liike nollaksi, jotta kyykky ei "liukuisi" reunan yli
                event.setForward(0);
                event.setStrafe(0);
            } else { // Silent
                if (wasSneaking) {
                    mc.options.keyShift.setDown(false);
                    wasSneaking = false;
                }
                shouldPreventFall = true;
            }
        } else {
            resetSafeWalk();
        }
    }

    private void resetSafeWalk() {
        if (wasSneaking && mode.getMode().equals("Legit")) {
            mc.options.keyShift.setDown(false);
            wasSneaking = false;
        }
        shouldPreventFall = false;
    }

    private boolean isAtEdge() {
        // Katsotaan pelaajan jalkojen alle
        // Käytetään pientä offsetia, jotta tarkistus on tarkempi kuin pelkkä blockPosition()
        double x = mc.player.getX();
        double y = mc.player.getY() - 0.5; // Blokki jalkojen alapuolella
        double z = mc.player.getZ();

        // Ennakoidaan liikevektori, jotta reagoidaan ennen putoamista
        double motionX = mc.player.getDeltaMovement().x;
        double motionZ = mc.player.getDeltaMovement().z;

        BlockPos pos = BlockPos.containing(x + motionX, y, z + motionZ);
        BlockState state = mc.level.getBlockState(pos);

        // 1. Tarkistetaan onko blokki ilmaa
        if (state.isAir()) return true;

        // 2. Tarkistetaan onko blokilla törmäyslaatikkoa (esim. ruoho ja kukat palauttavat tyhjän)
        VoxelShape shape = state.getCollisionShape(mc.level, pos);
        if (shape.isEmpty()) return true;

        // 3. Tarkistetaan onko kyseessä neste (vesi/laava)
        if (!state.getFluidState().isEmpty()) return true;

        return false;
    }
}