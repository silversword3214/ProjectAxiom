package silversword.axiom.client.modules.moduleutils.autopot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.gui.components.*;
import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.gui.window.WindowManager;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.combat.PotionRefill;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class AutoPotWindow implements UiComponent {

    private Rect bounds;
    private final PotionRefill module;
    private final SearchBar searchBar;
    private final ScrollContainer scroll;
    private final ActionButton selectAllButton;
    private final ActionButton clearAllButton;

    private String searchText = "";
    private List<Identifier> allEffects;
    private List<Identifier> filteredEffects = new ArrayList<>();

    private final ActionButton settingsButton;



    public AutoPotWindow(PotionRefill module) {
        this.module = module;
        this.searchBar = new SearchBar(() -> searchText, s -> {
            searchText = s;
            rebuildList();
        });
        this.scroll = new ScrollContainer();
        this.selectAllButton = new ActionButton("Enable All", () -> {
            for (Identifier id : filteredEffects) {
                module.setEffectSelected(id, true);
            }
            rebuildList();
        });
        this.clearAllButton = new ActionButton("Disable All", () -> {
            for (Identifier id : filteredEffects) {
                module.setEffectSelected(id, false);
            }
            rebuildList();
        });

        // Gather all mob effects from the registry
        allEffects = new ArrayList<>();
        BuiltInRegistries.MOB_EFFECT.forEach(effect -> {
            Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (id != null) {
                allEffects.add(id);
            }
        });
        allEffects.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
        rebuildList();

        scroll.setGap(2);
        scroll.setInnerPadding(4);

        this.settingsButton = new ActionButton("Settings", () -> {
            // Sulje tämä ikkuna
            WindowFactory factory = AxiomMod.getWindowFactory();
            WindowManager manager = AxiomMod.getWindowFactory().getWindowManager();
            if (manager != null) {
                manager.closeOverlay();
                Minecraft mc = Minecraft.getInstance();
                int sw = mc.getWindow().getGuiScaledWidth();
                int sh = mc.getWindow().getGuiScaledHeight();
                factory.openSettingsWindow(module, sw, sh);
            }
        });
    }

    private void rebuildList() {
        String search = searchText.toLowerCase();
        filteredEffects = allEffects.stream()
                .filter(id -> id.toString().toLowerCase().contains(search))
                .collect(Collectors.toList());

        scroll.clear();
        for (Identifier id : filteredEffects) {
            scroll.add(new EffectEntry(id, module));
        }
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        int topBarHeight = 22;
        int buttonWidth = 70;
        int searchWidth = bounds.w - 3 * buttonWidth - 12;

        searchBar.setBounds(new Rect(bounds.x, bounds.y, searchWidth, topBarHeight));
        selectAllButton.setBounds(new Rect(bounds.x + searchWidth + 4, bounds.y, buttonWidth, topBarHeight));
        clearAllButton.setBounds(new Rect(bounds.x + searchWidth + buttonWidth + 8, bounds.y, buttonWidth, topBarHeight));

        int scrollY = bounds.y + topBarHeight + 4;
        scroll.setBounds(new Rect(bounds.x, scrollY, bounds.w, bounds.h - topBarHeight - 4));


        searchBar.setBounds(new Rect(bounds.x, bounds.y, searchWidth, topBarHeight + 3));
        selectAllButton.setBounds(new Rect(bounds.x + searchWidth + 4, bounds.y, buttonWidth, topBarHeight));
        clearAllButton.setBounds(new Rect(bounds.x + searchWidth + buttonWidth + 8, bounds.y, buttonWidth, topBarHeight));
        settingsButton.setBounds(new Rect(bounds.x + searchWidth + 2*buttonWidth + 12, bounds.y, buttonWidth, topBarHeight));



    }

    @Override
    public int getPreferredHeight() { return 450; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        searchBar.render(ui, mouseX, mouseY, delta);
        selectAllButton.render(ui, mouseX, mouseY, delta);
        clearAllButton.render(ui, mouseX, mouseY, delta);
        scroll.render(ui, mouseX, mouseY, delta);
        settingsButton.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (searchBar.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (selectAllButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (clearAllButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (settingsButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
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
        return searchBar.keyPressed(ui, keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        return searchBar.charTyped(ui, chr, modifiers);
    }
}