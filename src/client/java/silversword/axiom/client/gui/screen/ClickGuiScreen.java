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
import silversword.axiom.client.gui.components.*;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.gui.window.Window;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.gui.window.WindowManager;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.utils.render.DrawTexture;

import java.util.List;

public final class ClickGuiScreen extends Screen {

    private final Theme theme;
    private enum TopMode { CLICKGUI, SETTINGS }
    private static final WindowManager WINDOW_MANAGER = new WindowManager();
    private final WindowManager windowManager = WINDOW_MANAGER;
    public static UiContext lastUi = null;
    private WindowFactory windowFactory;
    private TopMode topMode = TopMode.CLICKGUI;

    private static final int BTN_W = 80;
    private static final int BTN_H = 18;
    private static final int RESET_BTN_W = 60;
    private static final int RESET_BTN_H = 18;

    private ModuleSearchBar moduleSearchBar;
    private AxiomMod pendingHighlight = null;
    private long highlightStartTime = 0;
    private static final long HIGHLIGHT_DURATION = 1000;

    public static WindowFactory lastFactory = null;
    public static ModeDropdown currentDropdown = null;

    public ClickGuiScreen() {
        super(Component.literal("Axiom"));
        this.theme = ThemeManager.getCurrentTheme();
    }

    public static WindowManager getWindowManager() { return WINDOW_MANAGER; }
    public static void saveUiStatic() { UiConfigManager.saveGui(WINDOW_MANAGER); }
    public WindowFactory getWindowFactory() { return windowFactory; }

    @Override
    protected void init() {
        super.init();
        windowManager.clear();
        windowManager.closeOverlay();

        moduleSearchBar = new ModuleSearchBar(this::locateAndHighlightModule);

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
        // tyhjä
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        windowManager.updateAnimations();

        int width = ctx.guiWidth();
        int height = ctx.guiHeight();
        Matrix4f proj = new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
        Renderer2D renderer = new Renderer2D(ctx, RenderAPI.getInstance().getCore(), proj);
        lastUi = new UiContext(this.minecraft, ctx, theme, delta, renderer);

        TextRenderer.get().begin(1.0, false, false);

        if (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen()) {
            windowManager.render(lastUi, mouseX, mouseY);
        }

        boolean isPopup = windowManager.isOverlayOpen() || topMode == TopMode.SETTINGS;
        if (!isPopup) {
            drawTopBar(lastUi, mouseX, mouseY);
        }
        drawSearchBar(lastUi, mouseX, mouseY, delta);

        TextRenderer.get().end();
        RenderAPI.getInstance().getCore().flush();
        super.render(ctx, mouseX, mouseY, delta);

        RenderCore core = RenderAPI.getInstance().getCore();
        boolean wasScissor = core.isScissorEnabled();
        int sx = core.getScissorX();
        int sy = core.getScissorY();
        int sw = core.getScissorW();
        int sh = core.getScissorH();

        if (wasScissor) {
            core.enableScissor(sx, sy, sw, sh);
        }

        DrawTexture.renderAll();

        core.flush();

        if (wasScissor) {
            core.enableScissor(sx, sy, sw, sh);
        } else {
            core.disableScissor();
        }

        TooltipStack.renderAll(lastUi);
        core.flush();
    }

    private void drawRoundedButton(UiContext ui, int x, int y, int w, int h, int bgColor, int borderColor, int radius) {
        int alphaBg = (bgColor & 0x00FFFFFF) | 0x80000000;
        int alphaBorder = (borderColor & 0x00FFFFFF) | 0xC0000000;
        ui.fillRounded(x, y, w, h, alphaBorder, radius);
        ui.fillRounded(x + 1, y + 1, w - 2, h - 2, alphaBg, Math.max(0, radius - 1));
    }

    private void drawTopBar(UiContext ui, int mouseX, int mouseY) {
        int toggleW = 70;
        int toggleH = 16;
        int gap = 4;
        int centerX = this.width / 2;
        int topY = 6;
        int radius = theme.radius;

        int clickGuiX = centerX - toggleW - gap / 2;
        int settingsX = centerX + gap / 2;

        boolean hoverClickGui = mouseX >= clickGuiX && mouseX <= clickGuiX + toggleW && mouseY >= topY && mouseY <= topY + toggleH;
        int clickGuiBg = (topMode == TopMode.CLICKGUI) ? theme.accent : (hoverClickGui ? theme.buttonHover : theme.button);
        drawRoundedButton(ui, clickGuiX, topY, toggleW, toggleH, clickGuiBg, theme.border, radius);
        ui.text("ClickGUI", clickGuiX + 8, topY + 4, ui.theme.text);

        boolean hoverSettings = mouseX >= settingsX && mouseX <= settingsX + toggleW && mouseY >= topY && mouseY <= topY + toggleH;
        int settingsBg = (topMode == TopMode.SETTINGS) ? theme.accent : (hoverSettings ? theme.buttonHover : theme.button);
        drawRoundedButton(ui, settingsX, topY, toggleW, toggleH, settingsBg, theme.border, radius);
        ui.text("Settings", settingsX + 10, topY + 4, ui.theme.text);

        if (topMode == TopMode.SETTINGS) {
            int btnY = topY + toggleH + 6;
            int spacing = 6;
            int totalW = RESET_BTN_W + BTN_W * 2 + spacing * 2;
            int startX = (this.width - totalW) / 2;

            int hudEditX = startX;
            int hudCompX = hudEditX + BTN_W + spacing;
            int resetX = hudCompX + BTN_W + spacing;

            boolean hoverEdit = mouseX >= hudEditX && mouseX <= hudEditX + BTN_W && mouseY >= btnY && mouseY <= btnY + BTN_H;
            drawRoundedButton(ui, hudEditX, btnY, BTN_W, BTN_H, hoverEdit ? theme.buttonHover : theme.button, theme.border, radius);
            ui.text("HUD Edit", hudEditX + 12, btnY + 5, ui.theme.text);

            boolean hoverComp = mouseX >= hudCompX && mouseX <= hudCompX + BTN_W && mouseY >= btnY && mouseY <= btnY + BTN_H;
            drawRoundedButton(ui, hudCompX, btnY, BTN_W, BTN_H, hoverComp ? theme.buttonHover : theme.button, theme.border, radius);
            ui.text("Components", hudCompX + 8, btnY + 5, ui.theme.text);

            boolean resetHover = mouseX >= resetX && mouseX <= resetX + RESET_BTN_W && mouseY >= btnY && mouseY <= btnY + RESET_BTN_H;
            drawRoundedButton(ui, resetX, btnY, RESET_BTN_W, RESET_BTN_H, resetHover ? theme.buttonHover : theme.button, theme.border, radius);
            ui.text("Reset", resetX + 12, btnY + 5, ui.theme.text);
        }
    }

    private void drawSearchBar(UiContext ui, int mouseX, int mouseY, float delta) {
        if (topMode != TopMode.CLICKGUI) return;
        int barWidth = 180;
        int barHeight = 16;
        int centerX = this.width / 2;
        int topY = 6 + 16 + 6;
        moduleSearchBar.setBounds(new Rect(centerX - barWidth/2, topY, barWidth, barHeight));
        moduleSearchBar.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void saveUi() { UiConfigManager.saveGui(windowManager); }

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
            int toggleW = 70;
            int toggleH = 16;
            int gap = 4;
            int centerX = this.width / 2;
            int topY = 6;

            int clickGuiX = centerX - toggleW - gap / 2;
            int settingsX = centerX + gap / 2;

            if (currentDropdown != null) {
                if (currentDropdown.getBounds().contains(mouseX, mouseY)) {
                    if (currentDropdown.mouseClicked(lastUi, mouseX, mouseY, click.button())) return true;
                } else currentDropdown = null;
            }

            if (mouseX >= clickGuiX && mouseX <= clickGuiX + toggleW && mouseY >= topY && mouseY <= topY + toggleH) {
                topMode = TopMode.CLICKGUI;
                return true;
            }
            if (mouseX >= settingsX && mouseX <= settingsX + toggleW && mouseY >= topY && mouseY <= topY + toggleH) {
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

                if (mouseX >= hudEditX && mouseX <= hudEditX + BTN_W && mouseY >= btnY && mouseY <= btnY + BTN_H) {
                    this.onClose();
                    this.minecraft.setScreen(new HudEditScreen());
                    return true;
                }
                if (mouseX >= hudCompX && mouseX <= hudCompX + BTN_W && mouseY >= btnY && mouseY <= btnY + BTN_H) {
                    HudComponentsList list = new HudComponentsList();
                    windowFactory.openCustomWindow("hud_components", "HUD Components", this.width, this.height, list);
                    return true;
                }
                if (mouseX >= resetX && mouseX <= resetX + RESET_BTN_W && mouseY >= btnY && mouseY <= btnY + RESET_BTN_H) {
                    resetWindows();
                    return true;
                }
            }

            // Hakupalkin klikkaus - käytä moduleSearchBar
            // Etsi tämä kohta ClickGuiScreen.java -tiedostosta:
            if (topMode == TopMode.CLICKGUI) {

                if (moduleSearchBar.mouseClicked(lastUi, mouseX, mouseY, click.button())) return true;
            }
        }

        boolean consumed = super.mouseClicked(click, doubled);
        if (!consumed && lastUi != null && (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen())) {
            consumed = windowManager.mouseClicked(lastUi, mouseX, mouseY, click.button());
        }
        return consumed;
    }

    private void locateAndHighlightModule(AxiomMod module) {
        if (module == null) return;
        String targetCategory = module.getCategory().name();
        for (Window win : windowManager.getWindows()) {
            if (win.id.startsWith("category:") && win.id.endsWith(targetCategory.toLowerCase())) {
                if (!win.getChildren().isEmpty()) {
                    UiComponent listView = win.getChildren().get(0);
                    if (listView instanceof ModuleListView moduleListView) {
                        int index = moduleListView.getModuleRowIndex(module);
                        if (index >= 0) {
                            moduleListView.scrollToIndex(index);
                            List<UiComponent> rows = moduleListView.getScrollChildren();
                            if (index < rows.size() && rows.get(index) instanceof ModuleRow row) {
                                row.highlight(1000);
                            }
                        }
                    }
                }
                if (win.isMinimized()) win.toggleMinimize();
                break;
            }
        }
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
        String[] categories = new String[] { "Movement", "Combat", "Player", "World", "Render", "Misc" };
        int count = categories.length;
        int winW = 100;
        int winH = Math.max(160, screenH - 70);
        int margin = 6;
        int gapBetweenWindows = 4;
        int startY = 40;

        for (int i = 0; i < count; i++) {
            String cat = categories[i];
            String id = "category:" + cat.toLowerCase();
            int x = margin + i * (winW + gapBetweenWindows);

            Window win = new Window(id, cat, x, startY, winW, winH);
            win.setClosable(false);
            win.setMinimizable(true);

            ModuleListView list = new ModuleListView(
                    id,
                    () -> ModuleManager.getInstance().getModules(),
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
        if (input.input() == 256) {
            if (windowManager.isOverlayOpen()) {
                windowManager.closeOverlay();
                return true;
            } else {
                this.onClose();
                return true;
            }
        }
        // Käytä moduleSearchBar
        if (topMode == TopMode.CLICKGUI && moduleSearchBar.keyPressed(lastUi, input.input(), input.scancode(), input.modifiers())) {
            return true;
        }
        if (lastUi != null && (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen())) {
            if (windowManager.keyPressed(lastUi, input.input(), input.scancode(), input.modifiers())) return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        // Käytä moduleSearchBar
        if (topMode == TopMode.CLICKGUI && moduleSearchBar.charTyped(lastUi, input.codepointAsString().charAt(0), input.modifiers())) {
            return true;
        }
        if (lastUi != null && input.isAllowedChatCharacter() && (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen())) {
            String s = input.codepointAsString();
            for (int i = 0; i < s.length(); i++) {
                windowManager.charTyped(lastUi, s.charAt(i), input.modifiers());
            }
            return true;
        }
        return false;
    }
}