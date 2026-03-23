package silversword.axiom.client.main;

import silversword.axiom.client.gui.screen.ClickGuiScreen;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


/**
 * Base class for all Axiom modules.
 *
 * IMPORTANT DESIGN RULE:
 * - update() is FINAL and enforces enabled-check.
 * - Modules implement onTick() instead.
 */
public abstract class AxiomMod {

    protected final String name;
    protected final String description;
    protected final ModuleCategory category;

    /** Single source of truth for module state */
    private boolean enabled = false;

    private final List<Setting> settings = new ArrayList<>();
    private final List<Setting> hiddenSettings = new ArrayList<>();

    protected AxiomMod(String name, String description, ModuleCategory category) {
        this.name = name;
        this.description = description;
        this.category = category == null ? ModuleCategory.MISC : category;

        // Safety: never allow enabled-by-constructor
        this.enabled = false;
    }

    public List<Setting> getAllSettings() {
        List<Setting> all = new ArrayList<>(settings);
        all.addAll(hiddenSettings);
        return Collections.unmodifiableList(all);
    }

    public final void addHiddenSetting(Setting setting) {
        if (setting != null) hiddenSettings.add(setting);
    }



    public static WindowFactory getWindowFactory() {
        return ClickGuiScreen.lastFactory;
    }

    // ------------------------------------------------------------
    // Lifecycle control
    // ------------------------------------------------------------

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;

        if (enabled) {
            AxiomInitialize.EVENT_BUS.register(this);
            onEnable();
        } else {
            AxiomInitialize.EVENT_BUS.register(this);
            onDisable();
        }
    }

    // Lisää nämä tyhjät metodit, jotta moduulit voivat käyttää niitä tarvittaessa
    protected void onActivate() {}
    protected void onDeactivate() {}

    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Called ONCE when module is enabled.
     * Register events, store old values, etc.
     */
    protected void onEnable() {}

    /**
     * Called ONCE when module is disabled.
     * Unregister events, restore values, etc.
     */
    protected void onDisable() {}

    // ------------------------------------------------------------
    // Tick handling (ENFORCED)
    // ------------------------------------------------------------

    /**
     * Called each tick by ModuleManager.
     * FINAL so modules CANNOT forget enabled-check anymore.
     */
    public final void update() {
        if (!enabled) return;
        onTick();
    }

    /**
     * Module logic goes here.
     * This is only called when enabled == true.
     */
    protected abstract void onTick();

    // ------------------------------------------------------------
    // Info
    // ------------------------------------------------------------

    public final String getName() {
        return name;
    }

    /**
     * Stable id used for config saving (windows/pinning/settings).
     * Default = derived from module name (lowercase + underscores).
     */
    public final String getId() {
        return name
                .toLowerCase(Locale.ROOT)
                .trim()
                .replace(' ', '_');
    }

    public final String getDescription() {
        return description;
    }

    public final ModuleCategory getCategory() {
        return category;
    }

    // ------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------

    public final void addSetting(Setting setting) {
        if (setting != null) settings.add(setting);
    }

    public final List<Setting> getSettings() {
        return Collections.unmodifiableList(settings);
    }
}
