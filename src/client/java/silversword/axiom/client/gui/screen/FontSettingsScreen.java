package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
import silversword.axiom.client.utils.render.DrawTexture;

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
    private static Texture texOff;

    private static final int ENTRY_HEIGHT = 40;
    private static final int SIDES_MARGIN = 20;

    private UiContext lastUi = null;

    public FontSettingsScreen(Runnable onCloseCallback) {
        super(Component.literal("Font Settings"));
        this.theme = ThemeManager.getCurrentTheme().copy();
        this.onCloseCallback = onCloseCallback;
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        // tyhjä
    }

    @Override
    protected void init() {
        super.init();
        allFonts = Fonts.getAvailableFonts();

        if (texOff == null) texOff = TextureManager.getTexture(CHECKBOX_OFF);

        int containerWidth = Math.min(500, this.width - SIDES_MARGIN * 2);
        int containerX = (this.width - containerWidth) / 2;
        int searchBarY = 45;
        int searchBarHeight = 18;
        int containerY = searchBarY + searchBarHeight + 8;
        int containerHeight = this.height - containerY - 20;

        // Hakupalkki
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
        scrollContainer.setGap(4);
        scrollContainer.setInnerPadding(6);

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
        } catch (Exception e) {
            // fonttia ei voitu alustaa, piirretään ilman tekstiä
        }

        if (textOk) {
            int titleY = 15;
            String title = "Font Settings";
            lastUi.text(title, (width - lastUi.textWidth(title)) / 2, titleY, theme.text);

            int backW = 60, backH = 18, backX = width - backW - 10, backY = 10;
            boolean backHover = mouseX >= backX && mouseX <= backX + backW && mouseY >= backY && mouseY <= backY + backH;
            lastUi.fillRounded(backX, backY, backW, backH, backHover ? theme.buttonHover : theme.button, theme.radius);
            lastUi.text("Back", backX + (backW - lastUi.textWidth("Back")) / 2, backY + 5, theme.text);

            // Hakupalkki
            searchBar.render(lastUi, mouseX, mouseY, delta);
            // Scroll container
            scrollContainer.render(lastUi, mouseX, mouseY, delta);
        }

        if (textOk) {
            TextRenderer.get().end();
        }
        RenderAPI.getInstance().getCore().flush();


        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();

        int backW = 60, backH = 18, backX = width - backW - 10, backY = 10;
        if (click.button() == 0 && mouseX >= backX && mouseX <= backX + backW && mouseY >= backY && mouseY <= backY + backH) {
            onClose();
            return true;
        }

        if (lastUi != null) {
            if (searchBar.mouseClicked(lastUi, mouseX, mouseY, click.button())) return true;
            if (scrollContainer != null && scrollContainer.mouseClicked(lastUi, mouseX, mouseY, click.button())) return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (lastUi != null) {
            if (searchBar.keyPressed(lastUi, input.input(), input.scancode(), input.modifiers())) return true;
            if (scrollContainer != null && scrollContainer.keyPressed(lastUi, input.input(), input.scancode(), input.modifiers())) return true;
        }
        if (input.input() == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (lastUi != null && input.isAllowedChatCharacter()) {
            String s = input.codepointAsString();
            for (int i = 0; i < s.length(); i++) {
                if (searchBar.charTyped(lastUi, s.charAt(i), input.modifiers())) return true;
            }
        }
        if (lastUi != null && scrollContainer != null && input.isAllowedChatCharacter()) {
            String s = input.codepointAsString();
            for (int i = 0; i < s.length(); i++) {
                if (scrollContainer.charTyped(lastUi, s.charAt(i), input.modifiers())) return true;
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
    public boolean isPauseScreen() {
        return false;
    }

    private static class FontEntryComponent implements UiComponent {
        // Vain päälle-kuvake (checkbox_on)
        private static final Identifier CHECKBOX_ON = Identifier.fromNamespaceAndPath("projectaxiom", "textures/icons/checkbox_on.png");
        private static Texture texOn;

        private final String fontName;
        private boolean selected;
        private final Runnable onClick;
        private Rect bounds;

        FontEntryComponent(String fontName, boolean selected, Runnable onClick) {
            this.fontName = fontName;
            this.selected = selected;
            this.onClick = onClick;
            if (texOn == null) texOn = TextureManager.getTexture(CHECKBOX_ON);
        }

        public String getFontName() { return fontName; }
        public void setSelected(boolean selected) { this.selected = selected; }

        @Override
        public Rect getBounds() { return bounds; }

        @Override
        public void setBounds(Rect bounds) { this.bounds = bounds; }

        @Override
        public int getPreferredHeight() { return 40; }

        @Override
        public void render(UiContext ui, int mouseX, int mouseY, float delta) {
            if (bounds == null) return;
            boolean hover = bounds.contains(mouseX, mouseY);
            int bgColor;
            if (selected) bgColor = ui.theme.accent;
            else if (hover) bgColor = ui.theme.buttonHover;
            else bgColor = ui.theme.button;

            ui.fillRounded(bounds, bgColor, 4);

            // Fontin nimi
            ui.text(fontName, bounds.x + 8, bounds.y + 6, ui.theme.text);

            // Piirrä checkbox vain jos valittu
            if (selected) {
                int checkSize = 12;
                int checkX = bounds.x + bounds.w - checkSize - 8;
                int checkY = bounds.y + 6;
                if (texOn != null) {
                    ui.addTexture(CHECKBOX_ON, checkX, checkY, checkSize, checkSize, 0f, new Color(0xFFFFFFFF));
                } else {
                    // Fallback tekstimerkki
                    ui.text("✓", checkX + 2, checkY - 1, ui.theme.text);
                }
            }

            // Esikatseluteksti
            String previewText = "The quick brown fox jumps over the lazy dog. 1234567890";
            int previewY = bounds.y + bounds.h - 6 - ui.fontHeight();
            ui.text(previewText, bounds.x + 8, previewY, ui.theme.textDim);
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