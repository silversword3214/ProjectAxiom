package silversword.axiom.client.modules.moduleutils.noparticle;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.gui.components.*;
import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.modules.render.NoParticleModule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NoParticleWindow implements UiComponent {

    private Rect bounds;
    private final NoParticleModule module;
    private final SearchBar searchBar;
    private final ScrollContainer scroll;
    private final ActionButton selectAllButton;
    private final ActionButton clearAllButton;

    private String searchText = "";
    private List<Identifier> allParticles;
    private List<Identifier> filteredParticles = new ArrayList<>();

    public NoParticleWindow(NoParticleModule module) {
        this.module = module;
        this.searchBar = new SearchBar(() -> searchText, s -> {
            searchText = s;
            rebuildList();
        });
        this.scroll = new ScrollContainer();
        this.selectAllButton = new ActionButton("Enable All", () -> {
            for (Identifier id : filteredParticles) {
                module.setParticleDisabled(id, false);
            }
            rebuildList();
        });
        this.clearAllButton = new ActionButton("Disable All", () -> {
            for (Identifier id : filteredParticles) {
                module.setParticleDisabled(id, true);
            }
            rebuildList();
        });

        // Gather all particle types from the registry
        allParticles = new ArrayList<>();
        BuiltInRegistries.PARTICLE_TYPE.forEach(particle -> {
            Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(particle);
            if (id != null) {
                allParticles.add(id);
            }
        });
        allParticles.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
        rebuildList();

        scroll.setGap(2);
        scroll.setInnerPadding(4);
    }

    private void rebuildList() {
        String search = searchText.toLowerCase();
        filteredParticles = allParticles.stream()
                .filter(id -> id.toString().toLowerCase().contains(search))
                .collect(Collectors.toList());

        scroll.clear();
        for (Identifier id : filteredParticles) {
            scroll.add(new ParticleEntry(id, module));
        }
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        int topBarHeight = 22;
        int buttonWidth = 90;
        int searchWidth = bounds.w - 2 * buttonWidth - 8;

        searchBar.setBounds(new Rect(bounds.x, bounds.y, searchWidth, topBarHeight));
        selectAllButton.setBounds(new Rect(bounds.x + searchWidth + 4, bounds.y, buttonWidth, topBarHeight));
        clearAllButton.setBounds(new Rect(bounds.x + searchWidth + buttonWidth + 8, bounds.y, buttonWidth, topBarHeight));

        int scrollY = bounds.y + topBarHeight + 4;
        scroll.setBounds(new Rect(bounds.x, scrollY, bounds.w, bounds.h - topBarHeight - 4));
    }

    @Override
    public int getPreferredHeight() { return 500; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        searchBar.render(ui, mouseX, mouseY, delta);
        selectAllButton.render(ui, mouseX, mouseY, delta);
        clearAllButton.render(ui, mouseX, mouseY, delta);
        scroll.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (searchBar.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (selectAllButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (clearAllButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        return scroll.mouseClicked(ui, mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        scroll.mouseReleased(ui, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        return scroll.mouseDragged(ui, mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        return scroll.mouseScrolled(ui, mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        if (searchBar.keyPressed(ui, keyCode, scanCode, modifiers)) return true;
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        if (searchBar.charTyped(ui, chr, modifiers)) return true;
        return false;
    }
}