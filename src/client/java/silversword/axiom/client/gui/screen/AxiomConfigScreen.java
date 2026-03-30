package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import silversword.axiom.client.gui.core.ThemeManager;

public class AxiomConfigScreen extends Screen {
    private final Screen parent;

    public AxiomConfigScreen(Screen parent) {
        super(Component.literal("Axiom Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 4;

        // Keybinds
        this.addRenderableWidget(Button.builder(
                        Component.literal("Keybinds"),
                        button -> {
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(new KeybindsScreen(this));
                            }
                        })
                .bounds(centerX - 50, y, 100, 20)
                .build());

        // ClickGui Settings
        this.addRenderableWidget(Button.builder(
                        Component.literal("ClickGui Settings"),
                        button -> {
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(new ClickGuiSettingsScreen(this));
                            }
                        })
                .bounds(centerX - 50, y + 30, 100, 20)
                .build());

        // Back – käyttää accent-väriä
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