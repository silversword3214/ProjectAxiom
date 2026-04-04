package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.gui.components.*;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;

public class ThemePickerScreen extends Screen {
    private final Theme theme;
    private final Runnable onClose;
    private ScrollContainer scroll;
    private UiContext lastUi;
    private Slider alphaSlider;

    public ThemePickerScreen(Runnable onClose) {
        super(Component.literal("Theme Settings"));
        this.theme = ThemeManager.getCurrentTheme();
        this.onClose = onClose;
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        // tyhjä
    }

    @Override
    protected void init() {
        super.init();
        scroll = new ScrollContainer();
        scroll.setDrawBackground(true);
        scroll.setInnerPadding(8);
        scroll.setGap(4);
        rebuild();
    }

    private void rebuild() {
        scroll.clear();

        // Alpha slider
        alphaSlider = new Slider("Alpha", 0, 100, 1,
                () -> (double) ClickGuiConfigManager.getGlobalAlpha(),
                val -> ClickGuiConfigManager.setGlobalAlpha((int) Math.round(val)),
                val -> (int) Math.round(val) + "%");
        scroll.add(alphaSlider);



        // Theme rows
        for (String themeName : ThemeManager.getThemeNames()) {
            ThemeRow row = new ThemeRow(themeName);
            scroll.add(row);
        }
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        Matrix4f proj = new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
        Renderer2D renderer = new Renderer2D(ctx, RenderAPI.getInstance().getCore(), proj);
        lastUi = new UiContext(minecraft, ctx, theme, delta, renderer);

        TextRenderer.get().begin(1.0, false, false);

        lastUi.fill(0, 0, width, height, 0xCC000000);

        String title = "Theme Settings";
        int titleW = lastUi.textWidth(title);
        lastUi.text(title, (width - titleW) / 2, 20, theme.text);

        // --- Asettelu: vasen sarake (lista) ja oikea sarake (esikatselu) ---
        int leftWidth = Math.min(280, width - 320); // vähintään 320px oikealle
        int rightWidth = width - leftWidth - 60;
        int containerX = 30;
        int containerY = 50;
        int containerH = height - containerY - 30;

        // Vasen: scroll container
        scroll.setBounds(new Rect(containerX, containerY, leftWidth, containerH));
        scroll.render(lastUi, mouseX, mouseY, delta);

        // Oikea: esikatselualue
        int previewX = containerX + leftWidth + 20;
        int previewY = containerY;
        int previewW = rightWidth;
        int previewH = containerH;
        drawPreview(lastUi, previewX, previewY, previewW, previewH);

        TextRenderer.get().end();
        RenderAPI.getInstance().getCore().flush();
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawPreview(UiContext ui, int x, int y, int w, int h) {
        // Haetaan käytössä oleva teema (joka näkyy asetuksissa)
        Theme previewTheme = ThemeManager.getCurrentTheme();

        // Esikatseluikkunan tausta
        ui.fillRounded(x, y, w, h, previewTheme.panel, previewTheme.radius);
        ui.drawRoundedOutline(new Rect(x, y, w, h), previewTheme.border, previewTheme.radius, 1.0);

        // Otsikkopalkki
        int headerH = 20;
        ui.fillRoundedCustom(new Rect(x + 2, y + 2, w - 4, headerH), previewTheme.header,
                previewTheme.radius, true, true, false, false);
        ui.text("Preview Window", x + 8, y + 6, previewTheme.text);

        // Sulje- ja pienennä-painikkeet (mock)
        int btnSize = 12;
        int closeX = x + w - btnSize - 6;
        int minX = closeX - btnSize - 4;
        ui.fillRounded(new Rect(closeX, y + 4, btnSize, btnSize), previewTheme.button, 3);
        ui.fillRounded(new Rect(minX, y + 4, btnSize, btnSize), previewTheme.button, 3);
        ui.text("x", closeX + 3, y + 6, previewTheme.textDim);
        ui.text("-", minX + 4, y + 6, previewTheme.textDim);

        // Erotinviiva
        int sepY = y + headerH + 4;
        ui.fill(x + 4, sepY, w - 8, 2, previewTheme.accent);

        // Sisältöalue
        int contentY = sepY + 8;
        int contentX = x + 12;
        int contentW = w - 24;

        // Esimerkkipainike
        int btnY = contentY;
        ui.fillRounded(contentX, btnY, contentW, 22, previewTheme.button, 4);
        ui.text("Button", contentX + 8, btnY + 6, previewTheme.text);

        // Toggle-kytkin
        int toggleY = btnY + 32;
        int toggleW = 40;
        int toggleH = 16;
        boolean toggleState = true;
        ui.fillRounded(contentX, toggleY, toggleW, toggleH, toggleState ? previewTheme.toggleOn : previewTheme.toggleOff, toggleH / 2);
        ui.fillCircle(contentX + (toggleState ? toggleW - 8 : 8), toggleY + toggleH / 2, 6, previewTheme.knob);
        ui.text("Toggle", contentX + toggleW + 8, toggleY + 4, previewTheme.text);

        // Liukusäädin
        int sliderY = toggleY + 32;
        int sliderW = contentW - 40;
        ui.fill(contentX, sliderY + 4, sliderW, 4, previewTheme.sliderTrack);
        ui.fill(contentX, sliderY + 4, (int)(sliderW * 0.6), 4, previewTheme.sliderFill);
        ui.fillCircle(contentX + (int)(sliderW * 0.6), sliderY + 6, 6, previewTheme.accent);
        ui.text("Slider", contentX + sliderW + 8, sliderY, previewTheme.text);

        // Tekstiesimerkki
        int textY = sliderY + 32;
        ui.text("Sample text in theme color", contentX, textY, previewTheme.text);
        ui.text("Dimmed text example", contentX, textY + 12, previewTheme.textDim);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (lastUi != null && scroll != null) {
            return scroll.mouseClicked(lastUi, click.x(), click.y(), click.button());
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (lastUi != null && scroll != null) {
            scroll.mouseReleased(lastUi, click.x(), click.y(), click.button());
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (lastUi != null && scroll != null) {
            return scroll.mouseDragged(lastUi, click.x(), click.y(), click.button(), offsetX, offsetY);
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (lastUi != null && scroll != null) {
            return scroll.mouseScrolled(lastUi, mouseX, mouseY, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.input() == 256) {
            if (onClose != null) onClose.run();
            else onClose();
            return true;
        }
        if (lastUi != null && scroll != null) {
            return scroll.keyPressed(lastUi, input.input(), input.scancode(), input.modifiers());
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (lastUi != null && scroll != null && input.isAllowedChatCharacter()) {
            String s = input.codepointAsString();
            for (char c : s.toCharArray()) {
                if (scroll.charTyped(lastUi, c, input.modifiers())) return true;
            }
        }
        return super.charTyped(input);
    }

    @Override
    public void onClose() {
        if (onClose != null) onClose.run();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // Sisäinen teemarivi (ennallaan)
    private class ThemeRow implements UiComponent {
        private final String themeName;
        private Rect bounds;
        ThemeRow(String themeName) { this.themeName = themeName; }
        @Override public Rect getBounds() { return bounds; }
        @Override public void setBounds(Rect bounds) { this.bounds = bounds; }
        @Override public int getPreferredHeight() { return 28; }
        @Override
        public void render(UiContext ui, int mouseX, int mouseY, float delta) {
            if (bounds == null) return;
            boolean hover = bounds.contains(mouseX, mouseY);
            boolean selected = themeName.equals(ClickGuiConfigManager.getThemeName());
            int bg = selected ? ui.theme.accent : (hover ? ui.theme.buttonHover : ui.theme.button);
            ui.fillRounded(bounds, bg, ui.theme.radius);
            ui.text(themeName, bounds.x + 10, bounds.y + 8, ui.theme.text);
            // Esikatselupalkki
            Theme preview = ThemeManager.getTheme(themeName);
            int previewW = 50;
            int previewX = bounds.x + bounds.w - previewW - 10;
            int previewY = bounds.y + 5;
            ui.fill(previewX, previewY, previewW, bounds.h - 10, preview.accent);
        }
        @Override
        public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
            if (button == 0 && bounds != null && bounds.contains(mouseX, mouseY)) {
                ClickGuiConfigManager.setThemeName(themeName);
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