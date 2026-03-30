package silversword.axiom.client.modules.render;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import org.lwjgl.glfw.GLFW;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class Fullbright extends AxiomMod implements KeybindConfigurable {

    public final SettingBoolean noShadows;
    public final SettingKeybind toggleKey;

    public Fullbright() {
        super("Fullbright", "Brighter world", ModuleCategory.RENDER);

        noShadows = new SettingBoolean("No Shadows", false);
        addSetting(noShadows);

        toggleKey = new SettingKeybind("Toggle Key", GLFW.GLFW_KEY_UNKNOWN);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        FullbrightState.enabled = true;
        FullbrightState.noShadows = noShadows.get();
        forceLightmapUpdate();
    }

    @Override
    protected void onDisable() {
        FullbrightState.enabled = false;
        forceLightmapUpdate();
    }

    @Override
    protected void onTick() {
        // Päivitetään tila, jos asetus muuttuu
        FullbrightState.noShadows = noShadows.get();
    }

    private void forceLightmapUpdate() {
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }
}