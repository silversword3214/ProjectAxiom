package silversword.axiom.client.modules.waypoints;

import net.minecraft.client.Minecraft;
import silversword.axiom.client.gui.components.*;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.managers.WaypointManager;

public class AddWaypointDialog implements UiComponent {
    private Rect bounds;
    private final WaypointManager manager = WaypointManager.getInstance();
    private final Runnable onClose;

    private final TextField nameField;
    private final TextField xField, yField, zField;
    private final Button createButton;
    private final Button cancelButton;

    public AddWaypointDialog(Runnable onClose) {
        this.onClose = onClose;

        Minecraft mc = Minecraft.getInstance();
        double customX = 0, customY = 0, customZ = 0;
        if (mc.player != null) {
            customX = mc.player.getX();
            customY = mc.player.getY();
            customZ = mc.player.getZ();
        }

        nameField = new TextField();
        nameField.setPlaceholder("Name");

        xField = new TextField();
        xField.setText(String.valueOf(Math.round(customX)));
        yField = new TextField();
        yField.setText(String.valueOf(Math.round(customY)));
        zField = new TextField();
        zField.setText(String.valueOf(Math.round(customZ)));

        createButton = new Button("Create", () -> {
            try {
                double x = Double.parseDouble(xField.getText());
                double y = Double.parseDouble(yField.getText());
                double z = Double.parseDouble(zField.getText());
                Waypoint wp = new Waypoint(
                        nameField.getText().isEmpty() ? "Waypoint" : nameField.getText(),
                        x, y, z, 0xFF00FF00);
                manager.add(wp);
                onClose.run();
            } catch (NumberFormatException e) {
                // Virheelliset koordinaatit – voidaan halutessa näyttää virheilmoitus
            }
        });

        cancelButton = new Button("Cancel", onClose);
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        int padding = 8;
        int y = bounds.y + padding;
        int w = bounds.w - 2 * padding;
        int fieldH = 18;
        int spacing = 4;

        nameField.setBounds(new Rect(bounds.x + padding, y, w, fieldH));
        y += fieldH + spacing;

        int third = (w - 2*spacing)/3;
        xField.setBounds(new Rect(bounds.x + padding, y, third, fieldH));
        yField.setBounds(new Rect(bounds.x + padding + third + spacing, y, third, fieldH));
        zField.setBounds(new Rect(bounds.x + padding + 2*(third + spacing), y, third, fieldH));
        y += fieldH + spacing*2;

        createButton.setBounds(new Rect(bounds.x + padding, y, w, fieldH));
        y += fieldH + spacing;
        cancelButton.setBounds(new Rect(bounds.x + padding, y, w, fieldH));
    }

    @Override
    public int getPreferredHeight() { return 150; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        ui.fill(bounds, ui.theme.panel);
        nameField.render(ui, mouseX, mouseY, delta);
        xField.render(ui, mouseX, mouseY, delta);
        yField.render(ui, mouseX, mouseY, delta);
        zField.render(ui, mouseX, mouseY, delta);
        createButton.render(ui, mouseX, mouseY, delta);
        cancelButton.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (nameField.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (xField.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (yField.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (zField.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (createButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (cancelButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        if (nameField.keyPressed(ui, keyCode, scanCode, modifiers)) return true;
        if (xField.keyPressed(ui, keyCode, scanCode, modifiers)) return true;
        if (yField.keyPressed(ui, keyCode, scanCode, modifiers)) return true;
        if (zField.keyPressed(ui, keyCode, scanCode, modifiers)) return true;
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        if (nameField.charTyped(ui, chr, modifiers)) return true;
        if (xField.charTyped(ui, chr, modifiers)) return true;
        if (yField.charTyped(ui, chr, modifiers)) return true;
        if (zField.charTyped(ui, chr, modifiers)) return true;
        return false;
    }
}