package silversword.axiom.client.modules.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Breadcrumbs extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Väri
    final SettingColor trailColor;

    // Asetukset
    private final SettingNumber maxPoints;    // maksimimäärä pisteitä
    private final SettingNumber interval;     // tallennusväli (sekunteina, esim. 0.05 = 20 kertaa sekunnissa)
    private final SettingNumber fadeTime;     // kuinka monta sekuntia pisteet säilyvät (0 = ei koskaan häviä)
    private final SettingNumber lineWidth;    // viivan paksuus (vain jos renderöinti tukee)
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // Data
    private final List<BreadcrumbPoint> points = new ArrayList<>();
    private long lastRecordTime = 0;

    public Breadcrumbs() {
        super("Breadcrumbs", "Draws a trail where you have walked", ModuleCategory.RENDER);

        trailColor = new SettingColor("Trail Color", new Color(0, 255, 255, 200));

        maxPoints = new SettingNumber("Max Points", 10, 1000, 1, 200);
        interval = new SettingNumber("Interval (sec)", 0.01, 1.0, 0.01, 0.05);
        fadeTime = new SettingNumber("Fade Time (sec)", 0, 60, 1, 10);
        lineWidth = new SettingNumber("Line Width", 1, 5, 1, 2);

        addHiddenSetting(trailColor.getSetting());
        addHiddenSetting(toggleKey);
        addSetting(maxPoints);
        addSetting(interval);
        addSetting(fadeTime);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        if (!isEnabled() || mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        long intervalMs = (long) (interval.getValue() * 1000);

        if (now - lastRecordTime > intervalMs) {
            lastRecordTime = now;

            // Tallennetaan pelaajan nykyinen sijainti
            Vec3d pos = mc.player.getEntityPos();
            points.add(new BreadcrumbPoint(pos.x, pos.y, pos.z, now));

            // Rajoitetaan maksimimäärä
            int max = (int) maxPoints.getValue();
            while (points.size() > max) {
                points.remove(0);
            }
        }

        // Poistetaan vanhat pisteet, jos fadeTime > 0
        if (fadeTime.getValue() > 0) {
            long expireTime = now - (long) (fadeTime.getValue() * 1000);
            Iterator<BreadcrumbPoint> iter = points.iterator();
            while (iter.hasNext()) {
                if (iter.next().timestamp < expireTime) {
                    iter.remove();
                } else {
                    break; // oletetaan että pisteet ovat aikajärjestyksessä
                }
            }
        }
    }

    @AxiomEvent
    private void onRender(Render3DEvent event) {
        if (!isEnabled() || points.size() < 2) return;

        Color color = trailColor.getCurrentColor();
        float width = (float) lineWidth.getValue(); // voidaan käyttää myöhemmin

        // Piirretään viivat peräkkäisten pisteiden välille
        for (int i = 0; i < points.size() - 1; i++) {
            BreadcrumbPoint p1 = points.get(i);
            BreadcrumbPoint p2 = points.get(i + 1);
            event.render.drawLine(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, color);
        }
    }

    @Override
    public void onDisable() {
        points.clear(); // tyhjennetään jälki kun moduuli sammutetaan
    }

    @Override
    public List<NamedColor> getColors() {
        return List.of(new NamedColor("Trail", trailColor));
    }

    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("breadcrumbs_color", "Breadcrumbs Color Customizer", sw, sh, content);
    }

    // Apuluokka pisteelle (jotta voidaan tallentaa aikaleima)
    private static class BreadcrumbPoint {
        final double x, y, z;
        final long timestamp;

        BreadcrumbPoint(double x, double y, double z, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = timestamp;
        }
    }
}