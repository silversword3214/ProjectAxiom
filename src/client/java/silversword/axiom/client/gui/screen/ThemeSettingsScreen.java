package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.gui.core.ThemeManager;

public class ThemeSettingsScreen extends Screen {
    private final Screen parent;

    public ThemeSettingsScreen(Screen parent) {
        super(Text.literal("Theme Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 4;

        String currentTheme = ClickGuiConfigManager.getThemeName();

        // Teeman valinta -painike
        CyclingButtonWidget<String> themeButton = CyclingButtonWidget.builder(
                        (String value) -> Text.literal(value),
                        currentTheme
                )
                .values(ThemeManager.getThemeNames())
                .build(
                        centerX - 100, y, 200, 20,
                        Text.literal("Theme: "),
                        (button, value) -> {
                            ClickGuiConfigManager.setThemeName(value);
                        }
                );

        // Alpha-slider
        SliderWidget alphaSlider = new SliderWidget(centerX - 100, y + 40, 200, 20, Text.literal("Alpha: " + ClickGuiConfigManager.getGlobalAlpha() + "%"), ClickGuiConfigManager.getGlobalAlpha() / 100.0) {
            @Override
            protected void updateMessage() {
                int value = (int) Math.round(this.value * 100);
                this.setMessage(Text.literal("Alpha: " + value + "%"));
            }

            @Override
            protected void applyValue() {
                int value = (int) Math.round(this.value * 100);
                ClickGuiConfigManager.setGlobalAlpha(value);
            }
        };
        this.addDrawableChild(alphaSlider);
        this.addDrawableChild(themeButton);

        // Back-button – käyttää accent-väriä
        int accentColor = ThemeManager.getCurrentTheme().accent;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Back").styled(style -> style.withColor(accentColor)),
                button -> this.close()
        ).dimensions(centerX - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}