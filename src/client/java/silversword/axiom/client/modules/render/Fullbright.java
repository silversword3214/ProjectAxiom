package silversword.axiom.client.modules.render;

import com.mojang.blaze3d.platform.InputConstants;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class Fullbright extends AxiomMod implements KeybindConfigurable {

    public final SettingBoolean noShadows;
    public final SettingKeybind toggleKey;

    public Fullbright() {
        super("Fullbright", "Brighter world", ModuleCategory.RENDER);

        noShadows = new SettingBoolean("No Shadows", false);
        addSetting(noShadows);

        // 26.3: GLFW_KEY_UNKNOWN → InputConstants.UNKNOWN.getValue() (-1)
        toggleKey = new SettingKeybind("Toggle Key", InputConstants.UNKNOWN.getValue());
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
        boolean currentNoShadows = noShadows.get();
        if (currentNoShadows != FullbrightState.noShadows) {
            FullbrightState.noShadows = currentNoShadows;
            reloadChunks();
        }
    }

    // Nämä kutsuvat resetLevelRenderData()-metodia, mutta LevelRendererMixin
    // interceptaa sen pelin aikana ja korvaa turvallisella chunk-rebuildilla
    // (invalidateCompiledGeometry). Ei tarvitse muuttaa moduulia.

    private void reloadChunks() {
        if (mc != null && mc.levelRenderer != null) {
            mc.levelRenderer.resetLevelRenderData();
        }
    }

    private void forceLightmapUpdate() {
        if (mc != null && mc.levelRenderer != null) {
            mc.levelRenderer.resetLevelRenderData();
        }
    }
}