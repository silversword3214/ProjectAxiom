package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;

public final class IndentedRow implements UiComponent {

    private final UiComponent inner;
    private final int indent;
    private Rect bounds = new Rect(0, 0, 10, 10);

    public IndentedRow(UiComponent inner, int indent) {
        this.inner = inner;
        this.indent = indent;
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect r) {
        this.bounds = r;
        inner.setBounds(new Rect(r.x + indent, r.y, r.w - indent, r.h));
    }

    @Override
    public int getPreferredHeight() {
        return inner.getPreferredHeight();
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        // Piirrä pieni vertikaalinen viiva hierarkian osoittamiseksi
        int lineX = bounds.x + 4;
        int lineY1 = bounds.y;
        int lineY2 = bounds.y + bounds.h;
        ui.fill(lineX, lineY1, 1, lineY2 - lineY1, 0x44FFFFFF);

        inner.render(ui, mouseX, mouseY, delta);
    }

    @Override public boolean mouseClicked(UiContext ui, double mx, double my, int btn) { return inner.mouseClicked(ui, mx, my, btn); }
    @Override public void mouseReleased(UiContext ui, double mx, double my, int btn)  { inner.mouseReleased(ui, mx, my, btn); }
    @Override public boolean mouseDragged(UiContext ui, double mx, double my, int btn, double dx, double dy) { return inner.mouseDragged(ui, mx, my, btn, dx, dy); }
    @Override public boolean mouseScrolled(UiContext ui, double mx, double my, double amt) { return inner.mouseScrolled(ui, mx, my, amt); }
    @Override public boolean keyPressed(UiContext ui, int key, int scan, int mods)  { return inner.keyPressed(ui, key, scan, mods); }
    @Override public boolean charTyped(UiContext ui, char chr, int mods)            { return inner.charTyped(ui, chr, mods); }
}