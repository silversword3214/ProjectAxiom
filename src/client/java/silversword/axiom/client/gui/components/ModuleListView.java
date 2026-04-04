package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.main.AxiomMod;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ModuleListView implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);
    private final String ownerWindowId;
    private final Supplier<List<AxiomMod>> modulesSupplier;
    private final Supplier<String> selectedCategory;
    private final Consumer<AxiomMod> onOpenSettings;
    private final Consumer<String> onRemoveFromWindow;
    private final ScrollContainer scroll = new ScrollContainer();
    private String lastCategory = "";
    private int lastCount = -1;

    // Julkinen metodi scrollin lapsien hakemiseen
    public List<UiComponent> getScrollChildren() {
        return scroll.getChildren();
    }

    // Konstruktorit ilman searchText-parametria
    public ModuleListView(String ownerWindowId, Supplier<List<AxiomMod>> modulesSupplier,
                          Supplier<String> selectedCategory,
                          Consumer<AxiomMod> onOpenSettings) {
        this(ownerWindowId, modulesSupplier, selectedCategory, onOpenSettings, null);
    }

    public ModuleListView(String ownerWindowId, Supplier<List<AxiomMod>> modulesSupplier,
                          Supplier<String> selectedCategory,
                          Consumer<AxiomMod> onOpenSettings, Consumer<String> onRemoveFromWindow) {
        this.ownerWindowId = ownerWindowId;
        this.modulesSupplier = Objects.requireNonNull(modulesSupplier);
        this.selectedCategory = Objects.requireNonNull(selectedCategory);
        this.onOpenSettings = onOpenSettings;
        this.onRemoveFromWindow = onRemoveFromWindow;
        scroll.setDrawBackground(false);
        scroll.setInnerPadding(2);
        scroll.setGap(0);
        scroll.setShowScrollBar(false);
    }

    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect r) { bounds = r; scroll.setBounds(r); }
    @Override public int getPreferredHeight() { return 9999; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        String cat = safe(selectedCategory.get());
        List<AxiomMod> all = modulesSupplier.get();
        int count = all == null ? 0 : all.size();
        if (!cat.equals(lastCategory) || count != lastCount || scroll.getChildren().isEmpty()) {
            rebuild(all, cat);
            lastCategory = cat;
            lastCount = count;
        }
        scroll.render(ui, mouseX, mouseY, delta);
    }

    private void rebuild(List<AxiomMod> all, String cat) {
        scroll.clear();
        if (all == null) all = new ArrayList<>();
        List<AxiomMod> filtered = new ArrayList<>();
        for (AxiomMod m : all) {
            if (m == null) continue;
            if (!passesCategory(m, cat)) continue;
            filtered.add(m);
        }
        int rowIndex = 0;
        int total = filtered.size();
        for (AxiomMod m : filtered) {
            boolean isLast = (rowIndex == total - 1);
            scroll.add(new ModuleRow(m, onOpenSettings, ownerWindowId, rowIndex, isLast));
            rowIndex++;
        }
        if (total == 0) scroll.add(new DummyLabel("Drop modules here"));
    }

    public int getDropIndexAt(double mouseX, double mouseY) {
        List<UiComponent> kids = scroll.getChildren();
        int rows = 0;
        for (UiComponent c : kids) if (c instanceof ModuleRow) rows++;
        for (UiComponent c : kids) {
            if (!(c instanceof ModuleRow r)) continue;
            Rect br = r.getBounds();
            if (br.contains(mouseX, mouseY)) return r.getRowIndex();
            if (mouseY < (br.y + br.h / 2.0)) return r.getRowIndex();
        }
        return rows;
    }

    // --- Metodit moduulin paikannusta varten ---
    public void scrollToIndex(int index) {
        List<UiComponent> kids = scroll.getChildren();
        int y = 0;
        for (int i = 0; i < kids.size() && i <= index; i++) {
            if (kids.get(i) instanceof ModuleRow) {
                if (i == index) {
                    scroll.scrollTo(y);
                    return;
                }
                y += kids.get(i).getPreferredHeight();
            }
        }
    }

    public int getModuleRowIndex(AxiomMod module) {
        List<UiComponent> kids = scroll.getChildren();
        for (int i = 0; i < kids.size(); i++) {
            if (kids.get(i) instanceof ModuleRow row && row.getModule() == module) {
                return i;
            }
        }
        return -1;
    }

    private static boolean passesCategory(AxiomMod m, String cat) {
        if (cat == null || cat.isEmpty() || "All".equalsIgnoreCase(cat)) return true;
        if ("Enabled".equalsIgnoreCase(cat)) return m.isEnabled();
        String moduleCat = reflectCategoryName(m);
        return cat.equalsIgnoreCase(moduleCat);
    }

    private static String reflectCategoryName(AxiomMod m) {
        try {
            Object c = m.getClass().getMethod("getCategory").invoke(m);
            if (c == null) return "Misc";
            try { return String.valueOf(c.getClass().getMethod("name").invoke(c)); }
            catch (Throwable ignored) { return String.valueOf(c); }
        } catch (Throwable ignored) { return "Misc"; }
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    @Override public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) { return scroll.mouseClicked(ui, mouseX, mouseY, button); }
    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) { scroll.mouseReleased(ui, mouseX, mouseY, button); }
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return scroll.mouseDragged(ui, mouseX, mouseY, button, dx, dy); }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return scroll.mouseScrolled(ui, mouseX, mouseY, amount); }
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return scroll.keyPressed(ui, keyCode, scanCode, modifiers); }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return scroll.charTyped(ui, chr, modifiers); }
}