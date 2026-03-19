package silversword.axiom.client.modules.render;

import net.minecraft.entity.Entity;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.modules.moduleutils.TargetGroup;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;

import java.util.Arrays;
import java.util.List;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class ShaderESP extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    // Piirtotavat shaderille (outline.frag odottaa indeksiä: 0=Outline, 1=Fill, 2=Both)
    public enum ShapeMode {
        Outline, Fill, Both
    }

    // Moduulin oma tila – Shader käyttää meidän shaderiamme, Glow käyttää vaniljan outlinea
    public enum Mode {
        Shader, Glow
    }

    // Värit
    final SettingColor playerColor;
    final SettingColor hostileColor;
    final SettingColor passiveColor;
    final SettingColor neutralColor;
    final SettingColor waterColor;
    final SettingColor bossColor;

    // Suodatus
    private final SettingBoolean drawPlayers;
    private final SettingBoolean drawHostile;
    private final SettingBoolean drawPassive;
    private final SettingBoolean drawNeutral;
    private final SettingBoolean drawWater;
    private final SettingBoolean drawBoss;

    private final SettingSlider renderDistance;

    // Shader-spesifiset asetukset
    public final SettingMode mode; // Shader / Glow
    public final SettingSlider outlineWidth;
    public final SettingSlider fillOpacity;
    public final SettingMode shapeMode; // Outline, Fill, Both
    public final SettingSlider glowMultiplier;

    public ShaderESP() {
        super("ShaderESP", "Highlight entities", ModuleCategory.RENDER);

        // Värit (oletusarvot voivat olla samat kuin BoxESP:ssä)
        playerColor   = new SettingColor("Player Color",   new Color(0, 255, 200, 180));
        hostileColor  = new SettingColor("Hostile Color",  new Color(255, 50, 50, 180));
        passiveColor  = new SettingColor("Passive Color",  new Color(50, 255, 50, 180));
        neutralColor  = new SettingColor("Neutral Color",  new Color(255, 255, 0, 180));
        waterColor    = new SettingColor("Water Color",    new Color(50, 150, 255, 180));
        bossColor     = new SettingColor("Boss Color",     new Color(200, 0, 200, 180));

        // Suodatus
        drawPlayers   = new SettingBoolean("Draw Players", true);
        drawHostile   = new SettingBoolean("Draw Hostile", true);
        drawPassive   = new SettingBoolean("Draw Passive", true);
        drawNeutral   = new SettingBoolean("Draw Neutral", true);
        drawWater     = new SettingBoolean("Draw Water", true);
        drawBoss      = new SettingBoolean("Draw Boss", true);

        // Etäisyys
        renderDistance = new SettingSlider("Render Distance", new double[]{16, 32, 64, 96, 128, 256, 512}, 96);

        // Shader-tila
        String[] modeNames = new String[Mode.values().length];
        for (int i = 0; i < Mode.values().length; i++) {
            modeNames[i] = Mode.values()[i].name();
        }
        mode = new SettingMode("Mode", modeNames, Mode.Shader.name());

        // Viivan paksuus
        outlineWidth = new SettingSlider("Outline Width", new double[]{1,2,3,4,5,6,7,8,9,10}, 2);

        // Täytön läpinäkyvyys
        fillOpacity = new SettingSlider("Fill Opacity", new double[]{0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0}, 0.3);

        // Muoto (vastaa boxModea)
        String[] shapeNames = new String[ShapeMode.values().length];
        for (int i = 0; i < ShapeMode.values().length; i++) {
            shapeNames[i] = ShapeMode.values()[i].name();
        }
        shapeMode = new SettingMode("Shape Mode", shapeNames, ShapeMode.Both.name());

        // Hehkutuskerroin
        glowMultiplier = new SettingSlider("Glow Multiplier", new double[]{0.0,0.5,1.0,1.5,2.0,2.5,3.0}, 1.0);

        // Piilotetut väriasetukset
        addHiddenSetting(playerColor.getSetting());
        addHiddenSetting(hostileColor.getSetting());
        addHiddenSetting(passiveColor.getSetting());
        addHiddenSetting(neutralColor.getSetting());
        addHiddenSetting(waterColor.getSetting());
        addHiddenSetting(bossColor.getSetting());

        addHiddenSetting(toggleKey);


        // Näkyvät asetukset
        addSetting(mode);
        addSetting(shapeMode);
        addSetting(outlineWidth);
        addSetting(fillOpacity);
        addSetting(glowMultiplier);
        addSetting(renderDistance);
        addSetting(drawPlayers);
        addSetting(drawHostile);
        addSetting(drawPassive);
        addSetting(drawNeutral);
        addSetting(drawWater);
        addSetting(drawBoss);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        // Ei tarvita
    }

    // Apumetodit shadereita varten

    public boolean isShader() {
        return isEnabled() && mode.getMode().equals(Mode.Shader.name());

    }

    public boolean isGlow() {
        return isEnabled() && mode.getMode().equals(Mode.Glow.name());
    }

    public boolean forceRender() {
        return isEnabled() && (isShader() || isGlow());
    }

    public boolean shouldSkip(Entity entity) {
        if (!isEnabled()) return true;
        if (entity == mc.player) return true; // Ohitetaan itse
        TargetGroup group = TargetGroup.getGroup(entity);
        return !shouldDrawGroup(group);
    }

    private boolean shouldDrawGroup(TargetGroup group) {
        return switch (group) {
            case PLAYER  -> drawPlayers.get();
            case HOSTILE -> drawHostile.get();
            case PASSIVE -> drawPassive.get();
            case NEUTRAL -> drawNeutral.get();
            case WATER   -> drawWater.get();
            case BOSS    -> drawBoss.get();
            default      -> true; // Misc yms.
        };
    }

    public Color getColor(Entity entity) {
        if (!isEnabled()) return null;
        TargetGroup group = TargetGroup.getGroup(entity);
        return switch (group) {
            case PLAYER  -> playerColor;
            case HOSTILE -> hostileColor;
            case PASSIVE -> passiveColor;
            case NEUTRAL -> neutralColor;
            case WATER   -> waterColor;
            case BOSS    -> bossColor;
            default      -> new Color(255, 255, 255, 180);
        };
    }

    public int getShapeModeIndex() {
        return (int) shapeMode.getValue(); // 0,1,2
    }

    public int getOutlineWidth() {
        return (int) outlineWidth.getValue();
    }

    public float getFillOpacity() {
        return (float) fillOpacity.getValue();
    }

    public float getGlowMultiplier() {
        return (float) glowMultiplier.getValue();
    }

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Player", playerColor),
                new NamedColor("Hostile", hostileColor),
                new NamedColor("Passive", passiveColor),
                new NamedColor("Neutral", neutralColor),
                new NamedColor("Water", waterColor),
                new NamedColor("Boss", bossColor)
        );
    }

    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("shaderesp_color", "ShaderESP Color Customizer", sw, sh, content);
    }
}