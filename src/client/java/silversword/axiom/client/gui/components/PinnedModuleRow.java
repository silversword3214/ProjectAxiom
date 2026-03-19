package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.main.AxiomMod;

import java.util.function.IntConsumer;

public final class PinnedModuleRow implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);

    private final AxiomMod module;
    private final int index;
    private final IntConsumer onDragHoverIndex; // kerrotaan parentille mihin kohtaan "raahataan"
    private final Runnable onRemove;            // right click remove

    private boolean dragging = false;

    public PinnedModuleRow(AxiomMod module, int index, IntConsumer onDragHoverIndex, Runnable onRemove) {
        this.module = module;
        this.index = index;
        this.onDragHoverIndex = onDragHoverIndex;
        this.onRemove = onRemove;
    }

    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect r) { bounds = r; }
    @Override public int getPreferredHeight() { return 16; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        boolean hover = bounds.contains(mouseX, mouseY);
        int bg = hover ? ui.theme.buttonHover : ui.theme.button;

        ui.fill(bounds, bg);

        String name = module.getName();
        ui.text(name, bounds.x + ui.theme.innerPadding, bounds.y + 4, ui.theme.text);

        // enabled indicator
        String flag = module.isEnabled() ? "ON" : "OFF";
        int tx = bounds.right() - ui.textWidth(flag) - ui.theme.innerPadding;
        ui.text(flag, tx, bounds.y + 4, module.isEnabled() ? ui.theme.accent : ui.theme.textDim);

        if (dragging) {
            // pienen “dragging” -tuntuman voi lisätä myöhemmin
        }
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (!bounds.contains(mouseX, mouseY)) return false;

        if (button == 0) {
            // Left click toggles module
            module.toggle();
            dragging = true;
            return true;
        }

        if (button == 1) {
            // Right click removes from this pinned window
            if (onRemove != null) onRemove.run();
            return true;
        }

        return false;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        dragging = false;
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        if (!dragging || button != 0) return false;

        // Kerro parentille missä indeksissä hiiri on (swap logiikka parentissa)
        if (onDragHoverIndex != null) onDragHoverIndex.accept(index);
        return true;
    }
}
