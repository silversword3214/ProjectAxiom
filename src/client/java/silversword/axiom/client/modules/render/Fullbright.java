package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;

public class Fullbright extends AxiomMod {

    public final SettingBoolean noShadows;
    private Boolean savedAmbientOcclusion = null;

    public Fullbright() {
        super("Fullbright", "Brighter world", ModuleCategory.RENDER);
        noShadows = new SettingBoolean("No Shadows", false);
        addSetting(noShadows);
    }

    @Override
    protected void onEnable() {
        FullbrightState.enabled = true;
        FullbrightState.noShadows = noShadows.get();
        applyNoShadows();
    }

    @Override
    protected void onDisable() {
        FullbrightState.enabled = false;
        restoreAmbientOcclusion();
    }

    @Override
    protected void onTick() {
        boolean current = noShadows.get();
        if (current != FullbrightState.noShadows) {
            FullbrightState.noShadows = current;
            applyNoShadows();
            reloadChunks();
        }
    }

    private void applyNoShadows() {
        Options opts = Minecraft.getInstance().options;
        if (opts == null) return;
        if (FullbrightState.noShadows) {
            if (savedAmbientOcclusion == null) {
                savedAmbientOcclusion = opts.ambientOcclusion().get();
            }
            opts.ambientOcclusion().set(false);
        } else {
            restoreAmbientOcclusion();
        }
    }

    private void restoreAmbientOcclusion() {
        if (savedAmbientOcclusion == null) return;
        Minecraft.getInstance().options.ambientOcclusion().set(savedAmbientOcclusion);
        savedAmbientOcclusion = null;
    }

    private void reloadChunks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) mc.levelRenderer.resetLevelRenderData();
    }
}