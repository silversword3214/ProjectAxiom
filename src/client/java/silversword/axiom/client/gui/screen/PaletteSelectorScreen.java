package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.gui.components.ScrollContainer;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.rainbow.RainbowPalette;
import silversword.axiom.client.render.rendersystem.utils.color.rainbow.RainbowPalettes;


import java.util.ArrayList;
import java.util.List;

public class PaletteSelectorScreen extends Screen {
    private final Runnable onClose;
    private UiContext lastUi;
    private ScrollContainer scrollContainer;
    private List<PaletteCard> cards = new ArrayList<>();
    private int selectedIndex = 0;

    public PaletteSelectorScreen(Runnable onClose) {
        super(Component.literal("Select Rainbow Palette"));
        this.onClose = onClose;
    }

    @Override
    protected void init() {
        super.init();
        scrollContainer = new ScrollContainer();
        scrollContainer.setDrawBackground(false);
        scrollContainer.setInnerPadding(12);
        scrollContainer.setGap(12);

        RainbowPalette[] all = RainbowPalettes.ALL;
        cards.clear();
        for (int i = 0; i < all.length; i++) {
            PaletteCard card = new PaletteCard(all[i], i);
            cards.add(card);
            scrollContainer.add(card);
        }

        String currentName = ClickGuiConfigManager.getRainbowPalette().getName();
        for (int i = 0; i < all.length; i++) {
            if (all[i].getName().equals(currentName)) {
                selectedIndex = i;
                break;
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {}

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        Matrix4f proj = new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
        Renderer2D renderer = new Renderer2D(ctx, RenderAPI.getInstance().getCore(), proj);
        Theme theme = ThemeManager.getCurrentTheme();
        lastUi = new UiContext(minecraft, ctx, theme, delta, renderer);

        TextRenderer.get().begin(1.0, false, false);

        lastUi.fill(0, 0, width, height, 0xDD000000);

        String title = "Select Rainbow Palette";
        int titleW = lastUi.textWidth(title);
        lastUi.text(title, (width - titleW) / 2, 22, theme.text);

        int containerW = Math.min(560, width - 80);
        int containerX = (width - containerW) / 2;
        int containerY = 55;
        int containerH = height - containerY - 30;
        scrollContainer.setBounds(new Rect(containerX, containerY, containerW, containerH));
        scrollContainer.render(lastUi, mouseX, mouseY, delta);

        TextRenderer.get().end();
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
        if (lastUi != null && scrollContainer != null) {
            return scrollContainer.mouseClicked(lastUi, mx, my, click.button());
        }
        return super.mouseClicked(click, doubled);
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
    public boolean keyPressed(KeyEvent input) {
        if (input.input() == 256) {
            onClose();
            return true;
        }
        if (lastUi != null && scrollContainer != null) {
            return scrollContainer.keyPressed(lastUi, input.input(), input.scancode(), input.modifiers());
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (lastUi != null && scrollContainer != null && input.isAllowedChatCharacter()) {
            String s = input.codepointAsString();
            for (char c : s.toCharArray()) {
                if (scrollContainer.charTyped(lastUi, c, input.modifiers())) return true;
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

    private class PaletteCard implements UiComponent {
        private final RainbowPalette palette;
        private final int index;
        private Rect bounds;

        PaletteCard(RainbowPalette palette, int index) {
            this.palette = palette;
            this.index = index;
        }

        @Override
        public Rect getBounds() { return bounds; }
        @Override
        public void setBounds(Rect bounds) { this.bounds = bounds; }
        @Override
        public int getPreferredHeight() { return 90; }

        @Override
        public void render(UiContext ui, int mouseX, int mouseY, float delta) {
            if (bounds == null) return;
            boolean hover = bounds.contains(mouseX, mouseY);
            boolean selected = (index == selectedIndex);

            // varjo
            ui.fillRounded(new Rect(bounds.x + 2, bounds.y + 2, bounds.w, bounds.h), 0x44000000, ui.theme.radius);
            int bg = selected ? ui.theme.accent : (hover ? ui.theme.buttonHover : ui.theme.button);
            ui.fillRounded(bounds, bg, ui.theme.radius);

            if (selected) {
                ui.drawRoundedOutline(bounds, ui.theme.text, ui.theme.radius, 2);
            } else if (hover) {
                ui.drawRoundedOutline(bounds, ui.theme.textDim, ui.theme.radius, 1);
            }

            ui.text(palette.getName(), bounds.x + 12, bounds.y + 10, ui.theme.text);
            if (selected) {
                ui.text("✓", bounds.x + bounds.w - 22, bounds.y + 8, ui.theme.text);
            }

            int[] colors = palette.getColors();
            int barHeight = 24;
            int barY = bounds.y + bounds.h - barHeight - 8;
            int barWidth = bounds.w - 24;
            int step = barWidth / colors.length;
            for (int i = 0; i < colors.length; i++) {
                int x = bounds.x + 12 + i * step;
                int w = (i == colors.length - 1) ? barWidth - i * step : step;
                ui.fill(x, barY, w, barHeight, colors[i]);
            }
            // reunus palkille
            ui.drawRoundedOutline(new Rect(bounds.x + 12, barY, barWidth, barHeight), 0xAA000000, 4, 1);
        }

        @Override
        public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
            if (button == 0 && bounds != null && bounds.contains(mouseX, mouseY)) {
                selectedIndex = index;
                ClickGuiConfigManager.setRainbowPalette(palette);
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