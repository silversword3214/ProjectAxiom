package silversword.axiom.client.modules.render;

import net.minecraft.world.level.LightLayer;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;
import org.lwjgl.glfw.GLFW;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class Fullbright extends AxiomMod implements KeybindConfigurable {


    public enum LightTypeOption {
        Both, Sky, Block
    }

    public final SettingMode lightType;
    public final SettingSlider brightness;
    public final SettingKeybind toggleKey;

    public Fullbright() {
        super("Fullbright", "Brighter world", ModuleCategory.RENDER);

        String[] typeNames = new String[LightTypeOption.values().length];
        for (int i = 0; i < LightTypeOption.values().length; i++) {
            typeNames[i] = LightTypeOption.values()[i].name();
        }
        lightType = new SettingMode("Light Type", typeNames, LightTypeOption.Both.name());
        addSetting(lightType);

        brightness = new SettingSlider("Entity Brightness", new double[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}, 15);
        addSetting(brightness);

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
        updateState();
        forceLightmapUpdate();
    }

    @Override
    protected void onDisable() {
        FullbrightState.enabled = false;
        forceLightmapUpdate();
    }

    @Override
    protected void onTick() {
        // Päivitetään tila joka tikillä, jos asetukset muuttuvat
        updateState();
    }

    private void updateState() {
        FullbrightState.minimumLight = (int) brightness.getValue();
        String mode = lightType.getMode();
        if (mode.equals("Both")) {
            FullbrightState.type = null;
        } else if (mode.equals("Sky")) {
            FullbrightState.type = LightLayer.SKY;
        } else {
            FullbrightState.type = LightLayer.BLOCK;
        }
    }

    private void forceLightmapUpdate() {
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged(); // Pakottaa koko maailman uudelleenlatauksen, mikä päivittää lightmapin
        }
    }
}