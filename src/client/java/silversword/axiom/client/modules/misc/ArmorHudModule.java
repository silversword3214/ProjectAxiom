package silversword.axiom.client.modules.misc;

import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.hud.HudElement;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.hud.components.ArmorHudComponent;
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

public final class ArmorHudModule extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    private static final String HUD_ID = "Armor Stats";
    private ArmorHudComponent hud;

    // --- asetukset ---
    private final SettingMode mode = new SettingMode("Mode", new String[]{"SELF", "TARGET"}, "SELF");
    private final SettingNumber lingerMs = new SettingNumber("Linger Ms", 0, 5000, 50, 1500);
    private final SettingBoolean fadeOut = new SettingBoolean("Fade Out", true);
    private final SettingNumber fadeMs = new SettingNumber("Fade Ms", 0, 2000, 25, 250);
    private final SettingNumber maxRange = new SettingNumber("Max Range", 0, 256, 1, 64);
    private final SettingNumber textScale = new SettingNumber("Text Scale", 0.5, 2.0, 0.1, 0.5);
    private final SettingNumber backgroundScale = new SettingNumber("Background Scale", 0.5, 2.0, 0.1, 1.0); // UUSI

    private final SettingBoolean showHelmet = new SettingBoolean("Show Helmet", true);
    private final SettingBoolean showChestplate = new SettingBoolean("Show Chestplate", true);
    private final SettingBoolean showLeggings = new SettingBoolean("Show Leggings", true);
    private final SettingBoolean showBoots = new SettingBoolean("Show Boots", true);
    private final SettingBoolean showDurability = new SettingBoolean("Show Durability", true);
    private final SettingBoolean showDurabilityNumbers = new SettingBoolean("Show Durability %", true);
    private final SettingBoolean compact = new SettingBoolean("Compact", false);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // värit
    final SettingColor borderColor;
    final SettingColor textColor;
    final SettingColor backgroundColor;

    public ArmorHudModule() {
        super("ArmorHUD", "Displays armor and durability", ModuleCategory.MISC);

        borderColor = new SettingColor("Border Color", new Color(0xFF6A00FF));
        textColor = new SettingColor("Text Color", new Color(0xFFFFFFFF));
        backgroundColor = new SettingColor("Background Color", new Color(0x80000000));

        addSetting(mode);
        addSetting(lingerMs);
        addSetting(fadeOut);
        addSetting(fadeMs);
        addSetting(maxRange);
        addSetting(textScale);
        addSetting(backgroundScale); // LISÄTTY
        addSetting(showHelmet);
        addSetting(showChestplate);
        addSetting(showLeggings);
        addSetting(showBoots);
        addSetting(showDurability);
        addSetting(showDurabilityNumbers);
        addSetting(compact);

        addHiddenSetting(borderColor.getSetting());
        addHiddenSetting(textColor.getSetting());
        addHiddenSetting(backgroundColor.getSetting());
        addHiddenSetting(toggleKey);

        ensureHudRegistered();
        if (hud != null) hud.setEnabled(this.isEnabled());
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onEnable() { if (hud != null) hud.setEnabled(true); }
    @Override
    protected void onDisable() { if (hud != null) hud.setEnabled(false); }

    @Override
    protected void onTick() {
        if (hud == null) return;

        hud.setMode(mode.getMode());
        hud.setLingerMs((long) lingerMs.getValue());
        hud.setFadeOut(fadeOut.get());
        hud.setFadeMs((long) fadeMs.getValue());
        hud.setMaxRange(maxRange.getValue());
        hud.setTextScale((float) textScale.getValue());
        hud.setBackgroundScale((float) backgroundScale.getValue()); // UUSI

        hud.setShowHelmet(showHelmet.get());
        hud.setShowChestplate(showChestplate.get());
        hud.setShowLeggings(showLeggings.get());
        hud.setShowBoots(showBoots.get());
        hud.setShowDurability(showDurability.get());
        hud.setShowDurabilityNumbers(showDurabilityNumbers.get());
        hud.setCompact(compact.get());

        hud.setBorderColor(borderColor);
        hud.setTextColor(textColor);
        hud.setBackgroundColor(backgroundColor);
    }

    private void ensureHudRegistered() {
        if (hud == null) {
            for (HudElement e : HudManager.get().elements()) {
                if (HUD_ID.equals(e.id()) && e instanceof ArmorHudComponent) {
                    hud = (ArmorHudComponent) e;
                    return;
                }
            }
            hud = new ArmorHudComponent();
            HudManager.get().register(hud);
        }
    }

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Border", borderColor),
                new NamedColor("Text", textColor),
                new NamedColor("Background", backgroundColor)
        );
    }

    @Override
    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("armorhud_colors", "ArmorHUD Colors", sw, sh, content);
    }
}