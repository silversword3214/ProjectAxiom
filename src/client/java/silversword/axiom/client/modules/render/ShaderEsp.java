package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.modules.moduleutils.TargetGroup;
import silversword.axiom.client.render.rendersystem.axiomrenderer.shaderesp.ShaderEspRenderer;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingSlider;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ShaderEsp extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final Set<TargetGroup> HANDLED = EnumSet.of(
            TargetGroup.PLAYER, TargetGroup.HOSTILE, TargetGroup.PASSIVE,
            TargetGroup.NEUTRAL, TargetGroup.WATER, TargetGroup.BOSS);

    // ─── Keybind ───────────────────────────────────────────────────────────
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // ─── Render modes ──────────────────────────────────────────────────────
    private final SettingBoolean enableChams;
    private final SettingSlider  chamsTint;
    private final SettingSlider  chamsOpacity;

    private final SettingBoolean enableFill;
    private final SettingSlider  fillOpacity;

    private final SettingBoolean enableOutline;
    private final SettingSlider  outlineThickness;

    // ─── Quality ───────────────────────────────────────────────────────────
    private final SettingSlider  downscale;
    private final SettingSlider  renderDistance;

    // ─── Per-group toggles and colors ──────────────────────────────────────
    private final Map<TargetGroup, SettingBoolean> drawGroup    = new EnumMap<>(TargetGroup.class);
    private final Map<TargetGroup, SettingColor>   fillColor    = new EnumMap<>(TargetGroup.class);
    private final Map<TargetGroup, SettingColor>   outlineColor = new EnumMap<>(TargetGroup.class);

    public ShaderEsp() {
        super("ShaderEsp", "Chams, Fill and Outline for entities", ModuleCategory.RENDER);

        // ─── Chams ─────────────────────────────────────────────────────────
        enableChams = new SettingBoolean("Enable Chams", false);
        chamsTint = new SettingSlider("Chams Tint",
                new double[]{
                        0xFFFFFFFFL, 0xCCFF0000L, 0xCC00FF00L, 0xCC0000FFL,
                        0xCCFF00FFL, 0xCC00FFFFL, 0xCCFFFF00L, 0x80000000L
                }, 0);
        chamsOpacity = new SettingSlider("Chams Opacity",
                new double[]{10, 25, 50, 75, 100}, 100);

        // ─── Fill ──────────────────────────────────────────────────────────
        enableFill = new SettingBoolean("Enable Fill", false);
        fillOpacity = new SettingSlider("Fill Opacity",
                new double[]{10, 25, 50, 75, 100}, 60);

        // ─── Outline ───────────────────────────────────────────────────────
        enableOutline = new SettingBoolean("Enable Outline", true);
        outlineThickness = new SettingSlider("Outline Thickness",
                new double[]{1, 2, 3, 4, 5, 6, 7, 8}, 2);

        // ─── Quality ───────────────────────────────────────────────────────
        downscale = new SettingSlider("Mask Resolution",
                new double[]{1, 2, 3, 4}, 2);
        renderDistance = new SettingSlider("Render Distance",
                new double[]{16, 32, 64, 96, 128, 256, 512}, 128);

        // ─── Per-group toggles + colors ────────────────────────────────────
        for (TargetGroup g : HANDLED) {
            drawGroup.put(g, new SettingBoolean("Show " + niceName(g), defaultEnabled(g)));
            fillColor.put(g, new SettingColor(niceName(g) + " Fill", defaultFill(g)));
            outlineColor.put(g, new SettingColor(niceName(g) + " Outline", defaultOutline(g)));
        }

        // ─── Parent-suhteet (subsettings) ──────────────────────────────────
        chamsTint.setParent(enableChams);
        chamsOpacity.setParent(enableChams);

        fillOpacity.setParent(enableFill);

        outlineThickness.setParent(enableOutline);

        // ─── UI ordering ───────────────────────────────────────────────────

        // Chams
        addSetting(enableChams);
        addSetting(chamsTint);
        addSetting(chamsOpacity);

        // Fill
        addSetting(enableFill);
        addSetting(fillOpacity);

        // Outline
        addSetting(enableOutline);
        addSetting(outlineThickness);

        // Quality
        addSetting(downscale);
        addSetting(renderDistance);

        // Per-group toggles
        for (TargetGroup g : HANDLED) {
            addSetting(drawGroup.get(g));
        }

        // Hidden colors (accessed via Color Editor)
        for (TargetGroup g : HANDLED) {
            addHiddenSetting(fillColor.get(g).getSetting());
            addHiddenSetting(outlineColor.get(g).getSetting());
        }

        addHiddenSetting(toggleKey);
    }

    // ─── Keybind ───────────────────────────────────────────────────────────

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    // ─── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    protected void onEnable() {
        try { ShaderEspRenderer.init(); } catch (Throwable ignored) {}
        ShaderEspRenderer.setEnabled(true);
        applySettings();
    }

    @Override
    protected void onDisable() {
        ShaderEspRenderer.setEnabled(false);
    }

    @Override
    protected void onTick() { applySettings(); }

    // ─── Settings application ──────────────────────────────────────────────

    private void applySettings() {
        ShaderEspRenderer.setDownscale((int) downscale.getValue());

        ShaderEspRenderer.setRenderChams(enableChams.get());
        ShaderEspRenderer.setRenderFill(enableFill.get());
        ShaderEspRenderer.setRenderOutline(enableOutline.get());

        // Chams tint moduloi alphaa opacityn mukaan
        int tint = (int) chamsTint.getValue();
        float chamsAlphaMul = (float)(chamsOpacity.getValue() / 100.0);
        tint = applyAlphaMul(tint, chamsAlphaMul);
        ShaderEspRenderer.setChamsTint(tint);

        ShaderEspRenderer.setOutlineThickness((float) outlineThickness.getValue());

        // Per-group fill & outline colors, fill alpha moduloidaan opacityllä
        float fillAlphaMul = (float)(fillOpacity.getValue() / 100.0);
        for (TargetGroup g : HANDLED) {
            int fillArgb = fillColor.get(g).getCurrentColor().getARGB();
            fillArgb = applyAlphaMul(fillArgb, fillAlphaMul);
            ShaderEspRenderer.setFillColor(g, fillArgb);

            int outlineArgb = outlineColor.get(g).getCurrentColor().getARGB();
            ShaderEspRenderer.setOutlineColor(g, outlineArgb);
        }

        final double maxSq = renderDistance.getValue() * renderDistance.getValue();
        ShaderEspRenderer.setFilter(e -> {
            if (e == null) return false;
            TargetGroup g = TargetGroup.getGroup(e);
            if (!HANDLED.contains(g)) return false;
            SettingBoolean s = drawGroup.get(g);
            if (s == null || !s.get()) return false;
            Vec3 cam = mc.gameRenderer.mainCamera().position();
            return e.position().distanceToSqr(cam) <= maxSq;
        });
    }

    /** Kertoo ARGB-värin alpha-kanavan annetulla kertoimella (0..1). */
    private static int applyAlphaMul(int argb, float mul) {
        int a = (argb >>> 24) & 0xFF;
        int newA = Math.round(a * mul);
        newA = Math.max(0, Math.min(255, newA));
        return (newA << 24) | (argb & 0x00FFFFFF);
    }

    // ─── ColorConfigurable ─────────────────────────────────────────────────

    @Override
    public List<NamedColor> getColors() {
        List<NamedColor> list = new ArrayList<>();
        for (TargetGroup g : HANDLED) {
            String n = niceName(g);
            list.add(new NamedColor(n + " Fill",    fillColor.get(g)));
            list.add(new NamedColor(n + " Outline", outlineColor.get(g)));
        }
        return list;
    }

    @Override
    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        factory.openCustomWindow("shaderesp_color", "ShaderEsp Color Customizer",
                sw, sh, new ColorCustomizerView(this));
    }

    // ─── Names and defaults ────────────────────────────────────────────────

    private static String niceName(TargetGroup g) {
        return switch (g) {
            case PLAYER  -> "Players";
            case HOSTILE -> "Hostiles";
            case PASSIVE -> "Passives";
            case NEUTRAL -> "Neutrals";
            case WATER   -> "Water Mobs";
            case BOSS    -> "Bosses";
        };
    }

    private static boolean defaultEnabled(TargetGroup g) {
        return switch (g) {
            case PLAYER, HOSTILE, BOSS -> true;
            default -> false;
        };
    }

    private static Color defaultFill(TargetGroup g) {
        return switch (g) {
            case PLAYER  -> new Color(  0, 255, 200, 180);
            case HOSTILE -> new Color(255,  50,  50, 180);
            case PASSIVE -> new Color( 50, 255,  50, 180);
            case NEUTRAL -> new Color(255, 255,   0, 180);
            case WATER   -> new Color( 50, 150, 255, 180);
            case BOSS    -> new Color(200,   0, 200, 180);
        };
    }

    private static Color defaultOutline(TargetGroup g) {
        return switch (g) {
            case PLAYER  -> new Color(  0, 255, 200, 255);
            case HOSTILE -> new Color(255,  50,  50, 255);
            case PASSIVE -> new Color( 50, 255,  50, 255);
            case NEUTRAL -> new Color(255, 255,   0, 255);
            case WATER   -> new Color( 50, 150, 255, 255);
            case BOSS    -> new Color(200,   0, 200, 255);
        };
    }
}