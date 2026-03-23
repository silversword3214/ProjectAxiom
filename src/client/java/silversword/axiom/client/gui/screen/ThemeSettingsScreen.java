package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.gui.core.ThemeManager;

public class ThemeSettingsScreen extends Screen {
    private final Screen parent;

    public ThemeSettingsScreen(Screen parent) {
        super(Component.literal("Theme Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 4;

        String currentTheme = ClickGuiConfigManager.getThemeName();

        // Teeman valinta -painike
        CycleButton<String> themeButton = CycleButton.builder(
                        (String value) -> Component.literal(value),
                        currentTheme
                )
                .withValues(ThemeManager.getThemeNames())
                .create(
                        centerX - 100, y, 200, 20,
                        Component.literal("Theme: "),
                        (button, value) -> {
                            ClickGuiConfigManager.setThemeName(value);
                        }
                );

        // Alpha-slider
        AbstractSliderButton alphaSlider = new AbstractSliderButton(centerX - 100, y + 40, 200, 20, Component.literal("Alpha: " + ClickGuiConfigManager.getGlobalAlpha() + "%"), ClickGuiConfigManager.getGlobalAlpha() / 100.0) {
            @Override
            protected void updateMessage() {
                int value = (int) Math.round(this.value * 100);
                this.setMessage(Component.literal("Alpha: " + value + "%"));
            }

            @Override
            protected void applyValue() {
                int value = (int) Math.round(this.value * 100);
                ClickGuiConfigManager.setGlobalAlpha(value);
            }
        };
        this.addRenderableWidget(alphaSlider);
        this.addRenderableWidget(themeButton);

        // Back-button – käyttää accent-väriä
        int accentColor = ThemeManager.getCurrentTheme().accent;
        this.addRenderableWidget(Button.builder(
                Component.literal("Back").withStyle(style -> style.withColor(accentColor)),
                button -> this.onClose()
        ).bounds(centerX - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}