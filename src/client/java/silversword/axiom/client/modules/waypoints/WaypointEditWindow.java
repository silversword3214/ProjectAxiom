package silversword.axiom.client.modules.waypoints;

import silversword.axiom.client.gui.components.*;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.WaypointManager;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;

public class WaypointEditWindow implements UiComponent {
    private Rect bounds;
    private final Waypoint waypoint;
    private final WaypointManager manager;
    private final Runnable onClose;

    // Kentät
    private final TextField nameField;
    private final TextField xField, yField, zField;
    private final Button colorButton;
    private final Toggle enabledToggle;
    private final Slider scaleSlider;
    private final Toggle showBgToggle;
    private final Button bgColorButton;
    private final Toggle showOutlineToggle;
    private final Button outlineColorButton;
    private final ModeDropdown shapeDropdown;
    private final Button saveButton;
    private final Button cancelButton;

    private boolean colorPickerOpen = false;
    private boolean bgColorPickerOpen = false;
    private boolean outlineColorPickerOpen = false;

    // Väliaikaiset värit
    private int tempColor;
    private int tempBgColor;
    private int tempOutlineColor;

    public WaypointEditWindow(Waypoint waypoint, Runnable onClose) {
        this.waypoint = waypoint;
        this.manager = WaypointManager.getInstance();
        this.onClose = onClose;

        tempColor = waypoint.color;
        tempBgColor = waypoint.bgColor;
        tempOutlineColor = waypoint.outlineColor;

        nameField = new TextField();
        nameField.setText(waypoint.name);

        xField = new TextField();
        xField.setText(String.valueOf(Math.round(waypoint.x)));
        yField = new TextField();
        yField.setText(String.valueOf(Math.round(waypoint.y)));
        zField = new TextField();
        zField.setText(String.valueOf(Math.round(waypoint.z)));

        colorButton = new Button("", this::openColorPicker);
        colorButton.setDrawBackground(false);

        enabledToggle = new Toggle("Enabled", () -> waypoint.enabled, val -> waypoint.enabled = val);

        scaleSlider = new Slider("Scale", 0.5, 3.0, 0.1,
                () -> waypoint.scale,
                val -> waypoint.scale = val);

        showBgToggle = new Toggle("Show BG", () -> waypoint.showBg, val -> waypoint.showBg = val);
        bgColorButton = new Button("", this::openBgColorPicker);
        bgColorButton.setDrawBackground(false);

        showOutlineToggle = new Toggle("Show Outline", () -> waypoint.showOutline, val -> waypoint.showOutline = val);
        outlineColorButton = new Button("", this::openOutlineColorPicker);
        outlineColorButton.setDrawBackground(false);

        shapeDropdown = new ModeDropdown(new String[]{"Circle", "Square", "Rounded"},
                waypoint.shape, selected -> waypoint.shape = selected);

        saveButton = new Button("Save", () -> {
            waypoint.name = nameField.getText();
            try {
                waypoint.x = Double.parseDouble(xField.getText());
                waypoint.y = Double.parseDouble(yField.getText());
                waypoint.z = Double.parseDouble(zField.getText());
            } catch (NumberFormatException e) {}
            waypoint.color = tempColor;
            waypoint.bgColor = tempBgColor;
            waypoint.outlineColor = tempOutlineColor;
            manager.update(waypoint);
            onClose.run();
        });

        cancelButton = new Button("Cancel", onClose);
    }

    private void openColorPicker() {
        if (colorPickerOpen) return;
        colorPickerOpen = true;
        SettingColor tempSetting = new SettingColor("temp", new Color(tempColor));
        HsvColorPicker picker = new HsvColorPicker(tempSetting, () -> {
            tempColor = tempSetting.getCurrentColor().getPacked();
        });
        openPickerWindow("color_picker_" + waypoint.id, picker, () -> colorPickerOpen = false);
    }

    private void openBgColorPicker() {
        if (bgColorPickerOpen) return;
        bgColorPickerOpen = true;
        SettingColor tempSetting = new SettingColor("temp", new Color(tempBgColor));
        HsvColorPicker picker = new HsvColorPicker(tempSetting, () -> {
            tempBgColor = tempSetting.getCurrentColor().getPacked();
        });
        openPickerWindow("bg_color_picker_" + waypoint.id, picker, () -> bgColorPickerOpen = false);
    }

    private void openOutlineColorPicker() {
        if (outlineColorPickerOpen) return;
        outlineColorPickerOpen = true;
        SettingColor tempSetting = new SettingColor("temp", new Color(tempOutlineColor));
        HsvColorPicker picker = new HsvColorPicker(tempSetting, () -> {
            tempOutlineColor = tempSetting.getCurrentColor().getPacked();
        });
        openPickerWindow("outline_color_picker_" + waypoint.id, picker, () -> outlineColorPickerOpen = false);
    }

    private void openPickerWindow(String id, HsvColorPicker picker, Runnable onClosePicker) {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        factory.openPopupWindow(id, "Choose Color", 100, 100, 300, 400,
                new UiComponent() {
                    @Override
                    public Rect getBounds() {
                        return picker.getBounds(); // <-- TÄRKEÄ: palautetaan pickerin bounds
                    }
                    @Override
                    public void setBounds(Rect bounds) {
                        picker.setBounds(bounds);
                    }
                    @Override
                    public int getPreferredHeight() {
                        return picker.getPreferredHeight();
                    }
                    @Override
                    public void render(UiContext ui, int mx, int my, float delta) {
                        picker.render(ui, mx, my, delta);
                    }
                    @Override
                    public boolean mouseClicked(UiContext ui, double mx, double my, int btn) {
                        return picker.mouseClicked(ui, mx, my, btn);
                    }
                    @Override
                    public void mouseReleased(UiContext ui, double mx, double my, int btn) {
                        picker.mouseReleased(ui, mx, my, btn);
                        onClosePicker.run();
                    }
                    @Override
                    public boolean mouseDragged(UiContext ui, double mx, double my, int btn, double dx, double dy) {
                        return picker.mouseDragged(ui, mx, my, btn, dx, dy);
                    }
                    @Override
                    public boolean mouseScrolled(UiContext ui, double mx, double my, double amt) {
                        return picker.mouseScrolled(ui, mx, my, amt);
                    }
                    @Override
                    public boolean keyPressed(UiContext ui, int kc, int sc, int mod) {
                        return picker.keyPressed(ui, kc, sc, mod);
                    }
                    @Override
                    public boolean charTyped(UiContext ui, char c, int mod) {
                        return picker.charTyped(ui, c, mod);
                    }
                });
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        int padding = 8;
        int w = bounds.w - 2 * padding;
        int fieldH = 18;
        int spacing = 4;
        int half = (w - spacing) / 2;

        // Aloitetaan yläreunasta
        int y = bounds.y + padding;

        // Nimi
        nameField.setBounds(new Rect(bounds.x + padding, y, w, fieldH));
        y += fieldH + spacing;

        // Koordinaatit riviin
        int third = (w - 2*spacing)/3;
        xField.setBounds(new Rect(bounds.x + padding, y, third, fieldH));
        yField.setBounds(new Rect(bounds.x + padding + third + spacing, y, third, fieldH));
        zField.setBounds(new Rect(bounds.x + padding + 2*(third + spacing), y, third, fieldH));
        y += fieldH + spacing;

        // Väri ja enabled
        colorButton.setBounds(new Rect(bounds.x + padding, y, half, fieldH));
        enabledToggle.setBounds(new Rect(bounds.x + padding + half + spacing, y, half, fieldH));
        y += fieldH + spacing;

        // Scale
        scaleSlider.setBounds(new Rect(bounds.x + padding, y, w, 26));
        y += 26 + spacing;

        // Show BG ja väri
        showBgToggle.setBounds(new Rect(bounds.x + padding, y, half, fieldH));
        bgColorButton.setBounds(new Rect(bounds.x + padding + half + spacing, y, half, fieldH));
        y += fieldH + spacing;

        // Show Outline ja väri
        showOutlineToggle.setBounds(new Rect(bounds.x + padding, y, half, fieldH));
        outlineColorButton.setBounds(new Rect(bounds.x + padding + half + spacing, y, half, fieldH));
        y += fieldH + spacing;

        // Shape
        shapeDropdown.setBounds(new Rect(bounds.x + padding, y, w, fieldH));
        // y:tä ei enää kasvateta, koska napit sijoitetaan alareunaan

        // Napit alareunaan
        int buttonY = bounds.y + bounds.h - fieldH - padding;
        saveButton.setBounds(new Rect(bounds.x + padding, buttonY, half, fieldH));
        cancelButton.setBounds(new Rect(bounds.x + padding + half + spacing, buttonY, half, fieldH));
    }

    @Override
    public int getPreferredHeight() { return 350; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        ui.fill(bounds, ui.theme.panel);
        nameField.render(ui, mouseX, mouseY, delta);
        xField.render(ui, mouseX, mouseY, delta);
        yField.render(ui, mouseX, mouseY, delta);
        zField.render(ui, mouseX, mouseY, delta);

        Rect cb = colorButton.getBounds();
        Renderer2D.COLOR.quad(cb.x, cb.y, cb.w, cb.h, new Color(tempColor));
        if (cb.contains(mouseX, mouseY))
            Renderer2D.COLOR.boxLines(cb.x, cb.y, cb.w, cb.h, new Color(0xFFFFFFFF));

        enabledToggle.render(ui, mouseX, mouseY, delta);
        scaleSlider.render(ui, mouseX, mouseY, delta);
        showBgToggle.render(ui, mouseX, mouseY, delta);

        Rect bgcb = bgColorButton.getBounds();
        Renderer2D.COLOR.quad(bgcb.x, bgcb.y, bgcb.w, bgcb.h, new Color(tempBgColor));
        if (bgcb.contains(mouseX, mouseY))
            Renderer2D.COLOR.boxLines(bgcb.x, bgcb.y, bgcb.w, bgcb.h, new Color(0xFFFFFFFF));

        showOutlineToggle.render(ui, mouseX, mouseY, delta);
        Rect olcb = outlineColorButton.getBounds();
        Renderer2D.COLOR.quad(olcb.x, olcb.y, olcb.w, olcb.h, new Color(tempOutlineColor));
        if (olcb.contains(mouseX, mouseY))
            Renderer2D.COLOR.boxLines(olcb.x, olcb.y, olcb.w, olcb.h, new Color(0xFFFFFFFF));

        shapeDropdown.render(ui, mouseX, mouseY, delta);
        saveButton.render(ui, mouseX, mouseY, delta);
        cancelButton.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (nameField.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (xField.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (yField.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (zField.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (colorButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (enabledToggle.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (scaleSlider.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (showBgToggle.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (bgColorButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (showOutlineToggle.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (outlineColorButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (shapeDropdown.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (saveButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (cancelButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    public void mouseReleased(UiContext ui, double mx, double my, int btn) {
        nameField.mouseReleased(ui, mx, my, btn);
        xField.mouseReleased(ui, mx, my, btn);
        yField.mouseReleased(ui, mx, my, btn);
        zField.mouseReleased(ui, mx, my, btn);
        colorButton.mouseReleased(ui, mx, my, btn);
        enabledToggle.mouseReleased(ui, mx, my, btn);
        scaleSlider.mouseReleased(ui, mx, my, btn);
        showBgToggle.mouseReleased(ui, mx, my, btn);
        bgColorButton.mouseReleased(ui, mx, my, btn);
        showOutlineToggle.mouseReleased(ui, mx, my, btn);
        outlineColorButton.mouseReleased(ui, mx, my, btn);
        shapeDropdown.mouseReleased(ui, mx, my, btn);
        saveButton.mouseReleased(ui, mx, my, btn);
        cancelButton.mouseReleased(ui, mx, my, btn);
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mx, double my, int btn, double dx, double dy) {
        if (scaleSlider.mouseDragged(ui, mx, my, btn, dx, dy)) return true;
        return false;
    }

    @Override
    public boolean mouseScrolled(UiContext ui, double mx, double my, double amt) { return false; }

    @Override
    public boolean keyPressed(UiContext ui, int kc, int sc, int mod) {
        if (nameField.keyPressed(ui, kc, sc, mod)) return true;
        if (xField.keyPressed(ui, kc, sc, mod)) return true;
        if (yField.keyPressed(ui, kc, sc, mod)) return true;
        if (zField.keyPressed(ui, kc, sc, mod)) return true;
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char c, int mod) {
        if (nameField.charTyped(ui, c, mod)) return true;
        if (xField.charTyped(ui, c, mod)) return true;
        if (yField.charTyped(ui, c, mod)) return true;
        if (zField.charTyped(ui, c, mod)) return true;
        return false;
    }
}