package silversword.axiom.client.modules.misc;

import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.hud.HudElement;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.hud.components.PotionEffectsHud;
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

public final class PotionEffects extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    private static final String HUD_ID = "Potions";
    private PotionEffectsHud hud;

    // Settings
    private final SettingMode mode = new SettingMode("Mode", new String[]{"SELF", "TARGET"}, "SELF");
    private final SettingNumber maxEffects = new SettingNumber("Max Effects", 1, 32, 1, 8);
    private final SettingBoolean showDurations = new SettingBoolean("Show Durations", true);
    private final SettingBoolean compact = new SettingBoolean("Compact", false);
    private final SettingNumber lingerMs = new SettingNumber("Linger Ms", 0, 10000, 50, 1500);
    private final SettingBoolean fadeOut = new SettingBoolean("Fade Out", true);
    private final SettingNumber fadeMs = new SettingNumber("Fade Ms", 0, 2000, 25, 250);
    private final SettingNumber maxRange = new SettingNumber("Max Range", 0, 256, 1, 64);

    // Skaalaukset
    private final SettingNumber textScale = new SettingNumber("Text Scale", 0.5, 2.0, 0.1, 0.6);
    private final SettingNumber backgroundScale = new SettingNumber("Background Scale", 0.5, 2.0, 0.1, 1.0);
    private final SettingNumber outlineScale = new SettingNumber("Outline Scale", 0.5, 2.0, 0.1, 1.0);

    // Colors
    private final SettingColor backgroundColor;
    private final SettingColor borderColor;
    private final SettingColor textColor;

    // Keybind
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public PotionEffects() {
        super("Potion Effects", "Shows active potion effects", ModuleCategory.MISC);

        backgroundColor = new SettingColor("Background Color", new Color(0x90000000));
        borderColor = new SettingColor("Border Color", new Color(0xFF6A00FF));
        textColor = new SettingColor("Text Color", new Color(0xFFFFFFFF));

        addSetting(mode);
        addSetting(maxEffects);
        addSetting(showDurations);
        addSetting(compact);
        addSetting(lingerMs);
        addSetting(fadeOut);
        addSetting(fadeMs);
        addSetting(maxRange);
        addSetting(textScale);
        addSetting(backgroundScale);
        addSetting(outlineScale);

        // Hidden settings for colors and keybind
        addHiddenSetting(backgroundColor.getSetting());
        addHiddenSetting(borderColor.getSetting());
        addHiddenSetting(textColor.getSetting());
        addHiddenSetting(toggleKey);

        ensureHudRegistered();
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        PotionEffectsHud h = getHud();
        if (h != null) h.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        PotionEffectsHud h = getHud();
        if (h != null) h.setEnabled(false);
    }

    @Override
    protected void onTick() {
        PotionEffectsHud h = getHud();
        if (h == null) return;

        h.setMode(mode.getMode());
        h.setMaxEffects((int) maxEffects.getValue());
        h.setShowDurations(showDurations.get());
        h.setCompact(compact.get());
        h.setLingerMs((long) lingerMs.getValue());
        h.setFadeOut(fadeOut.get());
        h.setFadeMs((long) fadeMs.getValue());
        h.setMaxRange(maxRange.getValue());

        // Skaalaukset
        h.setTextScale((float) textScale.getValue());
        h.setBackgroundScale((float) backgroundScale.getValue());
        h.setOutlineScale((float) outlineScale.getValue());

        // Värit (getCurrentColor!)
        h.setBackgroundColor(backgroundColor.getCurrentColor().getARGB());
        h.setBorderColor(borderColor.getCurrentColor().getARGB());
        h.setTextColor(textColor.getCurrentColor().getARGB());
    }

    private void ensureHudRegistered() {
        if (hud == null) {
            for (HudElement e : HudManager.get().elements()) {
                if (HUD_ID.equals(e.id()) && e instanceof PotionEffectsHud) {
                    hud = (PotionEffectsHud) e;
                    return;
                }
            }
            hud = new PotionEffectsHud();
            HudManager.get().register(hud);
        }
    }

    private PotionEffectsHud getHud() {
        if (hud == null) ensureHudRegistered();
        return hud;
    }

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Background", backgroundColor),
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
        factory.openCustomWindow("potioneffects_colors", "Potion Effects Colors", sw, sh, content);
    }
}