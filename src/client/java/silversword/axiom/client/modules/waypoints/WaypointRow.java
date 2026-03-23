package silversword.axiom.client.modules.waypoints;

import net.minecraft.client.Minecraft;
import silversword.axiom.client.gui.components.*;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.WaypointManager;

import java.util.function.Consumer;

public class WaypointRow implements UiComponent {
    private Rect bounds;
    private final Waypoint waypoint;
    private final WaypointManager manager;
    private final Consumer<Waypoint> onUpdate;
    private final Runnable onDelete;
    private final Theme theme;

    // Sub-components (interactive)
    private final Button editButton;
    private final Toggle enabledToggle;
    private final Button deleteButton;

    // For layout we need to store the rectangles for name and coordinates
    private Rect nameRect;
    private Rect xRect, yRect, zRect;

    public WaypointRow(Waypoint waypoint, WaypointManager manager, Consumer<Waypoint> onUpdate, Runnable onDelete, Theme theme) {
        this.waypoint = waypoint;
        this.manager = manager;
        this.onUpdate = onUpdate;
        this.onDelete = onDelete;
        this.theme = theme;

        editButton = new Button("Edit", () -> openEditWindow());
        enabledToggle = new Toggle("", () -> waypoint.enabled, val -> {
            waypoint.enabled = val;
            onUpdate.accept(waypoint);
        });
        deleteButton = new Button("X", () -> {
            manager.remove(waypoint.id);
            onDelete.run();
        });
    }

    private void openEditWindow() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        WaypointEditWindow editWin = new WaypointEditWindow(waypoint, () -> {
            factory.getWindowManager().closeOverlay();
            onUpdate.accept(waypoint); // päivitetään manager
        });
        factory.openPopupWindow("edit_waypoint_" + waypoint.id, "Edit Waypoint",
                (sw - 400)/2, (sh - 400)/2, 400, 400, editWin);
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        int x = bounds.x;
        int w = bounds.w;
        int nameW = 100;
        int coordW = 60;
        int editW = 40;
        int toggleW = 40;
        int deleteW = 24;
        int spacing = 4;

        int cx = x;
        nameRect = new Rect(cx, bounds.y, nameW, bounds.h); cx += nameW + spacing;
        xRect = new Rect(cx, bounds.y, coordW, bounds.h); cx += coordW + spacing;
        yRect = new Rect(cx, bounds.y, coordW, bounds.h); cx += coordW + spacing;
        zRect = new Rect(cx, bounds.y, coordW, bounds.h); cx += coordW + spacing;
        editButton.setBounds(new Rect(cx, bounds.y, editW, bounds.h)); cx += editW + spacing;
        enabledToggle.setBounds(new Rect(cx, bounds.y, toggleW, bounds.h)); cx += toggleW + spacing;
        deleteButton.setBounds(new Rect(cx, bounds.y, deleteW, bounds.h));
    }

    @Override
    public int getPreferredHeight() { return 18; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        // Draw name
        ui.text(waypoint.name, nameRect.x + 4, nameRect.y + (nameRect.h - ui.fontHeight())/2 + 4, ui.theme.text);
        // Draw coordinates as integers
        String xStr = String.valueOf((int)Math.round(waypoint.x));
        String yStr = String.valueOf((int)Math.round(waypoint.y));
        String zStr = String.valueOf((int)Math.round(waypoint.z));
        ui.text(xStr, xRect.x + 4, xRect.y + (xRect.h - ui.fontHeight())/2 + 4, ui.theme.text);
        ui.text(yStr, yRect.x + 4, yRect.y + (yRect.h - ui.fontHeight())/2 + 4, ui.theme.text);
        ui.text(zStr, zRect.x + 4, zRect.y + (zRect.h - ui.fontHeight())/2 + 4, ui.theme.text);

        editButton.render(ui, mouseX, mouseY, delta);
        enabledToggle.render(ui, mouseX, mouseY, delta);
        deleteButton.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (editButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (enabledToggle.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (deleteButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        editButton.mouseReleased(ui, mouseX, mouseY, button);
        enabledToggle.mouseReleased(ui, mouseX, mouseY, button);
        deleteButton.mouseReleased(ui, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        if (editButton.mouseDragged(ui, mouseX, mouseY, button, dx, dy)) return true;
        if (enabledToggle.mouseDragged(ui, mouseX, mouseY, button, dx, dy)) return true;
        if (deleteButton.mouseDragged(ui, mouseX, mouseY, button, dx, dy)) return true;
        return false;
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
}