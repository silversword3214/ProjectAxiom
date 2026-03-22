package silversword.axiom.client.modules.misc;

import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.hud.HudElement;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.hud.components.PlayerInfoHudComponent;
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

public final class PlayerINFO extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    private static final String HUD_ID = "PlayerINFO";
    private PlayerInfoHudComponent hud;

    // -------- Settings --------
    private final SettingMode mode = new SettingMode("Mode", new String[]{"TARGET", "SELF"}, "SELF");
    private final SettingNumber lingerMs = new SettingNumber("LingerMs", 0, 10000, 50, 1500);
    private final SettingBoolean fadeOut = new SettingBoolean("FadeOut", true);
    private final SettingNumber fadeMs = new SettingNumber("FadeMs", 0, 2000, 25, 250);
    private final SettingNumber maxRange = new SettingNumber("MaxRange", 0, 256, 1, 64);

    // Skaalaukset
    private final SettingNumber textScale = new SettingNumber("Text Scale", 0.5, 2.0, 0.1, 1.0);
    private final SettingNumber backgroundScale = new SettingNumber("Background Scale", 0.5, 2.0, 0.1, 1.0); // SKAALAA SEKÄ TAUSTAA ETTÄ REUNUSTA

    private final SettingBoolean showName = new SettingBoolean("ShowName", true);
    private final SettingBoolean showHealth = new SettingBoolean("ShowHealth", true);
    private final SettingBoolean showDistance = new SettingBoolean("ShowDistance", true);
    private final SettingBoolean showPing = new SettingBoolean("ShowPing", true);
    private final SettingBoolean showArmor = new SettingBoolean("ShowArmor", true);
    private final SettingBoolean showDurability = new SettingBoolean("ShowDurability", true);
    private final SettingBoolean showHands = new SettingBoolean("ShowHands", true);
    private final SettingBoolean compact = new SettingBoolean("Compact", false);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // Väriasetukset (piilotetut) – eri värit taustalle ja reunukselle
    final SettingColor backgroundColor;
    final SettingColor borderColor;
    final SettingColor textColor;

    public PlayerINFO() {
        super("Player Info", "Shows info about targeted player", ModuleCategory.MISC);

        backgroundColor = new SettingColor("Background Color", new Color(0xAA000000)); // läpinäkyvä musta
        borderColor = new SettingColor("Border Color", new Color(0xFF6A00FF));
        textColor = new SettingColor("Text Color", new Color(0xFFFFFFFF));

        addSetting(mode);
        addSetting(lingerMs);
        addSetting(fadeOut);
        addSetting(fadeMs);
        addSetting(maxRange);
        addSetting(textScale);
        addSetting(backgroundScale);
        addSetting(showName);
        addSetting(showHealth);
        addSetting(showDistance);
        addSetting(showPing);
        addSetting(showArmor);
        addSetting(showDurability);
        addSetting(showHands);
        addSetting(compact);

        // Piilotetut väriasetukset
        addHiddenSetting(backgroundColor.getSetting());
        addHiddenSetting(borderColor.getSetting());
        addHiddenSetting(textColor.getSetting());
        addHiddenSetting(toggleKey);

        ensureHudRegistered();
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onEnable() {
        PlayerInfoHudComponent hud = getHud();
        if (hud != null) hud.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        PlayerInfoHudComponent hud = getHud();
        if (hud != null) hud.setEnabled(false);
    }

    @Override
    protected void onTick() {
        if (hud == null) {
            hud = getHud();
            if (hud == null) return;
        }

        hud.setMode(mode.getMode());
        hud.setLingerMs((long) lingerMs.getValue());
        hud.setFadeOut(fadeOut.get());
        hud.setFadeMs((long) fadeMs.getValue());
        hud.setMaxRange(maxRange.getValue());

        // Skaalaukset
        hud.setTextScale((float) textScale.getValue());
        hud.setBackgroundScale((float) backgroundScale.getValue()); // sama skaala taustalle ja reunukselle

        hud.setShowName(showName.get());
        hud.setShowHealth(showHealth.get());
        hud.setShowDistance(showDistance.get());
        hud.setShowPing(showPing.get());
        hud.setShowArmor(showArmor.get());
        hud.setShowDurability(showDurability.get());
        hud.setShowHands(showHands.get());
        hud.setCompact(compact.get());

        // Värit (getCurrentColor!)
        hud.setBackgroundColor(backgroundColor.getCurrentColor().getPacked());
        hud.setBorderColor(borderColor.getCurrentColor().getPacked());
        hud.setTextColor(textColor.getCurrentColor().getPacked());
    }

    private void ensureHudRegistered() {
        if (hud == null) {
            for (HudElement e : HudManager.get().elements()) {
                if (HUD_ID.equals(e.id()) && e instanceof PlayerInfoHudComponent) {
                    hud = (PlayerInfoHudComponent) e;
                    return;
                }
            }
            hud = new PlayerInfoHudComponent();
            HudManager.get().register(hud);
        }
    }

    private PlayerInfoHudComponent getHud() {
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
        factory.openCustomWindow("playerinfo_colors", "PlayerINFO Colors", sw, sh, content);
    }
}