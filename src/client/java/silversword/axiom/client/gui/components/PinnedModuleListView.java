package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.gui.window.PinnedWindowData;

import java.util.ArrayList;
import java.util.List;

public final class PinnedModuleListView implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);
    private final silversword.axiom.client.gui.components.ScrollContainer scroll = new ScrollContainer();

    private final ModuleManager moduleManager;
    private final PinnedWindowData data;

    // drag reorder state
    private int dragFrom = -1;
    private int lastHover = -1;

    public PinnedModuleListView(ModuleManager moduleManager, PinnedWindowData data) {
        this.moduleManager = moduleManager;
        this.data = data;

        scroll.setInnerPadding(4);
        scroll.setGap(4);
    }

    @Override public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect r) {
        bounds = r;
        scroll.setBounds(r);
    }

    @Override public int getPreferredHeight() { return 180; }

    public void pinModule(String moduleId) {
        if (moduleId == null || moduleId.isEmpty()) return;
        if (!data.moduleIds.contains(moduleId)) {
            data.moduleIds.add(moduleId);
            rebuild();
        }
    }

    public void unpinModule(String moduleId) {
        data.moduleIds.remove(moduleId);
        rebuild();
    }

    private void rebuild() {
        scroll.clear();

        List<String> ids = new ArrayList<>(data.moduleIds);
        if (ids.isEmpty()) {
            scroll.add(new DummyLabel("Drop modules here / pin modules here"));
            return;
        }

        for (int i = 0; i < ids.size(); i++) {
            final int index = i;
            String id = ids.get(i);

            AxiomMod mod = moduleManager.getById(id);
            if (mod == null) continue;

            scroll.add(new PinnedModuleRow(
                    mod,
                    index,
                    hoverIndex -> onDragHover(index, hoverIndex),
                    () -> unpinModule(id)
            ));
        }
    }

    private void onDragHover(int from, int hoverIndex) {
        if (dragFrom == -1) dragFrom = from;
        lastHover = hoverIndex;

        // Swap kun “raahataan” eri indeksin yli
        if (dragFrom != -1 && lastHover != -1 && dragFrom != lastHover) {
            // swap moduleIds order
            String a = data.moduleIds.get(dragFrom);
            data.moduleIds.set(dragFrom, data.moduleIds.get(lastHover));
            data.moduleIds.set(lastHover, a);

            dragFrom = lastHover; // jatka swap-ketjua sujuvasti
            rebuild();
        }
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        // rakenna ensimmäisellä renderillä
        if (scroll.getChildren().isEmpty()) rebuild();
        scroll.render(ui, mouseX, mouseY, delta);
    }

    @Override public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        return scroll.mouseClicked(ui, mouseX, mouseY, button);
    }

    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        dragFrom = -1;
        lastHover = -1;
        scroll.mouseReleased(ui, mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        return scroll.mouseDragged(ui, mouseX, mouseY, button, dx, dy);
    }

    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        return scroll.mouseScrolled(ui, mouseX, mouseY, amount);
    }
}
