package silversword.axiom.client.modules.render;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class NoFog extends AxiomMod implements KeybindConfigurable {

    public final SettingBoolean disableAtmosphericFog = new SettingBoolean("Atmospheric Fog", true);
    public final SettingBoolean disableWaterFog = new SettingBoolean("Water Fog", true);
    public final SettingBoolean disableLavaFog = new SettingBoolean("Lava Fog", true);
    public final SettingBoolean disablePowderSnowFog = new SettingBoolean("Powder Snow Fog", true);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private ResourceKey<Level> lastWorld;

    public NoFog() {
        super("NoFog", "Disables various types of fog", ModuleCategory.RENDER);

        addSetting(disableAtmosphericFog);
        addSetting(disableWaterFog);
        addSetting(disableLavaFog);
        addSetting(disablePowderSnowFog);

        addHiddenSetting(toggleKey);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isEnabled()) return;
            if (client.level == null) return;

            ResourceKey<Level> current = client.level.dimension();

            if (lastWorld != current) {
                lastWorld = current;
                refreshRenderer();
            }
        });
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        refreshRenderer();
    }

    @Override
    protected void onDisable() {
        if (mc.player != null)
            mc.levelRenderer.allChanged();
    }

    @Override
    protected void onTick() {

    }

    private void refreshRenderer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }
}