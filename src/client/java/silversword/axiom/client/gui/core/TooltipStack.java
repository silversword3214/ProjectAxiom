package silversword.axiom.client.gui.core;

import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.utils.render.TextUtils;

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

        // Aloitetaan uusi COLOR-renderöinti tooltipeille
        Renderer2D.COLOR.begin();
        // Aloitetaan uusi TEXT-renderöinti tooltipeille (scale 1.0, ei shadow)
        TextRenderer textRenderer = TextRenderer.get();
        textRenderer.begin(1, false, false);

        for (TooltipEntry entry : entries) {
            renderTooltip(ui, textRenderer, entry.text, entry.mouseX, entry.mouseY);
        }
        // Lopetetaan COLOR-puskuri ja renderöidään se
        Renderer2D.COLOR.render();

        textRenderer.end();

        entries.clear();
    }

    private static void renderTooltip(UiContext ui, TextRenderer textRenderer, String text, double mouseX, double mouseY) {
        String[] lines = text.split("\n");
        int lineCount = lines.length;

        // Lasketaan levein rivi TextUtils.CHAR_UNIT:lla
        int maxWidth = 0;
        for (String line : lines) {
            int lineWidth = line.length() * TextUtils.CHAR_UNIT;
            if (lineWidth > maxWidth) maxWidth = lineWidth;
        }

        // Laatikon koko: teksti + 2*padding molemmissa suunnissa
        int boxWidth = maxWidth + 2 * PADDING;
        int boxHeight = lineCount * TextUtils.FONT_HEIGHT + 2 * PADDING;

        // Sijoitetaan hiiren lähelle, pidetään ruudun sisällä
        double x = mouseX + 12;
        double y = mouseY - 12;
        int screenWidth = ui.mc.getWindow().getScaledWidth();
        int screenHeight = ui.mc.getWindow().getScaledHeight();

        if (x + boxWidth > screenWidth) {
            x = mouseX - boxWidth - 8;
        }
        if (y + boxHeight > screenHeight) {
            y = mouseY - boxHeight - 8;
        }
        if (x < 2) x = 2;
        if (y < 2) y = 2;

        // Piirretään tausta (lisätään suoraan COLOR-puskuriin)
        Renderer2D.COLOR.drawRoundedRect(x, y, boxWidth, boxHeight, RADIUS, new Color(ui.theme.panel));
        Renderer2D.COLOR.drawRoundedRectOutline(x, y, boxWidth, boxHeight, RADIUS, new Color(ui.theme.border), 1);

        // Piirretään teksti (lisätään suoraan TEXT-puskuriin)
        int textX = (int) x + PADDING;
        int textY = (int) y + PADDING;
        for (String line : lines) {
            textRenderer.render(line, textX, textY, new Color(ui.theme.text), false);
            textY += TextUtils.FONT_HEIGHT;
        }
    }
}