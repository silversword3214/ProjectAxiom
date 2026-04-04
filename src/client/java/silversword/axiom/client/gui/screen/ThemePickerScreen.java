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
        this.theme = ThemeManager.getCurrentTheme().copy();
        this.onClose = onClose;
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {}

    @Override
    protected void init() {
        super.init();
        scroll = new ScrollContainer();
        scroll.setDrawBackground(true);
        scroll.setInnerPadding(10);
        scroll.setGap(8);
        rebuild();
    }

    private void rebuild() {
        scroll.clear();

        alphaSlider = new Slider("Opacity", 0, 100, 1,
                () -> (double) ClickGuiConfigManager.getGlobalAlpha(),
                val -> ClickGuiConfigManager.setGlobalAlpha((int) Math.round(val)),
                val -> (int) Math.round(val) + "%");
        scroll.add(alphaSlider);

        Button paletteButton = new Button("Rainbow Palettes", () -> {
            if (minecraft != null) {
                minecraft.setScreen(new PaletteSelectorScreen(() -> minecraft.setScreen(this)));
            }
        });
        scroll.add(paletteButton);

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
        lastUi.fill(0, 0, width, height, 0xDD000000);

        String title = "Theme Settings";
        int titleW = lastUi.textWidth(title);
        lastUi.text(title, (width - titleW) / 2, 22, theme.text);


        int leftWidth = Math.min(300, width - 360);
        int rightWidth = width - leftWidth - 70;
        int containerX = 35;
        int containerY = 55;
        int containerH = height - containerY - 30;

        scroll.setBounds(new Rect(containerX, containerY, leftWidth, containerH));
        scroll.render(lastUi, mouseX, mouseY, delta);

        int previewX = containerX + leftWidth + 25;
        int previewY = containerY;
        int previewW = rightWidth;
        int previewH = containerH;
        drawPreview(lastUi, previewX, previewY, previewW, previewH);

        TextRenderer.get().end();
        RenderAPI.getInstance().getCore().flush();
        super.render(ctx, mouseX, mouseY, delta);
    }


    private void drawPreview(UiContext ui, int x, int y, int w, int h) {
        Theme previewTheme = ThemeManager.getCurrentTheme();
        Rect panelRect = new Rect(x, y, w, h);
        ui.fillRounded(new Rect(x + 3, y + 3, w, h), 0x44000000, previewTheme.radius);
        ui.fillRounded(panelRect, previewTheme.panel, previewTheme.radius);
        ui.drawRoundedOutline(panelRect, previewTheme.border, previewTheme.radius, 1.5);

        int headerH = 28;
        Rect headerRect = new Rect(x + 2, y + 2, w - 4, headerH);
        ui.fillRoundedCustom(headerRect, previewTheme.header, previewTheme.radius, true, true, false, false);
        ui.text("Preview Window", x + 12, y + 9, previewTheme.text);

        int btnSize = 14;
        int closeX = x + w - btnSize - 10;
        int minX = closeX - btnSize - 6;
        Rect closeRect = new Rect(closeX, y + 7, btnSize, btnSize);
        Rect minRect = new Rect(minX, y + 7, btnSize, btnSize);
        ui.fillRounded(closeRect, previewTheme.button, 4);
        ui.fillRounded(minRect, previewTheme.button, 4);
        ui.text("X", closeX + 4, y + 9, previewTheme.textDim);
        ui.text("-", minX + 4, y + 9, previewTheme.textDim);

        int sepY = y + headerH + 6;
        ui.fill(x + 6, sepY, w - 12, 2, previewTheme.accent);

        int contentY = sepY + 12;
        int contentX = x + 16;
        int contentW = w - 32;

        Rect btnRect = new Rect(contentX, contentY, contentW, 26);
        ui.fillRounded(btnRect, previewTheme.button, 6);
        ui.text("Sample Button", contentX + 12, contentY + 8, previewTheme.text);

        int toggleY = contentY + 38;
        int toggleW = 44;
        int toggleH = 20;
        boolean toggleState = true;
        Rect toggleRect = new Rect(contentX, toggleY, toggleW, toggleH);
        ui.fillRounded(toggleRect, toggleState ? previewTheme.toggleOn : previewTheme.toggleOff, toggleH / 2);
        ui.fillCircle(contentX + (toggleState ? toggleW - 10 : 10), toggleY + toggleH / 2, 7, previewTheme.knob);
        ui.text("Toggle", contentX + toggleW + 12, toggleY + 5, previewTheme.text);

        int sliderY = toggleY + 38;
        int sliderW = contentW - 50;
        ui.fill(contentX, sliderY + 6, sliderW, 4, previewTheme.sliderTrack);
        ui.fill(contentX, sliderY + 6, (int)(sliderW * 0.65), 4, previewTheme.sliderFill);
        ui.fillCircle(contentX + (int)(sliderW * 0.65), sliderY + 8, 7, previewTheme.accent);
        ui.text("Slider", contentX + sliderW + 12, sliderY + 2, previewTheme.text);

        int textY = sliderY + 38;
        ui.text("Primary text", contentX, textY, previewTheme.text);
        ui.text("Dimmed secondary text", contentX, textY + 14, previewTheme.textDim);
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
        if (lastUi != null && scroll != null) {
            return scroll.mouseClicked(lastUi, mx, my, click.button());
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
            onClose();
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

    private class ThemeRow implements UiComponent {
        private final String themeName;
        private Rect bounds;
        ThemeRow(String themeName) { this.themeName = themeName; }
        @Override public Rect getBounds() { return bounds; }
        @Override public void setBounds(Rect bounds) { this.bounds = bounds; }
        @Override public int getPreferredHeight() { return 36; }
        @Override
        public void render(UiContext ui, int mouseX, int mouseY, float delta) {
            if (bounds == null) return;
            boolean hover = bounds.contains(mouseX, mouseY);
            boolean selected = themeName.equals(ClickGuiConfigManager.getThemeName());
            int bg = selected ? ui.theme.accent : (hover ? ui.theme.buttonHover : ui.theme.button);
            ui.fillRounded(new Rect(bounds.x + 2, bounds.y + 2, bounds.w, bounds.h), 0x33000000, ui.theme.radius);
            ui.fillRounded(bounds, bg, ui.theme.radius);
            if (selected) {
                ui.drawRoundedOutline(bounds, ui.theme.text, ui.theme.radius, 2);
            } else if (hover) {
                ui.drawRoundedOutline(bounds, ui.theme.textDim, ui.theme.radius, 1);
            }
            ui.text(themeName, bounds.x + 12, bounds.y + 11, ui.theme.text);
            Theme preview = ThemeManager.getTheme(themeName);
            int previewW = 60;
            int previewX = bounds.x + bounds.w - previewW - 12;
            int previewY = bounds.y + 6;
            ui.fillRounded(new Rect(previewX, previewY, previewW, bounds.h - 12), preview.accent, 4);
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