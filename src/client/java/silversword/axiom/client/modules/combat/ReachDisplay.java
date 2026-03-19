package silversword.axiom.client.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.hud.components.ReachDisplayHudComponent;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;

import java.util.Arrays;
import java.util.List;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class ReachDisplay extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    private final ReachDisplayHudComponent element = new ReachDisplayHudComponent();
    private boolean registered = false;

    // Asetukset
    private final SettingMode targetMode = new SettingMode(
            "Target",
            new String[]{"Players", "Mobs", "Both"},
            "Players"
    );
    private final SettingNumber maxRange = new SettingNumber("Max Range", 1, 100, 1, 64);
    private final SettingBoolean showPing = new SettingBoolean("Show Ping", false);

    // Värit
    final SettingColor backgroundColor;
    final SettingColor borderColor;
    final SettingColor textColor;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public ReachDisplay() {
        super("Reach Display", "Shows exact distance to your target", ModuleCategory.COMBAT);

        backgroundColor = new SettingColor("Background Color", new Color(0x90000000));
        borderColor = new SettingColor("Border Color", new Color(0xFF00AAFF));
        textColor = new SettingColor("Text Color", new Color(0xFFFFFFFF));

        addSetting(targetMode);
        addSetting(maxRange);
        addSetting(showPing);
        addHiddenSetting(backgroundColor.getSetting());
        addHiddenSetting(borderColor.getSetting());
        addHiddenSetting(textColor.getSetting());
        addHiddenSetting(toggleKey);

        if (!registered) {
            HudManager.get().register(element);
            registered = true;
        }
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        if (!registered) {
            HudManager.get().register(element);
            registered = true;
        }
        element.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        element.setEnabled(false);
        element.clearData();
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Päivitetään värit elementille
        element.setBackgroundColor(backgroundColor.getCurrentColor().getPacked());
        element.setBorderColor(borderColor.getCurrentColor().getPacked());
        element.setTextColor(textColor.getCurrentColor().getPacked());

        LivingEntity target = findClosestTarget();
        if (target == null) {
            element.clearData();
            return;
        }

        double distance = mc.player.distanceTo(target);
        int ping = -1;
        if (showPing.get() && target instanceof PlayerEntity) {
            ping = getPing((PlayerEntity) target);
        }

        element.setData(target, distance, ping);
    }

    private LivingEntity findClosestTarget() {
        double maxDist = maxRange.getValue();
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entity.isAlive()) continue;
            if (!isValidTarget(entity)) continue;

            double dist = mc.player.distanceTo(entity);
            if (dist <= maxDist && dist < closestDist) {
                closestDist = dist;
                closest = (LivingEntity) entity;
            }
        }
        return closest;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        String mode = targetMode.getMode();
        if (mode.equals("Players")) return entity instanceof PlayerEntity;
        if (mode.equals("Mobs")) return !(entity instanceof PlayerEntity);
        return true;
    }

    private int getPing(PlayerEntity player) {
        if (mc.getNetworkHandler() == null) return -1;
        var entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return entry != null ? entry.getLatency() : -1;
    }

    // ----- ColorConfigurable toteutus -----
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
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("reachdisplay_colors", "ReachDisplay Colors", sw, sh, content);
    }
}