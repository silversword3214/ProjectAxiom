package silversword.axiom.client.gui.core;

import java.util.ArrayList;
import java.util.List;

public final class TooltipStack {
    private static final List<TooltipEntry> entries = new ArrayList<>();
    private static final int PADDING = 4;
    private static final int RADIUS = 4;

    private static class TooltipEntry {
        final String text;
        final double mouseX;
        final double mouseY;
        TooltipEntry(String text, double mouseX, double mouseY) {
            this.text = text;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }
    }

    public static void push(String text, double mouseX, double mouseY) {
        if (text == null || text.isEmpty()) return;
        entries.add(new TooltipEntry(text, mouseX, mouseY));
    }

    public static void renderAll(UiContext ui) {
        if (entries.isEmpty()) return;

        for (TooltipEntry entry : entries) {
            renderTooltip(ui, entry.text, entry.mouseX, entry.mouseY);
        }
        entries.clear();
    }

    private static void renderTooltip(UiContext ui, String text, double mouseX, double mouseY) {
        String[] lines = text.split("\n");
        int lineCount = lines.length;

        int maxWidth = 0;
        for (String line : lines) {
            int lineWidth = ui.textWidth(line);
            if (lineWidth > maxWidth) maxWidth = lineWidth;
        }

        int boxWidth = maxWidth + 2 * PADDING;
        int boxHeight = lineCount * ui.fontHeight() + 2 * PADDING;

        double x = mouseX + 12;
        double y = mouseY - 12;
        int screenWidth = ui.mc.getWindow().getGuiScaledWidth();
        int screenHeight = ui.mc.getWindow().getGuiScaledHeight();

        if (x + boxWidth > screenWidth) {
            x = mouseX - boxWidth - 8;
        }
        if (y + boxHeight > screenHeight) {
            y = mouseY - boxHeight - 8;
        }
        if (x < 2) x = 2;
        if (y < 2) y = 2;

        // Tausta
        ui.fillRounded((int) x, (int) y, boxWidth, boxHeight, ui.theme.panel, RADIUS);
        ui.drawRoundedOutline(new Rect((int) x, (int) y, boxWidth, boxHeight), ui.theme.border, RADIUS, 1.0);

        // Teksti
        int textX = (int) x + PADDING;
        int textY = (int) y + PADDING;
        for (String line : lines) {
            ui.text(line, textX, textY, ui.theme.text);
            textY += ui.fontHeight();
        }
    }
}