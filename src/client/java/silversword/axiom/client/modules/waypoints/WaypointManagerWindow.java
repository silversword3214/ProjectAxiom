package silversword.axiom.client.modules.waypoints;

import silversword.axiom.client.gui.components.*;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.WaypointManager;
import silversword.axiom.client.modules.render.WaypointModule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class WaypointManagerWindow implements UiComponent {
    private Rect bounds;
    private final WaypointModule module;
    private final WaypointManager manager = WaypointManager.getInstance();

    private final ScrollContainer scroll = new ScrollContainer();
    private final SearchBar searchBar;
    private final Toggle showDistanceToggle;
    private final ActionButton addButton;

    private List<Waypoint> filtered = new ArrayList<>();
    private String lastSearch = "";

    public WaypointManagerWindow(WaypointModule module) {
        this.module = module;
        Theme theme = ThemeManager.getCurrentTheme(); // or get from somewhere

        searchBar = new SearchBar(
                () -> lastSearch,
                (s) -> { lastSearch = s; rebuild(); }
        );

        showDistanceToggle = new Toggle("Show Distance",
                () -> module.showDistance.get(),
                val -> module.showDistance.set(val)
        );

        addButton = new ActionButton("Add Waypoint", () -> {
            // Avataan uusi ikkuna lisäystä varten (toteutetaan myöhemmin)
            openAddWaypointDialog();
        });

        scroll.setGap(2);
        scroll.setInnerPadding(4);
        rebuild();
    }

    private void rebuild() {
        scroll.clear();
        List<Waypoint> all = manager.getAll();
        String search = lastSearch.toLowerCase();
        filtered = all.stream()
                .filter(w -> w.name.toLowerCase().contains(search))
                .collect(Collectors.toList());

        for (Waypoint wp : filtered) {
            WaypointRow row = new WaypointRow(wp, manager,
                    updated -> manager.update(updated),  // EI rebuildia
                    () -> rebuild(),                      // poiston jälkeen rebuild
                    ThemeManager.getCurrentTheme());
            scroll.add(row);
        }
    }

    private void openAddWaypointDialog() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        AddWaypointDialog dialog = new AddWaypointDialog(() -> {
            // Suljetaan ikkuna ja päivitetään lista
            factory.getWindowManager().closeOverlay();
            rebuild();
        });
        factory.openPopupWindow("add_waypoint", "Add Waypoint",
                (sw - 300)/2, (sh - 200)/2, 300, 200, dialog);
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        int padding = 4;
        int y = bounds.y + padding;
        int w = bounds.w - 2 * padding;

        // Hakupalkki (leveys - 100, jotta toggle mahtuu)
        searchBar.setBounds(new Rect(bounds.x + padding, y, w - 100, 18));

        // Show Distance -toggle oikealle
        showDistanceToggle.setBounds(new Rect(bounds.right() - padding - 90, y, 90, 18));

        y += 22;

        int scrollY = y;
        int scrollH = bounds.bottom() - y - 30;
        scroll.setBounds(new Rect(bounds.x + padding, scrollY, w, scrollH));

        addButton.setBounds(new Rect(bounds.x + padding, bounds.bottom() - 24, w, 20));
    }

    @Override
    public int getPreferredHeight() { return 400; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        searchBar.render(ui, mouseX, mouseY, delta);
        showDistanceToggle.render(ui, mouseX, mouseY, delta);
        scroll.render(ui, mouseX, mouseY, delta);
        addButton.render(ui, mouseX, mouseY, delta);
    }

    // Delegate mouse events to subcomponents
    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (searchBar.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (showDistanceToggle.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (scroll.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (addButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        searchBar.mouseReleased(ui, mouseX, mouseY, button);
        showDistanceToggle.mouseReleased(ui, mouseX, mouseY, button);
        scroll.mouseReleased(ui, mouseX, mouseY, button);
        addButton.mouseReleased(ui, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        if (scroll.mouseDragged(ui, mouseX, mouseY, button, dx, dy)) return true;
        return false;
    }

    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        return scroll.mouseScrolled(ui, mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        if (searchBar.keyPressed(ui, keyCode, scanCode, modifiers)) return true;
        return scroll.keyPressed(ui, keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        if (searchBar.charTyped(ui, chr, modifiers)) return true;
        return scroll.charTyped(ui, chr, modifiers);
    }
}