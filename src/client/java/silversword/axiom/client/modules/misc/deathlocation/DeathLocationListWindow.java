package silversword.axiom.client.modules.misc.deathlocation;

import silversword.axiom.client.gui.components.*;
import silversword.axiom.client.gui.core.*;

import java.util.List;

public class DeathLocationListWindow implements UiComponent {
    private Rect bounds;
    private final ScrollContainer scroll = new ScrollContainer();
    private final DeathLocationManager manager = DeathLocationManager.getInstance();
    private final Button clearButton;

    public DeathLocationListWindow() {
        scroll.setGap(2);
        scroll.setInnerPadding(4);
        rebuild();

        clearButton = new Button("Clear All", () -> {
            manager.clear();
            rebuild();
        });
    }

    private void rebuild() {
        scroll.clear();
        List<DeathEntry> entries = manager.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            DeathEntry entry = entries.get(i);
            int index = i; // for lambda
            String text = entry.timestamp + " - [" + entry.getShortLocation() + "] [" + entry.world + "]";
            // Käytetään ActionButtonia, joka toimii rivinä
            ActionButton row = new ActionButton(text, () -> {
                // Voit lisätä toiminnon, esim. kopioi koordinaatit
                // Tällä hetkellä ei toimintoa
            });
            // Lisää delete-nappi erikseen? Tämä on yksinkertainen rivi.
            // Tehdään delete-nappi erillisenä komponenttina – vaatii hieman muokkausta.
            // Yksinkertaistetaan: jokainen rivi on poistettavissa klikkaamalla? Ei hyvä.
            // Parempi: luodaan oma riviluokka, mutta nyt tehdään nopeasti:
            scroll.add(row);
        }
    }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        int padding = 4;
        int w = bounds.w - 2 * padding;
        scroll.setBounds(new Rect(bounds.x + padding, bounds.y + padding, w, bounds.h - 40));
        clearButton.setBounds(new Rect(bounds.x + padding, bounds.bottom() - 30, w, 20));
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public int getPreferredHeight() { return 300; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        scroll.render(ui, mouseX, mouseY, delta);
        clearButton.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (scroll.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (clearButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        scroll.mouseReleased(ui, mouseX, mouseY, button);
        clearButton.mouseReleased(ui, mouseX, mouseY, button);
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
        return scroll.keyPressed(ui, keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        return scroll.charTyped(ui, chr, modifiers);
    }
}