package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.utils.animation.Animation;
import silversword.axiom.client.utils.animation.Ease;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ModuleSearchBar implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);
    private String text = "";
    private boolean focused = false;

    private final Animation heightAnim;
    private List<AxiomMod> searchResults = new ArrayList<>();
    private final Consumer<AxiomMod> onModuleSelected;
    private static final int MAX_DROPDOWN_HEIGHT = 200;
    private static final float ANIM_SPEED = 0.2f;

    public ModuleSearchBar(Consumer<AxiomMod> onModuleSelected) {
        this.onModuleSelected = onModuleSelected;
        this.heightAnim = new Animation(0f, ANIM_SPEED);
    }

    public String getText() { return text; }

    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect bounds) { this.bounds = bounds; }
    @Override public int getPreferredHeight() { return 18; }

    private void updateSearchResults() {
        searchResults.clear();
        if (text == null || text.trim().isEmpty()) return;
        String lower = text.toLowerCase();
        for (AxiomMod mod : ModuleManager.getInstance().getModules()) {
            if (mod.getName().toLowerCase().contains(lower)) {
                searchResults.add(mod);
            }
        }
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        boolean showDropdown = focused && !text.isEmpty();
        heightAnim.setTarget(showDropdown ? 1.0f : 0.0f);
        heightAnim.update(delta);

        float progress = heightAnim.getValue();
        float eased = Ease.easeOutQuad(Math.min(progress, 1.0f));
        float currentHeight = eased * MAX_DROPDOWN_HEIGHT;

        boolean hover = bounds.contains(mouseX, mouseY);
        int bg = focused ? ui.theme.panel : (hover ? ui.theme.buttonHover : ui.theme.button);

        boolean isOpened = currentHeight > 1;
        ui.fillRoundedCustom(bounds, bg, 4,
                true, true,
                !isOpened, !isOpened
        );

        String shown = text.isEmpty() && !focused ? "Search modules..." : text;
        int color = text.isEmpty() && !focused ? ui.theme.textDim : ui.theme.text;
        int textY = bounds.y + bounds.h / 2 - ui.fontHeight() / 2 + 4;
        ui.text(shown, bounds.x + ui.theme.innerPadding, textY, color);

        if (currentHeight > 0) {
            int availableBelow = ui.mc.getWindow().getGuiScaledHeight() - bounds.bottom();
            int drawHeight = (int) Math.min(currentHeight, availableBelow);

            if (drawHeight > 0) {
                Rect dropRect = new Rect(bounds.x, bounds.bottom(), bounds.w, drawHeight);

                ui.fillRoundedCustom(dropRect, ui.theme.panel, ui.theme.radius,
                        false, false,
                        true, true
                );

                if (searchResults.isEmpty()) {
                    String msg = "No modules found";
                    int tw = ui.textWidth(msg);
                    ui.text(msg, dropRect.x + (dropRect.w - tw) / 2, dropRect.y + 8, ui.theme.textDim);
                } else {
                    int yOffset = 2;
                    int rowHeight = 16;
                    int maxVisible = Math.min(searchResults.size(), drawHeight / rowHeight);

                    ui.enableScissor(dropRect.x, dropRect.y, dropRect.w, dropRect.h);

                    for (int i = 0; i < maxVisible; i++) {
                        AxiomMod mod = searchResults.get(i);
                        Rect rowRect = new Rect(dropRect.x + 2, dropRect.y + yOffset + i * rowHeight, dropRect.w - 4, rowHeight);

                        if (rowRect.contains(mouseX, mouseY)) {
                            ui.fill(rowRect, ui.theme.buttonHover);
                        }
                        ui.text(mod.getName(), rowRect.x + 4, rowRect.y + 4, ui.theme.text);
                    }

                    ui.disableScissor();
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        boolean clickedOnBar = bounds.contains(mouseX, mouseY);

        Rect dropRect = null;
        if (focused && !text.isEmpty()) {
            float eased = Ease.easeOutQuad(Math.min(heightAnim.getValue(), 1.0f));
            float currentHeight = eased * MAX_DROPDOWN_HEIGHT;
            if (currentHeight > 1) {
                dropRect = new Rect(bounds.x, bounds.bottom(), bounds.w, (int)currentHeight);
            }
        }

        if (dropRect != null && dropRect.contains(mouseX, mouseY)) {
            int rowHeight = 16;
            int yOffset = 2;
            int index = (int) ((mouseY - dropRect.y - yOffset) / rowHeight);
            if (index >= 0 && index < searchResults.size()) {
                onModuleSelected.accept(searchResults.get(index));
                focused = false;
                text = "";
                updateSearchResults();
            }
            return true;
        }

        focused = clickedOnBar;
        if (focused) {
            updateSearchResults();
        }

        return focused;
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        if (keyCode == 256 || keyCode == 257) {
            focused = false;
            return true;
        }
        if (keyCode == 259) {
            if (!text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
                updateSearchResults();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        if (!focused) return false;
        if (chr >= 32 && chr != 127) {
            text += chr;
            updateSearchResults();
            return true;
        }
        return false;
    }

    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
}