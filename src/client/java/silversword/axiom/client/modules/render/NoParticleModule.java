package silversword.axiom.client.modules.render;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.noparticle.NoParticleWindow;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingString;

import java.util.HashSet;
import java.util.Set;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class NoParticleModule extends AxiomMod {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    public final SettingBoolean enabled = new SettingBoolean("Enabled", true);

    private Set<String> disabledParticles = new HashSet<>();
    private final SettingString disabledList = new SettingString("DisabledParticles", "");
    private boolean loaded = false;

    public NoParticleModule() {
        super("No Particle", "Selectively disable individual particle types.", ModuleCategory.RENDER);
        addHiddenSetting(toggleKey);
        addSetting(enabled);
        addHiddenSetting(disabledList);
    }

    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    public void onTick() {
        // mixin handles filtering
    }

    private void loadDisabledList() {
        if (loaded) return;
        loaded = true;
        String json = disabledList.getString();
        if (json != null && !json.isEmpty()) {
            try {
                TypeToken<Set<String>> token = new TypeToken<>() {};
                disabledParticles = new Gson().fromJson(json, token.getType());
                if (disabledParticles == null) disabledParticles = new HashSet<>();
            } catch (Exception e) {
                disabledParticles = new HashSet<>();
            }
        }
    }

    private void saveDisabledList() {
        String json = new Gson().toJson(disabledParticles);
        disabledList.setValue(json);
    }

    public boolean isParticleDisabled(Identifier id) {
        loadDisabledList();
        return enabled.get() && disabledParticles.contains(id.toString());
    }

    public void setParticleDisabled(Identifier id, boolean disabled) {
        loadDisabledList();
        if (disabled) {
            disabledParticles.add(id.toString());
        } else {
            disabledParticles.remove(id.toString());
        }
        saveDisabledList();
    }

    public Set<String> getDisabledParticles() {
        loadDisabledList();
        return disabledParticles;
    }

    public void openManager() {
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        factory.openPopupWindow(
                "no_particle_manager",
                "Particle Filter",
                (sw - 450) / 2,
                (sh - 500) / 2,
                450,
                500,
                new NoParticleWindow(this)
        );
    }
}