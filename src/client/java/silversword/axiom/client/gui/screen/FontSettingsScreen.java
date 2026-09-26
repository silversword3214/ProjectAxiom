package silversword.axiom.client.gui.screen;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.sdl.SDLKeyboard;
import silversword.axiom.client.config.FontConfigManager;
import silversword.axiom.client.gui.components.ScrollContainer;
import silversword.axiom.client.gui.components.SearchBar;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.rendersystem.axiomrenderer.font.Fonts;
import silversword.axiom.client.rendersystem.axiomrenderer.api.RenderAPI;
import silversword.axiom.client.rendersystem.axiomrenderer.api.Renderer2D;
import silversword.axiom.client.rendersystem.utils.color.Color;
import silversword.axiom.client.rendersystem.utils.texture.Texture;
import silversword.axiom.client.rendersystem.utils.texture.TextureManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class FontSettingsScreen extends Screen {

    private final Theme theme;
    private final Runnable onCloseCallback;
    private ScrollContainer scrollContainer;
    private SearchBar searchBar;
    private final List<FontEntryComponent> entryComponents = new ArrayList<>();
    private List<String> allFonts = new ArrayList<>();
    private String filterText = "";

    private static final Identifier CHECKBOX_ON = Identifier.fromNamespaceAndPath("projectaxiom", "textures/icons/checkbox_on.png");
    private static Texture texOn;

    private UiContext lastUi = null;

    private long lastKeyHandledCharNs = 0L;
    private static final long CHAR_DEDUP_WINDOW_NS = 100_000_000L;

    private static final File FONTS_DIR = new File(
            Minecraft.getInstance().gameDirectory, "config/axiom/fonts");

    // Layout
    private Rect openFolderRect = new Rect(0, 0, 0, 0);
    private int labelTextX = 0;
    private int labelTextY = 0;

    // Automaattinen refresh
    private long lastFolderState = Long.MIN_VALUE;
    private static final long FOLDER_CHECK_INTERVAL_MS = 500;
    private long lastFolderCheck = 0;

    public FontSettingsScreen(Runnable onCloseCallback) {
        super(Component.literal("Font Settings"));
        this.theme = ThemeManager.getCurrentTheme().copy();
        this.onCloseCallback = onCloseCallback;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {}

    @Override
    protected void init() {
        super.init();
        allFonts = Fonts.getAvailableFonts();

        if (texOn == null) texOn = TextureManager.getTexture(CHECKBOX_ON);

        if (!FONTS_DIR.exists()) {
            FONTS_DIR.mkdirs();
        }

        int containerWidth = Math.max(160, Math.min(560, this.width - 48));
        int containerX = (this.width - containerWidth) / 2;

        int searchBarY = 55;
        int searchBarHeight = 22;
        int bottomRowHeight = 20;
        int bottomRowY = this.height - 30 - bottomRowHeight;
        int containerY = searchBarY + searchBarHeight + 8;
        int containerHeight = Math.max(40, bottomRowY - containerY - 10);

        searchBar = new SearchBar(
                () -> filterText,
                (newText) -> {
                    filterText = newText;
                    refreshFontList();
                }
        );
        searchBar.setBounds(new Rect(containerX, searchBarY, containerWidth, searchBarHeight));

        // Open Folder -nappi oikeaan alakulmaan
        int openFolderWidth = 100;
        int rightEdge = containerX + containerWidth;

        openFolderRect = new Rect(
                rightEdge - openFolderWidth,
                bottomRowY,
                openFolderWidth,
                bottomRowHeight);

        scrollContainer = new ScrollContainer();
        scrollContainer.setBounds(new Rect(containerX, containerY, containerWidth, containerHeight));
        scrollContainer.setDrawBackground(true);
        scrollContainer.setGap(6);
        scrollContainer.setInnerPadding(8);

        refreshFontList();
        lastFolderState = computeFolderState();
    }

    @Override
    public void tick() {
        super.tick();
        long now = System.currentTimeMillis();
        if (now - lastFolderCheck < FOLDER_CHECK_INTERVAL_MS) return;
        lastFolderCheck = now;

        long state = computeFolderState();
        if (state != lastFolderState) {
            lastFolderState = state;
            reloadFontsFromDisk();
        }
    }

    private long computeFolderState() {
        if (!FONTS_DIR.isDirectory()) return 0L;
        File[] files = FONTS_DIR.listFiles();
        if (files == null) return 0L;
        long sum = files.length;
        for (File f : files) {
            sum = sum * 31 + f.getName().hashCode();
            sum = sum * 31 + f.length();
            sum = sum * 31 + f.lastModified();
        }
        return sum;
    }

    private void reloadFontsFromDisk() {
        try {
            Fonts.refresh();
            allFonts = Fonts.getAvailableFonts();
            refreshFontList();
        } catch (Throwable t) {
            System.err.println("[Axiom] Failed to reload fonts: " + t);
            t.printStackTrace();
        }
    }

    private void refreshFontList() {
        scrollContainer.clear();
        entryComponents.clear();

        List<String> filtered = allFonts.stream()
                .filter(name -> filterText.isEmpty() || name.toLowerCase().contains(filterText.toLowerCase()))
                .collect(Collectors.toList());

        for (String fontName : filtered) {
            FontEntryComponent entry = new FontEntryComponent(
                    fontName,
                    fontName.equals(Fonts.currentFontName),
                    () -> {
                        Fonts.setFont(fontName);
                        FontConfigManager.saveFont(fontName);
                        for (FontEntryComponent e : entryComponents) {
                            e.setSelected(e.getFontName().equals(fontName));
                        }
                    }
            );
            entryComponents.add(entry);
            scrollContainer.add(entry);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        Matrix4f proj = new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
        Renderer2D renderer = new Renderer2D(ctx, RenderAPI.getInstance().getCore(), proj);
        lastUi = new UiContext(this.minecraft, ctx, theme, delta, renderer);

        lastUi.fill(0, 0, width, height, 0xDD000000);

        String title = "Font Settings";
        lastUi.text(title, (width - lastUi.textWidth(title)) / 2, 22, theme.text);

        searchBar.render(lastUi, mouseX, mouseY, delta);
        scrollContainer.render(lastUi, mouseX, mouseY, delta);

        renderBottomRow(lastUi, mouseX, mouseY);

        lastUi.renderTexts();
    }

    private void renderBottomRow(UiContext ui, int mouseX, int mouseY) {
        // "Custom .ttf fonts" — napin vasemmalla, ei taustaa
        String label = "Custom .ttf fonts";
        int labelWidth = ui.textWidth(label);
        int labelX = openFolderRect.x - 12 - labelWidth;
        int labelY = openFolderRect.y + openFolderRect.h / 2 - ui.fontHeight() / 2 + 3;
        ui.text(label, labelX, labelY, ui.theme.textDim);

        // Open Folder -nappi
        boolean hover = openFolderRect.contains(mouseX, mouseY);
        int bg = hover ? ui.theme.buttonHover : ui.theme.button;
        int border = hover ? ui.theme.accent : ui.theme.border;

        ui.fillRounded(openFolderRect, bg, 4);
        ui.drawRoundedOutline(openFolderRect, border, 4, 1.0);

        String btnText = "Open Folder";
        int btnTextX = openFolderRect.x + (openFolderRect.w - ui.textWidth(btnText)) / 2;
        int btnTextY = openFolderRect.y + openFolderRect.h / 2 - ui.fontHeight() / 2 + 3;
        ui.text(btnText, btnTextX, btnTextY, hover ? ui.theme.accent : ui.theme.text);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();

        // Close-nappi
        int btnW = 70, btnH = 24, btnX = width - btnW - 16, btnY = 12;
        Rect btnRect = new Rect(btnX, btnY, btnW, btnH);
        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT && btnRect.contains(mx, my)) {
            onClose();
            return true;
        }

        // Open Folder
        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT && openFolderRect.contains(mx, my)) {
            openFontsFolder();
            return true;
        }

        if (lastUi != null) {
            if (searchBar.mouseClicked(lastUi, mx, my, click.button())) return true;
            if (scrollContainer != null && scrollContainer.mouseClicked(lastUi, mx, my, click.button())) return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void openFontsFolder() {
        try {
            if (!FONTS_DIR.exists()) FONTS_DIR.mkdirs();
            Blaze3D.openPath(FONTS_DIR.toPath());
        } catch (Throwable t) {
            System.err.println("[Axiom] Failed to open fonts folder: " + t);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == InputConstants.KEY_ESCAPE) {
            if (searchBar != null && searchBar.isFocused()) {
                searchBar.setFocused(false);
                return true;
            }
            onClose();
            return true;
        }

        if (lastUi != null && searchBar != null && searchBar.isFocused()) {
            if (searchBar.keyPressed(lastUi, input.key(), input.keycode(), input.modifiers())) {
                return true;
            }
            if (tryHandleTyping(input)) return true;
            return false;
        }

        if (lastUi != null && scrollContainer != null) {
            if (scrollContainer.keyPressed(lastUi, input.key(), input.keycode(), input.modifiers())) {
                return true;
            }
        }
        return super.keyPressed(input);
    }

    private boolean tryHandleTyping(KeyEvent input) {
        int scancode  = input.key();
        int keycode   = input.keycode();
        int modifiers = input.modifiers();
        boolean shift = (modifiers & InputConstants.MOD_SHIFT) != 0;

        if (scancode == InputConstants.KEY_SPACE) {
            searchBar.appendChar(' ');
            lastKeyHandledCharNs = System.nanoTime();
            return true;
        }

        if (keycode != 0) {
            try {
                String name = SDLKeyboard.SDL_GetKeyName(keycode);
                if (name != null && name.codePointCount(0, name.length()) == 1) {
                    int cp = name.codePointAt(0);
                    if (!shift && cp >= 'A' && cp <= 'Z') {
                        cp = Character.toLowerCase(cp);
                    } else if (shift && cp >= 'a' && cp <= 'z') {
                        cp = Character.toUpperCase(cp);
                    } else if (shift) {
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
                    searchBar.appendChar((char) cp);
                    lastKeyHandledCharNs = System.nanoTime();
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (System.nanoTime() - lastKeyHandledCharNs < CHAR_DEDUP_WINDOW_NS) {
            return true;
        }
        if (lastUi == null) return super.charTyped(input);

        int cp = input.codepoint();
        if (cp <= 0) return false;

        if (searchBar != null && searchBar.isFocused()) {
            searchBar.appendChar((char) cp);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (lastUi != null && scrollContainer != null) {
            scrollContainer.mouseReleased(lastUi, click.x(), click.y(), click.button());
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (lastUi != null && scrollContainer != null) {
            return scrollContainer.mouseDragged(lastUi, click.x(), click.y(), click.button(), offsetX, offsetY);
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (lastUi != null && scrollContainer != null) {
            return scrollContainer.mouseScrolled(lastUi, mouseX, mouseY, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ═════════════════════════════════════════════════════════════
    //  Fonttirivi
    // ═════════════════════════════════════════════════════════════

    private static class FontEntryComponent implements UiComponent {
        private final String fontName;
        private boolean selected;
        private final Runnable onClick;
        private Rect bounds;

        FontEntryComponent(String fontName, boolean selected, Runnable onClick) {
            this.fontName = fontName;
            this.selected = selected;
            this.onClick = onClick;
        }

        public String getFontName() { return fontName; }
        public void setSelected(boolean selected) { this.selected = selected; }

        @Override public Rect getBounds() { return bounds; }
        @Override public void setBounds(Rect bounds) { this.bounds = bounds; }
        @Override public int getPreferredHeight() { return 30; }

        @Override
        public void render(UiContext ui, int mouseX, int mouseY, float delta) {
            if (bounds == null) return;
            boolean hover = bounds.contains(mouseX, mouseY);

            int bgColor;
            if (selected) bgColor = ui.theme.buttonHover;
            else if (hover) bgColor = ui.theme.buttonHover;
            else bgColor = ui.theme.button;

            ui.fillRounded(bounds, bgColor, 6);

            if (selected) {
                ui.drawRoundedOutline(bounds, ui.theme.accent, 6, 2);
            } else if (hover) {
                ui.drawRoundedOutline(bounds, ui.theme.textDim, 6, 1);
            }

            int textY = bounds.y + bounds.h / 2 - ui.fontHeight() / 2 + 3;
            ui.text(fontName, bounds.x + 12, textY, ui.theme.text);

            if (selected && texOn != null) {
                int checkSize = 14;
                int checkX = bounds.x + bounds.w - checkSize - 10;
                int checkY = bounds.y + bounds.h / 2 - checkSize / 2;
                ui.addTexture(CHECKBOX_ON, checkX, checkY, checkSize, checkSize, new Color(0xFFFFFFFF));
            }
        }

        @Override
        public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT && bounds != null && bounds.contains(mouseX, mouseY)) {
                onClick.run();
                return true;
            }
            return false;
        }

        @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
        @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
        @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
        @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
        @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
    }
}