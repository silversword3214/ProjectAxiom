package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.utils.animation.Animation;
import silversword.axiom.client.utils.animation.Ease;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class Toggle implements UiComponent {

    private static final float ANIMATION_SPEED = 0.2f;

    private Rect bounds = new Rect(0, 0, 10, 10);
    private final String label;
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final Animation knobAnim;
    private boolean lastValue = false;

    public Toggle(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        this.label = label;
        this.getter = getter;
        this.setter = setter;
        this.lastValue = getter.getAsBoolean();
        this.knobAnim = new Animation(lastValue ? 1.0f : 0.0f, ANIMATION_SPEED);
    }

    @Override
    public Rect getBounds() { return bounds; }
    @Override
    public void setBounds(Rect bounds) { this.bounds = bounds; }
    @Override
    public int getPreferredHeight() { return 18; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        boolean hover = bounds.contains(mouseX, mouseY);
        boolean currentValue = getter.getAsBoolean();

        if (currentValue != lastValue) {
            knobAnim.setTarget(currentValue ? 1.0f : 0.0f);
            lastValue = currentValue;
        }
        knobAnim.update(delta);

        // Label
        int labelColor = hover ? ui.theme.accent : ui.theme.text;
        int textY = bounds.y + (bounds.h - ui.fontHeight()) / 2 + 4;
        ui.text(label, bounds.x + ui.theme.innerPadding, textY, labelColor);

        // Pill switch
        int pillW = 34, pillH = 14;
        int sx = bounds.right() - pillW - ui.theme.innerPadding;
        int sy = bounds.y + (bounds.h - pillH) / 2;

        float t = knobAnim.getValue();
        int bgColor = interpolateColor(ui.theme.toggleOff, ui.theme.toggleOn, t);
        ui.fillRounded(sx, sy, pillW, pillH, bgColor, pillH / 2.0);

        // Knob
        int knobSize = pillH - 4;
        int knobRadius = knobSize / 2;
        int minKnobX = sx + 2 + knobRadius;
        int maxKnobX = sx + pillW - knobSize - 2 + knobRadius;
        float eased = Ease.easeOutQuad(t);
        int knobX = (int) (minKnobX + (maxKnobX - minKnobX) * eased);
        int knobY = sy + 2 + knobRadius;

        ui.fillCircle(knobX, knobY, knobRadius, ui.theme.panel);
        ui.fillCircle(knobX, knobY, knobRadius - 2, ui.theme.textDim);
    }

    private int interpolateColor(int color1, int color2, float t) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (!bounds.contains(mouseX, mouseY)) return false;

        boolean newValue = !getter.getAsBoolean();
        setter.accept(newValue);
        knobAnim.setTarget(newValue ? 1.0f : 0.0f);
        lastValue = newValue;
        return true;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        return false;
    }

    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        return false;
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        return false;
    }
}