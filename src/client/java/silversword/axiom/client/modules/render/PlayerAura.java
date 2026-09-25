package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.modules.moduleutils.TargetGroup;
import silversword.axiom.client.render.rendersystem.axiomrenderer.playeraura.PlayerAuraRenderer;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.color.rainbow.RainbowPalette;
import silversword.axiom.client.setting.*;

import java.util.Arrays;
import java.util.List;

public final class PlayerAura extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    private final Minecraft mc = Minecraft.getInstance();

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // ── Geometria ────────────────────────────────────────────────
    private final SettingNumber ringCount;
    private final SettingNumber radius;
    private final SettingNumber rotationSpeed;
    private final SettingNumber fresnelPower;

    // ── Kantama ──────────────────────────────────────────────────
    private final SettingNumber renderDistance;

    // ── Väri ─────────────────────────────────────────────────────
    private final SettingMode colorMode;   // HP / Static / Rainbow / Group
    final SettingColor auraColor;
    final SettingColor selfColor;

    // ── Kohdevalinta ─────────────────────────────────────────────
    private final SettingBoolean onlyPlayers;
    private final SettingBoolean includeSelf;
    private final SettingBoolean onlyEnemies;

    public PlayerAura() {
        super("Player Aura", "Glowing fresnel rings around players", ModuleCategory.RENDER);

        ringCount     = new SettingNumber("Rings", 1, 6, 1, 4);
        radius        = new SettingNumber("Radius", 0.4, 3.0, 0.1, 1.2);
        rotationSpeed = new SettingNumber("Rotation Speed", 0.0, 4.0, 0.1, 1.0);
        fresnelPower  = new SettingNumber("Fresnel Power", 0.5, 5.0, 0.1, 2.0);

        renderDistance = new SettingNumber("Render Distance", 8, 128, 1, 48);

        colorMode  = new SettingMode("Color Mode",
                new String[]{"HP-Based", "Static", "Rainbow", "Group"}, "HP-Based");

        auraColor = new SettingColor("Aura Color", new Color(0xFF4DD2FF));   // syaani
        selfColor = new SettingColor("Self Color", new Color(0xFFB84CFF));   // violetti

        onlyPlayers = new SettingBoolean("Only Players", true);
        includeSelf = new SettingBoolean("Include Self", false);
        onlyEnemies = new SettingBoolean("Only Enemies", false);

        addSetting(ringCount);
        addSetting(radius);
        addSetting(rotationSpeed);
        addSetting(fresnelPower);
        addSetting(renderDistance);
        addSetting(colorMode);
        addSetting(onlyPlayers);
        addSetting(includeSelf);
        addSetting(onlyEnemies);

        addHiddenSetting(auraColor.getSetting());
        addHiddenSetting(selfColor.getSetting());
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    @Override
    protected void onTick() {}

    @Subscribe
    private void onRender3D(Render3DEvent event) {
        if (!isEnabled()) return;
        if (mc.level == null || mc.player == null) return;
        PlayerAuraRenderer.render(event, this);
    }

    // ── Moduulin rajapinta renderöijälle ────────────────────────────

    public int getRingCount()      { return (int) ringCount.getValue(); }
    public double getRadius()      { return radius.getValue(); }
    public double getRotationSpeed() { return rotationSpeed.getValue(); }
    public double getFresnelPower()  { return fresnelPower.getValue(); }
    public double getRenderDistance() { return renderDistance.getValue(); }

    /** Onko entity validi kohde tälle moduulille. */
    public boolean isTarget(LivingEntity e) {
        if (e == mc.player) return includeSelf.get();

        if (onlyPlayers.get() && !(e instanceof Player)) return false;

        if (onlyEnemies.get()) {
            if (e instanceof Player p) {
                // Oletus: kaikki muut ovat vihollisia
                return true;
            }
            return false;
        }

        // Jos ei olla rajoitettu, mutta onlyPlayers = false → hyväksy kaikki living
        return e instanceof Player || !onlyPlayers.get();
    }

    /** Palauttaa 0xAARRGGBB värin annetulle entitylle. */
    public int resolveColor(LivingEntity e, float hpPct, long now) {
        String mode = colorMode.getMode();

        if ("Rainbow".equals(mode)) {
            try {
                RainbowPalette palette = ClickGuiConfigManager.getRainbowPalette();
                if (palette != null) {
                    float period = 5000f / 1.0f;
                    float t = (now % (long) period) / period;
                    t += e.getId() * 0.13f;
                    t %= 1.0f;
                    if (t < 0) t += 1.0f;
                    return palette.getColorInterpolatedLoop(t);
                }
            } catch (Throwable ignored) {}
            return auraColor.getCurrentColor().getARGB();
        }

        if ("HP-Based".equals(mode)) {
            // Vihreä → keltainen → punainen
            int r, g, b;
            if (hpPct >= 0.66f) {
                r = 0x44; g = 0xDD; b = 0x44;
            } else if (hpPct >= 0.33f) {
                r = 0xFF; g = 0xCC; b = 0x00;
            } else {
                r = 0xFF; g = 0x33; b = 0x44;
            }
            int alpha = auraColor.getCurrentColor().getAlpha();
            return (alpha << 24) | (r << 16) | (g << 8) | b;
        }

        if ("Group".equals(mode)) {
            if (e == mc.player) return selfColor.getCurrentColor().getARGB();
            return auraColor.getCurrentColor().getARGB();
        }

        // Static
        return auraColor.getCurrentColor().getARGB();
    }

    // ── ColorConfigurable ─────────────────────────────────────────

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Aura", auraColor),
                new NamedColor("Self", selfColor)
        );
    }

    @Override
    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("playeraura_colors", "Player Aura Colors", sw, sh, content);
    }
}