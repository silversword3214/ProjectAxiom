package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public final class Slider implements UiComponent {
    private Rect bounds = new Rect(0, 0, 10, 10);
    private final String label;
    private final double min, max, step;
    private final DoubleSupplier getter;
    private final DoubleConsumer setter;
    private final java.util.function.Function<Double, String> displayFormatter;
    private boolean dragging = false;

    private Rect dragSliderArea = null;

    private static final int PADDING = 8;
    private static final int EXTRA_PADDING = 8;

    public Slider(String label, double min, double max, double step,
                  DoubleSupplier getter, DoubleConsumer setter,
                  java.util.function.Function<Double, String> displayFormatter) {
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step <= 0 ? 0 : step;
        this.getter = getter;
        this.setter = setter;
        this.displayFormatter = displayFormatter;
    }

    public Slider(String label, double min, double max, double step,
                  DoubleSupplier getter, DoubleConsumer setter) {
        this(label, min, max, step, getter, setter, null);
    }

    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect bounds) { this.bounds = bounds; }
    @Override public int getPreferredHeight() { return 26; }

    private double clamp(double v) { return v < min ? min : v > max ? max : v; }
    private double applyStep(double v) {
        if (step <= 0) return v;
        double snapped = Math.round((v - min) / step) * step + min;
        return clamp(snapped);
    }

    private Rect calculateSliderArea(UiContext ui, double valueForWidth) {
        int labelWidth = ui.textWidth(label);
        String valStr = formatValue(valueForWidth);
        int valWidth = ui.textWidth(valStr);

        int labelEnd = bounds.x + ui.theme.innerPadding + labelWidth;
        int valueStart = bounds.right() - ui.theme.innerPadding - valWidth;
        int sliderX = labelEnd + PADDING;
        int sliderWidth = valueStart - sliderX - EXTRA_PADDING;

        if (sliderWidth < 10) {
            sliderWidth = 10;
            sliderX = (bounds.x + bounds.w / 2) - sliderWidth / 2;
        }

        int trackH = 6;
        int trackY = bounds.y + bounds.h / 2 - trackH / 2;
        return new Rect(sliderX, trackY, sliderWidth, trackH);
    }

    private Rect getSliderArea(UiContext ui) {
        double raw = clamp(getter.getAsDouble());
        double value = applyStep(raw);
        return calculateSliderArea(ui, value);
    }

    private double valueFromMouse(double mouseX, Rect sliderArea) {
        double t = (mouseX - sliderArea.x) / (double) sliderArea.w;
        t = Math.max(0, Math.min(1, t));
        return min + (max - min) * t;
    }

    private String formatValue(double v) {
        if (displayFormatter != null) return displayFormatter.apply(v);
        if (Math.abs(v - Math.round(v)) < 1e-9) return String.valueOf((int) Math.round(v));
        return String.format(Locale.ROOT, "%.2f", v);
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        // 26.3: GLFW pois â€“ MouseHandler:in kautta
        if (dragging && !ui.mc.mouseHandler.isLeftPressed()) {
            dragging = false;
            dragSliderArea = null;
        }

        boolean hover = bounds.contains(mouseX, mouseY);

        // Label
        int labelColor = hover ? ui.theme.accent : ui.theme.text;
        int textY = bounds.y + bounds.h / 2 - ui.fontHeight() / 2 + 4;
        ui.text(label, bounds.x + ui.theme.innerPadding, textY, labelColor);

        // Value text
        double raw = clamp(getter.getAsDouble());
        double value = applyStep(raw);
        String valStr = formatValue(value);

        int valWidth = ui.textWidth(valStr);
        int valX = bounds.right() - ui.theme.innerPadding - valWidth;
        ui.text(valStr, valX, textY, ui.theme.textDim);

        // KÃ¤ytetÃ¤Ã¤n raahauksen aikaista aluetta jos sellainen on
        Rect sliderArea = (dragging && dragSliderArea != null) ? dragSliderArea : getSliderArea(ui);

        int trackY = sliderArea.y;
        ui.fill(sliderArea.x, trackY, sliderArea.w, 6, ui.theme.sliderTrack);
        double range = max - min;
        double pct = range <= 0 ? 0 : (value - min) / range;
        int fillW = (int) Math.round(sliderArea.w * pct);
        if (fillW > 0) ui.fill(sliderArea.x, trackY, fillW, 6, ui.theme.sliderFill);

        // Knob
        int knobSize = 12;
        int knobX = sliderArea.x + fillW - knobSize / 2;
        int knobY = trackY - (knobSize - sliderArea.h) / 2;
        int knobCenterX = knobX + knobSize / 2;
        int knobCenterY = knobY + knobSize / 2;

        ui.fillCircle(knobCenterX, knobCenterY, knobSize / 2, ui.theme.scrollbarHover);
        ui.fillCircle(knobCenterX, knobCenterY, knobSize / 2 - 2, ui.theme.panel);

        if (dragging && dragSliderArea != null) {
            double nv = applyStep(clamp(valueFromMouse(mouseX, dragSliderArea)));
            setter.accept(nv);
        }
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 1) return false;
        if (!bounds.contains(mouseX, mouseY)) return false;
        dragging = true;
        dragSliderArea = getSliderArea(ui);
        double nv = applyStep(clamp(valueFromMouse(mouseX, dragSliderArea)));
        setter.accept(nv);
        return true;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        dragging = false;
        dragSliderArea = null;
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        if (!dragging || dragSliderArea == null) return false;
        double nv = applyStep(clamp(valueFromMouse(mouseX, dragSliderArea)));
        setter.accept(nv);
        return true;
    }

    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
}