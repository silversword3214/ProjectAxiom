package silversword.axiom.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.lwjgl.sdl.SDLKeyboard;
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
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.utils.KeyNames;
import silversword.axiom.client.utils.render.DrawTexture;

import java.util.List;

public final class ClickGuiScreen extends Screen {

    private final Theme theme;
    private enum TopMode { CLICKGUI }
    private static final WindowManager WINDOW_MANAGER = new WindowManager();
    private final WindowManager windowManager = WINDOW_MANAGER;
    public static UiContext lastUi = null;
    private WindowFactory windowFactory;
    private TopMode topMode = TopMode.CLICKGUI;

    private ModuleSearchBar moduleSearchBar;

    private long lastOverlayCloseTime = 0;
    private static final long OVERLAY_ESC_COOLDOWN_MS = 250;

    public static WindowFactory lastFactory = null;
    public static ModeDropdown currentDropdown = null;

    // Estää saman merkin tuplasyötön (keyPressed hoitaa, charTyped dedupataan)
    private long lastKeyHandledCharNs = 0L;
    private static final long CHAR_DEDUP_WINDOW_NS = 100_000_000L;

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
        UiConfigManager.loadGui(windowManager, sw, sh);
        HudConfigManager.load(HudManager.get());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        // tyhjä
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        windowManager.updateAnimations();

        int width = ctx.guiWidth();
        int height = ctx.guiHeight();

        Matrix4f proj = new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
        Renderer2D renderer = new Renderer2D(ctx, RenderAPI.getInstance().getCore(), proj);

        lastUi = new UiContext(this.minecraft, ctx, theme, delta, renderer);

        if (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen()) {
            windowManager.render(lastUi, mouseX, mouseY);
        }
        if (!windowManager.isOverlayOpen()) {
            drawTopBar(lastUi, mouseX, mouseY);
            drawSearchBar(lastUi, mouseX, mouseY, delta);
        }
        if (currentDropdown != null) {
            currentDropdown.render(lastUi, mouseX, mouseY, delta);
        }

        RenderCore core = RenderAPI.getInstance().getCore();
        boolean wasScissor = core.isScissorEnabled();
        int sx = core.getScissorX();
        int sy = core.getScissorY();
        int sw = core.getScissorW();
        int sh = core.getScissorH();

        if (wasScissor) core.enableScissor(sx, sy, sw, sh);
        DrawTexture.renderAll(renderer);
        if (wasScissor) core.enableScissor(sx, sy, sw, sh);
        else core.disableScissor();

        TooltipStack.renderAll(lastUi);
        lastUi.renderTexts();
        core.flush();
    }

    private void drawRoundedButton(UiContext ui, int x, int y, int w, int h, int bgColor, int borderColor, int radius) {
        int alphaBg = (bgColor & 0x00FFFFFF) | 0x80000000;
        int alphaBorder = (borderColor & 0x00FFFFFF) | 0xC0000000;
        ui.fillRounded(x, y, w, h, alphaBorder, radius);
        ui.fillRounded(x + 1, y + 1, w - 2, h - 2, alphaBg, Math.max(0, radius - 1));
    }

    private void drawTopBar(UiContext ui, int mouseX, int mouseY) {
        int toggleW = 70, toggleH = 16, gap = 4;
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
        int settingsBg = hoverSettings ? theme.buttonHover : theme.button;
        drawRoundedButton(ui, settingsX, topY, toggleW, toggleH, settingsBg, theme.border, radius);
        ui.text("Settings", settingsX + 10, topY + 4, ui.theme.text);
    }

    private void drawSearchBar(UiContext ui, int mouseX, int mouseY, float delta) {
        if (topMode != TopMode.CLICKGUI) return;
        int barWidth = 180, barHeight = 16;
        int centerX = this.width / 2;
        int topY = 6 + 16 + 6;
        moduleSearchBar.setBounds(new Rect(centerX - barWidth / 2, topY, barWidth, barHeight));
        moduleSearchBar.render(ui, mouseX, mouseY, delta);
    }

    @Override public boolean isPauseScreen() { return false; }
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

        if (currentDropdown != null && lastUi != null) {
            if (currentDropdown.getBounds().contains(mouseX, mouseY)) {
                if (currentDropdown.mouseClicked(lastUi, mouseX, mouseY, click.button())) return true;
            } else {
                currentDropdown = null;
            }
        }

        // 26.3: vasen klikki == 1
        if (click.button() == 1) {
            if (lastUi == null) return super.mouseClicked(click, doubled);

            int toggleW = 70, toggleH = 16, gap = 4;
            int centerX = this.width / 2;
            int topY = 6;
            int clickGuiX = centerX - toggleW - gap / 2;
            int settingsX = centerX + gap / 2;

            if (mouseX >= clickGuiX && mouseX <= clickGuiX + toggleW && mouseY >= topY && mouseY <= topY + toggleH) {
                topMode = TopMode.CLICKGUI;
                return true;
            }
            if (mouseX >= settingsX && mouseX <= settingsX + toggleW && mouseY >= topY && mouseY <= topY + toggleH) {
                this.minecraft.setScreenAndShow(new AxiomSettingsScreen(() -> {
                    this.minecraft.setScreenAndShow(new ClickGuiScreen());
                }));
                return true;
            }

            if (topMode == TopMode.CLICKGUI) {
                if (moduleSearchBar.mouseClicked(lastUi, mouseX, mouseY, click.button())) return true;
            }
        }

        if (lastUi != null && (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen())) {
            if (windowManager.mouseClicked(lastUi, mouseX, mouseY, click.button())) return true;
        }
        return super.mouseClicked(click, doubled);
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

    public void resetWindows() {
        WINDOW_MANAGER.clear();
        windowManager.clear();
        windowManager.closeOverlay();
        createCategoryWindows(this.width, this.height);
        UiConfigManager.saveGui(windowManager);
        this.init();
    }

    private void createCategoryWindows(int screenW, int screenH) {
        String[] categories = { "Movement", "Combat", "Render", "World", "Player", "Misc", "Utility" };
        int[] xCoords = { 5, 119, 236, 610, 725, 837, 837 };
        int winW = 110, winH = 250, startY = 50;

        for (int i = 0; i < categories.length; i++) {
            String cat = categories[i];
            String id = "category:" + cat.toLowerCase();
            int x = xCoords[i];
            int y = startY - 40;
            if (cat.equals("Utility")) y = startY + winH - 25;

            Window win = new Window(id, cat, x, y, winW, winH);
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

    // ═════════════════════════════════════════════════════════════
    //  NÄPPÄIMISTÖ — 26.3 / SDL
    // ═════════════════════════════════════════════════════════════

    @Override
    public boolean keyPressed(KeyEvent input) {
        // 1) ESC: ensin unfokusoi hakupalkki, sitten sulje overlay, sitten GUI
        if (input.key() == InputConstants.KEY_ESCAPE) {
            if (moduleSearchBar != null && moduleSearchBar.isFocused()) {
                moduleSearchBar.setFocused(false);
                return true;
            }
            currentDropdown = null;
            if (windowManager.isOverlayOpen()) {
                windowManager.closeOverlay();
                lastOverlayCloseTime = System.currentTimeMillis();
                return true;
            }
            if (System.currentTimeMillis() - lastOverlayCloseTime < OVERLAY_ESC_COOLDOWN_MS) {
                return true;
            }
            this.onClose();
            return true;
        }

        // 2) Hakupalkki fokusoituna → hoida KAIKKI syöttö tässä
        if (topMode == TopMode.CLICKGUI && moduleSearchBar != null
                && moduleSearchBar.isFocused() && lastUi != null) {

            // 2a) Erikoisnäppäimet (BACKSPACE, ENTER)
            if (moduleSearchBar.keyPressed(lastUi, input.key(), input.keycode(), input.modifiers())) {
                return true;
            }

            // 2b) Merkkinäppäimet → suora syöttö
            if (tryHandleTyping(input)) {
                return true;
            }
        }

        // 3) WindowManager
        if (lastUi != null && (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen())) {
            if (windowManager.keyPressed(lastUi, input.key(), input.keycode(), input.modifiers())) return true;
        }
        return super.keyPressed(input);
    }

    /**
     * 26.3 / SDL: input.key() = SCANCODE, input.keycode() = SDL_Keycode.
     * SDL_GetKeyName palauttaa layout-tietoisen merkin (ä, ö, 1, !, jne.).
     */
    private boolean tryHandleTyping(KeyEvent input) {
        int scancode = input.key();
        int keycode = input.keycode();
        int modifiers = input.modifiers();
        boolean shift = (modifiers & InputConstants.MOD_SHIFT) != 0;

        // Välilyönti hoidetaan erikseen (SDL_GetKeyName palauttaa "Space")
        if (scancode == InputConstants.KEY_SPACE) {
            moduleSearchBar.appendChar(' ');
            lastKeyHandledCharNs = System.nanoTime();
            return true;
        }

        // Ensisijainen: SDL layout-tietoinen nimi keycodesta
        if (keycode != 0) {
            try {
                String name = SDLKeyboard.SDL_GetKeyName(keycode);
                if (name != null && name.codePointCount(0, name.length()) == 1) {
                    int cp = name.codePointAt(0);
                    // SDL palauttaa perusmerkin — shift pitää soveltaa itse
                    if (!shift && cp >= 'A' && cp <= 'Z') {
                        cp = Character.toLowerCase(cp);
                    } else if (shift && cp >= 'a' && cp <= 'z') {
                        cp = Character.toUpperCase(cp);
                    } else if (shift) {
                        // Yleisimmät shift-variantit (US-QWERTY)
                        switch (cp) {
                            case '1': cp = '!'; break;
                            case '2': cp = '@'; break;
                            case '3': cp = '#'; break;
                            case '4': cp = '$'; break;
                            case '5': cp = '%'; break;
                            case '6': cp = '^'; break;
                            case '7': cp = '&'; break;
                            case '8': cp = '*'; break;
                            case '9': cp = '('; break;
                            case '0': cp = ')'; break;
                            case '-': cp = '_'; break;
                            case '=': cp = '+'; break;
                            case '[': cp = '{'; break;
                            case ']': cp = '}'; break;
                            case '\\': cp = '|'; break;
                            case ';': cp = ':'; break;
                            case '\'': cp = '"'; break;
                            case ',': cp = '<'; break;
                            case '.': cp = '>'; break;
                            case '/': cp = '?'; break;
                            case '`': cp = '~'; break;
                        }
                    }
                    moduleSearchBar.appendChar((char) cp);
                    lastKeyHandledCharNs = System.nanoTime();
                    return true;
                }
            } catch (Throwable ignored) {
                // SDL ei tunne keycodea → fallback
            }
        }

        // Fallback: KeyNames (scancode-pohjainen, US-QWERTY)
        String fallback = KeyNames.get(scancode);
        if (fallback == null || fallback.length() != 1) return false;
        char c = fallback.charAt(0);
        if (!shift && c >= 'A' && c <= 'Z') c = Character.toLowerCase(c);
        moduleSearchBar.appendChar(c);
        lastKeyHandledCharNs = System.nanoTime();
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent input) {
        if (input.key() == InputConstants.KEY_ESCAPE) return true;
        return super.keyReleased(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        // Estä tuplasyöttö: jos keyPressed jo käsitteli merkin, ohita
        if (System.nanoTime() - lastKeyHandledCharNs < CHAR_DEDUP_WINDOW_NS) {
            return true;
        }
        if (lastUi == null) return super.charTyped(input);

        int cp = input.codepoint();
        if (cp <= 0) return false;

        if (topMode == TopMode.CLICKGUI && moduleSearchBar != null
                && moduleSearchBar.isFocused()) {
            moduleSearchBar.appendChar((char) cp);
            return true;
        }

        if (input.isAllowedChatCharacter() && (topMode == TopMode.CLICKGUI || windowManager.isOverlayOpen())) {
            windowManager.charTyped(lastUi, (char) cp, 0);
            return true;
        }
        return false;
    }
}