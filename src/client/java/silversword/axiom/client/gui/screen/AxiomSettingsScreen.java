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

public class AxiomSettingsScreen extends Screen {
    private final Theme theme;
    private final Runnable onClose;
    private ScrollContainer scroll;
    private UiContext lastUi;

    public AxiomSettingsScreen(Runnable onClose) {
        super(Component.literal("Axiom Settings"));
        this.theme = ThemeManager.getCurrentTheme();
        this.onClose = onClose;
    }

    @Override
    protected void init() {
        super.init();
        scroll = new ScrollContainer();
        scroll.setDrawBackground(true);
        scroll.setInnerPadding(12);
        scroll.setGap(12);
        rebuild();
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {}

    private void rebuild() {
        scroll.clear();

        scroll.add(new Button("HUD Edit", () -> {
            if (minecraft != null) minecraft.setScreen(new HudEditScreen());
        }));

        scroll.add(new Button("HUD Components", () -> {
            scroll.clear();
            scroll.add(new HudComponentsList());
            scroll.add(new Button("Back to Main", this::rebuild));
        }));

        scroll.add(new Button("Theme Settings", () -> {
            if (minecraft != null) minecraft.setScreen(new ThemePickerScreen(() -> minecraft.setScreen(this)));
        }));

        scroll.add(new Button("Font Settings", () -> {
            if (minecraft != null) minecraft.setScreen(new FontSettingsScreen(() -> minecraft.setScreen(this)));
        }));

        // Etsi AxiomSettingsScreen.java:n rebuild() -metodista tämä kohta:
        scroll.add(new Button("Reset Windows", () -> {
            ClickGuiScreen gui = new ClickGuiScreen();
            gui.init(this.width, this.height);
            gui.resetWindows();

            if (minecraft != null) minecraft.setScreen(new ClickGuiScreen());
        }));

        scroll.add(new Toggle("Rainbow wave for modules",
                ClickGuiConfigManager::isRainbowWaveEnabled,
                val -> {
                    ClickGuiConfigManager.setRainbowWaveEnabled(val);
                    rebuild();
                }));

        if (ClickGuiConfigManager.isRainbowWaveEnabled()) {
            scroll.add(new Slider("Wave speed", 0.1, 5.0, 0.05,
                    () -> (double) ClickGuiConfigManager.getRainbowWaveSpeed(),
                    val -> ClickGuiConfigManager.setRainbowWaveSpeed((float) val),
                    val -> String.format("%.2f", val)));
        }
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        Matrix4f proj = new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
        Renderer2D renderer = new Renderer2D(ctx, RenderAPI.getInstance().getCore(), proj);
        lastUi = new UiContext(minecraft, ctx, theme, delta, renderer);

        TextRenderer.get().begin(1.0, false, false);
        lastUi.fill(0, 0, width, height, 0xDD000000);

        String title = "Settings";
        int titleW = lastUi.textWidth(title);
        lastUi.text(title, (width - titleW) / 2, 22, theme.text);

        int containerW = Math.min(440, width - 100);
        int containerX = (width - containerW) / 2;
        int containerY = 55;
        int containerH = height - containerY - 30;
        scroll.setBounds(new Rect(containerX, containerY, containerW, containerH));
        scroll.render(lastUi, mouseX, mouseY, delta);

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
}