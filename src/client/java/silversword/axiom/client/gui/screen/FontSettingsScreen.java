package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import silversword.axiom.client.config.FontConfigManager;
import silversword.axiom.client.gui.components.ScrollContainer;
import silversword.axiom.client.gui.components.SearchBar;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.render.font.Fonts;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.texture.Texture;
import silversword.axiom.client.render.rendersystem.utils.texture.TextureManager;

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

    private static final Identifier CHECKBOX_OFF = Identifier.fromNamespaceAndPath("projectaxiom", "textures/icons/checkbox_off.png");
    private static final Identifier CHECKBOX_ON = Identifier.fromNamespaceAndPath("projectaxiom", "textures/icons/checkbox_on.png");
    private static Texture texOff, texOn;

    private UiContext lastUi = null;

    public FontSettingsScreen(Runnable onCloseCallback) {
        super(Component.literal("Font Settings"));
        this.theme = ThemeManager.getCurrentTheme().copy();
        this.onCloseCallback = onCloseCallback;
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {}

    @Override
    protected void init() {
        super.init();
        allFonts = Fonts.getAvailableFonts();

        if (texOff == null) texOff = TextureManager.getTexture(CHECKBOX_OFF);
        if (texOn == null) texOn = TextureManager.getTexture(CHECKBOX_ON);

        int containerWidth = Math.min(560, this.width - 60);
        int containerX = (this.width - containerWidth) / 2;
        int searchBarY = 55;
        int searchBarHeight = 22;
        int containerY = searchBarY + searchBarHeight + 10;
        int containerHeight = this.height - containerY - 30;

        searchBar = new SearchBar(
                () -> filterText,
                (newText) -> {
                    filterText = newText;
                    refreshFontList();
                }
        );
        searchBar.setBounds(new Rect(containerX, searchBarY, containerWidth, searchBarHeight));

        scrollContainer = new ScrollContainer();
        scrollContainer.setBounds(new Rect(containerX, containerY, containerWidth, containerHeight));
        scrollContainer.setDrawBackground(true);
        scrollContainer.setGap(6);
        scrollContainer.setInnerPadding(8);

        refreshFontList();
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
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        Matrix4f proj = new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
        Renderer2D renderer = new Renderer2D(ctx, RenderAPI.getInstance().getCore(), proj);
        lastUi = new UiContext(this.minecraft, ctx, theme, delta, renderer);

        boolean textOk = false;
        try {
            TextRenderer.get().begin(1.0, false, false);
            textOk = true;
        } catch (Exception e) {}

        if (textOk) {
            lastUi.fill(0, 0, width, height, 0xDD000000);

            String title = "Font Settings";
            lastUi.text(title, (width - lastUi.textWidth(title)) / 2, 22, theme.text);

            searchBar.render(lastUi, mouseX, mouseY, delta);
            scrollContainer.render(lastUi, mouseX, mouseY, delta);
        }

        if (textOk) TextRenderer.get().end();
        super.render(ctx, mouseX, mouseY, delta);
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        int btnW = 70, btnH = 24, btnX = width - btnW - 16, btnY = 12;
        Rect btnRect = new Rect(btnX, btnY, btnW, btnH);
        if (click.button() == 0 && btnRect.contains(mx, my)) {
            onClose();
            return true;
        }
        if (lastUi != null) {
            if (searchBar.mouseClicked(lastUi, mx, my, click.button())) return true;
            if (scrollContainer != null && scrollContainer.mouseClicked(lastUi, mx, my, click.button())) return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.input() == 256) {
            onClose();
            return true;
        }
        if (lastUi != null) {
            if (searchBar.keyPressed(lastUi, input.input(), input.scancode(), input.modifiers())) return true;
            if (scrollContainer != null && scrollContainer.keyPressed(lastUi, input.input(), input.scancode(), input.modifiers())) return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (lastUi != null && input.isAllowedChatCharacter()) {
            String s = input.codepointAsString();
            for (char c : s.toCharArray()) {
                if (searchBar.charTyped(lastUi, c, input.modifiers())) return true;
                if (scrollContainer != null && scrollContainer.charTyped(lastUi, c, input.modifiers())) return true;
            }
        }
        return super.charTyped(input);
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
        if (onCloseCallback != null) onCloseCallback.run();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

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

        @Override
        public Rect getBounds() { return bounds; }
        @Override
        public void setBounds(Rect bounds) { this.bounds = bounds; }
        @Override
        public int getPreferredHeight() { return 52; }

        @Override
        public void render(UiContext ui, int mouseX, int mouseY, float delta) {
            if (bounds == null) return;
            boolean hover = bounds.contains(mouseX, mouseY);
            int bgColor;
            if (selected) bgColor = ui.theme.accent;
            else if (hover) bgColor = ui.theme.buttonHover;
            else bgColor = ui.theme.button;

            ui.fillRounded(new Rect(bounds.x + 2, bounds.y + 2, bounds.w, bounds.h), 0x33000000, 6);
            ui.fillRounded(bounds, bgColor, 6);
            if (selected) {
                ui.drawRoundedOutline(bounds, ui.theme.text, 6, 2);
            } else if (hover) {
                ui.drawRoundedOutline(bounds, ui.theme.textDim, 6, 1);
            }

            ui.text(fontName, bounds.x + 12, bounds.y + 8, ui.theme.text);

            int checkSize = 16;
            int checkX = bounds.x + bounds.w - checkSize - 12;
            int checkY = bounds.y + 6;
            if (selected && texOn != null) {
                ui.addTexture(CHECKBOX_ON, checkX, checkY, checkSize, checkSize, new Color(0xFFFFFFFF));
            }

            String previewText = "The quick brown fox jumps over the lazy dog. 1234567890";
            int previewY = bounds.y + bounds.h - 14;
            ui.text(previewText, bounds.x + 12, previewY, ui.theme.textDim);
        }

        @Override
        public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
            if (button == 0 && bounds != null && bounds.contains(mouseX, mouseY)) {
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