package silversword.axiom.client.modules.misc;

import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.hud.components.TargetHudComponent;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.*;

import java.util.Arrays;
import java.util.List;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class TargetHUD extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    private final TargetHudComponent element = new TargetHudComponent();
    private boolean registered = false;

    // ---- Settings ----
    private final SettingNumber lingerMs = new SettingNumber("Linger (ms)", 0, 10000, 50, 1500);
    private final SettingBoolean fadeOut = new SettingBoolean("Fade Out", true);
    private final SettingNumber fadeMs = new SettingNumber("Fade (ms)", 0, 2000, 25, 250);
    private final SettingNumber maxRange = new SettingNumber("Max Range", 0, 256, 1, 48);
    private final SettingBoolean showName = new SettingBoolean("Show Name", true);
    private final SettingBoolean showHealthText = new SettingBoolean("Show Health Text", true);
    private final SettingBoolean showDistance = new SettingBoolean("Show Distance", true);
    private final SettingBoolean showHealthBar = new SettingBoolean("Show Health Bar", true);

    // Skaalaukset
    private final SettingNumber textScale = new SettingNumber("Text Scale", 0.5, 2.0, 0.1, 0.6);
    private final SettingNumber backgroundScale = new SettingNumber("Background Scale", 0.5, 2.0, 0.1, 1.0);
    private final SettingNumber outlineScale = new SettingNumber("Outline Scale", 0.5, 2.0, 0.1, 1.0);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // Väriasetukset (piilotetut)
    final SettingColor borderColor;
    final SettingColor textColor;

    public TargetHUD() {
        super("Target Hud", "Shows info about the entity you're aiming at", ModuleCategory.MISC);

        borderColor = new SettingColor("Border Color", new Color(0xFF6A00FF));
        textColor = new SettingColor("Text Color", new Color(0xFFFFFFFF));

        addSetting(lingerMs);
        addSetting(fadeOut);
        addSetting(fadeMs);
        addSetting(maxRange);
        addSetting(showName);
        addSetting(showHealthText);
        addSetting(showDistance);
        addSetting(showHealthBar);
        addSetting(textScale);
        addSetting(backgroundScale);
        addSetting(outlineScale);

        // Piilotetut väriasetukset
        addHiddenSetting(borderColor.getSetting());
        addHiddenSetting(textColor.getSetting());
        addHiddenSetting(toggleKey);

        if (!registered) {
            HudManager.get().register(element);
            registered = true;
        }
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        if (!registered) {
            HudManager.get().register(element);
            registered = true;
        }
        element.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        element.setEnabled(false);
    }

    @Override
    public void onTick() {
        element.setLingerMs((long) lingerMs.getValue());
        element.setFadeOut(fadeOut.get());
        element.setFadeMs((long) fadeMs.getValue());
        element.setMaxRange(maxRange.getValue());
        element.setShowName(showName.get());
        element.setShowHealthText(showHealthText.get());
        element.setShowDistance(showDistance.get());
        element.setShowHealthBar(showHealthBar.get());
        element.setTextScale((float) textScale.getValue());
        element.setBackgroundScale((float) backgroundScale.getValue());
        element.setOutlineScale((float) outlineScale.getValue());
        element.setBorderColor(borderColor.getCurrentColor().getARGB());
        element.setTextColor(textColor.getCurrentColor().getARGB());
    }

    // ----- ColorConfigurable toteutus -----
    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Border", borderColor),
                new NamedColor("Text", textColor)
        );
    }

    @Override
    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("targethud_colors", "TargetHUD Colors", sw, sh, content);
    }
}