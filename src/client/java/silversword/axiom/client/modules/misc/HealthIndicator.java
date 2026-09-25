package silversword.axiom.client.modules.misc;

import silversword.axiom.client.hud.HudElement;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.hud.components.HealthIndicatorHudComponent;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

public final class HealthIndicator extends AxiomMod implements KeybindConfigurable {

    private static final String HUD_ID = "HealthIndicator";
    private HealthIndicatorHudComponent hud;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private final SettingNumber scale = new SettingNumber("Scale", 0.5, 3.0, 0.1, 1.0);
    private final SettingNumber maxRange = new SettingNumber("Max Range", 0, 128, 1, 48);
    private final SettingBoolean playersOnly = new SettingBoolean("Players Only", false);
    private final SettingBoolean showHeartCounter = new SettingBoolean("Show Heart Counter", true);
    private final SettingBoolean showNumericHp = new SettingBoolean("Show Numeric HP", true);
    private final SettingBoolean showEntityIcon = new SettingBoolean("Show Entity Icon", true);

    private final SettingBoolean rainbowHealthBar = new SettingBoolean("Rainbow Health Bar", false);
    private final SettingNumber rainbowSpeed = new SettingNumber("Rainbow Speed", 0.1, 5.0, 0.1, 1.0);

    public HealthIndicator() {
        super("Health Indicator", "Shows target health next to crosshair", ModuleCategory.MISC);

        addSetting(scale);
        addSetting(maxRange);
        addSetting(playersOnly);
        addSetting(showHeartCounter);
        addSetting(showNumericHp);
        addSetting(showEntityIcon);
        addSetting(rainbowHealthBar);
        addSetting(rainbowSpeed);
        addHiddenSetting(toggleKey);

        ensureHudRegistered();
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onEnable() {
        HealthIndicatorHudComponent h = getHud();
        if (h != null) h.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        HealthIndicatorHudComponent h = getHud();
        if (h != null) h.setEnabled(false);
    }

    @Override
    protected void onTick() {
        if (hud == null) {
            hud = getHud();
            if (hud == null) return;
        }
        hud.setScale((float) scale.getValue());
        hud.setMaxRange(maxRange.getValue());
        hud.setPlayersOnly(playersOnly.get());
        hud.setShowHeartCounter(showHeartCounter.get());
        hud.setShowNumericHp(showNumericHp.get());
        hud.setShowEntityIcon(showEntityIcon.get());
        hud.setRainbowHealthBar(rainbowHealthBar.get());
        hud.setRainbowSpeed((float) rainbowSpeed.getValue());
    }

    private void ensureHudRegistered() {
        if (hud != null) return;
        for (HudElement e : HudManager.get().elements()) {
            if (HUD_ID.equals(e.id()) && e instanceof HealthIndicatorHudComponent comp) {
                hud = comp;
                return;
            }
        }
        hud = new HealthIndicatorHudComponent();
        HudManager.get().register(hud);
    }

    private HealthIndicatorHudComponent getHud() {
        if (hud == null) ensureHudRegistered();
        return hud;
    }
}