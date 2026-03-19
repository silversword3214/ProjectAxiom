package silversword.axiom.client.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.joml.Matrix4f;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;

import java.util.Arrays;
import java.util.List;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class ReachCheck extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    private final SettingMode targetMode = new SettingMode(
            "Target",
            new String[]{"Players", "Mobs", "Both"},
            "Players"
    );

    private final SettingNumber reachDistance = new SettingNumber(
            "Reach Distance",
            2.0, 6.0, 0.1,
            3.0
    );

    private final SettingBoolean requireCooldown = new SettingBoolean(
            "Require Full Cooldown",
            true
    );

    private final SettingNumber circleRadius = new SettingNumber(
            "Circle Radius",
            5, 30, 1,
            12
    );

    private final SettingNumber circleSegments = new SettingNumber(
            "Circle Segments",
            8, 64, 1,
            32
    );

    final SettingColor activeColor;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public ReachCheck() {
        super("Reach Check", "Draws a circle around crosshair when target is in reach", ModuleCategory.COMBAT);

        activeColor = new SettingColor("Active Color", new Color(0, 255, 0, 255));

        addSetting(targetMode);
        addSetting(reachDistance);
        addSetting(requireCooldown);
        addSetting(circleRadius);
        addSetting(circleSegments);
        addHiddenSetting(activeColor.getSetting());
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @AxiomEvent
    public void onRender2D(Render2DEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.world == null) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int centerX = sw / 2;
        int centerY = sh / 2;

        // Save the current projection (likely the 3D perspective matrix)
        Matrix4f savedProj = new Matrix4f(RenderUtils.projection);

        // Set up 2D orthographic projection for the circle
        RenderUtils.setup2DProjection(sw, sh);

        boolean inReach = false;

        HitResult hit = mc.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (isValidTarget(target)) {
                double dist = mc.player.distanceTo(target);
                if (dist <= reachDistance.getValue()) {
                    inReach = true;
                }
            }
        }

        if (inReach && requireCooldown.get()) {
            float cooldown = mc.player.getAttackCooldownProgress(0.5f);
            if (cooldown < 0.99f) {
                inReach = false;
            }
        }

        if (inReach) {
            double radius = circleRadius.getValue();
            int segments = (int) circleSegments.getValue();
            Color color = activeColor.getCurrentColor();

            Renderer2D.COLOR.begin();

            double angleStep = 2 * Math.PI / segments;
            for (int i = 0; i < segments; i++) {
                double angle1 = i * angleStep;
                double angle2 = (i + 1) * angleStep;
                double x1 = centerX + Math.cos(angle1) * radius;
                double y1 = centerY + Math.sin(angle1) * radius;
                double x2 = centerX + Math.cos(angle2) * radius;
                double y2 = centerY + Math.sin(angle2) * radius;
                Renderer2D.COLOR.line(x1, y1, x2, y2, color);
            }

            Renderer2D.COLOR.end();
            Renderer2D.COLOR.render();
        }

        // Restore the original projection for other modules (e.g., NameTags)
        RenderUtils.projection.set(savedProj);
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == mc.player) return false;
        if (!entity.isAlive()) return false;
        if (!(entity instanceof LivingEntity)) return false;

        String mode = targetMode.getMode();
        if (mode.equals("Players")) return entity instanceof PlayerEntity;
        if (mode.equals("Mobs")) return !(entity instanceof PlayerEntity);
        return true;
    }

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(new NamedColor("Active Color", activeColor));
    }

    @Override
    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("reachcheck_colors", "ReachCheck Colors", sw, sh, content);
    }

    @Override
    protected void onTick() {

    }
}