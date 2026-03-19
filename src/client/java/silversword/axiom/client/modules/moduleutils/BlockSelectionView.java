package silversword.axiom.client.modules.moduleutils;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import silversword.axiom.client.gui.components.ActionButton;
import silversword.axiom.client.gui.components.ScrollContainer;
import silversword.axiom.client.gui.components.SearchBar;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BlockSelectionView implements UiComponent {
    private Rect bounds;
    private final BlockSelectable module;
    private final SearchBar searchBar;
    private final ScrollContainer scroll;
    private final ActionButton filterButton;
    private String searchText = "";
    private boolean showSelectedOnly = false;
    private List<Block> allBlocks;
    private List<Block> lastFiltered = new ArrayList<>();

    public BlockSelectionView(BlockSelectable module) {
        this.module = module;
        this.searchBar = new SearchBar(() -> searchText, s -> searchText = s);
        this.scroll = new ScrollContainer();
        this.filterButton = new ActionButton("Show All", () -> {
            showSelectedOnly = !showSelectedOnly;
            updateFilterButtonLabel();
            rebuildList();
        });

        this.allBlocks = new ArrayList<>();
        for (Block block : Registries.BLOCK) {
            allBlocks.add(block);
        }
        allBlocks.sort((a, b) -> a.getName().getString().compareToIgnoreCase(b.getName().getString()));
    }

    private void updateFilterButtonLabel() {
        filterButton.setLabel(showSelectedOnly ? "Show All" : "Show Selected");
    }

    private void rebuildList() {
        // suodatus tapahtuu renderissä, joten tyhjennetään lastFiltered pakottamaan uusi rakennus
        lastFiltered.clear();
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        int topBarHeight = 18;
        int buttonWidth = 80;
        int searchWidth = bounds.w - buttonWidth - 4;

        searchBar.setBounds(new Rect(bounds.x, bounds.y, searchWidth, topBarHeight));
        filterButton.setBounds(new Rect(bounds.x + searchWidth + 4, bounds.y, buttonWidth, topBarHeight));

        int scrollY = bounds.y + topBarHeight + 4;
        scroll.setBounds(new Rect(bounds.x, scrollY, bounds.w, bounds.h - topBarHeight - 4));
    }

    @Override
    public int getPreferredHeight() { return 200; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        searchBar.render(ui, mouseX, mouseY, delta);
        filterButton.render(ui, mouseX, mouseY, delta);

        String search = searchText.toLowerCase();
        List<Block> filtered = allBlocks.stream()
                .filter(b -> b.getName().getString().toLowerCase().contains(search))
                .filter(b -> !showSelectedOnly || module.isBlockSelected(b))
                .collect(Collectors.toList());

        if (!filtered.equals(lastFiltered)) {
            lastFiltered = filtered;
            scroll.clear();
            for (Block block : filtered) {
                scroll.add(new BlockEntry(block, module));
            }
        }

        scroll.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (searchBar.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (filterButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
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