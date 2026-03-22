package silversword.axiom.client.gui.screen;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import silversword.axiom.client.config.HudConfigManager;
import silversword.axiom.client.config.SettingsConfigManager;
import silversword.axiom.client.config.UiConfigManager;
import silversword.axiom.client.gui.components.HudComponentsList;
import silversword.axiom.client.gui.components.ModeDropdown;
import silversword.axiom.client.gui.components.ModuleListView;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.gui.window.Window;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.gui.window.WindowManager;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;
import silversword.axiom.client.utils.render.DrawTexture;

public final class ClickGuiScreen extends Screen {

    private final Theme theme;

    private enum TopMode {
        CLICKGUI,
        SETTINGS
    }

    private static final WindowManager WINDOW_MANAGER = new WindowManager();
    private final WindowManager windowManager = WINDOW_MANAGER;

    public static UiContext lastUi = null;
    private WindowFactory windowFactory;
    private TopMode topMode = TopMode.CLICKGUI;

    private static final int BTN_W = 90;
    private static final int BTN_H = 20;
    private static final int RESET_BTN_W = 70;
    private static final int RESET_BTN_H = 20;

    public static WindowFactory lastFactory = null;
    public static ModeDropdown currentDropdown = null;

    public ClickGuiScreen() {
        super(Component.literal("Axiom"));
        this.theme = ThemeManager.getCurrentTheme();
    }

    public static WindowManager getWindowManager() {
        return WINDOW_MANAGER;
    }

    public static void saveUiStatic() {
        UiConfigManager.saveGui(WINDOW_MANAGER);
    }

    public WindowFactory getWindowFactory() {
        return windowFactory;
    }

    @Override
    protected void init() {
        super.init();
        windowManager.clear();
        windowManager.closeOverlay();

        int sw = this.minecraft.getWindow().getGuiScaledWidth();
        int sh = this.minecraft.getWindow().getGuiScaledHeight();

        windowFactory = new WindowFactory(windowManager);
        lastFactory = windowFactory;

        createCategoryWindows(sw, sh);
        UiConfigManager.loadGui(windowManager);
        HudConfigManager.load(HudManager.get());
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        // Älä tee mitään
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {


        // Tallenna alkuperäinen projektio
        Matrix4f savedProj = new Matrix4f(RenderUtils.projection);

        RenderUtils.rendering3D = false;
        RenderUtils.setup2DProjection(this.width, this.height);

        windowManager.updateAnimations();

        Renderer2D.COLOR.begin();

        lastUi = new UiContext(this.minecraft, ctx, theme, delta);
        TextRenderer.get().begin(1.0, false, false);

        if (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen()) {
            windowManager.render(lastUi, mouseX, mouseY);
        }

        drawTopBar(lastUi, mouseX, mouseY);

        Renderer2D.COLOR.render();



        super.render(ctx, mouseX, mouseY, delta);

        DrawTexture.renderAll();


        TextRenderer.get().end();

        TooltipStack.renderAll(lastUi);

        RenderUtils.projection.set(savedProj);

        RenderUtils.rendering3D = true;
    }

    private void drawRoundedButton(UiContext ui, int x, int y, int w, int h, int bgColor, int borderColor, int radius) {
        // Reunus (piirretään ensin)
        Renderer2D.COLOR.drawRoundedRect(x, y, w, h, radius, new Color(borderColor));
        // Täyttö (hieman pienempi, jotta reunus jää näkyviin)
        Renderer2D.COLOR.drawRoundedRect(x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), new Color(bgColor));
    }

    private void drawTopBar(UiContext ui, int mouseX, int mouseY) {
        int toggleW = 80;
        int toggleH = 18;
        int gap = 4;
        int centerX = this.width / 2;
        int topY = 8;
        int radius = theme.radius;

        int clickGuiX = centerX - toggleW - gap / 2;
        int settingsX = centerX + gap / 2;

        // ClickGUI-nappi
        boolean hoverClickGui = mouseX >= clickGuiX && mouseX <= clickGuiX + toggleW && mouseY >= topY && mouseY <= topY + toggleH;
        int clickGuiBg = (topMode == TopMode.CLICKGUI) ? theme.accent : (hoverClickGui ? theme.buttonHover : theme.button);
        drawRoundedButton(ui, clickGuiX, topY, toggleW, toggleH, clickGuiBg, theme.border, radius);
        ui.text("ClickGUI", clickGuiX + 10, topY + 5, ui.theme.text);

        // Settings-nappi
        boolean hoverSettings = mouseX >= settingsX && mouseX <= settingsX + toggleW && mouseY >= topY && mouseY <= topY + toggleH;
        int settingsBg = (topMode == TopMode.SETTINGS) ? theme.accent : (hoverSettings ? theme.buttonHover : theme.button);
        drawRoundedButton(ui, settingsX, topY, toggleW, toggleH, settingsBg, theme.border, radius);
        ui.text("Settings", settingsX + 12, topY + 5, ui.theme.text);

        // HUD-painikkeet (vain Settings-tilassa)
        if (topMode == TopMode.SETTINGS) {
            int btnY = topY + toggleH + 6;
            int spacing = 6;
            int totalW = RESET_BTN_W + BTN_W * 2 + spacing * 2;
            int startX = (this.width - totalW) / 2;

            int hudEditX = startX;
            int hudCompX = hudEditX + BTN_W + spacing;
            int resetX = hudCompX + BTN_W + spacing;

            // HUD Edit
            boolean hoverEdit = mouseX >= hudEditX && mouseX <= hudEditX + BTN_W && mouseY >= btnY && mouseY <= btnY + BTN_H;
            drawRoundedButton(ui, hudEditX, btnY, BTN_W, BTN_H, hoverEdit ? theme.buttonHover : theme.button, theme.border, radius);
            ui.text("HUD Edit", hudEditX + 18, btnY + 6, ui.theme.text);

            // Components
            boolean hoverComp = mouseX >= hudCompX && mouseX <= hudCompX + BTN_W && mouseY >= btnY && mouseY <= btnY + BTN_H;
            drawRoundedButton(ui, hudCompX, btnY, BTN_W, BTN_H, hoverComp ? theme.buttonHover : theme.button, theme.border, radius);
            ui.text("Components", hudCompX + 10, btnY + 6, ui.theme.text);

            // Reset
            boolean resetHover = mouseX >= resetX && mouseX <= resetX + RESET_BTN_W && mouseY >= btnY && mouseY <= btnY + RESET_BTN_H;
            drawRoundedButton(ui, resetX, btnY, RESET_BTN_W, RESET_BTN_H, resetHover ? theme.buttonHover : theme.button, theme.border, radius);
            ui.text("Reset", resetX + 14, btnY + 6, ui.theme.text);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void saveUi() {
        UiConfigManager.saveGui(windowManager);
    }

    @Override
    public void removed() {
        UiConfigManager.saveGui(windowManager);
        SettingsConfigManager.saveAll();
        HudConfigManager.save(HudManager.get());
        windowManager.closeOverlay();
        saveUi();
        super.removed();
    }

    @Override
    public void onClose() {
        saveUi();
        HudConfigManager.save(HudManager.get());
        super.onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();

        if (click.button() == 0) {
            int toggleW = 80;
            int toggleH = 18;
            int gap = 4;
            int centerX = this.width / 2;
            int topY = 8;

            int clickGuiX = centerX - toggleW - gap / 2;
            int settingsX = centerX + gap / 2;

            if (currentDropdown != null) {
                if (currentDropdown.getBounds().contains(mouseX, mouseY)) {
                    boolean handled = currentDropdown.mouseClicked(lastUi, mouseX, mouseY, click.button());
                    if (handled) return true;
                } else {
                    currentDropdown = null;
                }
            }

            if (mouseX >= clickGuiX && mouseX <= clickGuiX + toggleW &&
                    mouseY >= topY && mouseY <= topY + toggleH) {
                topMode = TopMode.CLICKGUI;
                return true;
            }
            if (mouseX >= settingsX && mouseX <= settingsX + toggleW &&
                    mouseY >= topY && mouseY <= topY + toggleH) {
                topMode = TopMode.SETTINGS;
                return true;
            }

            if (topMode == TopMode.SETTINGS) {
                int btnY = topY + toggleH + 6;
                int spacing = 6;
                int totalW = RESET_BTN_W + BTN_W * 2 + spacing * 2;
                int startX = (this.width - totalW) / 2;

                int hudEditX = startX;
                int hudCompX = hudEditX + BTN_W + spacing;
                int resetX = hudCompX + BTN_W + spacing;

                if (mouseX >= hudEditX && mouseX <= hudEditX + BTN_W &&
                        mouseY >= btnY && mouseY <= btnY + BTN_H) {
                    this.onClose();
                    this.minecraft.setScreen(new HudEditScreen());
                    return true;
                }

                if (mouseX >= hudCompX && mouseX <= hudCompX + BTN_W &&
                        mouseY >= btnY && mouseY <= btnY + BTN_H) {
                    HudComponentsList list = new HudComponentsList();
                    windowFactory.openCustomWindow("hud_components", "HUD Components",
                            this.width, this.height, list);
                    return true;
                }

                if (mouseX >= resetX && mouseX <= resetX + RESET_BTN_W &&
                        mouseY >= btnY && mouseY <= btnY + RESET_BTN_H) {
                    resetWindows();
                    return true;
                }
            }
        }

        boolean consumed = super.mouseClicked(click, doubled);

        if (!consumed && lastUi != null &&
                (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen())) {
            consumed = windowManager.mouseClicked(lastUi, mouseX, mouseY, click.button());
        }

        return consumed;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        super.mouseReleased(click);

        if (lastUi != null && (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen())) {
            windowManager.mouseReleased(lastUi, click.x(), click.y(), click.button());
            return true;
        }
        return false;
    }

    private void resetWindows() {
        windowManager.clear();
        windowManager.closeOverlay();
        createCategoryWindows(this.width, this.height);
        UiConfigManager.saveGui(windowManager);
    }

    private void createCategoryWindows(int screenW, int screenH) {
        String[] categories = new String[] {
                "Movement", "Combat", "Player", "World", "Render", "Misc"
        };

        int count = categories.length;
        int margin = 10;
        int availableW = Math.max(1, screenW - margin * 2);
        int winW = Math.max(140, availableW / count - 4);
        int winH = Math.max(180, screenH - 80);
        int startY = 40;

        for (int i = 0; i < count; i++) {
            String cat = categories[i];
            String id = "category:" + cat.toLowerCase();

            int x = margin + i * (winW + 4);

            Window win = new Window(id, cat, x, startY, winW, winH);
            win.setClosable(false);
            win.setMinimizable(true);

            ModuleListView list = new ModuleListView(
                    id,
                    () -> ModuleManager.getInstance().getModules(),
                    () -> "",
                    () -> cat,
                    mod -> windowFactory.openSettingsWindow(mod, screenW, screenH)
            );

            win.clearChildren();
            win.add(list);

            windowManager.add(win);
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (lastUi != null && (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen())) {
            windowManager.mouseDragged(lastUi, click.x(), click.y(), click.button(), offsetX, offsetY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (lastUi != null && (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen())) {
            windowManager.mouseScrolled(lastUi, mouseX, mouseY, verticalAmount);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.input() == 256) { // ESC-näppäin
            if (windowManager.isOverlayOpen()) {
                windowManager.closeOverlay(); // sulkee päällimmäisen overlayn ja palaa edelliseen
                return true;
            } else {
                this.onClose(); // sulkee koko GUI:n
                return true;
            }
        }

        if (lastUi != null && (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen())) {
            if (windowManager.keyPressed(lastUi, input.input(), input.scancode(), input.modifiers())) {
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (lastUi != null && input.isAllowedChatCharacter() &&
                (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen())) {
            String s = input.codepointAsString();
            for (int i = 0; i < s.length(); i++) {
                windowManager.charTyped(lastUi, s.charAt(i), input.modifiers());
            }
            return true;
        }
        return false;
    }

    private int accentColor() {
        try {
            var f = theme.getClass().getDeclaredField("accentColor");
            f.setAccessible(true);
            Object v = f.get(theme);
            if (v instanceof Integer i) return i;
        } catch (Throwable ignored) {}
        try {
            var m = theme.getClass().getDeclaredMethod("getAccentColor");
            m.setAccessible(true);
            Object v = m.invoke(theme);
            if (v instanceof Integer i) return i;
        } catch (Throwable ignored) {}
        try {
            var m = theme.getClass().getDeclaredMethod("accentColor");
            m.setAccessible(true);
            Object v = m.invoke(theme);
            if (v instanceof Integer i) return i;
        } catch (Throwable ignored) {}
        return 0xFF8E2DE2;
    }
}