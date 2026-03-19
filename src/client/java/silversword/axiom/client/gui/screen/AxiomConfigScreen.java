package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import silversword.axiom.client.gui.core.ThemeManager;

public class AxiomConfigScreen extends Screen {
    private final Screen parent;

    public AxiomConfigScreen(Screen parent) {
        super(Text.literal("Obsidian Client Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 4;

        // Keybinds
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Keybinds"),
                        button -> {
                            if (this.client != null) {
                                this.client.setScreen(new KeybindsScreen(this));
                            }
                        })
                .dimensions(centerX - 50, y, 100, 20)
                .build());

        // ClickGui Settings
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("ClickGui Settings"),
                        button -> {
                            if (this.client != null) {
                                this.client.setScreen(new ClickGuiSettingsScreen(this));
                            }
                        })
                .dimensions(centerX - 50, y + 30, 100, 20)
                .build());

        // Back – käyttää accent-väriä
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