package silversword.axiom.client.hud.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingNumber;

import java.util.ArrayList;
import java.util.List;

public final class HardwareHud extends BaseHudElement {
    // Asetukset
    private final SettingBoolean showCpuUsage;
    private final SettingBoolean showRamUsage;
    private final SettingBoolean ramFormat; // true = %, false = MB/GB

    // Ulkoasu
    private final SettingNumber textScale;
    private final SettingNumber backgroundPadding;
    private final SettingNumber backgroundRadius;
    private final SettingNumber outlineThickness;
    private final SettingColor backgroundColor;
    private final SettingColor borderColor;
    private final SettingColor textColor;
    private final SettingColor valueColor;

    // OSHI (CPU)
    private static SystemInfo systemInfo;
    private static HardwareAbstractionLayer hal;
    private static CentralProcessor cpu;
    private static long[] oldTicks;

    // Välimuisti CPU-kuormalle (500 ms)
    private long lastCpuUpdate = 0;
    private double cachedCpuUsage = 0.0;

    public HardwareHud() {
        super("Hardware Monitor", 10, 10);

        if (systemInfo == null) {
            systemInfo = new SystemInfo();
            hal = systemInfo.getHardware();
            cpu = hal.getProcessor();
            oldTicks = cpu.getSystemCpuLoadTicks();
        }

        showCpuUsage = new SettingBoolean("CPU Usage", true);
        showRamUsage = new SettingBoolean("RAM Usage", true);
        ramFormat = new SettingBoolean("RAM as %", true); // true = %, false = MB/GB

        textScale = new SettingNumber("Text Scale", 0.5, 3.0, 0.1, 1.0);
        backgroundPadding = new SettingNumber("Background Padding", 0, 20, 1, 4);
        backgroundRadius = new SettingNumber("Background Radius", 0, 10, 1, 4);
        outlineThickness = new SettingNumber("Outline Thickness", 0.5, 5.0, 0.5, 1.0);
        backgroundColor = new SettingColor("Background Color", new Color(0x80000000));
        borderColor = new SettingColor("Border Color", new Color(0xFFAAAAAA));
        textColor = new SettingColor("Text Color", new Color(0xFFFFFFFF));
        valueColor = new SettingColor("Value Color", new Color(0xFF00FF00));

        settings.addSetting(showCpuUsage);
        settings.addSetting(showRamUsage);
        settings.addSetting(ramFormat);
        settings.addSetting(textScale);
        settings.addSetting(backgroundPadding);
        settings.addSetting(backgroundRadius);
        settings.addSetting(outlineThickness);
        settings.addNamedColor(new NamedColor("Background", backgroundColor));
        settings.addNamedColor(new NamedColor("Border", borderColor));
        settings.addNamedColor(new NamedColor("Text", textColor));
        settings.addNamedColor(new NamedColor("Value", valueColor));
    }

    @Override
    public int width(Minecraft mc) {
        float scale = (float) textScale.getValue();
        int maxW = 0;
        for (String line : getDisplayLines()) {
            maxW = Math.max(maxW, (int) (TextRenderer.get().getWidth(line) * scale));
        }
        return maxW + (int) (backgroundPadding.getValue() * 2);
    }

    @Override
    public int height(Minecraft mc) {
        float scale = (float) textScale.getValue();
        int lines = getDisplayLines().size();
        int lineH = (int) (TextRenderer.get().getHeight() * scale);
        return lines * lineH + (int) (backgroundPadding.getValue() * 2);
    }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        Color bgCol = backgroundColor.getCurrentColor();
        Color borderCol = borderColor.getCurrentColor();
        Color txtCol = textColor.getCurrentColor();
        Color valCol = valueColor.getCurrentColor();

        float scale = (float) textScale.getValue();
        int padding = (int) backgroundPadding.getValue();
        int radius = (int) backgroundRadius.getValue();
        double thickness = outlineThickness.getValue();

        List<String> lines = getDisplayLines();
        int lineH = (int) (TextRenderer.get().getHeight() * scale);
        int textW = 0;
        for (String line : lines) {
            textW = Math.max(textW, (int) (TextRenderer.get().getWidth(line) * scale));
        }

        int bgW = textW + padding * 2;
        int bgH = lines.size() * lineH + padding * 2;
        int bgX = x;
        int bgY = y;

        if (radius > 0) {
            Renderer2D.COLOR.drawRoundedRect(bgX, bgY, bgW, bgH, radius, bgCol);
        } else {
            Renderer2D.COLOR.quad(bgX, bgY, bgW, bgH, bgCol);
        }

        if (borderCol.getAlpha() != 0 && thickness > 0) {
            Renderer2D.COLOR.drawRoundedRectOutline(bgX, bgY, bgW, bgH, radius, borderCol, thickness);
        }

        int yOffset = bgY + padding;
        for (String line : lines) {
            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                String textPart = line.substring(0, colonIdx + 1);
                String valuePart = line.substring(colonIdx + 1).trim();
                int textPartWidth = (int) (TextRenderer.get().getWidth(textPart) * scale);
                ctx.drawScaledText(textPart, bgX + padding, yOffset, txtCol.getPacked(), true, scale);
                ctx.drawScaledText(valuePart, bgX + padding + textPartWidth + (int)(2 * scale), yOffset, valCol.getPacked(), true, scale);
            } else {
                ctx.drawScaledText(line, bgX + padding, yOffset, txtCol.getPacked(), true, scale);
            }
            yOffset += lineH;
        }
    }

    private List<String> getDisplayLines() {
        List<String> lines = new ArrayList<>();

        // CPU Usage (OSHIn kautta)
        if (showCpuUsage.get()) {
            long now = System.currentTimeMillis();
            if (now - lastCpuUpdate > 500) {
                cachedCpuUsage = cpu.getSystemCpuLoadBetweenTicks(oldTicks) * 100;
                oldTicks = cpu.getSystemCpuLoadTicks();
                lastCpuUpdate = now;
            }
            lines.add("CPU: " + String.format("%.1f%%", cachedCpuUsage));
        }

        // Minecraftin allokoitu muisti
        if (showRamUsage.get()) {
            Runtime runtime = Runtime.getRuntime();
            long used = runtime.totalMemory() - runtime.freeMemory();
            long max = runtime.maxMemory();

            if (ramFormat.get()) {
                // Prosentteina maksimista
                double percent = used * 100.0 / max;
                lines.add("RAM: " + String.format("%.1f%% / %d MB", percent, max / 1_048_576));
            } else {
                // Megatavuina / gigatavuina
                double usedMB = used / 1_048_576.0;
                double maxMB = max / 1_048_576.0;
                if (maxMB > 1024) {
                    lines.add(String.format("RAM: %.2f/%.2f GB", usedMB / 1024, maxMB / 1024));
                } else {
                    lines.add(String.format("RAM: %.0f/%.0f MB", usedMB, maxMB));
                }
            }
        }

        return lines;
    }

    @Override
    public void renderEdit(HudContext ctx) {
        render(ctx, null);
    }
}