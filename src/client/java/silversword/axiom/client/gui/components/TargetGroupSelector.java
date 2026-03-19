package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.modules.moduleutils.TargetGroup;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TargetGroupSelector implements UiComponent {
    private Rect bounds = new Rect(0, 0, 100, 18);
    private final Supplier<TargetGroup> getter;
    private final Consumer<TargetGroup> setter;

    public TargetGroupSelector(Supplier<TargetGroup> getter, Consumer<TargetGroup> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect bounds) { this.bounds = bounds; }
    @Override public int getPreferredHeight() { return 18; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        boolean hover = bounds.contains(mouseX, mouseY);
        ui.fill(bounds, hover ? ui.theme.buttonHover : ui.theme.button);
        String text = getter.get().name();
        int textX = bounds.x + (bounds.w - ui.textWidth(text)) / 2;
        int textY = bounds.y + bounds.h / 2 - ui.fontHeight() / 2 + 4;
        ui.text(text, textX, textY, ui.theme.text);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (!bounds.contains(mouseX, mouseY)) return false;
        TargetGroup[] values = TargetGroup.values();
        int index = getter.get().ordinal();
        if (button == 0) index = (index + 1) % values.length;
        else if (button == 1) index = (index - 1 + values.length) % values.length;
        setter.accept(values[index]);
        return true;
    }

    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
}