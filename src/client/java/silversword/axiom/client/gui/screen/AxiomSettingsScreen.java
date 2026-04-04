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
        scroll.setInnerPadding(8);
        scroll.setGap(10);
        rebuild();
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        // tyhjä
    }

    private void rebuild() {
        scroll.clear();

        // HUD Edit -painike
        Button hudEditBtn = new Button("HUD Edit", () -> {
            if (minecraft != null) {
                minecraft.setScreen(new HudEditScreen());
            }
        });
        scroll.add(hudEditBtn);

        // Components -painike
        // Components -painike
        Button componentsBtn = new Button("HUD Components", () -> {
            // Tyhjennetään nykyiset napit ja ladataan tilalle lista + takaisin-nappi
            scroll.clear();
            scroll.add(new HudComponentsList());
            scroll.add(new Button("Back", this::rebuild)); // Palauttaa päävalikon
        });
        scroll.add(componentsBtn);



// Theme Settings -painike
        Button themeBtn = new Button("Theme Settings", () -> {
            if (minecraft != null) {
                minecraft.setScreen(new ThemePickerScreen(() -> minecraft.setScreen(this)));
            }
        });
        scroll.add(themeBtn);

        // Font Settings -painike
        Button fontBtn = new Button("Font Settings", () -> {
            if (minecraft != null) {
                minecraft.setScreen(new FontSettingsScreen(() -> minecraft.setScreen(this)));
            }
        });
        scroll.add(fontBtn);

        // Reset -painike (nollaa ikkunat)
        Button resetBtn = new Button("Reset Windows", () -> {
            ClickGuiScreen.getWindowManager().clear();
            // Tarvitaan uudelleenluonti – tallennetaan nykyinen screen ja avataan uusi ClickGuiScreen
            if (minecraft != null) {
                minecraft.setScreen(new ClickGuiScreen());
            }
        });
        scroll.add(resetBtn);

        // Rainbow wave -asetukset (valinnainen)
        Toggle rainbowToggle = new Toggle("Rainbow wave (modules)",
                ClickGuiConfigManager::isRainbowWaveEnabled,
                val -> ClickGuiConfigManager.setRainbowWaveEnabled(val));
        scroll.add(rainbowToggle);

        Slider speedSlider = new Slider("Wave speed", 0.1, 5.0, 0.05,
                () -> (double) ClickGuiConfigManager.getRainbowWaveSpeed(),
                val -> ClickGuiConfigManager.setRainbowWaveSpeed((float) val),
                val -> String.format("%.2f", val));
        scroll.add(speedSlider);

    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        Matrix4f proj = new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
        Renderer2D renderer = new Renderer2D(ctx, RenderAPI.getInstance().getCore(), proj);
        lastUi = new UiContext(minecraft, ctx, theme, delta, renderer);

        TextRenderer.get().begin(1.0, false, false);
        lastUi.fill(0, 0, width, height, 0xCC000000);

        String title = "Axiom Settings";
        int titleW = lastUi.textWidth(title);
        lastUi.text(title, (width - titleW) / 2, 20, theme.text);

        int containerW = Math.min(400, width - 80);
        int containerX = (width - containerW) / 2;
        int containerY = 50;
        int containerH = height - containerY - 30;
        scroll.setBounds(new Rect(containerX, containerY, containerW, containerH));
        scroll.render(lastUi, mouseX, mouseY, delta);

        TextRenderer.get().end();
        RenderAPI.getInstance().getCore().flush();
        super.render(ctx, mouseX, mouseY, delta);
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
}