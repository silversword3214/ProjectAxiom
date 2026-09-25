package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.render.rendersystem.axiomrenderer.esp2d.Esp2DRenderer;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.color.rainbow.RainbowPalette;
import silversword.axiom.client.setting.*;

import java.util.Arrays;
import java.util.List;

public final class Esp2D extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    private final Minecraft mc = Minecraft.getInstance();

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // ── Box ──────────────────────────────────────────────────────
    private final SettingMode boxMode;         // Full / Corners
    private final SettingNumber thickness;
    private final SettingNumber cornerSize;
    private final SettingBoolean rounded;

    // ── Health bar ───────────────────────────────────────────────
    private final SettingBoolean showHealthBar;
    private final SettingNumber barWidth;

    // ── Text ─────────────────────────────────────────────────────
    private final SettingBoolean showName;
    private final SettingBoolean showDistance;
    private final SettingBoolean showHpText;
    private final SettingNumber textScale;

    // ── Range ────────────────────────────────────────────────────
    private final SettingNumber renderDistance;

    // ── Colors ───────────────────────────────────────────────────
    private final SettingMode colorMode;       // Static / HP / Rainbow / Group
    final SettingColor playerColor;
    final SettingColor mobColor;
    final SettingColor selfColor;
    final SettingColor nameColor;

    // ── Target filtering ─────────────────────────────────────────
    private final SettingBoolean onlyPlayers;
    private final SettingBoolean includeSelf;

    public Esp2D() {
        super("Esp2D", "2D bounding boxes around players and mobs", ModuleCategory.RENDER);

        boxMode     = new SettingMode("Box Mode", new String[]{"Full", "Corners"}, "Full");
        thickness   = new SettingNumber("Thickness", 0.5, 4.0, 0.5, 1.5);
        cornerSize  = new SettingNumber("Corner Size", 4.0, 40.0, 1.0, 12.0);
        rounded     = new SettingBoolean("Rounded Corners", true);

        showHealthBar = new SettingBoolean("Health Bar", true);
        barWidth      = new SettingNumber("Bar Width", 1.0, 6.0, 0.5, 2.0);

        showName     = new SettingBoolean("Show Name", true);
        showDistance = new SettingBoolean("Show Distance", true);
        showHpText   = new SettingBoolean("Show HP Text", false);
        textScale    = new SettingNumber("Text Scale", 0.5, 2.0, 0.1, 1.0);

        renderDistance = new SettingNumber("Render Distance", 8, 128, 1, 64);

        colorMode  = new SettingMode("Color Mode",
                new String[]{"Static", "HP-Based", "Rainbow", "Group"}, "Group");

        playerColor = new SettingColor("Player Color", new Color(0xFFFF3355));
        mobColor    = new SettingColor("Mob Color",    new Color(0xFFFFCC00));
        selfColor   = new SettingColor("Self Color",   new Color(0xFF4DD2FF));
        nameColor   = new SettingColor("Name Color",   new Color(0xFFFFFFFF));

        onlyPlayers = new SettingBoolean("Only Players", false);
        includeSelf = new SettingBoolean("Include Self", false);

        addSetting(boxMode);
        addSetting(thickness);
        addSetting(cornerSize);
        addSetting(rounded);
        addSetting(showHealthBar);
        addSetting(barWidth);
        addSetting(showName);
        addSetting(showDistance);
        addSetting(showHpText);
        addSetting(textScale);
        addSetting(renderDistance);
        addSetting(colorMode);
        addSetting(onlyPlayers);
        addSetting(includeSelf);

        addHiddenSetting(playerColor.getSetting());
        addHiddenSetting(mobColor.getSetting());
        addHiddenSetting(selfColor.getSetting());
        addHiddenSetting(nameColor.getSetting());
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
    private void onRender2D(Render2DEvent event) {
        if (!isEnabled()) return;
        if (mc.level == null || mc.player == null) return;
        Esp2DRenderer.render(event, this);
    }

    // ── Moduulin rajapinta renderöijälle ──────────────────────────

    public String getBoxMode()        { return boxMode.getMode(); }
    public double getThickness()      { return thickness.getValue(); }
    public double getCornerSize()     { return cornerSize.getValue(); }
    public boolean isRounded()        { return rounded.get(); }
    public boolean showHealthBar()    { return showHealthBar.get(); }
    public double getBarWidth()       { return barWidth.getValue(); }
    public boolean showName()         { return showName.get(); }
    public boolean showDistance()     { return showDistance.get(); }
    public boolean showHpText()       { return showHpText.get(); }
    public double getTextScale()      { return textScale.getValue(); }
    public double getRenderDistance() { return renderDistance.getValue(); }
    public boolean includeSelf()      { return includeSelf.get(); }
    public int getNameColor()         { return nameColor.getCurrentColor().getARGB(); }

    public boolean isTarget(LivingEntity le) {
        if (le == mc.player) return false; // käsitellään erikseen
        if (onlyPlayers.get() && !(le instanceof Player)) return false;
        return true;
    }

    public int resolveColor(LivingEntity le, float hpPct, long now) {
        String mode = colorMode.getMode();

        if ("Rainbow".equals(mode)) {
            try {
                RainbowPalette palette = ClickGuiConfigManager.getRainbowPalette();
                if (palette != null) {
                    float period = 5000f;
                    float t = (now % (long) period) / period;
                    t += le.getId() * 0.13f;
                    t %= 1.0f;
                    if (t < 0) t += 1.0f;
                    return palette.getColorInterpolatedLoop(t);
                }
            } catch (Throwable ignored) {}
            return playerColor.getCurrentColor().getARGB();
        }

        if ("HP-Based".equals(mode)) {
            int a = playerColor.getCurrentColor().getAlpha();
            int r, g, b;
            if (hpPct >= 0.66f) { r = 0x44; g = 0xDD; b = 0x44; }
            else if (hpPct >= 0.33f) { r = 0xFF; g = 0xCC; b = 0x00; }
            else { r = 0xFF; g = 0x33; b = 0x44; }
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        if ("Group".equals(mode)) {
            if (le == mc.player) return selfColor.getCurrentColor().getARGB();
            if (le instanceof Player) return playerColor.getCurrentColor().getARGB();
            return mobColor.getCurrentColor().getARGB();
        }

        return playerColor.getCurrentColor().getARGB();
    }

    // ── ColorConfigurable ─────────────────────────────────────────

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Player", playerColor),
                new NamedColor("Mob", mobColor),
                new NamedColor("Self", selfColor),
                new NamedColor("Name", nameColor)
        );
    }

    @Override
    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("esp2d_colors", "Esp2D Colors", sw, sh, content);
    }
}