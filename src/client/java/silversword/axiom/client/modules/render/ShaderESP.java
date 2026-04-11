package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.gui.components.ColorCustomizerView;
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
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.setting.SettingSlider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ShaderESP extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private final SettingNumber thickness;
    private final SettingBoolean fill;
    private final SettingSlider renderDistance;
    private final SettingBoolean drawPlayers;
    private final SettingBoolean drawHostile;
    private final SettingBoolean drawPassive;
    private final SettingBoolean drawNeutral;
    private final SettingBoolean drawWater;
    private final SettingBoolean drawBoss;

    private final SettingColor outlineColor;
    private final SettingColor fillColor;

    public ShaderESP() {
        super("ShaderESP", "Mask + post-process entity silhouettes rendered through walls", ModuleCategory.RENDER);

        thickness = new SettingNumber("Thickness", 1.0, 8.0, 0.25, 2.0);
        fill = new SettingBoolean("Fill", true);
        renderDistance = new SettingSlider("Render Distance", new double[]{16, 32, 64, 96, 128, 256, 512}, 96);
        drawPlayers = new SettingBoolean("Draw Players", true);
        drawHostile = new SettingBoolean("Draw Hostile", true);
        drawPassive = new SettingBoolean("Draw Passive", true);
        drawNeutral = new SettingBoolean("Draw Neutral", true);
        drawWater = new SettingBoolean("Draw Water", true);
        drawBoss = new SettingBoolean("Draw Boss", true);

        outlineColor = new SettingColor("Outline", new Color(0, 255, 200, 255));
        fillColor = new SettingColor("Fill Color", new Color(0, 255, 200, 70));

        addSetting(thickness);
        addSetting(fill);
        addSetting(renderDistance);
        addSetting(drawPlayers);
        addSetting(drawHostile);
        addSetting(drawPassive);
        addSetting(drawNeutral);
        addSetting(drawWater);
        addSetting(drawBoss);

        addHiddenSetting(outlineColor.getSetting());
        addHiddenSetting(fillColor.getSetting());
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
    }

    public List<Entity> collectTargets(Vec3 cameraPos) {
        List<Entity> targets = new ArrayList<>();
        if (mc.level == null || mc.player == null) return targets;

        double maxDistanceSq = renderDistance.getValue() * renderDistance.getValue();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!shouldRender(entity, cameraPos, maxDistanceSq)) continue;
            targets.add(entity);
        }

        return targets;
    }

    private boolean shouldRender(Entity entity, Vec3 cameraPos, double maxDistanceSq) {
        if (entity == null || entity == mc.player || !entity.isAlive()) return false;
        if (!(entity instanceof LivingEntity)) return false;
        if (entity.distanceToSqr(cameraPos) > maxDistanceSq) return false;

        TargetGroup group = TargetGroup.getGroup(entity);
        return switch (group) {
            case PLAYER -> drawPlayers.get();
            case HOSTILE -> drawHostile.get();
            case PASSIVE -> drawPassive.get();
            case NEUTRAL -> drawNeutral.get();
            case WATER -> drawWater.get();
            case BOSS -> drawBoss.get();
        };
    }

    public SettingNumber getThickness() {
        return thickness;
    }

    public boolean isFillEnabled() {
        return fill.get();
    }

    public SettingColor getOutlineColor() {
        return outlineColor;
    }

    public SettingColor getFillColor() {
        return fillColor;
    }

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Outline", outlineColor),
                new NamedColor("Fill", fillColor)
        );
    }

    @Override
    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        factory.openCustomWindow("shaderesp_color", "ShaderESP Color Customizer", sw, sh, new ColorCustomizerView(this));
    }
}
